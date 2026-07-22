package com.omnilabs.omfiles.domain.repository

import com.omnilabs.omfiles.domain.model.FileInfo
import kotlinx.coroutines.flow.Flow

/**
 * Simple search repository — directly searches the filesystem.
 * No indexing required.
 */
interface SearchRepository {
    /** Direct filesystem search — works immediately, no indexing needed */
    fun search(query: String): Flow<List<FileInfo>>
}
