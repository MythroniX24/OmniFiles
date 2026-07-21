package com.omnilabs.omfiles.data.repository

import com.omnilabs.omfiles.data.local.dao.FavoriteDao
import com.omnilabs.omfiles.data.local.entity.FavoriteEntity
import com.omnilabs.omfiles.domain.model.FileInfo
import com.omnilabs.omfiles.domain.model.OperationResult
import com.omnilabs.omfiles.domain.repository.FavoriteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRepositoryImpl @Inject constructor(
    private val favoriteDao: FavoriteDao
) : FavoriteRepository {

    override fun getFavorites(): Flow<List<FileInfo>> {
        return favoriteDao.getAllFavorites().map { entities ->
            entities.map { entity ->
                FileInfo(
                    path = entity.path,
                    name = entity.name,
                    extension = entity.extension,
                    isDirectory = entity.isDirectory,
                    isHidden = entity.name.startsWith('.'),
                    size = entity.size,
                    lastModified = entity.addedAt,
                    parentPath = entity.path.substringBeforeLast('/'),
                    isSymbolicLink = false
                )
            }
        }
    }

    override suspend fun addFavorite(path: String): OperationResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val file = java.io.File(path)
                val entity = FavoriteEntity(
                    path = path,
                    name = file.name,
                    extension = file.extension,
                    isDirectory = file.isDirectory,
                    size = if (file.isFile) file.length() else 0L,
                    addedAt = System.currentTimeMillis()
                )
                favoriteDao.insert(entity)
                OperationResult.Success(Unit)
            } catch (e: Exception) {
                OperationResult.Error("Failed to add favorite: ${e.message}", e)
            }
        }

    override suspend fun removeFavorite(path: String): OperationResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                favoriteDao.deleteByPath(path)
                OperationResult.Success(Unit)
            } catch (e: Exception) {
                OperationResult.Error("Failed to remove favorite: ${e.message}", e)
            }
        }

    override suspend fun isFavorite(path: String): Boolean =
        withContext(Dispatchers.IO) {
            favoriteDao.isFavorite(path) > 0
        }
}
