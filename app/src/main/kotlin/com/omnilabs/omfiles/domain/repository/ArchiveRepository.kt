package com.omnilabs.omfiles.domain.repository

import com.omnilabs.omfiles.domain.model.OperationResult

interface ArchiveRepository {
    suspend fun extractArchive(archivePath: String, destinationDir: String, password: String? = null): OperationResult<Unit>
    suspend fun createArchive(
        sourcePaths: List<String>,
        archivePath: String,
        password: String? = null
    ): OperationResult<Unit>
    suspend fun getArchiveEntries(archivePath: String): OperationResult<List<String>>
    suspend fun isPasswordProtected(archivePath: String): Boolean
}
