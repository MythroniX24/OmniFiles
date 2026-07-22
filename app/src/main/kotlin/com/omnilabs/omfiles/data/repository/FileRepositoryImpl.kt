package com.omnilabs.omfiles.data.repository

import android.os.Environment
import android.os.StatFs
import com.omnilabs.omfiles.domain.model.FileInfo
import com.omnilabs.omfiles.domain.model.FileSortOptions
import com.omnilabs.omfiles.domain.model.OperationResult
import com.omnilabs.omfiles.domain.model.SortMode
import com.omnilabs.omfiles.domain.model.SortOrder
import com.omnilabs.omfiles.domain.model.StorageInfo
import com.omnilabs.omfiles.domain.model.StorageType
import com.omnilabs.omfiles.domain.repository.FileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Optimized file repository using NIO DirectoryStream for lazy directory traversal,
 * Files.readAttributes for batch attribute reads, and an LRU cache for recently
 * viewed directories to eliminate redundant filesystem scans.
 */
@Singleton
class FileRepositoryImpl @Inject constructor() : FileRepository {

    // ── LRU Directory Cache ───────────────────────────────────────────────
    // Cache recently listed directories so back-navigation is instant.
    // Key = "path|showHidden|sortMode|sortOrder|foldersFirst"
    private val dirCache = Collections.synchronizedMap(
        object : LinkedHashMap<String, CachedDirEntry>(MAX_CACHE_ENTRIES, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedDirEntry>?): Boolean =
                size > MAX_CACHE_ENTRIES
        }
    )

    private data class CachedDirEntry(
        val files: List<FileInfo>,
        val timestamp: Long
    )

    companion object {
        private const val MAX_CACHE_ENTRIES = 32          // Max cached directories
        private const val CACHE_TTL_MS = 5_000L            // 5 seconds cache TTL
        private const val DIRECTORY_STREAM_THRESHOLD = 500 // Use NIO DirectoryStream for dirs > this size

        private fun cacheKey(path: String, opts: FileSortOptions, showHidden: Boolean): String =
            "$path|$showHidden|${opts.mode}|${opts.order}|${opts.foldersFirst}"
    }

    // ── Core: Get Files (optimized) ───────────────────────────────────────

    override fun getFiles(
        path: String,
        sortOptions: FileSortOptions,
        showHidden: Boolean
    ): Flow<List<FileInfo>> = flow {
        val key = cacheKey(path, sortOptions, showHidden)
        val now = System.currentTimeMillis()

        // Check cache first
        val cached = dirCache[key]
        if (cached != null && (now - cached.timestamp) < CACHE_TTL_MS) {
            emit(cached.files)
            return@flow
        }

        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) {
            emit(emptyList())
            return@flow
        }

        val fileList = listDirectoryFast(dir, showHidden)
        val sorted = fileList.sortedWith(fileComparator(sortOptions))

        // Update cache
        dirCache[key] = CachedDirEntry(sorted, now)

        emit(sorted)
    }.flowOn(Dispatchers.IO)

    /**
     * Fast directory listing using NIO DirectoryStream for large directories
     * (avoids allocating huge File[] arrays) and Files.readAttributes for
     * single-syscall metadata retrieval.
     *
     * For small directories (< threshold), falls back to the standard
     * File.listFiles() since it has less overhead.
     */
    private fun listDirectoryFast(dir: File, showHidden: Boolean): List<FileInfo> {
        // Quick size check: count entries to decide strategy
        val estimatedSize = try {
            var count = 0
            Files.newDirectoryStream(dir.toPath()).use { stream ->
                val iter = stream.iterator()
                while (iter.hasNext() && count < DIRECTORY_STREAM_THRESHOLD) {
                    iter.next()
                    count++
                }
            }
            count
        } catch (_: Exception) { 0 }

        // For small directories, use the simpler File-based approach (less overhead)
        if (estimatedSize < DIRECTORY_STREAM_THRESHOLD) {
            return listDirectorySimple(dir, showHidden)
        }

        // For large directories, use NIO DirectoryStream + readAttributes
        return listDirectoryNio(dir, showHidden)
    }

    /**
     * Simple file listing using File.listFiles() for small directories.
     * This is faster than NIO for small directories due to lower overhead.
     */
    private fun listDirectorySimple(dir: File, showHidden: Boolean): List<FileInfo> {
        return dir.listFiles()?.toList()?.let { files ->
            val results = java.util.ArrayList<FileInfo>(files.size)
            for (file in files) {
                if (!showHidden && file.isHidden) continue
                results.add(FileInfo.fromFile(file))
            }
            results
        } ?: emptyList()
    }

    /**
     * NIO-based directory listing optimized for large directories.
     * Uses DirectoryStream (lazy iteration, no huge array allocation) and
     * Files.readAttributes (single syscall per file instead of 5+).
     */
    private fun listDirectoryNio(dir: File, showHidden: Boolean): List<FileInfo> {
        val results = mutableListOf<FileInfo>()
        try {
            Files.newDirectoryStream(dir.toPath()).use { stream ->
                for (path in stream) {
                    if (!showHidden && path.fileName.toString().startsWith('.')) continue
                    val info = fileInfoFromPath(path, dir.absolutePath)
                    if (info != null) results.add(info)
                }
            }
        } catch (_: Exception) { }
        return results
    }

    /**
     * Extract FileInfo from a Path using a single Files.readAttributes call.
     */
    private fun fileInfoFromPath(
        path: java.nio.file.Path,
        parentPath: String
    ): FileInfo? {
        return try {
            val attrs = Files.readAttributes(path, BasicFileAttributes::class.java)
            val name = path.fileName.toString()
            val dotIndex = name.lastIndexOf('.')
            val isDir = attrs.isDirectory
            FileInfo(
                path = path.toString(),
                name = name,
                nameLowercase = name.lowercase(),
                extension = if (dotIndex > 0) name.substring(dotIndex + 1) else "",
                extensionLowercase = if (dotIndex > 0) name.substring(dotIndex + 1).lowercase() else "",
                isDirectory = isDir,
                isHidden = name.startsWith('.'),
                size = if (attrs.isRegularFile) attrs.size() else 0L,
                lastModified = attrs.lastModifiedTime().toMillis(),
                parentPath = parentPath,
                isSymbolicLink = attrs.isSymbolicLink,
                itemCount = -1 // lazy: computed on demand
            )
        } catch (_: Exception) { null }
    }

    /**
     * Optimized comparator using pre-computed lowercase fields to
     * avoid repeated String.lowercase() calls during sorting.
     */
    private fun fileComparator(options: FileSortOptions): Comparator<FileInfo> {
        val primaryComparator: Comparator<FileInfo> = when (options.mode) {
            SortMode.NAME -> compareBy { it.nameLowercase }
            SortMode.DATE -> compareByDescending { it.lastModified }
            SortMode.SIZE -> compareBy { it.size }
            SortMode.EXTENSION -> compareBy { it.extensionLowercase }
        }

        val directionComparator = if (options.order == SortOrder.DESCENDING) {
            when (options.mode) {
                SortMode.NAME -> compareByDescending<FileInfo> { it.nameLowercase }
                SortMode.DATE -> compareBy<FileInfo> { it.lastModified }
                SortMode.SIZE -> compareByDescending<FileInfo> { it.size }
                SortMode.EXTENSION -> compareByDescending<FileInfo> { it.extensionLowercase }
            }
        } else {
            primaryComparator
        }

        return if (options.foldersFirst) {
            compareByDescending<FileInfo> { it.isDirectory }.then(directionComparator)
        } else {
            compareBy<FileInfo> { it.isDirectory }.then(directionComparator)
        }
    }

    // ── Rest of interface methods remain the same ──────────────────────────

    override suspend fun getFileInfo(path: String): FileInfo? = withContext(Dispatchers.IO) {
        val file = File(path)
        if (file.exists()) FileInfo.fromFile(file) else null
    }

    override suspend fun getStorageVolumes(): List<StorageInfo> = withContext(Dispatchers.IO) {
        val volumes = mutableListOf<StorageInfo>()

        // Internal storage
        val internalPath = Environment.getExternalStorageDirectory().absolutePath
        val internalStat = StatFs(internalPath)
        val internalTotal = internalStat.totalBytes.takeIf { it > 0 } ?: 0L
        val internalFree = internalStat.availableBytes.takeIf { it >= 0 } ?: 0L
        volumes.add(
            StorageInfo(
                path = internalPath,
                label = "Internal Storage",
                type = StorageType.INTERNAL,
                totalSpace = internalTotal,
                freeSpace = internalFree,
                usedSpace = internalTotal - internalFree,
                isAvailable = true,
                isPrimary = true
            )
        )

        // External SD cards
        try {
            val externalFilesDirs = Environment.getExternalStorageDirectory().parentFile?.listFiles()
            externalFilesDirs?.forEach { dir ->
                if (dir.absolutePath != internalPath && dir.isDirectory && dir.canRead()) {
                    try {
                        val stat = StatFs(dir.absolutePath)
                        val total = stat.totalBytes
                        val free = stat.availableBytes
                        if (total > 0) {
                            volumes.add(
                                StorageInfo(
                                    path = dir.absolutePath,
                                    label = "SD Card",
                                    type = StorageType.SD_CARD,
                                    totalSpace = total,
                                    freeSpace = free,
                                    usedSpace = total - free,
                                    isAvailable = true,
                                    isPrimary = false
                                )
                            )
                        }
                    } catch (_: Exception) { }
                }
            }
        } catch (_: Exception) { }

        volumes
    }

    override suspend fun copyFile(source: String, destination: String): OperationResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val sourceFile = File(source)
                val destFile = File(destination)

                if (!sourceFile.exists()) {
                    return@withContext OperationResult.Error("Source file does not exist")
                }

                if (sourceFile.isDirectory) {
                    copyDirectory(sourceFile, destFile)
                } else {
                    copyFileChannel(sourceFile, destFile)
                }

                OperationResult.Success(Unit)
            } catch (e: Exception) {
                OperationResult.Error("Copy failed: ${e.message}", e)
            }
        }

    override suspend fun moveFile(source: String, destination: String): OperationResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val sourceFile = File(source)
                val destFile = File(destination)

                if (!sourceFile.exists()) {
                    return@withContext OperationResult.Error("Source file does not exist")
                }

                if (sourceFile.renameTo(destFile)) {
                    OperationResult.Success(Unit)
                } else {
                    val copyResult = copyFile(source, destination)
                    if (copyResult is OperationResult.Success) {
                        sourceFile.deleteRecursively()
                        OperationResult.Success(Unit)
                    } else {
                        copyResult
                    }
                }
            } catch (e: Exception) {
                OperationResult.Error("Move failed: ${e.message}", e)
            }
        }

    override suspend fun deleteFile(path: String): OperationResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val file = File(path)
                if (!file.exists()) {
                    return@withContext OperationResult.Error("File does not exist")
                }
                val deleted = if (file.isDirectory) {
                    file.deleteRecursively()
                } else {
                    file.delete()
                }
                if (deleted) {
                    invalidateCache()
                    OperationResult.Success(Unit)
                } else {
                    OperationResult.Error("Failed to delete file")
                }
            } catch (e: Exception) {
                OperationResult.Error("Delete failed: ${e.message}", e)
            }
        }

    override suspend fun renameFile(path: String, newName: String): OperationResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val file = File(path)
                val parent = file.parentFile
                    ?: return@withContext OperationResult.Error("No parent directory")
                val newFile = File(parent, newName)

                if (newFile.exists()) {
                    return@withContext OperationResult.Error("A file with this name already exists")
                }

                if (file.renameTo(newFile)) {
                    invalidateCache()
                    OperationResult.Success(Unit)
                } else {
                    OperationResult.Error("Failed to rename file")
                }
            } catch (e: Exception) {
                OperationResult.Error("Rename failed: ${e.message}", e)
            }
        }

    override suspend fun createFolder(parentPath: String, folderName: String): OperationResult<FileInfo> =
        withContext(Dispatchers.IO) {
            try {
                val folder = File(parentPath, folderName)
                if (folder.exists()) {
                    return@withContext OperationResult.Error("Folder already exists")
                }
                if (folder.mkdirs()) {
                    invalidateCache()
                    OperationResult.Success(FileInfo.fromFile(folder))
                } else {
                    OperationResult.Error("Failed to create folder")
                }
            } catch (e: Exception) {
                OperationResult.Error("Create folder failed: ${e.message}", e)
            }
        }

    override suspend fun createFile(parentPath: String, fileName: String): OperationResult<FileInfo> =
        withContext(Dispatchers.IO) {
            try {
                val file = File(parentPath, fileName)
                if (file.exists()) {
                    return@withContext OperationResult.Error("File already exists")
                }
                if (file.createNewFile()) {
                    invalidateCache()
                    OperationResult.Success(FileInfo.fromFile(file))
                } else {
                    OperationResult.Error("Failed to create file")
                }
            } catch (e: Exception) {
                OperationResult.Error("Create file failed: ${e.message}", e)
            }
        }

    override suspend fun getFileSize(path: String): Long = withContext(Dispatchers.IO) {
        try {
            val p = Paths.get(path)
            val attrs = Files.readAttributes(p, BasicFileAttributes::class.java)
            if (attrs.isDirectory) {
                var total = 0L
                Files.walk(p).use { walk ->
                    for (f in walk) {
                        try {
                            val fa = Files.readAttributes(f, BasicFileAttributes::class.java)
                            if (fa.isRegularFile) total += fa.size()
                        } catch (_: Exception) { }
                    }
                }
                total
            } else {
                attrs.size()
            }
        } catch (_: Exception) { 0L }
    }

    override suspend fun getFileCount(path: String): Int = withContext(Dispatchers.IO) {
        try {
            var count = 0
            Files.newDirectoryStream(Paths.get(path)).use { stream ->
                val iter = stream.iterator()
                while (iter.hasNext()) { iter.next(); count++ }
            }
            count
        } catch (_: Exception) { 0 }
    }

    override suspend fun exists(path: String): Boolean = withContext(Dispatchers.IO) {
        File(path).exists()
    }

    override suspend fun getStorageUsage(path: String): Pair<Long, Long> =
        withContext(Dispatchers.IO) {
            try {
                val stat = StatFs(path)
                val total = stat.totalBytes
                val free = stat.availableBytes
                Pair(total - free, total)
            } catch (e: Exception) {
                Pair(0L, 0L)
            }
        }

    // ── Cache helpers ─────────────────────────────────────────────────────

    fun invalidateCache() {
        dirCache.clear()
    }

    fun invalidateCacheForPath(path: String) {
        val keysToRemove = dirCache.keys.filter { it.startsWith("$path|") }
        keysToRemove.forEach { dirCache.remove(it) }
    }

    // ── File I/O helpers ──────────────────────────────────────────────────

    private fun copyFileChannel(source: File, dest: File) {
        dest.parentFile?.mkdirs()
        FileInputStream(source).use { input ->
            FileOutputStream(dest).use { output ->
                input.channel.transferTo(0, source.length(), output.channel)
            }
        }
    }

    private fun copyDirectory(source: File, dest: File) {
        dest.mkdirs()
        source.listFiles()?.forEach { child ->
            val destChild = File(dest, child.name)
            if (child.isDirectory) {
                copyDirectory(child, destChild)
            } else {
                copyFileChannel(child, destChild)
            }
        }
    }
}
