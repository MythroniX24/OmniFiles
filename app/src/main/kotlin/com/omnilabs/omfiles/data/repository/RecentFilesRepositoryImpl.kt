package com.omnilabs.omfiles.data.repository

import com.omnilabs.omfiles.data.local.dao.RecentFileDao
import com.omnilabs.omfiles.data.local.entity.RecentFileEntity
import com.omnilabs.omfiles.domain.model.FileInfo
import com.omnilabs.omfiles.domain.repository.RecentFilesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecentFilesRepositoryImpl @Inject constructor(
    private val recentFileDao: RecentFileDao
) : RecentFilesRepository {

    override fun getRecentFiles(): Flow<List<FileInfo>> {
        return recentFileDao.getRecentFiles().map { entities ->
            entities
                .filter { File(it.path).exists() }
                .map { entity ->
                    FileInfo(
                        path = entity.path,
                        name = entity.name,
                        extension = entity.extension,
                        isDirectory = entity.isDirectory,
                        isHidden = entity.name.startsWith('.'),
                        size = entity.size,
                        lastModified = entity.lastModified,
                        parentPath = entity.path.substringBeforeLast('/'),
                        isSymbolicLink = false
                    )
                }
        }
    }

    override suspend fun addRecentFile(path: String) = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (file.exists()) {
                val entity = RecentFileEntity(
                    path = path,
                    name = file.name,
                    extension = file.extension,
                    isDirectory = file.isDirectory,
                    size = if (file.isFile) file.length() else 0L,
                    lastModified = file.lastModified(),
                    openedAt = System.currentTimeMillis()
                )
                recentFileDao.insert(entity)
                recentFileDao.trimToLimit()
            }
        } catch (_: Exception) { }
    }

    override suspend fun clearRecentFiles() = withContext(Dispatchers.IO) {
        recentFileDao.clearAll()
    }
}
