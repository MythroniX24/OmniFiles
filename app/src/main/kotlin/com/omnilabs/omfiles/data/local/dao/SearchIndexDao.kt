package com.omnilabs.omfiles.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.omnilabs.omfiles.data.local.entity.SearchIndexEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchIndexDao {
    @Query("""
        SELECT * FROM search_index 
        WHERE name LIKE '%' || :query || '%'
        AND (:extension IS NULL OR extension = :extension)
        AND (:minSize IS NULL OR size >= :minSize)
        AND (:maxSize IS NULL OR size <= :maxSize)
        AND (:minDate IS NULL OR last_modified >= :minDate)
        AND (:maxDate IS NULL OR last_modified <= :maxDate)
        ORDER BY name ASC
    """)
    fun search(
        query: String,
        extension: String?,
        minSize: Long?,
        maxSize: Long?,
        minDate: Long?,
        maxDate: Long?
    ): Flow<List<SearchIndexEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<SearchIndexEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: SearchIndexEntity)

    @Query("DELETE FROM search_index WHERE path LIKE :folderPath || '%'")
    suspend fun deleteByFolder(folderPath: String)

    @Query("DELETE FROM search_index")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM search_index")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(DISTINCT parent_path) FROM search_index")
    suspend fun getFolderCount(): Int
}
