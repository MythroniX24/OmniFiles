package com.omnilabs.omfiles.data.storage.engine

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.omnilabs.omfiles.domain.model.FileInfo
import com.omnilabs.omfiles.domain.model.OperationResult
import com.omnilabs.omfiles.domain.storage.StorageEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MediaStore backed storage engine for read-only access to media collections
 * (Images, Videos, Audio) via content://media/... URIs.
 *
 * Note: MediaStore does not support general file operations such as create,
 * delete, rename, or copy. Those will return an descriptive error.
 */
@Singleton
class MediaStoreStorageEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : StorageEngine {

    override suspend fun getFiles(path: String, showHidden: Boolean): List<FileInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val collectionUri = collectionUriFromPath(path) ?: return@withContext emptyList()
                queryMediaStore(collectionUri)
            } catch (_: Exception) { emptyList() }
        }
    }

    override suspend fun getFileInfo(path: String): FileInfo? {
        return withContext(Dispatchers.IO) {
            try {
                if (!path.startsWith("content://media/")) return@withContext null
                val uri = Uri.parse(path)
                querySingleMedia(uri)
            } catch (_: Exception) { null }
        }
    }

    override suspend fun copyFile(source: String, destination: String): OperationResult<Unit> {
        return OperationResult.Error("Copy is not supported through MediaStore. Use local or SAF storage instead.")
    }

    override suspend fun moveFile(source: String, destination: String): OperationResult<Unit> {
        return OperationResult.Error("Move is not supported through MediaStore. Use local or SAF storage instead.")
    }

    override suspend fun deleteFile(path: String): OperationResult<Unit> {
        return OperationResult.Error("Delete through MediaStore is not supported. Use local or SAF storage instead.")
    }

    override suspend fun renameFile(path: String, newName: String): OperationResult<Unit> {
        return OperationResult.Error("Rename through MediaStore is not supported. Use local or SAF storage instead.")
    }

    override suspend fun createFolder(parentPath: String, folderName: String): OperationResult<FileInfo> {
        return OperationResult.Error("Creating folders through MediaStore is not supported.")
    }

    override suspend fun createFile(parentPath: String, fileName: String): OperationResult<FileInfo> {
        return OperationResult.Error("Creating files through MediaStore is not supported.")
    }

    override suspend fun exists(path: String): Boolean {
        return getFileInfo(path) != null
    }

    override suspend fun getFileSize(path: String): Long {
        return getFileInfo(path)?.size ?: 0L
    }

    override suspend fun getFileCount(path: String): Int {
        return withContext(Dispatchers.IO) {
            try {
                val collectionUri = collectionUriFromPath(path) ?: return@withContext 0
                context.contentResolver.query(collectionUri, null, null, null, null)?.use { cursor ->
                    cursor.count
                } ?: 0
            } catch (_: Exception) { 0 }
        }
    }

    override suspend fun getStorageUsage(path: String): Pair<Long, Long> {
        return Pair(0L, 0L)
    }

    private fun collectionUriFromPath(path: String): Uri? {
        return when {
            path.contains("image", ignoreCase = true) -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            path.contains("video", ignoreCase = true) -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            path.contains("audio", ignoreCase = true) || path.contains("music", ignoreCase = true) ->
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            else -> MediaStore.Files.getContentUri("external")
        }
    }

    private fun queryMediaStore(collectionUri: Uri): List<FileInfo> {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.DATA
        )

        val results = mutableListOf<FileInfo>()
        context.contentResolver.query(collectionUri, projection, null, null, null)?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val dateIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            val dataIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)

            while (cursor.moveToNext()) {
                try {
                    val id = cursor.getLong(idIndex)
                    val name = cursor.getString(nameIndex) ?: continue
                    val size = cursor.getLong(sizeIndex)
                    val lastModified = cursor.getLong(dateIndex) * 1000L
                    val dataPath = cursor.getString(dataIndex) ?: collectionUri.toString()
                    val dotIndex = name.lastIndexOf('.')

                    results.add(
                        FileInfo(
                            path = dataPath,
                            name = name,
                            nameLowercase = name.lowercase(),
                            extension = if (dotIndex > 0) name.substring(dotIndex + 1) else "",
                            extensionLowercase = if (dotIndex > 0) name.substring(dotIndex + 1).lowercase() else "",
                            isDirectory = false,
                            isHidden = name.startsWith('.'),
                            size = size,
                            lastModified = lastModified,
                            parentPath = java.io.File(dataPath).parent,
                            isSymbolicLink = false,
                            itemCount = -1
                        )
                    )
                } catch (_: Exception) { }
            }
        }
        return results
    }

    private fun querySingleMedia(uri: Uri): FileInfo? {
        val projection = arrayOf(
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.DATA
        )

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return null
            try {
                val name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME))
                val size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE))
                val lastModified = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)) * 1000L
                val dataPath = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA)) ?: uri.toString()
                val dotIndex = name.lastIndexOf('.')
                return FileInfo(
                    path = dataPath,
                    name = name,
                    nameLowercase = name.lowercase(),
                    extension = if (dotIndex > 0) name.substring(dotIndex + 1) else "",
                    extensionLowercase = if (dotIndex > 0) name.substring(dotIndex + 1).lowercase() else "",
                    isDirectory = false,
                    isHidden = name.startsWith('.'),
                    size = size,
                    lastModified = lastModified,
                    parentPath = java.io.File(dataPath).parent,
                    isSymbolicLink = false,
                    itemCount = -1
                )
            } catch (_: Exception) { return null }
        }
        return null
    }
}
