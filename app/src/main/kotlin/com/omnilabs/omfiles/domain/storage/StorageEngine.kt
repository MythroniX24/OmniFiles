package com.omnilabs.omfiles.domain.storage

import com.omnilabs.omfiles.domain.model.FileInfo
import com.omnilabs.omfiles.domain.model.OperationResult

/**
 * Abstraction over all file storage providers (internal, SD card, USB OTG, SAF, MediaStore).
 * Implementations must be thread-safe and never block the UI thread.
 */
interface StorageEngine {

    suspend fun getFiles(path: String, showHidden: Boolean): List<FileInfo>

    suspend fun getFileInfo(path: String): FileInfo?

    suspend fun copyFile(source: String, destination: String): OperationResult<Unit>

    suspend fun moveFile(source: String, destination: String): OperationResult<Unit>

    suspend fun deleteFile(path: String): OperationResult<Unit>

    suspend fun renameFile(path: String, newName: String): OperationResult<Unit>

    suspend fun createFolder(parentPath: String, folderName: String): OperationResult<FileInfo>

    suspend fun createFile(parentPath: String, fileName: String): OperationResult<FileInfo>

    suspend fun exists(path: String): Boolean

    suspend fun getFileSize(path: String): Long

    suspend fun getFileCount(path: String): Int

    suspend fun getStorageUsage(path: String): Pair<Long, Long>
}
