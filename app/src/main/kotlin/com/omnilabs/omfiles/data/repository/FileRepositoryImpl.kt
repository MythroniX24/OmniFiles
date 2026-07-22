package com.omnilabs.omfiles.data.repository

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import com.omnilabs.omfiles.domain.model.FileInfo
import com.omnilabs.omfiles.domain.model.FileSortOptions
import com.omnilabs.omfiles.domain.model.OperationResult
import com.omnilabs.omfiles.domain.model.SortMode
import com.omnilabs.omfiles.domain.model.SortOrder
import com.omnilabs.omfiles.domain.model.StorageInfo
import com.omnilabs.omfiles.domain.model.StorageType
import com.omnilabs.omfiles.domain.repository.FileRepository
import com.omnilabs.omfiles.domain.storage.StorageEngineProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

/**
 * File repository that delegates file I/O to the [StorageEngineProvider],
 * keeping an LRU cache for directory listings and storage volume detection.
 */
@Singleton
class FileRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val engineProvider: StorageEngineProvider
) : FileRepository {

    // ── LRU Directory Cache ───────────────────────────────────────────────
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
        private const val MAX_CACHE_ENTRIES = 32
        private const val CACHE_TTL_MS = 30_000L

        private fun cacheKey(path: String, opts: FileSortOptions, showHidden: Boolean): String =
            "$path|$showHidden|${opts.mode}|${opts.order}|${opts.foldersFirst}"
    }

    // ── Core: Get Files ────────────────────────────────────────────────────

    override fun getFiles(
        path: String,
        sortOptions: FileSortOptions,
        showHidden: Boolean
    ): Flow<List<FileInfo>> = flow {
        val key = cacheKey(path, sortOptions, showHidden)
        val now = System.currentTimeMillis()

        val cached = dirCache[key]
        if (cached != null && (now - cached.timestamp) < CACHE_TTL_MS) {
            emit(cached.files)
            return@flow
        }

        val engine = engineProvider.getEngineForPath(path)
        val fileList = engine.getFiles(path, showHidden)
        val sorted = fileList.sortedWith(fileComparator(sortOptions))

        dirCache[key] = CachedDirEntry(sorted, now)
        emit(sorted)
    }.flowOn(Dispatchers.IO)

    // ── File metadata and storage ─────────────────────────────────────────

    override suspend fun getFileInfo(path: String): FileInfo? = withContext(Dispatchers.IO) {
        engineProvider.getEngineForPath(path).getFileInfo(path)
    }

    override suspend fun getStorageVolumes(): List<StorageInfo> = withContext(Dispatchers.IO) {
        val volumes = mutableListOf<StorageInfo>()

        try {
            val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
            val storageVolumes = storageManager?.storageVolumes ?: emptyList()

            storageVolumes.forEach { volume ->
                try {
                    val dir = volume.directory
                    if (dir != null && dir.exists()) {
                        val stat = StatFs(dir.absolutePath)
                        val total = stat.totalBytes
                        val free = stat.availableBytes
                        val pathLower = dir.absolutePath.lowercase()
                        val isUsb = volume.isRemovable && (pathLower.contains("usb") || pathLower.contains("otg"))
                        val type = when {
                            volume.isPrimary -> StorageType.INTERNAL
                            isUsb -> StorageType.USB_OTG
                            volume.isRemovable -> StorageType.SD_CARD
                            else -> StorageType.SD_CARD
                        }
                        val label = when {
                            volume.isPrimary -> "Internal Storage"
                            isUsb -> "USB OTG"
                            volume.isRemovable -> "SD Card"
                            else -> "SD Card"
                        }
                        volumes.add(
                            StorageInfo(
                                path = dir.absolutePath,
                                label = label,
                                type = type,
                                totalSpace = total,
                                freeSpace = free,
                                usedSpace = total - free,
                                isAvailable = true,
                                isPrimary = volume.isPrimary
                            )
                        )
                    }
                } catch (_: Exception) { }
            }
        } catch (_: Exception) { }

        if (volumes.isEmpty()) {
            try {
                val internalPath = Environment.getExternalStorageDirectory().absolutePath
                val stat = StatFs(internalPath)
                val total = stat.totalBytes
                val free = stat.availableBytes
                volumes.add(
                    StorageInfo(
                        path = internalPath,
                        label = "Internal Storage",
                        type = StorageType.INTERNAL,
                        totalSpace = total,
                        freeSpace = free,
                        usedSpace = total - free,
                        isAvailable = true,
                        isPrimary = true
                    )
                )
            } catch (_: Exception) { }
        }

        volumes
    }

    // ── File operations ───────────────────────────────────────────────────

    override suspend fun copyFile(source: String, destination: String): OperationResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val sourceEngine = engineProvider.getEngineForPath(source)
                val result = if (sourceEngine == engineProvider.getEngineForPath(destination)) {
                    sourceEngine.copyFile(source, destination)
                } else {
                    crossEngineCopy(source, destination)
                }
                invalidateParentCache(destination)
                invalidateParentCache(source)
                result
            } catch (e: Exception) {
                OperationResult.Error("Copy failed: ${e.message}", e)
            }
        }

    override suspend fun moveFile(source: String, destination: String): OperationResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val sourceEngine = engineProvider.getEngineForPath(source)
                val destEngine = engineProvider.getEngineForPath(destination)
                val result = if (sourceEngine == destEngine) {
                    sourceEngine.moveFile(source, destination)
                } else {
                    val copyResult = crossEngineCopy(source, destination)
                    if (copyResult is OperationResult.Success) {
                        sourceEngine.deleteFile(source)
                    }
                    copyResult
                }
                invalidateParentCache(source)
                invalidateParentCache(destination)
                result
            } catch (e: Exception) {
                OperationResult.Error("Move failed: ${e.message}", e)
            }
        }

    override suspend fun deleteFile(path: String): OperationResult<Unit> =
        withContext(Dispatchers.IO) {
            val result = engineProvider.getEngineForPath(path).deleteFile(path)
            invalidateParentCache(path)
            result
        }

    override suspend fun renameFile(path: String, newName: String): OperationResult<Unit> =
        withContext(Dispatchers.IO) {
            val result = engineProvider.getEngineForPath(path).renameFile(path, newName)
            invalidateParentCache(path)
            result
        }

    override suspend fun createFolder(parentPath: String, folderName: String): OperationResult<FileInfo> =
        withContext(Dispatchers.IO) {
            val result = engineProvider.getEngineForPath(parentPath).createFolder(parentPath, folderName)
            invalidateCacheForPath(parentPath)
            result
        }

    override suspend fun createFile(parentPath: String, fileName: String): OperationResult<FileInfo> =
        withContext(Dispatchers.IO) {
            val result = engineProvider.getEngineForPath(parentPath).createFile(parentPath, fileName)
            invalidateCacheForPath(parentPath)
            result
        }

    override suspend fun getFileSize(path: String): Long = withContext(Dispatchers.IO) {
        engineProvider.getEngineForPath(path).getFileSize(path)
    }

    override suspend fun getFileCount(path: String): Int = withContext(Dispatchers.IO) {
        engineProvider.getEngineForPath(path).getFileCount(path)
    }

    override suspend fun exists(path: String): Boolean = withContext(Dispatchers.IO) {
        engineProvider.getEngineForPath(path).exists(path)
    }

    override suspend fun getStorageUsage(path: String): Pair<Long, Long> =
        withContext(Dispatchers.IO) {
            engineProvider.getEngineForPath(path).getStorageUsage(path)
        }

    // ── Cache helpers ─────────────────────────────────────────────────────

    fun invalidateCache() {
        dirCache.clear()
    }

    fun invalidateCacheForPath(path: String) {
        val keysToRemove = dirCache.keys.filter { it.startsWith("$path|") }
        keysToRemove.forEach { dirCache.remove(it) }
    }

    private fun invalidateParentCache(path: String) {
        try {
            val parent = when {
                path.startsWith("content://") -> extractUriParent(path)
                else -> File(path).parent
            }
            if (!parent.isNullOrBlank()) {
                invalidateCacheForPath(parent)
            }
        } catch (_: Exception) { }
    }

    private fun extractUriParent(uriString: String): String? {
        return try {
            val uri = android.net.Uri.parse(uriString)
            val authority = uri.authority
            val path = uri.path
            if (authority.isNullOrBlank() || path.isNullOrBlank()) return null
            val trimmed = path.trim('/')
            val lastSlash = trimmed.lastIndexOf('/')
            if (lastSlash <= 0) return null
            "content://$authority/${trimmed.substring(0, lastSlash)}"
        } catch (_: Exception) { null }
    }

    // ── Sorting ───────────────────────────────────────────────────────────

    private fun fileComparator(options: FileSortOptions): Comparator<FileInfo> {
        val directionComparator: Comparator<FileInfo> = when (options.mode) {
            SortMode.NAME -> if (options.order == SortOrder.ASCENDING) {
                compareBy { it.nameLowercase }
            } else {
                compareByDescending { it.nameLowercase }
            }
            SortMode.DATE -> if (options.order == SortOrder.ASCENDING) {
                compareBy { it.lastModified }
            } else {
                compareByDescending { it.lastModified }
            }
            SortMode.SIZE -> if (options.order == SortOrder.ASCENDING) {
                compareBy { it.size }
            } else {
                compareByDescending { it.size }
            }
            SortMode.EXTENSION -> if (options.order == SortOrder.ASCENDING) {
                compareBy { it.extensionLowercase }
            } else {
                compareByDescending { it.extensionLowercase }
            }
        }

        return if (options.foldersFirst) {
            compareByDescending<FileInfo> { it.isDirectory }.then(directionComparator)
        } else {
            compareBy<FileInfo> { it.isDirectory }.then(directionComparator)
        }
    }

    // ── Cross-engine copy helper ──────────────────────────────────────────

    private suspend fun crossEngineCopy(source: String, destination: String): OperationResult<Unit> {
        return try {
            val sourceEngine = engineProvider.getEngineForPath(source)

            val sourceInfo = sourceEngine.getFileInfo(source)
                ?: return OperationResult.Error("Cannot read source file info")

            if (sourceInfo.isDirectory) {
                return OperationResult.Error("Cross-engine directory copy is not supported")
            }

            copyAcrossEngines(source, destination)
            OperationResult.Success(Unit)
        } catch (e: Exception) {
            OperationResult.Error("Cross-engine copy failed: ${e.message}", e)
        }
    }

    private fun copyAcrossEngines(source: String, destination: String) {
        val sourceUri = source.toContentUriOrNull()
        val destUri = destination.toContentUriOrNull()

        if (sourceUri != null && !contentUriExists(sourceUri)) {
            throw IllegalStateException("Source content URI does not exist")
        }

        val input = if (sourceUri != null) {
            context.contentResolver.openInputStream(sourceUri)
                ?: throw IllegalStateException("Cannot open source content URI")
        } else {
            java.io.FileInputStream(java.io.File(source))
        }

        val output = if (destUri != null) {
            if (!contentUriExists(destUri)) {
                throw IllegalStateException("Destination content file must be created before cross-engine copy")
            }
            context.contentResolver.openOutputStream(destUri)
        } else {
            val destFile = java.io.File(destination)
            destFile.parentFile?.mkdirs()
            java.io.FileOutputStream(destFile)
        }

        input.use { inStream ->
            output.use { outStream ->
                if (outStream == null) throw IllegalStateException("Cannot open destination output stream")
                inStream.copyTo(outStream)
            }
        }
    }

    private fun contentUriExists(uri: android.net.Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { }
            true
        } catch (_: Exception) { false }
    }

    private fun String.toContentUriOrNull(): android.net.Uri? {
        return if (this.startsWith("content://")) android.net.Uri.parse(this) else null
    }
}
