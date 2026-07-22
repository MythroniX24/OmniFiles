package com.omnilabs.omfiles.search

import android.os.Environment
import com.omnilabs.omfiles.domain.model.FileInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parallel filesystem search engine.
 *
 * No database indexing — roots are scanned concurrently using NIO.
 * Early filtering (name matching) happens before any attribute read,
 * and the scan cooperatively cancels as soon as the query changes.
 */
@Singleton
class SearchEngine @Inject constructor() {

    companion object {
        private const val MAX_RESULTS = 200
        private const val MAX_DEPTH = 4
        private const val MIN_QUERY_LENGTH = 1
        private const val MAX_PER_ROOT = 100

        private val rawSearchRoots: List<String> by lazy {
            val root = Environment.getExternalStorageDirectory().absolutePath
            listOf(
                "$root/Download",
                "$root/Documents",
                "$root/DCIM",
                "$root/Pictures",
                "$root/Music",
                "$root/Movies",
                root
            )
        }

        /** Keep only roots that are not children of another root in the list. */
        private fun deduplicatedRoots(): List<String> {
            val all = rawSearchRoots
            return all.filter { candidate ->
                all.none { other ->
                    other != candidate && candidate.startsWith(other + "/")
                }
            }
        }
    }

    /**
     * Search for files matching the query across common directories in parallel.
     */
    fun search(query: String): Flow<List<FileInfo>> = flow {
        val q = query.trim().lowercase()
        if (q.length < MIN_QUERY_LENGTH) {
            emit(emptyList())
            return@flow
        }

        val results = ConcurrentLinkedQueue<FileInfo>()
        val seen = ConcurrentHashMap.newKeySet<String>()
        val resultCount = AtomicInteger(0)
        val roots = deduplicatedRoots()

        coroutineScope {
            roots.map { root ->
                async(Dispatchers.IO) {
                    scanRoot(Paths.get(root), q, results, seen, resultCount)
                }
            }.awaitAll()
        }

        // Sort: exact match first, then prefix, then contains
        val sorted = results.toList().sortedWith(
            compareBy<FileInfo> {
                when {
                    it.name.equals(q, ignoreCase = true) -> 0
                    it.name.lowercase().startsWith(q) -> 1
                    else -> 2
                }
            }.thenBy { it.name.lowercase() }
        )

        emit(sorted)
    }.flowOn(Dispatchers.IO)

    private suspend fun scanRoot(
        rootPath: Path,
        q: String,
        results: ConcurrentLinkedQueue<FileInfo>,
        seen: MutableSet<String>,
        resultCount: AtomicInteger
    ) {
        if (!Files.isDirectory(rootPath)) return

        try {
            Files.walk(rootPath, MAX_DEPTH).use { stream ->
                for (filePath in stream) {
                    // Cooperative cancellation: stop immediately when the query changes
                    if (!coroutineContext.isActive) break
                    if (resultCount.get() >= MAX_RESULTS) break

                    // Filter by name before reading any filesystem attributes
                    val name = filePath.fileName?.toString() ?: continue
                    if (name.startsWith('.')) continue
                    if (!name.lowercase().contains(q)) continue
                    if (!seen.add(filePath.toString())) continue

                    val info = createFileInfo(filePath) ?: continue
                    results.add(info)
                    resultCount.incrementAndGet()
                }
            }
        } catch (_: Exception) {
            // Skip directories we can't access
        }
    }

    private fun createFileInfo(filePath: Path): FileInfo? {
        return try {
            val attrs = Files.readAttributes(filePath, java.nio.file.attribute.BasicFileAttributes::class.java)
            val name = filePath.fileName.toString()
            val dotIndex = name.lastIndexOf('.')
            FileInfo(
                path = filePath.toAbsolutePath().toString(),
                name = name,
                extension = if (attrs.isRegularFile && dotIndex > 0) name.substring(dotIndex + 1) else "",
                isDirectory = attrs.isDirectory,
                isHidden = name.startsWith('.'),
                size = if (attrs.isRegularFile) attrs.size() else 0L,
                lastModified = attrs.lastModifiedTime().toMillis(),
                parentPath = filePath.parent?.toAbsolutePath()?.toString() ?: "",
                isSymbolicLink = attrs.isSymbolicLink
            )
        } catch (_: Exception) {
            null
        }
    }
}
