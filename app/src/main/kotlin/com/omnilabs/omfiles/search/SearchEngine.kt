package com.omnilabs.omfiles.search

import android.os.Environment
import com.omnilabs.omfiles.domain.model.FileInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simple, direct filesystem search engine.
 * No database indexing — just walks the filesystem using NIO when you type.
 * Results appear in 1-3 seconds for common directories.
 */
@Singleton
class SearchEngine @Inject constructor() {

    companion object {
        private const val MAX_RESULTS = 200
        private const val MAX_DEPTH = 4
        private const val MIN_QUERY_LENGTH = 1

        // Common directories searched in order of relevance
        private val SEARCH_ROOTS = listOf(
            "/storage/emulated/0/Download",
            "/storage/emulated/0/Documents",
            "/storage/emulated/0/DCIM",
            "/storage/emulated/0/Pictures",
            "/storage/emulated/0/Music",
            "/storage/emulated/0/Movies",
            "/storage/emulated/0"
        )
    }

    /**
     * Search for files matching the query across common directories.
     * Uses direct NIO filesystem walking — no pre-indexing required.
     */
    fun search(query: String): Flow<List<FileInfo>> = flow {
        val q = query.trim().lowercase()
        if (q.length < MIN_QUERY_LENGTH) {
            emit(emptyList())
            return@flow
        }

        val results = mutableListOf<FileInfo>()
        val seen = mutableSetOf<String>()

        for (root in SEARCH_ROOTS) {
            if (results.size >= MAX_RESULTS) break

            val rootPath = Paths.get(root)
            if (!Files.isDirectory(rootPath)) continue

            try {
                Files.walk(rootPath, MAX_DEPTH).use { stream ->
                    var dirCount = 0
                    for (filePath in stream) {
                        if (results.size >= MAX_RESULTS) break

                        // Limit per-directory scanning
                        if (filePath != rootPath) {
                            dirCount++
                            if (dirCount > 2000) break
                        }

                        val name = filePath.fileName.toString()
                        if (name.startsWith('.')) continue
                        if (!name.lowercase().contains(q)) continue
                        if (!seen.add(filePath.toString())) continue

                        try {
                            val attrs = Files.readAttributes(filePath, BasicFileAttributes::class.java)
                            val dotIndex = name.lastIndexOf('.')
                            results.add(
                                FileInfo(
                                    path = filePath.toAbsolutePath().toString(),
                                    name = name,
                                    extension = if (attrs.isRegularFile && dotIndex > 0)
                                        name.substring(dotIndex + 1) else "",
                                    isDirectory = attrs.isDirectory(),
                                    isHidden = name.startsWith('.'),
                                    size = if (attrs.isRegularFile) attrs.size() else 0L,
                                    lastModified = attrs.lastModifiedTime().toMillis(),
                                    parentPath = filePath.parent?.toAbsolutePath()?.toString() ?: "",
                                    isSymbolicLink = attrs.isSymbolicLink
                                )
                            )
                        } catch (_: Exception) {
                            // Skip files we can't read
                        }
                    }
                }
            } catch (_: Exception) {
                // Skip directories we can't access
            }
        }

        // Sort: exact match first, then name starts with query, then contains
        results.sortWith(compareBy<FileInfo> {
            when {
                it.name.equals(q, ignoreCase = true) -> 0
                it.name.lowercase().startsWith(q) -> 1
                else -> 2
            }
        }.thenBy { it.name.lowercase() })

        emit(results)
    }.flowOn(Dispatchers.IO)
}
