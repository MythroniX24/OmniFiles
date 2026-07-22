package com.omnilabs.omfiles.native_lister

import android.util.Log
import com.omnilabs.omfiles.domain.model.FileInfo
import java.io.File

/**
 * JNI bridge to the native C++ file lister.
 *
 * Uses a threshold-based approach: for small directories (< THRESHOLD),
 * falls back to the standard Java File API since JNI overhead outweighs
 * benefits for small listings. For large directories, uses the native
 * C++ implementation which is significantly faster.
 */
object NativeFileLister {

    private const val TAG = "NativeFileLister"
    private const val THRESHOLD = 500 // Use native for directories with 500+ files

    private var nativeAvailable: Boolean = false

    init {
        try {
            System.loadLibrary("native_lister")
            nativeAvailable = true
            Log.i(TAG, "Native library loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            nativeAvailable = false
            Log.w(TAG, "Native library not available, using Java fallback: ${e.message}")
        }
    }

    /**
     * List directory contents, using native C++ for large directories
     * and Java fallback for small ones.
     *
     * @param dirPath Absolute path to directory
     * @param showHidden Whether to include hidden files
     * @return List of FileInfo objects
     */
    fun listDirectory(dirPath: String, showHidden: Boolean = false): List<FileInfo> {
        if (!nativeAvailable) {
            return listDirectoryJava(dirPath, showHidden)
        }

        // Quick count to decide strategy
        val count = getDirectoryCountNative(dirPath)
        if (count < 0) return emptyList()

        if (count < THRESHOLD) {
            // Small directory: use Java (JNI overhead not worth it)
            return listDirectoryJava(dirPath, showHidden)
        }

        // Large directory: use native
        return try {
            listDirectoryViaNative(dirPath, showHidden)
        } catch (e: Exception) {
            Log.e(TAG, "Native listing failed, falling back to Java: ${e.message}")
            listDirectoryJava(dirPath, showHidden)
        }
    }

    /**
     * Calls the JNI native method and unpacks the parallel arrays into FileInfo objects.
     */
    private fun listDirectoryViaNative(dirPath: String, showHidden: Boolean): List<FileInfo> {
        val result = listDirectoryNative(dirPath, showHidden)

        val paths = result[0] as Array<*>
        val names = result[1] as Array<*>
        val sizes = result[2] as LongArray
        val isDirs = result[3] as BooleanArray
        val lastMods = result[4] as LongArray
        val isHiddens = result[5] as BooleanArray

        val count = paths.size
        val fileList = java.util.ArrayList<FileInfo>(count)

        for (i in 0 until count) {
            val name = names[i] as String
            val path = paths[i] as String
            val dotIndex = name.lastIndexOf('.')
            fileList.add(FileInfo(
                path = path,
                name = name,
                extension = if (dotIndex > 0) name.substring(dotIndex + 1) else "",
                isDirectory = isDirs[i],
                isHidden = isHiddens[i],
                size = sizes[i],
                lastModified = lastMods[i],
                parentPath = dirPath,
                isSymbolicLink = false, // stat() resolves symlinks; use lstat() if needed
                itemCount = -1
            ))
        }

        return fileList
    }

    /**
     * Java fallback using standard File API for small directories.
     */
    private fun listDirectoryJava(dirPath: String, showHidden: Boolean): List<FileInfo> {
        val dir = File(dirPath)
        if (!dir.exists() || !dir.isDirectory) return emptyList()

        val files = dir.listFiles() ?: return emptyList()
        val results = java.util.ArrayList<FileInfo>(files.size)

        for (file in files) {
            if (!showHidden && file.isHidden) continue
            results.add(FileInfo.fromFile(file))
        }

        return results
    }

    // ── JNI native methods ────────────────────────────────────────────────

    /**
     * Native directory listing using C++ dirent.h + stat().
     * Returns [0]String[], [1]String[], [2]long[], [3]boolean[], [4]long[], [5]boolean[]
     */
    private external fun listDirectoryNative(dirPath: String, showHidden: Boolean): Array<Any>

    /**
     * Quickly count entries in a directory using C++ readdir().
     */
    private external fun getDirectoryCountNative(dirPath: String): Int
}
