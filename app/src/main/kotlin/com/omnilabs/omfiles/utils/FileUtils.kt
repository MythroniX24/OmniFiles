package com.omnilabs.omfiles.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File

object FileUtils {

    fun getFileExtension(name: String): String {
        val dotIndex = name.lastIndexOf('.')
        return if (dotIndex > 0) name.substring(dotIndex + 1).lowercase() else ""
    }

    fun getMimeType(extension: String): String {
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
            ?: when (extension.lowercase()) {
                "apk" -> "application/vnd.android.package-archive"
                "zip", "7z", "rar", "tar", "gz" -> "application/x-compressed"
                else -> "*/*"
            }
    }

    fun openFile(context: Context, file: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val mimeType = getMimeType(file.extension)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun shareFile(context: Context, file: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val mimeType = getMimeType(file.extension)
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun getStorageDirectories(): List<File> {
        val dirs = mutableListOf<File>()

        // Internal storage
        Environment.getExternalStorageDirectory()?.let { dirs.add(it) }

        // External SD cards
        try {
            Environment.getExternalStorageDirectory().parentFile?.listFiles()?.forEach { dir ->
                if (dir.isDirectory && dir.canRead() && dir !in dirs) {
                    dirs.add(dir)
                }
            }
        } catch (_: Exception) { }

        return dirs
    }

    fun countItemsInDirectory(path: String): Int {
        return try {
            File(path).listFiles()?.size ?: 0
        } catch (_: Exception) {
            0
        }
    }

    fun getDirectorySize(path: String): Long {
        return try {
            File(path).walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } catch (_: Exception) {
            0L
        }
    }

    fun isPathAccessible(path: String): Boolean {
        return try {
            val file = File(path)
            file.exists() && file.canRead()
        } catch (_: Exception) {
            false
        }
    }

    fun getRelativePath(fullPath: String, basePath: String): String {
        return try {
            fullPath.removePrefix(basePath).trimStart('/')
        } catch (_: Exception) {
            fullPath
        }
    }
}
