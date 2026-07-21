package com.omnilabs.omfiles.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.omnilabs.omfiles.data.local.entity.RecycleBinEntity

@Dao
interface RecycleBinDao {

    @Query("SELECT * FROM recycle_bin ORDER BY deleted_at DESC")
    suspend fun getAllItems(): List<RecycleBinEntity>

    @Query("SELECT * FROM recycle_bin WHERE id = :id")
    suspend fun getItemById(id: Long): RecycleBinEntity?

    @Query("SELECT * FROM recycle_bin WHERE originalPath = :originalPath LIMIT 1")
    suspend fun getItemByOriginalPath(originalPath: String): RecycleBinEntity?

    @Insert
    suspend fun insert(item: RecycleBinEntity): Long

    @Query("DELETE FROM recycle_bin WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM recycle_bin WHERE originalPath = :originalPath")
    suspend fun deleteByOriginalPath(originalPath: String)

    @Query("DELETE FROM recycle_bin")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM recycle_bin")
    suspend fun getCount(): Int

    @Query("SELECT SUM(size) FROM recycle_bin")
    suspend fun getTotalSize(): Long?

    @Query("DELETE FROM recycle_bin WHERE deleted_at < :cutoffTime")
    suspend fun deleteOlderThan(cutoffTime: Long): Int
}
