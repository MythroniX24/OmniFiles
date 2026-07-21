package com.omnilabs.omfiles.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FileInfoTest {

    @Test
    fun fileType_image_returnsCorrectType() {
        val file = FileInfo(
            path = "/test/image.jpg",
            name = "image.jpg",
            extension = "jpg",
            isDirectory = false,
            isHidden = false,
            size = 1024,
            lastModified = 1000,
            parentPath = "/test",
            isSymbolicLink = false
        )
        assertThat(file.fileType).isEqualTo(FileType.IMAGE)
    }

    @Test
    fun fileType_video_returnsCorrectType() {
        val file = FileInfo(
            path = "/test/video.mp4",
            name = "video.mp4",
            extension = "mp4",
            isDirectory = false,
            isHidden = false,
            size = 1024,
            lastModified = 1000,
            parentPath = "/test",
            isSymbolicLink = false
        )
        assertThat(file.fileType).isEqualTo(FileType.VIDEO)
    }

    @Test
    fun fileType_audio_returnsCorrectType() {
        val file = FileInfo(
            path = "/test/song.mp3",
            name = "song.mp3",
            extension = "mp3",
            isDirectory = false,
            isHidden = false,
            size = 1024,
            lastModified = 1000,
            parentPath = "/test",
            isSymbolicLink = false
        )
        assertThat(file.fileType).isEqualTo(FileType.AUDIO)
    }

    @Test
    fun fileType_archive_returnsCorrectType() {
        val file = FileInfo(
            path = "/test/archive.zip",
            name = "archive.zip",
            extension = "zip",
            isDirectory = false,
            isHidden = false,
            size = 1024,
            lastModified = 1000,
            parentPath = "/test",
            isSymbolicLink = false
        )
        assertThat(file.fileType).isEqualTo(FileType.ARCHIVE)
    }

    @Test
    fun fileType_apk_returnsCorrectType() {
        val file = FileInfo(
            path = "/test/app.apk",
            name = "app.apk",
            extension = "apk",
            isDirectory = false,
            isHidden = false,
            size = 1024,
            lastModified = 1000,
            parentPath = "/test",
            isSymbolicLink = false
        )
        assertThat(file.fileType).isEqualTo(FileType.APK)
    }

    @Test
    fun fileType_document_returnsCorrectType() {
        val file = FileInfo(
            path = "/test/doc.pdf",
            name = "doc.pdf",
            extension = "pdf",
            isDirectory = false,
            isHidden = false,
            size = 1024,
            lastModified = 1000,
            parentPath = "/test",
            isSymbolicLink = false
        )
        assertThat(file.fileType).isEqualTo(FileType.DOCUMENT)
    }

    @Test
    fun fileType_folder_returnsCorrectType() {
        val file = FileInfo(
            path = "/test/folder",
            name = "folder",
            extension = "",
            isDirectory = true,
            isHidden = false,
            size = 0,
            lastModified = 1000,
            parentPath = "/test",
            isSymbolicLink = false
        )
        assertThat(file.fileType).isEqualTo(FileType.FOLDER)
    }

    @Test
    fun mimeType_knownExtension_returnsCorrectMime() {
        val file = FileInfo(
            path = "/test/image.png",
            name = "image.png",
            extension = "png",
            isDirectory = false,
            isHidden = false,
            size = 1024,
            lastModified = 1000,
            parentPath = "/test",
            isSymbolicLink = false
        )
        assertThat(file.mimeType).isEqualTo("image/*")
    }

    @Test
    fun mimeType_unknownExtension_returnsWildcard() {
        val file = FileInfo(
            path = "/test/file.xyz",
            name = "file.xyz",
            extension = "xyz",
            isDirectory = false,
            isHidden = false,
            size = 1024,
            lastModified = 1000,
            parentPath = "/test",
            isSymbolicLink = false
        )
        assertThat(file.mimeType).isEqualTo("*/*")
    }
}
