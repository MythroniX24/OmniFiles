package com.omnilabs.omfiles.domain.repository

import com.omnilabs.omfiles.domain.model.FileInfo
import com.omnilabs.omfiles.domain.model.OperationResult
import kotlinx.coroutines.flow.Flow

data class SearchFilters(
    val query: String = "",
    val extension: String? = null,
    val minSize: Long? = null,
    val maxSize: Long? = null,
    val minDate: Long? = null,
    val maxDate: Long? = null,
    val searchInFolder: String? = null,
    val caseSensitive: Boolean = false
)

/**
 * Search result subtypes for better UX categorization.
 */
enum class SearchMatchType {
    /** User typed the exact filename */
    EXACT,
    /** Filename starts with the query */
    PREFIX,
    /** Filename contains the query as a word or substring */
    FUZZY
}

interface SearchRepository {
    /** Full-featured search with filters */
    fun search(filters: SearchFilters): Flow<List<FileInfo>>

    /** Ultra-fast prefix-only suggestions (top 5) — < 10 ms */
    fun suggest(query: String): Flow<List<FileInfo>>

    /** Index a folder for fast searching */
    suspend fun indexFolder(
        path: String,
        onProgress: ((indexed: Int, total: Int) -> Unit)? = null
    ): OperationResult<Int>

    /** Incrementally index a folder (adds new files only) */
    suspend fun indexFolderIncremental(path: String): OperationResult<Int>

    /** Auto-index primary storage if index is empty */
    suspend fun autoIndexIfNeeded(onProgress: ((Int, Int) -> Unit)? = null): OperationResult<Int>

    /** Check if index exists */
    suspend fun isIndexNeeded(): Boolean

    suspend fun clearIndex(): OperationResult<Unit>
    suspend fun getIndexStatus(): SearchIndexStatus
}

data class SearchIndexStatus(
    val isIndexing: Boolean = false,
    val lastIndexedPath: String? = null,
    val indexedFilesCount: Int = 0,
    val indexedFoldersCount: Int = 0,
    val lastIndexTime: Long? = null
)
