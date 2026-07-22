package com.omnilabs.omfiles.preview

import android.webkit.MimeTypeMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lightweight MIME type and preview category registry.
 * Falls back to Android's MimeTypeMap and a built-in dictionary.
 */
@Singleton
class MimeTypeRegistry @Inject constructor() {

    fun getMimeType(extension: String): String {
        val ext = extension.lowercase().trimStart('.')
        if (ext.isEmpty()) return "application/octet-stream"

        MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(ext)
            ?.let { return it }

        return EXTENSION_TO_MIME[ext] ?: "application/octet-stream"
    }

    fun detectPreviewType(extension: String, mimeType: String): PreviewType {
        val ext = extension.lowercase().trimStart('.')
        return when {
            ext in IMAGE_EXTENSIONS -> PreviewType.IMAGE
            ext in VIDEO_EXTENSIONS -> PreviewType.VIDEO
            ext in AUDIO_EXTENSIONS -> PreviewType.AUDIO
            ext in CODE_EXTENSIONS || ext in PLAIN_TEXT_EXTENSIONS -> PreviewType.TEXT
            ext == "pdf" -> PreviewType.PDF
            ext in ARCHIVE_EXTENSIONS -> PreviewType.ARCHIVE
            ext == "apk" -> PreviewType.APK
            ext in FONT_EXTENSIONS -> PreviewType.FONT
            mimeType.startsWith("image/") -> PreviewType.IMAGE
            mimeType.startsWith("video/") -> PreviewType.VIDEO
            mimeType.startsWith("audio/") -> PreviewType.AUDIO
            mimeType.startsWith("text/") -> PreviewType.TEXT
            else -> PreviewType.UNKNOWN
        }
    }

    companion object {
        val IMAGE_EXTENSIONS = setOf(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "heic", "heif",
            "tif", "tiff", "ico", "raw", "cr2", "nef", "dng"
        )

        val VIDEO_EXTENSIONS = setOf(
            "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "3gp", "mpeg",
            "mpg", "m4v", "ts", "mts"
        )

        val AUDIO_EXTENSIONS = setOf(
            "mp3", "wav", "aac", "flac", "ogg", "wma", "m4a", "opus", "wma",
            "aiff", "ape", "mka"
        )

        val DOCUMENT_EXTENSIONS = setOf(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods",
            "odp", "rtf"
        )

        val PLAIN_TEXT_EXTENSIONS = setOf(
            "txt", "md", "log", "csv"
        )

        val CODE_EXTENSIONS = setOf(
            "kt", "java", "cpp", "c", "h", "hpp", "py", "js", "ts", "html",
            "css", "xml", "json", "yaml", "yml", "sql", "sh", "php", "go",
            "rs", "swift", "vb", "cs", "pl", "rb", "lua", "dart", "jsx",
            "tsx", "scss", "sass", "less", "bat", "ps1", "toml", "ini",
            "properties", "gradle", "kts"
        )

        val ARCHIVE_EXTENSIONS = setOf(
            "zip", "rar", "7z", "tar", "gz", "gzip", "bz2", "xz", "iso",
            "cab", "lzma", "tgz", "tbz2", "txz"
        )

        val FONT_EXTENSIONS = setOf("ttf", "otf", "woff", "woff2")

        val EBOOK_EXTENSIONS = setOf("epub", "mobi", "azw", "azw3")

        private val EXTENSION_TO_MIME = mapOf(
            "apk" to "application/vnd.android.package-archive",
            "7z" to "application/x-7z-compressed",
            "zip" to "application/zip",
            "tar" to "application/x-tar",
            "gz" to "application/gzip",
            "bz2" to "application/x-bzip2",
            "xz" to "application/x-xz",
            "rar" to "application/vnd.rar",
            "md" to "text/markdown",
            "json" to "application/json",
            "xml" to "application/xml",
            "js" to "application/javascript",
            "kt" to "text/x-kotlin",
            "cpp" to "text/x-c++src",
            "h" to "text/x-c++hdr",
            "py" to "text/x-python",
            "yaml" to "application/x-yaml",
            "yml" to "application/x-yaml"
        )
    }
}
