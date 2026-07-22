package com.omnilabs.omfiles.preview

import com.omnilabs.omfiles.domain.model.FileInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreviewRegistry @Inject constructor(
    private val mimeRegistry: MimeTypeRegistry,
    private val signatureDetector: FileSignatureDetector
) {

    data class PreviewEntry(
        val type: PreviewType,
        val mimeType: String,
        val actualExtension: String,
        val canOpenInternally: Boolean
    )

    fun resolve(fileInfo: FileInfo): PreviewEntry {
        val ext = fileInfo.extension.lowercase()
        val signature = signatureDetector.detect(java.io.File(fileInfo.path))

        val resolvedMime = signature?.mimeType ?: mimeRegistry.getMimeType(ext)
        val resolvedExt = signature?.extension ?: ext

        val type = when {
            ext in MimeTypeRegistry.CODE_EXTENSIONS -> PreviewType.CODE
            else -> mimeRegistry.detectPreviewType(resolvedExt, resolvedMime)
        }

        return PreviewEntry(
            type = type,
            mimeType = resolvedMime,
            actualExtension = resolvedExt,
            canOpenInternally = type in INTERNAL_PREVIEW_TYPES
        )
    }

    companion object {
        private val INTERNAL_PREVIEW_TYPES = setOf(
            PreviewType.IMAGE,
            PreviewType.TEXT,
            PreviewType.CODE,
            PreviewType.AUDIO,
            PreviewType.VIDEO,
            PreviewType.ARCHIVE,
            PreviewType.APK,
            PreviewType.PDF,
            PreviewType.UNKNOWN
        )
    }
}
