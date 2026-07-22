package com.omnilabs.omfiles.data.storage.engine

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import com.omnilabs.omfiles.domain.model.FileInfo
import com.omnilabs.omfiles.domain.model.OperationResult
import com.omnilabs.omfiles.domain.storage.StorageEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Storage Access Framework (SAF) backed storage engine.
 * Handles content:// URIs and removable storage paths that require SAF permissions.
 */
@Singleton
class SafStorageEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : StorageEngine {

    override suspend fun getFiles(path: String, showHidden: Boolean): List<FileInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val doc = documentForPath(path) ?: return@withContext emptyList()
                doc.listFiles().mapNotNull { fileInfoFromDocumentFile(it) }
            } catch (_: Exception) { emptyList() }
        }
    }

    override suspend fun getFileInfo(path: String): FileInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val doc = documentForPath(path) ?: return@withContext null
                fileInfoFromDocumentFile(doc)
            } catch (_: Exception) { null }
        }
    }

    override suspend fun copyFile(source: String, destination: String): OperationResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val sourceDoc = documentForPath(source)
                val destDoc = documentForPath(destination)
                if (sourceDoc == null || destDoc == null) {
                    return@withContext OperationResult.Error("Invalid SAF document")
                }
                copyDocumentFile(sourceDoc, destDoc)
                OperationResult.Success(Unit)
            } catch (e: Exception) {
                OperationResult.Error("SAF copy failed: ${e.message}", e)
            }
        }
    }

    override suspend fun moveFile(source: String, destination: String): OperationResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val sourceDoc = documentForPath(source)
                val destDoc = documentForPath(destination)
                if (sourceDoc == null || destDoc == null) {
                    return@withContext OperationResult.Error("Invalid SAF document")
                }
                copyDocumentFile(sourceDoc, destDoc)
                val deleteResult = deleteFile(source)
                if (deleteResult is OperationResult.Error) {
                    return@withContext OperationResult.Error("Copied to destination but failed to remove source: ${deleteResult.message}", deleteResult.exception)
                }
                OperationResult.Success(Unit)
            } catch (e: Exception) {
                OperationResult.Error("SAF move failed: ${e.message}", e)
            }
        }
    }

    override suspend fun deleteFile(path: String): OperationResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val doc = documentForPath(path) ?: return@withContext OperationResult.Error("Invalid SAF document")
                if (doc.delete()) {
                    OperationResult.Success(Unit)
                } else {
                    OperationResult.Error("Failed to delete SAF document")
                }
            } catch (e: Exception) {
                OperationResult.Error("SAF delete failed: ${e.message}", e)
            }
        }
    }

    override suspend fun renameFile(path: String, newName: String): OperationResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val doc = documentForPath(path) ?: return@withContext OperationResult.Error("Invalid SAF document")
                if (doc.renameTo(newName)) {
                    OperationResult.Success(Unit)
                } else {
                    OperationResult.Error("Failed to rename SAF document")
                }
            } catch (e: Exception) {
                OperationResult.Error("SAF rename failed: ${e.message}", e)
            }
        }
    }

    override suspend fun createFolder(parentPath: String, folderName: String): OperationResult<FileInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val parentDoc = documentForPath(parentPath)
                    ?: return@withContext OperationResult.Error("Invalid SAF parent document")
                val newDoc = parentDoc.createDirectory(folderName)
                    ?: return@withContext OperationResult.Error("Failed to create SAF directory")
                val info = fileInfoFromDocumentFile(newDoc)
                    ?: return@withContext OperationResult.Error("Failed to read created SAF document info")
                OperationResult.Success(info)
            } catch (e: Exception) {
                OperationResult.Error("SAF create folder failed: ${e.message}", e)
            }
        }
    }

    override suspend fun createFile(parentPath: String, fileName: String): OperationResult<FileInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val parentDoc = documentForPath(parentPath)
                    ?: return@withContext OperationResult.Error("Invalid SAF parent document")
                val extension = fileName.substringAfterLast('.', "")
                val mimeType = if (extension.isNotEmpty()) {
                    MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase()) ?: "*/*"
                } else "*/*"
                val newDoc = parentDoc.createFile(mimeType, fileName)
                    ?: return@withContext OperationResult.Error("Failed to create SAF file")
                val info = fileInfoFromDocumentFile(newDoc)
                    ?: return@withContext OperationResult.Error("Failed to read created SAF document info")
                OperationResult.Success(info)
            } catch (e: Exception) {
                OperationResult.Error("SAF create file failed: ${e.message}", e)
            }
        }
    }

    override suspend fun exists(path: String): Boolean {
        return documentForPath(path)?.exists() ?: false
    }

    override suspend fun getFileSize(path: String): Long {
        return documentForPath(path)?.length() ?: 0L
    }

    override suspend fun getFileCount(path: String): Int {
        return try {
            documentForPath(path)?.listFiles()?.size ?: 0
        } catch (_: Exception) { 0 }
    }

    override suspend fun getStorageUsage(path: String): Pair<Long, Long> {
        return try {
            val doc = documentForPath(path)
            if (doc != null && doc.exists()) {
                Pair(doc.length(), doc.length())
            } else {
                Pair(0L, 0L)
            }
        } catch (_: Exception) { Pair(0L, 0L) }
    }

    private fun documentForPath(path: String): DocumentFile? {
        return if (path.startsWith("content://")) {
            DocumentFile.fromTreeUri(context, Uri.parse(path))
                ?: DocumentFile.fromSingleUri(context, Uri.parse(path))
        } else {
            val persistedUri = findPersistedUriForPath(path)
            if (persistedUri != null) {
                val tree = DocumentFile.fromTreeUri(context, persistedUri)
                if (tree != null) {
                    val relativePath = path.removePrefix(tree.uri.path ?: "")
                    return if (relativePath.isEmpty() || relativePath == path) {
                        tree
                    } else {
                        resolveChild(tree, relativePath.trim('/'))
                    }
                }
            }
            val file = File(path)
            if (file.exists()) DocumentFile.fromFile(file) else null
        }
    }

    private fun findPersistedUriForPath(path: String): Uri? {
        try {
            context.contentResolver.persistedUriPermissions.forEach { permission ->
                val treeUri = permission.uri
                val treePath = treeUri.path ?: return@forEach
                if (path.startsWith(treePath)) {
                    return treeUri
                }
            }
        } catch (_: Exception) { }
        return null
    }

    private fun resolveChild(root: DocumentFile, relativePath: String): DocumentFile? {
        var current: DocumentFile? = root
        for (segment in relativePath.split('/')) {
            if (segment.isEmpty()) continue
            current = current?.findFile(segment) ?: return null
        }
        return current
    }

    private fun fileInfoFromDocumentFile(doc: DocumentFile): FileInfo? {
        return try {
            val name = doc.name ?: return null
            val dotIndex = name.lastIndexOf('.')
            val uriString = doc.uri.toString()
            FileInfo(
                path = uriString,
                name = name,
                nameLowercase = name.lowercase(),
                extension = if (dotIndex > 0) name.substring(dotIndex + 1) else "",
                extensionLowercase = if (dotIndex > 0) name.substring(dotIndex + 1).lowercase() else "",
                isDirectory = doc.isDirectory,
                isHidden = name.startsWith('.'),
                size = doc.length(),
                lastModified = doc.lastModified(),
                parentPath = doc.parentFile?.uri?.toString(),
                isSymbolicLink = false,
                itemCount = if (doc.isDirectory) doc.listFiles().size else -1
            )
        } catch (_: Exception) { null }
    }

    private fun copyDocumentFile(sourceDoc: DocumentFile, destDoc: DocumentFile) {
        val mimeType = sourceDoc.type ?: "*/*"
        val targetFile = when {
            destDoc.isDirectory -> {
                destDoc.createFile(mimeType, sourceDoc.name ?: "copied")
                    ?: throw IllegalStateException("Failed to create destination file")
            }
            else -> destDoc
        }

        context.contentResolver.openInputStream(sourceDoc.uri)?.use { input ->
            context.contentResolver.openOutputStream(targetFile.uri)?.use { output ->
                input.copyTo(output)
            }
        }
    }
}
