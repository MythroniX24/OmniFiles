package com.omnilabs.omfiles.data.repository

import com.omnilabs.omfiles.domain.model.FileInfo
import com.omnilabs.omfiles.domain.model.OperationResult
import com.omnilabs.omfiles.domain.repository.SearchFilters
import com.omnilabs.omfiles.domain.repository.SearchIndexStatus
import com.omnilabs.omfiles.domain.repository.SearchRepository
import com.omnilabs.omfiles.search.SearchEngine
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val searchEngine: SearchEngine
) : SearchRepository {

    override fun search(filters: SearchFilters): Flow<List<FileInfo>> {
        return searchEngine.search(filters)
    }

    override suspend fun indexFolder(path: String): OperationResult<Int> {
        return searchEngine.indexFolder(path)
    }

    override suspend fun clearIndex(): OperationResult<Unit> {
        return searchEngine.clearIndex()
    }

    override suspend fun getIndexStatus(): SearchIndexStatus {
        return searchEngine.getIndexStatus()
    }
}
