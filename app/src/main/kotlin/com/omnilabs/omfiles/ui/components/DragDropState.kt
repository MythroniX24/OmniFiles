package com.omnilabs.omfiles.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import com.omnilabs.omfiles.domain.model.FileInfo

/**
 * Represents the current state of a drag-and-drop operation in the file browser.
 * Tracks the dragged item, finger position, and the current drop target folder.
 *
 * Usage:
 * ```
 * val dragDropState = rememberDragDropState()
 * ```
 */
class DragDropState {

    /** Whether a drag operation is currently in progress */
    var isDragging by mutableStateOf(false)
        private set

    /** The file currently being dragged */
    var draggedFile by mutableStateOf<FileInfo?>(null)
        private set

    /** Current cumulative drag offset in pixels (relative to drag start position) */
    var dragOffsetY by mutableFloatStateOf(0f)
        private set

    /** The folder currently being hovered as a drop target (null = no target) */
    var dropTargetFile by mutableStateOf<FileInfo?>(null)
        private set

    /** The screen Y position (in root coordinates) where the drag started */
    private var dragStartScreenY = 0f

    /** The touch Y offset within the dragged item (finger position relative to item top) */
    private var touchOffsetY = 0f

    /** Map of file paths to their screen bounds for hit testing */
    private val fileBoundsMap = mutableMapOf<String, FileBounds>()

    /** Map of file paths to their FileInfo for lookups */
    private val fileInfoMap = mutableMapOf<String, FileInfo>()

    /**
     * Start a drag operation for the given file.
     * @param file The file being dragged
     * @param itemScreenTop The top Y position of the item in root coordinates
     * @param touchOffsetY The Y offset within the item where the touch occurred
     */
    fun startDrag(file: FileInfo, itemScreenTop: Float, touchOffsetY: Float) {
        isDragging = true
        draggedFile = file
        this.dragStartScreenY = itemScreenTop
        this.touchOffsetY = touchOffsetY
        this.dragOffsetY = 0f
        dropTargetFile = null
    }

    /**
     * Update the drag position and detect which folder is being hovered.
     * @param deltaY The Y-axis movement delta since the last update (in pixels)
     */
    fun updateDrag(deltaY: Float) {
        if (!isDragging) return
        dragOffsetY += deltaY

        // Calculate the finger Y position in root coordinates
        val fingerY = dragStartScreenY + touchOffsetY + dragOffsetY

        // Find which folder is under the finger
        dropTargetFile = null
        val draggedPath = draggedFile?.path
        for ((path, bounds) in fileBoundsMap) {
            if (fingerY in bounds.top..bounds.bottom) {
                val info = fileInfoMap[path]
                // Only allow dropping on directories (not the dragged file itself)
                if (info?.isDirectory == true && path != draggedPath) {
                    dropTargetFile = info
                }
                break
            }
        }
    }

    /**
     * End the drag operation.
     * @return The path of the drop target folder, or null if dropped outside a valid target
     */
    fun endDrag(): String? {
        val target = dropTargetFile
        val result = if (target != null && target.isDirectory) target.path else null
        reset()
        return result
    }

    /**
     * Cancel the drag operation without dropping.
     */
    fun cancelDrag() {
        reset()
    }

    /**
     * Register a file item's screen position for hit testing.
     */
    fun registerFileBounds(path: String, top: Float, bottom: Float) {
        fileBoundsMap[path] = FileBounds(top, bottom)
    }

    /**
     * Register a FileInfo for path-based lookups.
     */
    fun registerFileInfo(fileInfo: FileInfo) {
        fileInfoMap[fileInfo.path] = fileInfo
    }

    /**
     * Clear all registered file bounds and info.
     */
    fun clear() {
        fileBoundsMap.clear()
        fileInfoMap.clear()
    }

    /**
     * Get the current drop target file path.
     */
    val dropTargetPath: String?
        get() = dropTargetFile?.path

    /**
     * Get the current finger Y position in root coordinates.
     */
    val fingerScreenY: Float
        get() = dragStartScreenY + touchOffsetY + dragOffsetY

    private fun reset() {
        isDragging = false
        draggedFile = null
        dragOffsetY = 0f
        dropTargetFile = null
        fileBoundsMap.clear()
        fileInfoMap.clear()
    }

    private data class FileBounds(val top: Float, val bottom: Float)
}

/**
 * Create a remembered DragDropState instance.
 */
@Composable
fun rememberDragDropState(): DragDropState {
    return androidx.compose.runtime.remember { DragDropState() }
}
