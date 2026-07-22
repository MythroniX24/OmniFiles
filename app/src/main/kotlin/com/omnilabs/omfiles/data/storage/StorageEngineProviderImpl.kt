package com.omnilabs.omfiles.data.storage

import com.omnilabs.omfiles.data.storage.engine.LocalStorageEngine
import com.omnilabs.omfiles.data.storage.engine.MediaStoreStorageEngine
import com.omnilabs.omfiles.data.storage.engine.SafStorageEngine
import com.omnilabs.omfiles.domain.storage.StorageEngine
import com.omnilabs.omfiles.domain.storage.StorageEngineProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Routes file operations to the appropriate [StorageEngine] based on the path.
 *
 * - Local paths (e.g. /storage/emulated/0/...) -> [LocalStorageEngine]
 * - MediaStore content URIs (content://media/...) -> [MediaStoreStorageEngine]
 * - Other content:// URIs and removable storage paths -> [SafStorageEngine]
 */
@Singleton
class StorageEngineProviderImpl @Inject constructor(
    private val localStorageEngine: LocalStorageEngine,
    private val safStorageEngine: SafStorageEngine,
    private val mediaStoreStorageEngine: MediaStoreStorageEngine
) : StorageEngineProvider {

    override fun getEngineForPath(path: String): StorageEngine {
        return when {
            path.startsWith("content://media/") -> mediaStoreStorageEngine
            path.startsWith("content://") -> safStorageEngine
            isRemovableStoragePath(path) -> safStorageEngine
            else -> localStorageEngine
        }
    }

    override fun getLocalEngine(): StorageEngine = localStorageEngine

    /**
     * Heuristic to detect paths on removable storage that may require SAF on Android 11+.
     * Matches paths like /storage/XXXX-XXXX/... used by SD cards and common USB OTG paths.
     */
    private fun isRemovableStoragePath(path: String): Boolean {
        val lowerPath = path.lowercase()
        if (lowerPath.startsWith("/storage/emulated/")) return false

        // SD card UUID pattern: /storage/XXXX-XXXX/
        if (Regex("^/storage/[0-9A-F]{4}-[0-9A-F]{4}/.*", RegexOption.IGNORE_CASE).matches(path)) return true

        // Common USB OTG / removable mount points
        val removableRoots = listOf(
            "/storage/usb",
            "/mnt/media_rw/usb",
            "/mnt/usb",
            "/storage/removable",
            "/mnt/sdcard"
        )
        return removableRoots.any { lowerPath.startsWith(it.lowercase()) }
    }
}
