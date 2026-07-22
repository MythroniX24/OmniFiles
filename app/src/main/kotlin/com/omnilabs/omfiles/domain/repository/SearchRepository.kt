package com.omnilabs.omfiles.domain.repository

import com.omnilabs.omfiles.domain.model.FileInfo
import kotlinx.coroutines.flow.Flow

/**
 * Simple search repository — directly searches the filesystem.
 * No indexing required.
 */
data class SearchFilters(
    val query: String = "",
    val extension: String? = null,
    val searchInFolder: String? = null
)

interface SearchRepository {
    /** Direct filesystem search — works immediately, no indexing needed */
    fun search(query: String): Flow<List<FileInfo>>
}
