package com.omnilabs.omfiles.ui.screens.files

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnilabs.omfiles.domain.model.FileInfo
import com.omnilabs.omfiles.domain.model.FileSortOptions
import com.omnilabs.omfiles.domain.model.OperationResult
import com.omnilabs.omfiles.domain.model.SortMode
import com.omnilabs.omfiles.domain.model.SortOrder
import com.omnilabs.omfiles.domain.repository.ArchiveRepository
import com.omnilabs.omfiles.domain.repository.FileRepository
import com.omnilabs.omfiles.domain.repository.RecentFilesRepository
import com.omnilabs.omfiles.domain.repository.RecycleBinRepository
import com.omnilabs.omfiles.domain.repository.SettingsRepository
import com.omnilabs.omfiles.utils.FileUtils
import com.omnilabs.omfiles.utils.PermissionHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class FilesUiState(
    val currentPath: String = "/",
    val files: List<FileInfo> = emptyList(),
    val selectedFiles: Set<String> = emptySet(),
    val sortOptions: FileSortOptions = FileSortOptions(),
    val showHidden: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectionMode: Boolean = false,
    val currentFolderInfo: FileInfo? = null,
    val hasStoragePermission: Boolean = true,
    val showDeleteConfirmation: Boolean = false,
    val showRenameDialog: Boolean = false,
    val renameTarget: String? = null,
    val showCreateFolderDialog: Boolean = false,
    val showCreateFileDialog: Boolean = false,
    val pendingDeletePaths: Set<String> = emptySet(),
    val showPropertiesDialog: Boolean = false,
    val propertiesTarget: FileInfo? = null,
    val showExtractDialog: Boolean = false,
    val extractTarget: String? = null,
    val operationInProgress: Boolean = false,
    val operationMessage: String? = null,
    // Clipboard for copy/cut → paste flow
    val clipboardPaths: List<String> = emptyList(),
    val clipboardMode: String? = null  // "copy" or "cut"
)

@HiltViewModel
class FilesViewModel @Inject constructor(
    private val fileRepository: FileRepository,
    private val recentFilesRepository: RecentFilesRepository,
    private val archiveRepository: ArchiveRepository,
    private val recycleBinRepository: RecycleBinRepository,
    settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    private val _currentPath = MutableStateFlow("/")
    private val _files = MutableStateFlow<List<FileInfo>>(emptyList())
    private val _sortOptions = MutableStateFlow(FileSortOptions())
    private val _showHidden = MutableStateFlow(false)
    private val _selectedFiles = MutableStateFlow<Set<String>>(emptySet())
    private val _selectionMode = MutableStateFlow(false)
    private val _hasStoragePermission = MutableStateFlow(true)
    private val _showDeleteConfirmation = MutableStateFlow(false)
    private val _showRenameDialog = MutableStateFlow(false)
    private val _renameTarget = MutableStateFlow<String?>(null)
    private val _showCreateFolderDialog = MutableStateFlow(false)
    private val _showCreateFileDialog = MutableStateFlow(false)
    private val _pendingDeletePaths = MutableStateFlow<Set<String>>(emptySet())
    private val _showPropertiesDialog = MutableStateFlow(false)
    private val _propertiesTarget = MutableStateFlow<FileInfo?>(null)
    private val _showExtractDialog = MutableStateFlow(false)
    private val _extractTarget = MutableStateFlow<String?>(null)
    private val _operationInProgress = MutableStateFlow(false)
    private val _operationMessage = MutableStateFlow<String?>(null)
    private val _clipboardPaths = MutableStateFlow<List<String>>(emptyList())
    private val _clipboardMode = MutableStateFlow<String?>(null)

    // Use Iterable-based combine to avoid Kotlin 2.1 flow count limit on combine overloads
    val uiState: StateFlow<FilesUiState> = combine(
        listOf<StateFlow<*>>(
            _currentPath, _files, _sortOptions, _showHidden, _isLoading,
            _selectedFiles, _selectionMode, _hasStoragePermission,
            _showDeleteConfirmation, _showRenameDialog, _renameTarget,
            _showCreateFolderDialog, _showCreateFileDialog, _pendingDeletePaths,
            _showPropertiesDialog, _propertiesTarget, _showExtractDialog, _extractTarget,
            _operationInProgress, _operationMessage, _clipboardPaths, _clipboardMode
        )
    ) { values ->
        FilesUiState(
            currentPath = values[0] as String,
            files = values[1] as List<FileInfo>,
            sortOptions = values[2] as FileSortOptions,
            showHidden = values[3] as Boolean,
            isLoading = values[4] as Boolean,
            selectedFiles = values[5] as Set<String>,
            selectionMode = values[6] as Boolean,
            hasStoragePermission = values[7] as Boolean,
            showDeleteConfirmation = values[8] as Boolean,
            showRenameDialog = values[9] as Boolean,
            renameTarget = values[10] as String?,
            showCreateFolderDialog = values[11] as Boolean,
            showCreateFileDialog = values[12] as Boolean,
            pendingDeletePaths = values[13] as Set<String>,
            showPropertiesDialog = values[14] as Boolean,
            propertiesTarget = values[15] as FileInfo?,
            showExtractDialog = values[16] as Boolean,
            extractTarget = values[17] as String?,
            operationInProgress = values[18] as Boolean,
            operationMessage = values[19] as String?,
            clipboardPaths = values[20] as List<String>,
            clipboardMode = values[21] as String?
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FilesUiState()
    )

    fun navigateToFolder(path: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _currentPath.value = path
            _hasStoragePermission.value = PermissionHandler.hasStoragePermission(context)
            if (_hasStoragePermission.value) {
                loadFiles(path)
            } else {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadFiles(path: String) {
        withContext(Dispatchers.IO) {
            try {
                val sortOptions = FileSortOptions(
                    mode = SortMode.NAME,
                    order = SortOrder.ASCENDING,
                    foldersFirst = true
                )
                _sortOptions.value = sortOptions

                val fileList = fileRepository.getFiles(path, sortOptions, _showHidden.value).first()
                _files.value = fileList
                recentFilesRepository.addRecentFile(path)
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Failed to load files: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun checkPermission() {
        _hasStoragePermission.value = PermissionHandler.hasStoragePermission(context)
        if (_hasStoragePermission.value) refresh()
    }

    init {
        navigateToFolder(_currentPath.value)
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            loadFiles(_currentPath.value)
        }
    }

    fun navigateUp() {
        val current = _currentPath.value
        if (current == "/") return
        val parent = File(current).parent?.takeIf { it.isNotEmpty() } ?: "/"
        navigateToFolder(parent)
    }

    fun toggleSelection(path: String) {
        val current = _selectedFiles.value.toMutableSet()
        if (current.contains(path)) {
            current.remove(path)
            if (current.isEmpty()) exitSelectionMode()
        } else {
            current.add(path)
        }
        _selectedFiles.value = current
        _selectionMode.value = true
    }

    fun enterSelectionMode(path: String) {
        _selectionMode.value = true
        _selectedFiles.value = setOf(path)
    }

    fun exitSelectionMode() {
        _selectionMode.value = false
        _selectedFiles.value = emptySet()
    }

    // ── Copy / Cut / Paste (Clipboard system) ──

    fun copyToClipboard() {
        val paths = _selectedFiles.value.toList()
        if (paths.isEmpty()) return
        _clipboardPaths.value = paths
        _clipboardMode.value = "copy"
        exitSelectionMode()
        _error.value = "${paths.size} item(s) copied to clipboard"
    }

    fun cutToClipboard() {
        val paths = _selectedFiles.value.toList()
        if (paths.isEmpty()) return
        _clipboardPaths.value = paths
        _clipboardMode.value = "cut"
        exitSelectionMode()
        _error.value = "${paths.size} item(s) cut to clipboard — navigate to destination and tap Paste"
    }

    fun pasteFromClipboard() {
        val paths = _clipboardPaths.value
        val mode = _clipboardMode.value
        if (paths.isEmpty() || mode == null) return

        val destFolder = _currentPath.value
        // Don't paste into source folder for copy (no-op) — keep clipboard intact
        val allInSameFolder = paths.all { File(it).parent == destFolder }
        if (allInSameFolder && mode == "copy") {
            _error.value = "Files are already in this folder — navigate elsewhere to paste, or use Duplicate"
            return
        }

        viewModelScope.launch {
            setOperation(if (mode == "cut") "Moving ${paths.size} item(s)…" else "Copying ${paths.size} item(s)…")
            var successCount = 0
            var failCount = 0

            for (path in paths) {
                val fileName = File(path).name
                val destPath = "$destFolder/$fileName"
                val result = if (mode == "cut") {
                    fileRepository.moveFile(path, destPath)
                } else {
                    fileRepository.copyFile(path, destPath)
                }
                when (result) {
                    is OperationResult.Success -> successCount++
                    is OperationResult.Error -> failCount++
                }
            }

            clearClipboard()
            clearOperation()
            loadFiles(_currentPath.value)

            val verb = if (mode == "cut") "Moved" else "Copied"
            if (failCount > 0) {
                _error.value = "$verb $successCount item(s), $failCount failed"
            } else {
                _error.value = "$verb $successCount item(s) successfully"
            }
        }
    }

    fun clearClipboard() {
        _clipboardPaths.value = emptyList()
        _clipboardMode.value = null
    }

    fun hasClipboardContent(): Boolean = _clipboardPaths.value.isNotEmpty()

    // ── Create ──

    fun showCreateFolderDialog() { _showCreateFolderDialog.value = true }
    fun dismissCreateFolderDialog() { _showCreateFolderDialog.value = false }

    fun createFolder(folderName: String) {
        viewModelScope.launch {
            _showCreateFolderDialog.value = false
            setOperation("Creating folder…")
            when (val result = fileRepository.createFolder(_currentPath.value, folderName)) {
                is OperationResult.Success -> {
                    clearOperation()
                    loadFiles(_currentPath.value)
                }
                is OperationResult.Error -> {
                    clearOperation()
                    _error.value = result.message
                }
            }
        }
    }

    fun showCreateFileDialog() { _showCreateFileDialog.value = true }
    fun dismissCreateFileDialog() { _showCreateFileDialog.value = false }

    fun createFile(fileName: String) {
        viewModelScope.launch {
            _showCreateFileDialog.value = false
            setOperation("Creating file…")
            when (val result = fileRepository.createFile(_currentPath.value, fileName)) {
                is OperationResult.Success -> {
                    clearOperation()
                    loadFiles(_currentPath.value)
                }
                is OperationResult.Error -> {
                    clearOperation()
                    _error.value = result.message
                }
            }
        }
    }

    // ── Rename ──

    fun showRenameDialog(path: String) {
        _showRenameDialog.value = true
        _renameTarget.value = path
    }

    fun dismissRenameDialog() {
        _showRenameDialog.value = false
        _renameTarget.value = null
    }

    fun renameFile(path: String, newName: String) {
        viewModelScope.launch {
            _showRenameDialog.value = false
            _renameTarget.value = null
            setOperation("Renaming…")
            when (val result = fileRepository.renameFile(path, newName)) {
                is OperationResult.Success -> {
                    clearOperation()
                    loadFiles(_currentPath.value)
                }
                is OperationResult.Error -> {
                    clearOperation()
                    _error.value = result.message
                }
            }
        }
    }

    // ── Delete ──

    fun showDeleteConfirmation(paths: Set<String>) {
        _pendingDeletePaths.value = paths
        _showDeleteConfirmation.value = true
    }

    fun dismissDeleteConfirmation() {
        _showDeleteConfirmation.value = false
        _pendingDeletePaths.value = emptySet()
    }

    fun confirmDelete() {
        viewModelScope.launch {
            _showDeleteConfirmation.value = false
            val paths = _pendingDeletePaths.value.toList()
            _pendingDeletePaths.value = emptySet()
            setOperation("Moving ${paths.size} item${if (paths.size != 1) "s" else ""} to Recycle Bin…")

            var successCount = 0
            var failCount = 0
            for (path in paths) {
                when (val result = recycleBinRepository.moveToTrash(path)) {
                    is OperationResult.Success -> successCount++
                    is OperationResult.Error -> failCount++
                }
            }

            clearOperation()
            exitSelectionMode()
            loadFiles(_currentPath.value)
            if (failCount > 0) {
                _error.value = "Moved $successCount item(s) to Recycle Bin, $failCount failed"
            } else {
                _error.value = "$successCount item(s) moved to Recycle Bin"
            }
        }
    }

    // ── Open / Share ──

    fun openFile(fileInfo: FileInfo) {
        if (fileInfo.isDirectory) return
        viewModelScope.launch {
            try {
                val file = File(fileInfo.path)
                val intent = FileUtils.openFile(context, file)
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                } else {
                    _error.value = "No app available to open this file type"
                }
            } catch (e: Exception) {
                _error.value = "Failed to open file: ${e.message}"
            }
        }
    }

    fun shareFile(fileInfo: FileInfo) {
        viewModelScope.launch {
            try {
                val file = File(fileInfo.path)
                val intent = FileUtils.shareFile(context, file)
                context.startActivity(Intent.createChooser(intent, "Share ${fileInfo.name}"))
            } catch (e: Exception) {
                _error.value = "Failed to share file: ${e.message}"
            }
        }
    }

    // ── Properties ──

    fun showPropertiesDialog(fileInfo: FileInfo) {
        _showPropertiesDialog.value = true
        _propertiesTarget.value = fileInfo
    }

    fun dismissPropertiesDialog() {
        _showPropertiesDialog.value = false
        _propertiesTarget.value = null
    }

    // ── Archive ──

    fun showExtractDialog(path: String) {
        _showExtractDialog.value = true
        _extractTarget.value = path
    }

    fun dismissExtractDialog() {
        _showExtractDialog.value = false
        _extractTarget.value = null
    }

    fun extractArchive(archivePath: String, destinationDir: String) {
        viewModelScope.launch {
            _showExtractDialog.value = false
            _extractTarget.value = null
            setOperation("Extracting…")
            when (val result = archiveRepository.extractArchive(archivePath, destinationDir)) {
                is OperationResult.Success -> {
                    clearOperation()
                    _error.value = "Extraction completed"
                    loadFiles(_currentPath.value)
                }
                is OperationResult.Error -> {
                    clearOperation()
                    _error.value = result.message
                }
            }
        }
    }

    // ── Sorting ──

    fun setSortMode(mode: SortMode) {
        _sortOptions.value = _sortOptions.value.copy(mode = mode)
        refresh()
    }

    fun toggleFoldersFirst() {
        val current = _sortOptions.value
        _sortOptions.value = current.copy(foldersFirst = !current.foldersFirst)
        refresh()
    }

    // ── Copy Path ──

    fun copyPath(path: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Path", path)
            clipboard.setPrimaryClip(clip)
            _error.value = "Path copied to clipboard"
        } catch (e: Exception) {
            _error.value = "Failed to copy path"
        }
    }

    // ── Duplicate ──

    fun duplicateFile(path: String) {
        viewModelScope.launch {
            val file = File(path)
            val parent = file.parent ?: return@launch
            val name = file.nameWithoutExtension
            val ext = file.extension
            var counter = 1
            var newName = if (ext.isNotEmpty()) "$name (copy $counter).$ext" else "$name (copy $counter)"
            if (counter == 1) {
                newName = if (ext.isNotEmpty()) "$name (copy).$ext" else "$name (copy)"
            }
            while (File(parent, newName).exists()) {
                counter++
                newName = if (ext.isNotEmpty()) "$name (copy $counter).$ext" else "$name (copy $counter)"
            }

            setOperation("Duplicating…")
            when (val result = fileRepository.copyFile(path, "$parent/$newName")) {
                is OperationResult.Success -> {
                    clearOperation()
                    loadFiles(_currentPath.value)
                }
                is OperationResult.Error -> {
                    clearOperation()
                    _error.value = result.message
                }
            }
        }
    }

    // ── Helpers ──

    fun clearError() { _error.value = null }

    private fun setOperation(message: String) {
        _operationInProgress.value = true
        _operationMessage.value = message
    }

    private fun clearOperation() {
        _operationInProgress.value = false
        _operationMessage.value = null
    }

    fun getContextForPermission(): Context = context

    // ── Drag and Drop ──

    fun moveFileToFolder(sourcePath: String, targetFolderPath: String) {
        viewModelScope.launch {
            val sourceFile = File(sourcePath)
            val destPath = File(targetFolderPath, sourceFile.name).absolutePath
            setOperation("Moving ${sourceFile.name}…")
            when (val result = fileRepository.moveFile(sourcePath, destPath)) {
                is OperationResult.Success -> {
                    clearOperation()
                    loadFiles(_currentPath.value)
                }
                is OperationResult.Error -> {
                    clearOperation()
                    _error.value = result.message
                }
            }
        }
    }
}
