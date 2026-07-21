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

interface SearchRepository {
    fun search(filters: SearchFilters): Flow<List<FileInfo>>
    suspend fun indexFolder(path: String): OperationResult<Int>
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
