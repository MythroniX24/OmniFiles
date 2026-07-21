package com.omnilabs.omfiles.search

import com.omnilabs.omfiles.data.local.dao.SearchIndexDao
import com.omnilabs.omfiles.data.local.entity.SearchIndexEntity
import com.omnilabs.omfiles.domain.model.FileInfo
import com.omnilabs.omfiles.domain.model.OperationResult
import com.omnilabs.omfiles.domain.repository.SearchFilters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchEngine @Inject constructor(
    private val searchIndexDao: SearchIndexDao
) {

    fun search(filters: SearchFilters): Flow<List<FileInfo>> {
        val query = if (filters.caseSensitive) filters.query else filters.query.lowercase()
        return searchIndexDao.search(
            query = if (filters.caseSensitive) query else "%$query%",
            extension = filters.extension?.lowercase(),
            minSize = filters.minSize,
            maxSize = filters.maxSize,
            minDate = filters.minDate,
            maxDate = filters.maxDate
        ).map { entities ->
            entities
                .filter { entity ->
                    if (filters.searchInFolder != null) {
                        entity.parentPath.startsWith(filters.searchInFolder)
                    } else true
                }
                .map { entity ->
                    FileInfo(
                        path = entity.path,
                        name = entity.name,
                        extension = entity.extension,
                        isDirectory = entity.isDirectory,
                        isHidden = entity.name.startsWith('.'),
                        size = entity.size,
                        lastModified = entity.lastModified,
                        parentPath = entity.parentPath,
                        isSymbolicLink = false
                    )
                }
        }
    }

    suspend fun indexFolder(path: String): OperationResult<Int> = withContext(Dispatchers.IO) {
        try {
            val rootDir = File(path)
            if (!rootDir.exists() || !rootDir.isDirectory) {
                return@withContext OperationResult.Error("Invalid directory: $path")
            }

            // Clear existing index for this folder
            searchIndexDao.deleteByFolder(path)

            val entries = mutableListOf<SearchIndexEntity>()
            val fileQueue = ArrayDeque<File>()
            fileQueue.add(rootDir)

            while (fileQueue.isNotEmpty()) {
                val currentDir = fileQueue.removeFirst()
                val files = currentDir.listFiles() ?: continue

                for (file in files) {
                    if (file.isDirectory && !file.isHidden) {
                        fileQueue.add(file)
                        entries.add(
                            SearchIndexEntity(
                                path = file.absolutePath,
                                name = file.name,
                                extension = "",
                                isDirectory = true,
                                size = 0L,
                                lastModified = file.lastModified(),
                                parentPath = file.parent ?: ""
                            )
                        )
                    } else if (file.isFile) {
                        entries.add(
                            SearchIndexEntity(
                                path = file.absolutePath,
                                name = file.name,
                                extension = file.extension.lowercase(),
                                isDirectory = false,
                                size = file.length(),
                                lastModified = file.lastModified(),
                                parentPath = file.parent ?: ""
                            )
                        )
                    }

                    // Batch insert every 500 entries to avoid memory issues
                    if (entries.size >= 500) {
                        searchIndexDao.insertAll(entries.toList())
                        entries.clear()
                    }
                }
            }

            // Insert remaining entries
            if (entries.isNotEmpty()) {
                searchIndexDao.insertAll(entries)
            }

            OperationResult.Success(searchIndexDao.getCount())
        } catch (e: Exception) {
            OperationResult.Error("Indexing failed: ${e.message}", e)
        }
    }

    suspend fun clearIndex(): OperationResult<Unit> = withContext(Dispatchers.IO) {
        try {
            searchIndexDao.clearAll()
            OperationResult.Success(Unit)
        } catch (e: Exception) {
            OperationResult.Error("Failed to clear index: ${e.message}", e)
        }
    }

    suspend fun getIndexStatus(): com.omnilabs.omfiles.domain.repository.SearchIndexStatus =
        withContext(Dispatchers.IO) {
            val count = searchIndexDao.getCount()
            val folderCount = searchIndexDao.getFolderCount()
            com.omnilabs.omfiles.domain.repository.SearchIndexStatus(
                isIndexing = false,
                indexedFilesCount = count,
                indexedFoldersCount = folderCount,
                lastIndexTime = System.currentTimeMillis()
            )
        }
}
