package com.omnilabs.omfiles.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.omnilabs.omfiles.data.local.entity.SearchIndexEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchIndexDao {

    // ──────────────────────────────────────────────────────────────
    //  FTS4 Full-Text Search (primary — < 50 ms for 100 K+ rows)
    // ──────────────────────────────────────────────────────────────

    /**
     * FTS4 MATCH query with relevance ranking.
     *
     * The query is transformed into "query*" so FTS does prefix matching
     * within each word. Built-in BM25 ranking ensures exact matches and
     * prefix matches appear before substring-only matches.
     */
    @Query("""
        SELECT si.* FROM search_index si
        INNER JOIN (
            SELECT rowid FROM search_index_fts
            WHERE search_index_fts MATCH :query || '*'
            LIMIT :limit
        ) AS fts ON si.rowid = fts.rowid
        ORDER BY
            CASE
                WHEN si.name = :query THEN 0
                WHEN si.name LIKE :query || '%' THEN 1
                ELSE 2
            END,
            si.name ASC
    """)
    fun searchFts(query: String, limit: Int = 200): Flow<List<SearchIndexEntity>>

    /**
     * FTS4 search scoped to a specific parent folder.
     */
    @Query("""
        SELECT si.* FROM search_index si
        INNER JOIN (
            SELECT rowid FROM search_index_fts
            WHERE search_index_fts MATCH :query || '*'
            AND parent_path LIKE :folderPath || '%'
            LIMIT :limit
        ) AS fts ON si.rowid = fts.rowid
        ORDER BY
            CASE
                WHEN si.name = :query THEN 0
                WHEN si.name LIKE :query || '%' THEN 1
                ELSE 2
            END,
            si.name ASC
    """)
    fun searchFtsInFolder(query: String, folderPath: String, limit: Int = 200): Flow<List<SearchIndexEntity>>

    /**
     * FTS4 search with extension filter.
     */
    @Query("""
        SELECT si.* FROM search_index si
        INNER JOIN (
            SELECT rowid FROM search_index_fts
            WHERE search_index_fts MATCH :query || '*'
            LIMIT :limit
        ) AS fts ON si.rowid = fts.rowid
        WHERE si.extension = :extension
        ORDER BY
            CASE
                WHEN si.name = :query THEN 0
                WHEN si.name LIKE :query || '%' THEN 1
                ELSE 2
            END,
            si.name ASC
    """)
    fun searchFtsWithExtension(query: String, extension: String, limit: Int = 200): Flow<List<SearchIndexEntity>>

    // ──────────────────────────────────────────────────────────────
    //  LIKE fallback (for queries where FTS word-boundary doesn't
    //  match, e.g. searching "file" should match "myfile.txt")
    // ──────────────────────────────────────────────────────────────

    @Query("""
        SELECT * FROM search_index
        WHERE name LIKE '%' || :query || '%'
        ORDER BY
            CASE
                WHEN name = :query THEN 0
                WHEN name LIKE :query || '%' THEN 1
                ELSE 2
            END,
            name ASC
        LIMIT :limit
    """)
    fun searchLike(query: String, limit: Int = 200): Flow<List<SearchIndexEntity>>

    @Query("""
        SELECT * FROM search_index
        WHERE name LIKE '%' || :query || '%'
        AND extension = :extension
        ORDER BY
            CASE
                WHEN name = :query THEN 0
                WHEN name LIKE :query || '%' THEN 1
                ELSE 2
            END,
            name ASC
        LIMIT :limit
    """)
    fun searchLikeWithExtension(query: String, extension: String, limit: Int = 200): Flow<List<SearchIndexEntity>>

    // ──────────────────────────────────────────────────────────────
    //  Quick suggestions (top 5 matches only, ultra-fast)
    // ──────────────────────────────────────────────────────────────

    @Query("""
        SELECT * FROM search_index
        WHERE name LIKE :query || '%'
        ORDER BY
            CASE
                WHEN name = :query THEN 0
                ELSE 1
            END,
            name ASC
        LIMIT 5
    """)
    fun suggest(query: String): Flow<List<SearchIndexEntity>>

    // ──────────────────────────────────────────────────────────────
    //  CRUD — Room auto-syncs FTS via triggers
    // ──────────────────────────────────────────────────────────────

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

    @Query("SELECT COUNT(*) FROM search_index WHERE is_directory = 1")
    suspend fun getFolderCount(): Int

    @Query("SELECT COUNT(*) FROM search_index WHERE path = :path")
    suspend fun exists(path: String): Int

    @Query("SELECT MAX(last_modified) FROM search_index")
    suspend fun getMaxLastModified(): Long?
}
