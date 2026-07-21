package com.omnilabs.omfiles.domain.repository

import com.omnilabs.omfiles.domain.model.FileInfo
import com.omnilabs.omfiles.domain.model.FileSortOptions
import com.omnilabs.omfiles.domain.model.OperationResult
import com.omnilabs.omfiles.domain.model.StorageInfo
import kotlinx.coroutines.flow.Flow

interface FileRepository {
    fun getFiles(path: String, sortOptions: FileSortOptions = FileSortOptions(), showHidden: Boolean = false): Flow<List<FileInfo>>
    suspend fun getFileInfo(path: String): FileInfo?
    suspend fun getStorageVolumes(): List<StorageInfo>
    suspend fun copyFile(source: String, destination: String): OperationResult<Unit>
    suspend fun moveFile(source: String, destination: String): OperationResult<Unit>
    suspend fun deleteFile(path: String): OperationResult<Unit>
    suspend fun renameFile(path: String, newName: String): OperationResult<Unit>
    suspend fun createFolder(parentPath: String, folderName: String): OperationResult<FileInfo>
    suspend fun createFile(parentPath: String, fileName: String): OperationResult<FileInfo>
    suspend fun getFileSize(path: String): Long
    suspend fun getFileCount(path: String): Int
    suspend fun exists(path: String): Boolean
    suspend fun getStorageUsage(path: String): Pair<Long, Long> // used, total
}
