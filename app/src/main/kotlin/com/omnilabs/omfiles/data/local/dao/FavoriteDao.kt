package com.omnilabs.omfiles.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.omnilabs.omfiles.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY added_at DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteEntity)

    @Delete
    suspend fun delete(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE path = :path")
    suspend fun deleteByPath(path: String)

    @Query("SELECT COUNT(*) FROM favorites WHERE path = :path")
    suspend fun isFavorite(path: String): Int

    @Query("SELECT path FROM favorites")
    suspend fun getAllFavoritePaths(): List<String>

    @Query("SELECT path FROM favorites WHERE path IN (:paths)")
    suspend fun getFavoritePathsIn(paths: Collection<String>): List<String>

    @Query("DELETE FROM favorites")
    suspend fun clearAll()
}
