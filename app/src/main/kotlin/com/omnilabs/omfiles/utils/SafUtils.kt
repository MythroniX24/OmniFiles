package com.omnilabs.omfiles.utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Storage Access Framework utility for Android 11+ (Scoped Storage).
 * Provides file operations that work with both legacy java.io.File and SAF URIs.
 */
object SafUtils {

    /**
     * Check if MANAGE_EXTERNAL_STORAGE permission is granted (Android 11+).
     */
    fun hasManageStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true // Pre-Android 11, regular storage permissions suffice
        }
    }

    /**
     * Read a file's content to a byte array. Works with SAF URIs.
     */
    fun readFileBytes(context: Context, uri: Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Write bytes to a file via SAF URI.
     */
    fun writeFile(context: Context, uri: Uri, data: ByteArray): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(data)
                true
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get DocumentFile from a File path, handling SAF where needed.
     */
    fun getDocumentFile(context: Context, file: File): DocumentFile? {
        return try {
            DocumentFile.fromFile(file)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * List files in a directory using java.io.File (works with full storage access).
     */
    fun listFiles(file: File): List<File> {
        return try {
            file.listFiles()?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Copy a file using streams (works with both SAF and legacy).
     */
    fun copyFile(context: Context, source: File, destinationDir: File, newName: String): Boolean {
        return try {
            val destFile = File(destinationDir, newName)
            FileInputStream(source).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Delete a file or directory recursively.
     */
    fun deleteRecursively(file: File): Boolean {
        return try {
            if (file.isDirectory) {
                file.listFiles()?.forEach { child ->
                    deleteRecursively(child)
                }
            }
            file.delete()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get the root path for internal storage.
     */
    fun getInternalStoragePath(): String {
        return Environment.getExternalStorageDirectory().absolutePath
    }

    /**
     * Check if a path is on the internal storage.
     */
    fun isOnInternalStorage(path: String): Boolean {
        val internalPath = getInternalStoragePath()
        return path.startsWith(internalPath)
    }

    /**
     * Get the URI for a file for use with FileProvider.
     */
    fun getFileUri(context: Context, file: File): Uri {
        return androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * List all available storage volumes.
     * Uses StorageManager on Android 11+ for proper volume detection.
     */
    fun getStorageVolumes(context: Context): List<StorageVolume> {
        val volumes = mutableListOf<StorageVolume>()

        // Internal storage
        val internalPath = getInternalStoragePath()
        try {
            val stat = android.os.StatFs(internalPath)
            val total = stat.totalBytes
            val free = stat.availableBytes
            volumes.add(
                StorageVolume(
                    path = internalPath,
                    label = "Internal Storage",
                    isPrimary = true,
                    isRemovable = false,
                    totalSpace = total,
                    freeSpace = free
                )
            )
        } catch (_: Exception) { }

        // External storage volumes (SD cards, USB OTG)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val storageManager = context.getSystemService(Context.STORAGE_SERVICE)
                    as android.os.storage.StorageManager
                val storageVolumes = storageManager.storageVolumes

                storageVolumes.forEach { volume ->
                    val dir = volume.directory
                    if (dir != null && dir.absolutePath != internalPath && dir.exists()) {
                        try {
                            val stat = android.os.StatFs(dir.absolutePath)
                            val total = stat.totalBytes
                            val free = stat.availableBytes
                            val label = when {
                                volume.isRemovable -> if (volume.isPrimary) "Internal Storage" else "SD Card"
                                else -> "USB OTG"
                            }
                            volumes.add(
                                StorageVolume(
                                    path = dir.absolutePath,
                                    label = label,
                                    isPrimary = volume.isPrimary,
                                    isRemovable = volume.isRemovable,
                                    totalSpace = total,
                                    freeSpace = free,
                                    uuid = volume.uuid
                                )
                            )
                        } catch (_: Exception) { }
                    }
                }
            } catch (_: Exception) { }
        }

        return volumes
    }

    data class StorageVolume(
        val path: String,
        val label: String,
        val isPrimary: Boolean,
        val isRemovable: Boolean,
        val totalSpace: Long,
        val freeSpace: Long,
        val uuid: String? = null
    ) {
        val usedSpace: Long get() = totalSpace - freeSpace
        val usedPercentage: Float
            get() = if (totalSpace > 0) (usedSpace.toFloat() / totalSpace.toFloat()) * 100f else 0f
    }
}
