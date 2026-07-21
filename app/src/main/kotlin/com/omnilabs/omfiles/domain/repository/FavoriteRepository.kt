package com.omnilabs.omfiles.domain.repository

import com.omnilabs.omfiles.domain.model.FileInfo
import com.omnilabs.omfiles.domain.model.OperationResult
import kotlinx.coroutines.flow.Flow

interface FavoriteRepository {
    fun getFavorites(): Flow<List<FileInfo>>
    suspend fun addFavorite(path: String): OperationResult<Unit>
    suspend fun removeFavorite(path: String): OperationResult<Unit>
    suspend fun isFavorite(path: String): Boolean
}
