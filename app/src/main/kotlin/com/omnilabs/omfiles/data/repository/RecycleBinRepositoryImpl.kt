package com.omnilabs.omfiles.data.repository

import com.omnilabs.omfiles.data.local.dao.RecycleBinDao
import com.omnilabs.omfiles.data.local.entity.RecycleBinEntity
import com.omnilabs.omfiles.domain.model.OperationResult
import com.omnilabs.omfiles.domain.repository.RecycleBinItem
import com.omnilabs.omfiles.domain.repository.RecycleBinRepository
import com.omnilabs.omfiles.recycle.RecycleBinManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecycleBinRepositoryImpl @Inject constructor(
    private val recycleBinDao: RecycleBinDao,
    private val recycleBinManager: RecycleBinManager
) : RecycleBinRepository {

    override suspend fun getAllItems(): List<RecycleBinItem> {
        return recycleBinDao.getAllItems().map { it.toDomain() }
    }

    override suspend fun getItemCount(): Int = recycleBinDao.getCount()

    override suspend fun getTotalSize(): Long = recycleBinDao.getTotalSize() ?: 0L

    override suspend fun moveToTrash(originalPath: String): OperationResult<Unit> {
        return try {
            val file = java.io.File(originalPath)
            val result = recycleBinManager.moveToTrash(originalPath)
            if (result.isSuccess) {
                val trashPath = result.getOrThrow()

                // Save to database
                val entity = RecycleBinEntity(
                    originalPath = originalPath,
                    trashPath = trashPath,
                    name = file.name,
                    extension = file.extension,
                    isDirectory = file.isDirectory,
                    size = if (file.isDirectory) 0L else file.length(),
                    deletedAt = System.currentTimeMillis(),
                    originalParent = file.parent ?: ""
                )
                recycleBinDao.insert(entity)
                OperationResult.Success(Unit)
            } else {
                OperationResult.Error(result.exceptionOrNull()?.message ?: "Failed to move to trash")
            }
        } catch (e: Exception) {
            OperationResult.Error("Failed to move to trash: ${e.message}")
        }
    }

    override suspend fun restoreItem(id: Long): OperationResult<String> {
        return try {
            val entity = recycleBinDao.getItemById(id)
                ?: return OperationResult.Error("Item not found in recycle bin")

            val result = recycleBinManager.restoreFromTrash(entity)
            if (result.isSuccess) {
                recycleBinDao.deleteById(id)
                OperationResult.Success(result.getOrThrow())
            } else {
                OperationResult.Error(result.exceptionOrNull()?.message ?: "Failed to restore")
            }
        } catch (e: Exception) {
            OperationResult.Error("Failed to restore: ${e.message}")
        }
    }

    override suspend fun permanentDelete(id: Long): OperationResult<Unit> {
        return try {
            val entity = recycleBinDao.getItemById(id)
                ?: return OperationResult.Error("Item not found")

            recycleBinManager.permanentlyDelete(entity.trashPath)
            recycleBinDao.deleteById(id)
            OperationResult.Success(Unit)
        } catch (e: Exception) {
            OperationResult.Error("Failed to delete: ${e.message}")
        }
    }

    override suspend fun emptyTrash(): OperationResult<Unit> {
        return try {
            recycleBinManager.emptyTrash()
            recycleBinDao.clearAll()
            OperationResult.Success(Unit)
        } catch (e: Exception) {
            OperationResult.Error("Failed to empty trash: ${e.message}")
        }
    }

    override suspend fun cleanOldItems(maxAgeDays: Int): Int {
        val cutoffTime = System.currentTimeMillis() - (maxAgeDays * 24L * 60 * 60 * 1000)
        val deletedCount = recycleBinDao.deleteOlderThan(cutoffTime)

        // Also delete physical files for cleaned entities
        // Re-read to find which ones were deleted - actually the DAO already handles this
        return deletedCount
    }

    private fun RecycleBinEntity.toDomain() = RecycleBinItem(
        id = id,
        originalPath = originalPath,
        trashPath = trashPath,
        name = name,
        extension = extension,
        isDirectory = isDirectory,
        size = size,
        deletedAt = deletedAt,
        originalParent = originalParent
    )
}
