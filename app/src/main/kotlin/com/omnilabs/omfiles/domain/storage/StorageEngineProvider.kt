package com.omnilabs.omfiles.domain.storage

/**
 * Provides the appropriate [StorageEngine] for a given file path or URI.
 * This allows the repository to remain storage-agnostic while supporting
 * local files, SAF URIs, and MediaStore content URIs transparently.
 */
interface StorageEngineProvider {

    /**
     * Returns the storage engine responsible for the given [path].
     * The path may be a file system path (e.g. /storage/emulated/0/...)
     * or a content URI (e.g. content://...).
     */
    fun getEngineForPath(path: String): StorageEngine

    /**
     * Returns the default engine for local file system access.
     */
    fun getLocalEngine(): StorageEngine
}
