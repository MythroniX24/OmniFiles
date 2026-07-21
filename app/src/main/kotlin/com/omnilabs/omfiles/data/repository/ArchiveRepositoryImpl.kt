package com.omnilabs.omfiles.data.repository

import com.omnilabs.omfiles.archive.ArchiveEngine
import com.omnilabs.omfiles.domain.model.OperationResult
import com.omnilabs.omfiles.domain.repository.ArchiveRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArchiveRepositoryImpl @Inject constructor(
    private val archiveEngine: ArchiveEngine
) : ArchiveRepository {

    override suspend fun extractArchive(
        archivePath: String,
        destinationDir: String,
        password: String?
    ): OperationResult<Unit> = withContext(Dispatchers.IO) {
        when (val result = archiveEngine.extractArchive(archivePath, destinationDir, password)) {
            is ArchiveEngine.ArchiveResult.Success -> OperationResult.Success(Unit)
            is ArchiveEngine.ArchiveResult.Error -> OperationResult.Error(result.message)
        }
    }

    override suspend fun createArchive(
        sourcePaths: List<String>,
        archivePath: String,
        password: String?
    ): OperationResult<Unit> = withContext(Dispatchers.IO) {
        when (val result = archiveEngine.createArchive(sourcePaths, archivePath, password)) {
            is ArchiveEngine.ArchiveResult.Success -> OperationResult.Success(Unit)
            is ArchiveEngine.ArchiveResult.Error -> OperationResult.Error(result.message)
        }
    }

    override suspend fun getArchiveEntries(archivePath: String): OperationResult<List<String>> =
        withContext(Dispatchers.IO) {
            when (val result = archiveEngine.getArchiveEntries(archivePath)) {
                is ArchiveEngine.ArchiveResult.Success -> {
                    val entries = result.message.split("\n").filter { it.isNotEmpty() }
                    OperationResult.Success(entries)
                }
                is ArchiveEngine.ArchiveResult.Error -> OperationResult.Error(result.message)
            }
        }

    override suspend fun isPasswordProtected(archivePath: String): Boolean =
        withContext(Dispatchers.IO) {
            archiveEngine.isPasswordProtected(archivePath)
        }
}
