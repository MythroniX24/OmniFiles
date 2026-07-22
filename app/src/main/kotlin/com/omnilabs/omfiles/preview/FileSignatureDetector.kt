package com.omnilabs.omfiles.preview

import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detects real file type by reading magic numbers/signatures from the first bytes.
 * This catches files with wrong or missing extensions.
 */
@Singleton
class FileSignatureDetector @Inject constructor() {

    data class SignatureResult(
        val mimeType: String,
        val extension: String,
        val description: String
    )

    fun detect(file: File): SignatureResult? {
        return detect(file.inputStream().use { it.readNBytes(MAX_HEADER_BYTES) })
    }

    fun detect(header: ByteArray): SignatureResult? {
        if (header.isEmpty()) return null
        for ((signature, result) in SIGNATURES) {
            if (header.size >= signature.size && header.copyOf(signature.size).contentEquals(signature)) {
                return result
            }
        }
        return null
    }

    companion object {
        private const val MAX_HEADER_BYTES = 16

        private fun hexToBytes(hex: String): ByteArray =
            hex.split(" ").map { it.toInt(16).toByte() }.toByteArray()

        private val SIGNATURES = listOf(
            // Images
            "89 50 4E 47 0D 0A 1A 0A" to SignatureResult("image/png", "png", "PNG image"),
            "FF D8 FF" to SignatureResult("image/jpeg", "jpg", "JPEG image"),
            "47 49 46 38" to SignatureResult("image/gif", "gif", "GIF image"),
            "42 4D" to SignatureResult("image/bmp", "bmp", "BMP image"),
            "57 45 42 50" to SignatureResult("image/webp", "webp", "WebP image"),
            "49 49 2A 00" to SignatureResult("image/tiff", "tiff", "TIFF image"),
            "4D 4D 00 2A" to SignatureResult("image/tiff", "tiff", "TIFF image"),
            // Archives
            "50 4B 03 04" to SignatureResult("application/zip", "zip", "ZIP archive"),
            "50 4B 05 06" to SignatureResult("application/zip", "zip", "ZIP archive"),
            "50 4B 07 08" to SignatureResult("application/zip", "zip", "ZIP archive"),
            "37 7A BC AF 27 1C" to SignatureResult("application/x-7z-compressed", "7z", "7-Zip archive"),
            "52 61 72 21 1A 07 00" to SignatureResult("application/vnd.rar", "rar", "RAR archive"),
            "1F 8B 08" to SignatureResult("application/gzip", "gz", "GZIP archive"),
            "75 73 74 61 72" to SignatureResult("application/x-tar", "tar", "TAR archive"),
            "FD 37 7A 58 5A 00" to SignatureResult("application/x-xz", "xz", "XZ archive"),
            // Documents
            "25 50 44 46 2D" to SignatureResult("application/pdf", "pdf", "PDF document"),
            // Audio/Video
            "49 44 33" to SignatureResult("audio/mpeg", "mp3", "MP3 audio"),
            "FF F1" to SignatureResult("audio/aac", "aac", "AAC audio"),
            "FF F9" to SignatureResult("audio/aac", "aac", "AAC audio"),
            "52 49 46 46" to SignatureResult("video/avi", "avi", "AVI video"),
        ).map { hexToBytes(it.first) to it.second }
    }
}
