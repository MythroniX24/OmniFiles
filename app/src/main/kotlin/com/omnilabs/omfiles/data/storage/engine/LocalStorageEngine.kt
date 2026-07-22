package com.omnilabs.omfiles.data.storage.engine

import android.os.StatFs
import com.omnilabs.omfiles.domain.model.FileInfo
import com.omnilabs.omfiles.domain.model.OperationResult
import com.omnilabs.omfiles.domain.storage.StorageEngine
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local file system storage engine backed by java.io.File and NIO.
 * Optimized for internal storage and mounted volumes accessible via file paths.
 */
@Singleton
class LocalStorageEngine @Inject constructor() : StorageEngine {

    override suspend fun getFiles(path: String, showHidden: Boolean): List<FileInfo> {
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) return emptyList()

        val results = mutableListOf<FileInfo>()
        try {
            Files.newDirectoryStream(dir.toPath()).use { stream ->
                for (filePath in stream) {
                    val name = filePath.fileName.toString()
                    if (!showHidden && name.startsWith('.')) continue
                    fileInfoFromPath(filePath.toString(), dir.absolutePath)?.let { results.add(it) }
                }
            }
        } catch (_: Exception) { }
        return results
    }

    override suspend fun getFileInfo(path: String): FileInfo? {
        return fileInfoFromPath(path, File(path).parent ?: "")
    }

    override suspend fun copyFile(source: String, destination: String): OperationResult<Unit> {
        return try {
            val sourceFile = File(source)
            val destFile = File(destination)

            if (!sourceFile.exists()) {
                return OperationResult.Error("Source file does not exist")
            }

            if (sourceFile.isDirectory) {
                copyDirectory(sourceFile, destFile)
            } else {
                copyFileChannel(sourceFile, destFile)
            }

            OperationResult.Success(Unit)
        } catch (e: Exception) {
            OperationResult.Error("Copy failed: ${e.message}", e)
        }
    }

    override suspend fun moveFile(source: String, destination: String): OperationResult<Unit> {
        return try {
            val sourceFile = File(source)
            val destFile = File(destination)

            if (!sourceFile.exists()) {
                return OperationResult.Error("Source file does not exist")
            }

            if (sourceFile.renameTo(destFile)) {
                OperationResult.Success(Unit)
            } else {
                val copyResult = copyFile(source, destination)
                if (copyResult is OperationResult.Success) {
                    sourceFile.deleteRecursively()
                    OperationResult.Success(Unit)
                } else {
                    copyResult
                }
            }
        } catch (e: Exception) {
            OperationResult.Error("Move failed: ${e.message}", e)
        }
    }

    override suspend fun deleteFile(path: String): OperationResult<Unit> {
        return try {
            val file = File(path)
            if (!file.exists()) {
                return OperationResult.Error("File does not exist")
            }
            val deleted = if (file.isDirectory) file.deleteRecursively() else file.delete()
            if (deleted) OperationResult.Success(Unit) else OperationResult.Error("Failed to delete file")
        } catch (e: Exception) {
            OperationResult.Error("Delete failed: ${e.message}", e)
        }
    }

    override suspend fun renameFile(path: String, newName: String): OperationResult<Unit> {
        return try {
            val file = File(path)
            val parent = file.parentFile ?: return OperationResult.Error("No parent directory")
            val newFile = File(parent, newName)

            if (newFile.exists()) {
                return OperationResult.Error("A file with this name already exists")
            }

            if (file.renameTo(newFile)) {
                OperationResult.Success(Unit)
            } else {
                OperationResult.Error("Failed to rename file")
            }
        } catch (e: Exception) {
            OperationResult.Error("Rename failed: ${e.message}", e)
        }
    }

    override suspend fun createFolder(parentPath: String, folderName: String): OperationResult<FileInfo> {
        return try {
            val folder = File(parentPath, folderName)
            if (folder.exists()) {
                return OperationResult.Error("Folder already exists")
            }
            if (folder.mkdirs()) {
                OperationResult.Success(FileInfo.fromFile(folder))
            } else {
                OperationResult.Error("Failed to create folder")
            }
        } catch (e: Exception) {
            OperationResult.Error("Create folder failed: ${e.message}", e)
        }
    }

    override suspend fun createFile(parentPath: String, fileName: String): OperationResult<FileInfo> {
        return try {
            val file = File(parentPath, fileName)
            if (file.exists()) {
                return OperationResult.Error("File already exists")
            }
            if (file.createNewFile()) {
                OperationResult.Success(FileInfo.fromFile(file))
            } else {
                OperationResult.Error("Failed to create file")
            }
        } catch (e: Exception) {
            OperationResult.Error("Create file failed: ${e.message}", e)
        }
    }

    override suspend fun exists(path: String): Boolean {
        return File(path).exists()
    }

    override suspend fun getFileSize(path: String): Long {
        return try {
            val p = Paths.get(path)
            val attrs = Files.readAttributes(p, BasicFileAttributes::class.java)
            if (attrs.isDirectory) {
                var total = 0L
                Files.walk(p).use { walk ->
                    for (f in walk) {
                        try {
                            val fa = Files.readAttributes(f, BasicFileAttributes::class.java)
                            if (fa.isRegularFile) total += fa.size()
                        } catch (_: Exception) { }
                    }
                }
                total
            } else {
                attrs.size()
            }
        } catch (_: Exception) { 0L }
    }

    override suspend fun getFileCount(path: String): Int {
        return try {
            var count = 0
            Files.newDirectoryStream(Paths.get(path)).use { stream ->
                val iter = stream.iterator()
                while (iter.hasNext()) { iter.next(); count++ }
            }
            count
        } catch (_: Exception) { 0 }
    }

    override suspend fun getStorageUsage(path: String): Pair<Long, Long> {
        return try {
            val stat = StatFs(path)
            val total = stat.totalBytes
            val free = stat.availableBytes
            Pair(total - free, total)
        } catch (e: Exception) {
            Pair(0L, 0L)
        }
    }

    private fun fileInfoFromPath(path: String, parentPath: String): FileInfo? {
        return try {
            val attrs = Files.readAttributes(Paths.get(path), BasicFileAttributes::class.java)
            val file = File(path)
            val name = file.name
            val dotIndex = name.lastIndexOf('.')
            FileInfo(
                path = path,
                name = name,
                nameLowercase = name.lowercase(),
                extension = if (dotIndex > 0) name.substring(dotIndex + 1) else "",
                extensionLowercase = if (dotIndex > 0) name.substring(dotIndex + 1).lowercase() else "",
                isDirectory = attrs.isDirectory,
                isHidden = name.startsWith('.'),
                size = if (attrs.isRegularFile) attrs.size() else 0L,
                lastModified = attrs.lastModifiedTime().toMillis(),
                parentPath = parentPath,
                isSymbolicLink = attrs.isSymbolicLink,
                itemCount = -1
            )
        } catch (_: Exception) { null }
    }

    private fun copyFileChannel(source: File, dest: File) {
        dest.parentFile?.mkdirs()
        FileInputStream(source).use { input ->
            FileOutputStream(dest).use { output ->
                input.channel.transferTo(0, source.length(), output.channel)
            }
        }
    }

    private fun copyDirectory(source: File, dest: File) {
        dest.mkdirs()
        source.listFiles()?.forEach { child ->
            val destChild = File(dest, child.name)
            if (child.isDirectory) {
                copyDirectory(child, destChild)
            } else {
                copyFileChannel(child, destChild)
            }
        }
    }
}
