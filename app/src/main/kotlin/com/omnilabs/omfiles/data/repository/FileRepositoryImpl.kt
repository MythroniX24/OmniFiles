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
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepositoryImpl @Inject constructor() : FileRepository {

    override fun getFiles(
        path: String,
        sortOptions: FileSortOptions,
        showHidden: Boolean
    ): Flow<List<FileInfo>> = flow {
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) {
            emit(emptyList())
            return@flow
        }

        val files = dir.listFiles()?.toList() ?: emptyList()
        val mapped = files
            .filter { showHidden || !it.isHidden }
            .map { FileInfo.fromFile(it) }
            .sortedWith(compareBy(sortOptions))

        emit(mapped)
    }.flowOn(Dispatchers.IO)

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
                    // Fallback: copy + delete
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
                val parent = file.parentFile ?: return@withContext OperationResult.Error("No parent directory")
                val newFile = File(parent, newName)

                if (newFile.exists()) {
                    return@withContext OperationResult.Error("A file with this name already exists")
                }

                if (file.renameTo(newFile)) {
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
                    OperationResult.Success(FileInfo.fromFile(file))
                } else {
                    OperationResult.Error("Failed to create file")
                }
            } catch (e: Exception) {
                OperationResult.Error("Create file failed: ${e.message}", e)
            }
        }

    override suspend fun getFileSize(path: String): Long = withContext(Dispatchers.IO) {
        val file = File(path)
        if (file.isDirectory) {
            file.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } else {
            file.length()
        }
    }

    override suspend fun getFileCount(path: String): Int = withContext(Dispatchers.IO) {
        val dir = File(path)
        if (dir.isDirectory) {
            dir.listFiles()?.size ?: 0
        } else {
            0
        }
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

    // Helper methods

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

    private fun compareBy(
        options: FileSortOptions
    ): Comparator<FileInfo> {
        val comparator = when (options.mode) {
            SortMode.NAME -> compareBy<FileInfo> { it.name.lowercase() }
            SortMode.DATE -> compareByDescending<FileInfo> { it.lastModified }
            SortMode.SIZE -> compareBy<FileInfo> { it.size }
            SortMode.EXTENSION -> compareBy<FileInfo> { it.extension.lowercase() }
        }

        val withOrder = if (options.order == SortOrder.DESCENDING) {
            when (options.mode) {
                SortMode.NAME -> compareByDescending<FileInfo> { it.name.lowercase() }
                SortMode.DATE -> compareBy<FileInfo> { it.lastModified }
                SortMode.SIZE -> compareByDescending<FileInfo> { it.size }
                SortMode.EXTENSION -> compareByDescending<FileInfo> { it.extension.lowercase() }
            }
        } else {
            comparator
        }

        return if (options.foldersFirst) {
            compareByDescending<FileInfo> { it.isDirectory }.then(withOrder)
        } else {
            compareBy<FileInfo> { it.isDirectory }.then(withOrder)
        }
    }
}
