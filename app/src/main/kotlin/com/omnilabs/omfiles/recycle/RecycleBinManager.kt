package com.omnilabs.omfiles.recycle

import android.content.Context
import com.omnilabs.omfiles.data.local.dao.RecycleBinDao
import com.omnilabs.omfiles.data.local.entity.RecycleBinEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the Recycle Bin - a trash folder where deleted files are temporarily stored
 * before permanent deletion or restoration.
 */
@Singleton
class RecycleBinManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recycleBinDao: RecycleBinDao
) {

    companion object {
        private const val TRASH_FOLDER_NAME = ".omnifiles_trash"
        const val DEFAULT_MAX_AGE_DAYS = 30
    }

    private val trashDir: File
        get() {
            val dir = File(context.filesDir, TRASH_FOLDER_NAME)
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    /**
     * Get the trash directory path.
     */
    fun getTrashPath(): String = trashDir.absolutePath

    /**
     * Move a file or directory to the trash.
     * Returns the trash path on success.
     */
    fun moveToTrash(sourcePath: String): Result<String> {
        return try {
            val sourceFile = File(sourcePath)
            if (!sourceFile.exists()) {
                return Result.failure(Exception("File does not exist: $sourcePath"))
            }

            // Generate unique name in trash to avoid conflicts
            val uniqueSuffix = UUID.randomUUID().toString().take(8)
            val trashName = "${sourceFile.name}.$uniqueSuffix"
            val trashFile = File(trashDir, trashName)
            val success = if (sourceFile.isDirectory) {
                sourceFile.renameTo(trashFile) || copyAndDeleteDir(sourceFile, trashFile)
            } else {
                sourceFile.renameTo(trashFile) || copyAndDeleteFile(sourceFile, trashFile)
            }

            if (success) {
                Result.success(trashFile.absolutePath)
            } else {
                Result.failure(Exception("Failed to move file to trash"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Restore a file from trash to its original location.
     * Returns the restored path on success.
     */
    fun restoreFromTrash(entity: RecycleBinEntity): Result<String> {
        return try {
            val trashFile = File(entity.trashPath)
            if (!trashFile.exists()) {
                return Result.failure(Exception("Trash file not found"))
            }

            val originalFile = File(entity.originalPath)
            val parentDir = originalFile.parentFile

            // Create parent directories if needed
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs()
            }

            // If original location has a file with same name, append (restored)
            val restoreTarget = if (originalFile.exists()) {
                val baseName = originalFile.nameWithoutExtension
                val ext = originalFile.extension
                val newName = if (ext.isNotEmpty()) "${baseName}_restored.$ext" else "${baseName}_restored"
                File(parentDir, newName)
            } else {
                originalFile
            }

            val success = trashFile.renameTo(restoreTarget) ||
                    copyAndDeleteFile(trashFile, restoreTarget)

            if (success) {
                Result.success(restoreTarget.absolutePath)
            } else {
                Result.failure(Exception("Failed to restore file"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Permanently delete a file from trash.
     */
    fun permanentlyDelete(trashPath: String): Boolean {
        return try {
            val file = File(trashPath)
            if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Empty the entire trash.
     */
    fun emptyTrash(): Int {
        var count = 0
        trashDir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                if (file.deleteRecursively()) count++
            } else {
                if (file.delete()) count++
            }
        }
        return count
    }

    /**
     * Get total size of all files in trash.
     */
    fun getTrashSize(): Long {
        return trashDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    /**
     * Get number of items in trash.
     */
    fun getTrashCount(): Int {
        return trashDir.listFiles()?.size ?: 0
    }

    private fun copyAndDeleteFile(source: File, dest: File): Boolean {
        return try {
            dest.parentFile?.mkdirs()
            source.inputStream().use { input ->
                dest.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            source.delete()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun copyAndDeleteDir(source: File, dest: File): Boolean {
        return try {
            dest.mkdirs()
            source.listFiles()?.forEach { child ->
                val destChild = File(dest, child.name)
                if (child.isDirectory) {
                    copyAndDeleteDir(child, destChild)
                } else {
                    child.inputStream().use { input ->
                        destChild.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
            source.deleteRecursively()
            true
        } catch (e: Exception) {
            false
        }
    }
}
