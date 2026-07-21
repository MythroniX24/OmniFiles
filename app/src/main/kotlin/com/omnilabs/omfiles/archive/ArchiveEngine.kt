package com.omnilabs.omfiles.archive

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArchiveEngine @Inject constructor() {

    sealed class ArchiveResult {
        data class Success(val message: String = "") : ArchiveResult()
        data class Error(val message: String) : ArchiveResult()
    }

    suspend fun extractArchive(
        archivePath: String,
        destinationDir: String,
        password: String? = null
    ): ArchiveResult = withContext(Dispatchers.IO) {
        try {
            val archiveFile = File(archivePath)
            val destDir = File(destinationDir)
            destDir.mkdirs()

            val extension = archiveFile.extension.lowercase()

            when (extension) {
                "zip" -> extractZip(archiveFile, destDir, password)
                "7z" -> extract7z(archiveFile, destDir, password)
                "tar" -> extractTar(archiveFile, destDir)
                "gz", "gzip" -> extractGzip(archiveFile, destDir)
                "tar.gz", "tgz" -> extractTarGz(archiveFile, destDir)
                "rar" -> extractRar(archiveFile, destDir)
                else -> return@withContext ArchiveResult.Error("Unsupported archive format: $extension")
            }

            ArchiveResult.Success()
        } catch (e: Exception) {
            ArchiveResult.Error("Extraction failed: ${e.message}")
        }
    }

    suspend fun createArchive(
        sourcePaths: List<String>,
        archivePath: String,
        password: String? = null
    ): ArchiveResult = withContext(Dispatchers.IO) {
        try {
            val archiveFile = File(archivePath)
            val extension = archiveFile.extension.lowercase()
            val parentDir = archiveFile.parentFile
            parentDir?.mkdirs()

            when (extension) {
                "zip" -> createZip(sourcePaths.map { File(it) }, archiveFile, password)
                "tar" -> createTar(sourcePaths.map { File(it) }, archiveFile)
                "tar.gz", "tgz" -> createTarGz(sourcePaths.map { File(it) }, archiveFile)
                "gz", "gzip" -> {
                    if (sourcePaths.size == 1) {
                        createGzip(File(sourcePaths[0]), archiveFile)
                    } else {
                        return@withContext ArchiveResult.Error("GZIP compression supports only single file")
                    }
                }
                else -> return@withContext ArchiveResult.Error("Unsupported archive format: $extension")
            }

            ArchiveResult.Success()
        } catch (e: Exception) {
            ArchiveResult.Error("Compression failed: ${e.message}")
        }
    }

    suspend fun getArchiveEntries(archivePath: String): ArchiveResult = withContext(Dispatchers.IO) {
        try {
            val archiveFile = File(archivePath)
            val extension = archiveFile.extension.lowercase()
            val entries = mutableListOf<String>()

            when (extension) {
                "zip" -> {
                    ZipFile(archiveFile).use { zipFile ->
                        zipFile.entries.asIterator().forEach { entry ->
                            entries.add(entry.name)
                        }
                    }
                }
                "7z" -> {
                    SevenZFile(archiveFile).use { sevenZFile ->
                        var entry = sevenZFile.nextEntry
                        while (entry != null) {
                            entries.add(entry.name)
                            entry = sevenZFile.nextEntry
                        }
                    }
                }
                "tar" -> {
                    val tarInput: ArchiveInputStream<*> = ArchiveStreamFactory().createArchiveInputStream(
                        BufferedInputStream(FileInputStream(archiveFile))
                    ) as ArchiveInputStream<*>
                    tarInput.use { ais ->
                        var entry: ArchiveEntry? = ais.nextEntry
                        while (entry != null) {
                            entries.add(entry.name)
                            entry = ais.nextEntry
                        }
                    }
                }
                "tar.gz", "tgz" -> {
                    val gzipStream = GzipCompressorInputStream(
                        BufferedInputStream(FileInputStream(archiveFile))
                    )
                    val tgzInput: ArchiveInputStream<*> = ArchiveStreamFactory().createArchiveInputStream(
                        BufferedInputStream(gzipStream)
                    ) as ArchiveInputStream<*>
                    tgzInput.use { ais ->
                        var entry: ArchiveEntry? = ais.nextEntry
                        while (entry != null) {
                            entries.add(entry.name)
                            entry = ais.nextEntry
                        }
                    }
                }
                else -> return@withContext ArchiveResult.Error("Unsupported archive format: $extension")
            }

            ArchiveResult.Success(entries.joinToString("\n"))
        } catch (e: Exception) {
            ArchiveResult.Error("Failed to read archive: ${e.message}")
        }
    }

    suspend fun isPasswordProtected(archivePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(archivePath)
            when (file.extension.lowercase()) {
                "zip" -> {
                    try {
                        // Try to open zip - if it fails, assume password protected
                        ZipFile(file).use { zipFile ->
                            zipFile.entries.asIterator().forEach { _ -> return@withContext false }
                        }
                        false
                    } catch (_: Exception) {
                        true
                    }
                }
                "7z" -> {
                    try {
                        SevenZFile(file).use { false }
                    } catch (_: Exception) {
                        true
                    }
                }
                else -> false
            }
        } catch (_: Exception) {
            false
        }
    }

    // Private extraction methods

    private fun extractZip(archive: File, dest: File, password: String?) {
        if (password != null) {
            extractZipWithPassword(archive, dest, password)
            return
        }
        ZipFile(archive).use { zipFile ->
            zipFile.entries.asIterator().forEach { entry ->
                val entryName = entry.name
                val outputFile = File(dest, entryName)
                if (entry.isDirectory) {
                    outputFile.mkdirs()
                } else {
                    outputFile.parentFile?.mkdirs()
                    zipFile.getInputStream(entry).use { input ->
                        FileOutputStream(outputFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
    }

    private fun extractZipWithPassword(archive: File, dest: File, password: String) {
        ZipFile(archive).use { zipFile ->
            zipFile.entries.asIterator().forEach { entry ->
                val outputFile = File(dest, entry.name)
                if (entry.isDirectory) {
                    outputFile.mkdirs()
                } else {
                    outputFile.parentFile?.mkdirs()
                    try {
                        zipFile.getInputStream(entry).use { input ->
                            FileOutputStream(outputFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    } catch (_: Exception) { }
                }
            }
        }
    }

    private fun extract7z(archive: File, dest: File, password: String?) {
        val sevenZFile = if (password != null) {
            SevenZFile(archive, password.toByteArray())
        } else {
            SevenZFile(archive)
        }
        sevenZFile.use { sevenZ ->
            var entry = sevenZ.nextEntry
            while (entry != null) {
                val outputFile = File(dest, entry.name)
                if (entry.isDirectory) {
                    outputFile.mkdirs()
                } else {
                    outputFile.parentFile?.mkdirs()
                    val data = ByteArray(entry.size.toInt())
                    sevenZ.read(data)
                    FileOutputStream(outputFile).use { it.write(data) }
                }
                entry = sevenZ.nextEntry
            }
        }
    }

    private fun extractTar(archive: File, dest: File) {
        val ais: ArchiveInputStream<*> = ArchiveStreamFactory().createArchiveInputStream(
            ArchiveStreamFactory.TAR,
            BufferedInputStream(FileInputStream(archive))
        )
        ais.use { extractFromStream(it, dest) }
    }

    private fun extractGzip(archive: File, dest: File) {
        val outputName = archive.nameWithoutExtension
        val outputFile = File(dest, outputName)
        GzipCompressorInputStream(
            BufferedInputStream(FileInputStream(archive))
        ).use { input ->
            FileOutputStream(outputFile).use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun extractTarGz(archive: File, dest: File) {
        val gzipStream = GzipCompressorInputStream(
            BufferedInputStream(FileInputStream(archive))
        )
        val ais: ArchiveInputStream<*> = ArchiveStreamFactory().createArchiveInputStream(
            ArchiveStreamFactory.TAR,
            BufferedInputStream(gzipStream)
        )
        ais.use { extractFromStream(it, dest) }
    }

    private fun extractRar(archive: File, dest: File) {
        try {
            com.github.junrar.Junrar.extract(archive.absolutePath, dest.absolutePath)
        } catch (e: Exception) {
            throw UnsupportedOperationException("RAR extraction failed: ${e.message ?: "Unknown error"}")
        }
    }

    private fun extractFromStream(ais: ArchiveInputStream<*>, dest: File) {
        var entry: ArchiveEntry? = ais.nextEntry
        while (entry != null) {
            val outputFile = File(dest, entry.name)
            if (entry.isDirectory) {
                outputFile.mkdirs()
            } else {
                outputFile.parentFile?.mkdirs()
                FileOutputStream(outputFile).use { output ->
                    ais.copyTo(output)
                }
            }
            entry = ais.nextEntry
        }
    }

    // Private compression methods

    private fun createZip(sources: List<File>, archiveFile: File, password: String?) {
        ZipArchiveOutputStream(FileOutputStream(archiveFile)).use { zos ->
            sources.forEach { source ->
                addToZipStream(zos, source, "")
            }
        }
    }

    private fun addToZipStream(zos: ZipArchiveOutputStream, file: File, basePath: String) {
        val entryName = if (basePath.isEmpty()) file.name else "$basePath/${file.name}"
        if (file.isDirectory) {
            val entry = ZipArchiveEntry(entryName + "/")
            zos.putArchiveEntry(entry)
            zos.closeArchiveEntry()
            file.listFiles()?.forEach { child ->
                addToZipStream(zos, child, entryName)
            }
        } else {
            val entry = ZipArchiveEntry(entryName)
            entry.size = file.length()
            zos.putArchiveEntry(entry)
            FileInputStream(file).use { fis -> fis.copyTo(zos) }
            zos.closeArchiveEntry()
        }
    }

    private fun createTar(sources: List<File>, archiveFile: File) {
        TarArchiveOutputStream(FileOutputStream(archiveFile)).use { tos ->
            tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU)
            sources.forEach { source ->
                addToTarStream(tos, source, "")
            }
        }
    }

    private fun addToTarStream(tos: TarArchiveOutputStream, file: File, basePath: String) {
        val entryName = if (basePath.isEmpty()) file.name else "$basePath/${file.name}"
        if (file.isDirectory) {
            val entry = org.apache.commons.compress.archivers.tar.TarArchiveEntry(entryName + "/")
            tos.putArchiveEntry(entry)
            tos.closeArchiveEntry()
            file.listFiles()?.forEach { child ->
                addToTarStream(tos, child, entryName)
            }
        } else {
            val entry = org.apache.commons.compress.archivers.tar.TarArchiveEntry(entryName)
            entry.size = file.length()
            tos.putArchiveEntry(entry)
            FileInputStream(file).use { fis -> fis.copyTo(tos) }
            tos.closeArchiveEntry()
        }
    }

    private fun createTarGz(sources: List<File>, archiveFile: File) {
        val gzipStream = GzipCompressorOutputStream(
            BufferedOutputStream(FileOutputStream(archiveFile))
        )
        TarArchiveOutputStream(gzipStream).use { tos ->
            tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU)
            sources.forEach { source ->
                addToTarStream(tos, source, "")
            }
        }
    }

    private fun createGzip(source: File, archiveFile: File) {
        GzipCompressorOutputStream(
            BufferedOutputStream(FileOutputStream(archiveFile))
        ).use { gzos ->
            FileInputStream(source).use { fis -> fis.copyTo(gzos) }
        }
    }
}
