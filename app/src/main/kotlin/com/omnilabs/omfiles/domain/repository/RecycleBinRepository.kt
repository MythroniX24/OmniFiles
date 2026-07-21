package com.omnilabs.omfiles.domain.repository

import com.omnilabs.omfiles.domain.model.OperationResult

data class RecycleBinItem(
    val id: Long,
    val originalPath: String,
    val trashPath: String,
    val name: String,
    val extension: String,
    val isDirectory: Boolean,
    val size: Long,
    val deletedAt: Long,
    val originalParent: String
)

interface RecycleBinRepository {

    suspend fun getAllItems(): List<RecycleBinItem>

    suspend fun getItemCount(): Int

    suspend fun getTotalSize(): Long

    suspend fun moveToTrash(originalPath: String): OperationResult<Unit>

    suspend fun restoreItem(id: Long): OperationResult<String>

    suspend fun permanentDelete(id: Long): OperationResult<Unit>

    suspend fun emptyTrash(): OperationResult<Unit>

    suspend fun cleanOldItems(maxAgeDays: Int = 30): Int
}
