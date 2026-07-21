package com.omnilabs.omfiles.ui.components

import com.google.common.truth.Truth.assertThat
import com.omnilabs.omfiles.domain.model.FileInfo
import com.omnilabs.omfiles.domain.model.FileType
import org.junit.Before
import org.junit.Test

class DragDropStateTest {

    private lateinit var dragDropState: DragDropState
    private lateinit var folderInfo: FileInfo
    private lateinit var fileInfo: FileInfo
    private lateinit var draggedFile: FileInfo

    @Before
    fun setUp() {
        dragDropState = DragDropState()

        draggedFile = FileInfo(
            path = "/storage/test/document.txt",
            name = "document.txt",
            extension = "txt",
            isDirectory = false,
            isHidden = false,
            size = 1024,
            lastModified = 1000,
            parentPath = "/storage/test",
            isSymbolicLink = false
        )

        folderInfo = FileInfo(
            path = "/storage/test/target",
            name = "target",
            extension = "",
            isDirectory = true,
            isHidden = false,
            size = 0,
            lastModified = 1000,
            parentPath = "/storage/test",
            isSymbolicLink = false
        )

        fileInfo = FileInfo(
            path = "/storage/test/other.txt",
            name = "other.txt",
            extension = "txt",
            isDirectory = false,
            isHidden = false,
            size = 2048,
            lastModified = 1000,
            parentPath = "/storage/test",
            isSymbolicLink = false
        )
    }

    @Test
    fun initialState_isNotDragging() {
        assertThat(dragDropState.isDragging).isFalse()
        assertThat(dragDropState.draggedFile).isNull()
        assertThat(dragDropState.dropTargetFile).isNull()
        assertThat(dragDropState.dragOffsetY).isEqualTo(0f)
    }

    @Test
    fun startDrag_setsDraggingState() {
        dragDropState.startDrag(draggedFile, itemScreenTop = 100f, touchOffsetY = 20f)

        assertThat(dragDropState.isDragging).isTrue()
        assertThat(dragDropState.draggedFile).isEqualTo(draggedFile)
        assertThat(dragDropState.dragOffsetY).isEqualTo(0f)
        assertThat(dragDropState.dropTargetFile).isNull()
    }

    @Test
    fun updateDrag_accumulatesOffset() {
        dragDropState.startDrag(draggedFile, itemScreenTop = 100f, touchOffsetY = 20f)

        dragDropState.updateDrag(50f)
        assertThat(dragDropState.dragOffsetY).isEqualTo(50f)

        dragDropState.updateDrag(30f)
        assertThat(dragDropState.dragOffsetY).isEqualTo(80f)

        dragDropState.updateDrag(-20f)
        assertThat(dragDropState.dragOffsetY).isEqualTo(60f)
    }

    @Test
    fun fingerScreenY_calculatesCorrectPosition() {
        dragDropState.startDrag(draggedFile, itemScreenTop = 100f, touchOffsetY = 20f)
        // fingerY = itemScreenTop + touchOffsetY + dragOffsetY = 100 + 20 + 0 = 120
        assertThat(dragDropState.fingerScreenY).isEqualTo(120f)

        dragDropState.updateDrag(50f)
        // fingerY = 100 + 20 + 50 = 170
        assertThat(dragDropState.fingerScreenY).isEqualTo(170f)
    }

    @Test
    fun dropTarget_detectsFolderWithinBounds() {
        dragDropState.startDrag(draggedFile, itemScreenTop = 100f, touchOffsetY = 20f)

        // Register folder at screen position 200-250
        dragDropState.registerFileInfo(folderInfo)
        dragDropState.registerFileBounds(folderInfo.path, top = 200f, bottom = 250f)

        // Move finger Y to 220 (inside folder bounds)
        dragDropState.updateDrag(100f) // dragOffsetY = 100, fingerY = 100 + 20 + 100 = 220

        assertThat(dragDropState.dropTargetFile).isEqualTo(folderInfo)
        assertThat(dragDropState.dropTargetPath).isEqualTo(folderInfo.path)
    }

    @Test
    fun dropTarget_ignoresOwnPath() {
        dragDropState.startDrag(draggedFile, itemScreenTop = 100f, touchOffsetY = 20f)

        // Register dragged file path as a folder (same path)
        val samePathFolder = FileInfo(
            path = draggedFile.path,
            name = draggedFile.name,
            extension = draggedFile.extension,
            isDirectory = true,
            isHidden = false,
            size = 0,
            lastModified = 1000,
            parentPath = draggedFile.parentPath,
            isSymbolicLink = false
        )
        dragDropState.registerFileInfo(samePathFolder)
        dragDropState.registerFileBounds(samePathFolder.path, top = 200f, bottom = 250f)

        // Move finger to same position
        dragDropState.updateDrag(100f) // fingerY = 220

        // Should not detect own path as drop target
        assertThat(dragDropState.dropTargetFile).isNull()
    }

    @Test
    fun dropTarget_ignoresNonDirectoryFiles() {
        dragDropState.startDrag(draggedFile, itemScreenTop = 100f, touchOffsetY = 20f)

        // Register a non-directory file
        dragDropState.registerFileInfo(fileInfo)
        dragDropState.registerFileBounds(fileInfo.path, top = 200f, bottom = 250f)

        // Move finger to file position
        dragDropState.updateDrag(100f) // fingerY = 220

        // Should NOT detect non-directory as drop target
        assertThat(dragDropState.dropTargetFile).isNull()
    }

    @Test
    fun dropTarget_noMatch_whenFingerOutsideAllBounds() {
        dragDropState.startDrag(draggedFile, itemScreenTop = 100f, touchOffsetY = 20f)

        dragDropState.registerFileInfo(folderInfo)
        dragDropState.registerFileBounds(folderInfo.path, top = 200f, bottom = 250f)

        // Move finger to position outside any bounds
        dragDropState.updateDrag(300f) // fingerY = 420

        assertThat(dragDropState.dropTargetFile).isNull()
    }

    @Test
    fun updateDrag_noOp_whenNotDragging() {
        // Without calling startDrag
        dragDropState.updateDrag(50f)

        assertThat(dragDropState.dragOffsetY).isEqualTo(0f)
        assertThat(dragDropState.isDragging).isFalse()
    }

    @Test
    fun endDrag_returnsTargetPathAndResets() {
        dragDropState.startDrag(draggedFile, itemScreenTop = 100f, touchOffsetY = 20f)

        dragDropState.registerFileInfo(folderInfo)
        dragDropState.registerFileBounds(folderInfo.path, top = 200f, bottom = 250f)

        // Move finger to folder bounds
        dragDropState.updateDrag(100f) // fingerY = 220

        val result = dragDropState.endDrag()

        assertThat(result).isEqualTo(folderInfo.path)
        assertThat(dragDropState.isDragging).isFalse()
        assertThat(dragDropState.draggedFile).isNull()
        assertThat(dragDropState.dropTargetFile).isNull()
    }

    @Test
    fun endDrag_returnsNull_whenNoTarget() {
        dragDropState.startDrag(draggedFile, itemScreenTop = 100f, touchOffsetY = 20f)

        // No folder registered, move somewhere
        dragDropState.updateDrag(50f)

        val result = dragDropState.endDrag()

        assertThat(result).isNull()
        assertThat(dragDropState.isDragging).isFalse()
    }

    @Test
    fun cancelDrag_resetsAllState() {
        dragDropState.startDrag(draggedFile, itemScreenTop = 100f, touchOffsetY = 20f)
        dragDropState.updateDrag(50f)

        dragDropState.cancelDrag()

        assertThat(dragDropState.isDragging).isFalse()
        assertThat(dragDropState.draggedFile).isNull()
        assertThat(dragDropState.dragOffsetY).isEqualTo(0f)
        assertThat(dragDropState.dropTargetFile).isNull()
    }

    @Test
    fun clearFileBounds_removesExpectedFile() {
        dragDropState.startDrag(draggedFile, itemScreenTop = 100f, touchOffsetY = 20f)
        dragDropState.registerFileInfo(folderInfo)
        dragDropState.registerFileBounds(folderInfo.path, top = 200f, bottom = 250f)

        // Call clear and re-register different file
        dragDropState.clear()
        val otherFolder = FileInfo(
            path = "/other/folder",
            name = "folder",
            extension = "",
            isDirectory = true,
            isHidden = false,
            size = 0,
            lastModified = 1000,
            parentPath = "/other",
            isSymbolicLink = false
        )
        dragDropState.registerFileInfo(otherFolder)
        dragDropState.registerFileBounds(otherFolder.path, top = 300f, bottom = 350f)

        dragDropState.updateDrag(200f) // fingerY = 320

        assertThat(dragDropState.dropTargetPath).isEqualTo(otherFolder.path)
    }
}
