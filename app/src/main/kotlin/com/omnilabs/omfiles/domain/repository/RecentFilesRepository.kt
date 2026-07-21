package com.omnilabs.omfiles.domain.repository

import com.omnilabs.omfiles.domain.model.FileInfo
import kotlinx.coroutines.flow.Flow

interface RecentFilesRepository {
    fun getRecentFiles(): Flow<List<FileInfo>>
    suspend fun addRecentFile(path: String)
    suspend fun clearRecentFiles()
}
