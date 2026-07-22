package com.omnilabs.omfiles.data.repository

import com.omnilabs.omfiles.domain.model.FileInfo
import com.omnilabs.omfiles.domain.repository.SearchRepository
import com.omnilabs.omfiles.search.SearchEngine
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val searchEngine: SearchEngine
) : SearchRepository {

    override fun search(query: String): Flow<List<FileInfo>> {
        return searchEngine.search(query)
    }
}
