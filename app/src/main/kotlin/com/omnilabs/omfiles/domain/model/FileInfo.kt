package com.omnilabs.omfiles.domain.model

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File

data class FileInfo(
    val path: String,
    val name: String,
    val nameLowercase: String = name.lowercase(), // pre-computed for fast sorting
    val extension: String,
    val extensionLowercase: String = extension.lowercase(), // pre-computed for fast sorting
    val isDirectory: Boolean,
    val isHidden: Boolean,
    val size: Long,
    val lastModified: Long,
    val parentPath: String?,
    val isSymbolicLink: Boolean,
    val itemCount: Int = -1 // cached item count for directories (-1 = unknown)
) {
    val isFile: Boolean get() = !isDirectory

    val mimeType: String
        get() = when (extension.lowercase()) {
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg" -> "image/*"
            "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm" -> "video/*"
            "mp3", "wav", "aac", "flac", "ogg", "wma", "m4a" -> "audio/*"
            "pdf" -> "application/pdf"
            "zip", "7z", "tar", "gz", "rar" -> "application/archive"
            "apk" -> "application/vnd.android.package-archive"
            "txt", "md", "json", "xml", "csv", "log" -> "text/plain"
            "html", "htm", "css", "js" -> "text/html"
            "doc", "docx" -> "application/msword"
            "xls", "xlsx" -> "application/vnd.ms-excel"
            "ppt", "pptx" -> "application/vnd.ms-powerpoint"
            else -> "*/*"
        }

    val fileType: FileType
        get() = when {
            isDirectory -> FileType.FOLDER
            extension.lowercase() in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg") -> FileType.IMAGE
            extension.lowercase() in listOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm") -> FileType.VIDEO
            extension.lowercase() in listOf("mp3", "wav", "aac", "flac", "ogg", "wma", "m4a") -> FileType.AUDIO
            extension.lowercase() in listOf("zip", "7z", "tar", "gz", "rar") -> FileType.ARCHIVE
            extension.lowercase() == "apk" -> FileType.APK
            extension.lowercase() in listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt") -> FileType.DOCUMENT
            else -> FileType.OTHER
        }

    companion object {

        fun fromDocumentFile(doc: DocumentFile): FileInfo {
            val name = doc.name ?: ""
            val dotIndex = name.lastIndexOf('.')
            val uriString = doc.uri.toString()
            return FileInfo(
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
        }

        fun fromUri(context: Context, uri: Uri): FileInfo? {
            return try {
                val doc = DocumentFile.fromSingleUri(context, uri)
                doc?.let { fromDocumentFile(it) }
            } catch (_: Exception) { null }
        }

        fun fromFile(file: File): FileInfo {
            val name = file.name
            val dotIndex = name.lastIndexOf('.')
            return FileInfo(
                path = file.absolutePath,
                name = name,
                nameLowercase = name.lowercase(),
                extension = if (dotIndex > 0) name.substring(dotIndex + 1) else "",
                extensionLowercase = if (dotIndex > 0) name.substring(dotIndex + 1).lowercase() else "",
                isDirectory = file.isDirectory,
                isHidden = file.isHidden,
                size = if (file.isFile) file.length() else 0L,
                lastModified = file.lastModified(),
                parentPath = file.parent,
                isSymbolicLink = try { java.nio.file.Files.isSymbolicLink(file.toPath()) } catch (e: Exception) { false },
                itemCount = if (file.isDirectory) file.listFiles()?.size ?: 0 else -1
            )
        }

        /**
         * Fast factory using java.nio.file.Files.readAttributes to get all
         * file metadata in a single syscall per file (instead of 5+ syscalls
         * with the File-based approach).
         */
        fun fromNioPath(filePath: String, itemCount: Int = -1): FileInfo? {
            return try {
                val pathObj = java.nio.file.Paths.get(filePath)
                val attrs = java.nio.file.Files.readAttributes(
                    pathObj, java.nio.file.attribute.BasicFileAttributes::class.java
                )
                val name = pathObj.fileName.toString()
                val dotIndex = name.lastIndexOf('.')
                FileInfo(
                    path = filePath,
                    name = name,
                    nameLowercase = name.lowercase(),
                    extension = if (dotIndex > 0) name.substring(dotIndex + 1) else "",
                    extensionLowercase = if (dotIndex > 0) name.substring(dotIndex + 1).lowercase() else "",
                    isDirectory = attrs.isDirectory,
                    isHidden = name.startsWith('.'),
                    size = if (attrs.isRegularFile) attrs.size() else 0L,
                    lastModified = attrs.lastModifiedTime().toMillis(),
                    parentPath = pathObj.parent.toString(),
                    isSymbolicLink = attrs.isSymbolicLink,
                    itemCount = itemCount
                )
            } catch (e: Exception) {
                null
            }
        }

        /**
         * Batch read attributes for all files in a directory using DirectoryStream.
         * Uses NIO for optimal performance: avoids allocating File[] array,
         * uses single syscall per file via readAttributes.
         */
        fun listDirectoryFast(
            dirPath: String,
            showHidden: Boolean = false,
            includeCounts: Boolean = true
        ): List<FileInfo> {
            val results = mutableListOf<FileInfo>()
            val dir = java.io.File(dirPath)
            if (!dir.exists() || !dir.isDirectory) return emptyList()

            try {
                java.nio.file.Files.newDirectoryStream(dir.toPath()).use { stream ->
                    for (path in stream) {
                        if (!showHidden && path.fileName.toString().startsWith('.')) continue
                        val filePath = path.toString()
                        val attrs = java.nio.file.Files.readAttributes(
                            path, java.nio.file.attribute.BasicFileAttributes::class.java
                        )
                        val name = path.fileName.toString()
                        val dotIndex = name.lastIndexOf('.')

                        // Count directory items in batch to reduce overhead
                        val count = if (includeCounts && attrs.isDirectory) {
                            try { java.nio.file.Files.newDirectoryStream(path).use { s -> s.count() } }
                            catch (_: Exception) { 0 }
                        } else -1

                        results.add(FileInfo(
                            path = filePath,
                            name = name,
                            nameLowercase = name.lowercase(),
                            extension = if (dotIndex > 0) name.substring(dotIndex + 1) else "",
                            extensionLowercase = if (dotIndex > 0) name.substring(dotIndex + 1).lowercase() else "",
                            isDirectory = attrs.isDirectory,
                            isHidden = name.startsWith('.'),
                            size = if (attrs.isRegularFile) attrs.size() else 0L,
                            lastModified = attrs.lastModifiedTime().toMillis(),
                            parentPath = dirPath,
                            isSymbolicLink = attrs.isSymbolicLink,
                            itemCount = count
                        ))
                    }
                }
            } catch (_: Exception) { }

            return results
        }
    }
}

enum class FileType {
    FOLDER,
    IMAGE,
    VIDEO,
    AUDIO,
    ARCHIVE,
    APK,
    DOCUMENT,
    OTHER
}
