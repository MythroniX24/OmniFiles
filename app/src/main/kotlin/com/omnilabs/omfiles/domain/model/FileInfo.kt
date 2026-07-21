package com.omnilabs.omfiles.domain.model

import java.io.File

data class FileInfo(
    val path: String,
    val name: String,
    val extension: String,
    val isDirectory: Boolean,
    val isHidden: Boolean,
    val size: Long,
    val lastModified: Long,
    val parentPath: String?,
    val isSymbolicLink: Boolean
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
        fun fromFile(file: File): FileInfo {
            val name = file.name
            val dotIndex = name.lastIndexOf('.')
            return FileInfo(
                path = file.absolutePath,
                name = name,
                extension = if (dotIndex > 0) name.substring(dotIndex + 1) else "",
                isDirectory = file.isDirectory,
                isHidden = file.isHidden,
                size = if (file.isFile) file.length() else 0L,
                lastModified = file.lastModified(),
                parentPath = file.parent,
                isSymbolicLink = try { java.nio.file.Files.isSymbolicLink(file.toPath()) } catch (e: Exception) { false }
            )
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
