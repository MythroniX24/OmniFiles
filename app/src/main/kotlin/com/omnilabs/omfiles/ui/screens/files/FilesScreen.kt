package com.omnilabs.omfiles.ui.screens.files

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MoveDown
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.hilt.navigation.compose.hiltViewModel
import com.omnilabs.omfiles.domain.model.FileInfo
import com.omnilabs.omfiles.domain.model.FileType
import com.omnilabs.omfiles.domain.model.SortMode
import com.omnilabs.omfiles.domain.model.SortOrder
import com.omnilabs.omfiles.ui.components.FileListItem
import com.omnilabs.omfiles.utils.PermissionHandler
import com.omnilabs.omfiles.utils.formatFileSize
import com.omnilabs.omfiles.utils.formatDate

// Drag auto-scroll constants
private const val SCROLL_ZONE_DP = 120
private const val MAX_SCROLL_SPEED = 250

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    initialPath: String,
    onNavigateBack: () -> Unit,
    onNavigateToFolder: (String) -> Unit,
    viewModel: FilesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val error by viewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Dialogs state
    var renameText by remember { mutableStateOf("") }
    var createFolderText by remember { mutableStateOf("") }
    var createFileText by remember { mutableStateOf("") }

    // Drag-and-drop state
    var isDragging by remember { mutableStateOf(false) }
    var draggedFilePath by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var dropTargetPath by remember { mutableStateOf<String?>(null) }

    // Track item positions for drop target detection: path -> (top, bottom) in root coordinates
    val itemPositions = remember { mutableMapOf<String, Pair<Float, Float>>() }

    // Track where the finger was when drag started (root Y coordinate)
    var touchStartScreenY by remember { mutableFloatStateOf(0f) }

    // Auto-scroll during drag
    val lazyListState = rememberLazyListState()
    var listTop by remember { mutableFloatStateOf(0f) }
    var listBottom by remember { mutableFloatStateOf(0f) }
    var scrollSpeed by remember { mutableIntStateOf(0) } // pixels/second, positive=down, negative=up

    // Density for pixel-dp conversion
    val density = androidx.compose.ui.platform.LocalDensity.current

    // Auto-scroll effect: scrolls the list while dragging near top/bottom edges
    // Keyed on scrollSpeed so it restarts smoothly on speed changes
    LaunchedEffect(scrollSpeed) {
        if (scrollSpeed != 0 && isDragging) {
            while (isDragging && scrollSpeed != 0) {
                val currentSpeed = scrollSpeed
                if (currentSpeed != 0) {
                    // Positive speed = scroll forward (toward end)
                    // Negative speed = scroll backward (toward start)
                    lazyListState.dispatchRawDelta(currentSpeed / 60f)
                }
                delay(16) // ~60fps
            }
        }
    }

    // Get the dragged file info
    val draggedFile = uiState.files.find { it.path == draggedFilePath }

    LaunchedEffect(initialPath) {
        viewModel.navigateToFolder(initialPath)
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Permission denied screen
    if (!uiState.hasStoragePermission) {
        NoPermissionScreen(
            onRetry = { viewModel.checkPermission() },
            onOpenSettings = {
                PermissionHandler.getStoragePermissionIntent(context)?.let { intent ->
                    context.startActivity(intent)
                }
            }
        )
        return
    }

    // ── Dialogs ──

    if (uiState.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteConfirmation,
            title = { Text("Delete ${uiState.pendingDeletePaths.size} item${if (uiState.pendingDeletePaths.size != 1) "s" else ""}?") },
            text = { Text("This action cannot be undone. Are you sure you want to delete?") },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                OutlinedButton(onClick = viewModel::dismissDeleteConfirmation) { Text("Cancel") }
            }
        )
    }

    if (uiState.showRenameDialog) {
        val targetName = uiState.renameTarget?.let { java.io.File(it).name } ?: ""
        AlertDialog(
            onDismissRequest = viewModel::dismissRenameDialog,
            title = { Text("Rename") },
            text = {
                OutlinedTextField(
                    value = renameText.ifEmpty { targetName },
                    onValueChange = { renameText = it },
                    label = { Text("New name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(                        onClick = {
                            val path = uiState.renameTarget
                            if (path != null) {
                                viewModel.renameFile(path, renameText.ifEmpty { targetName })
                                renameText = ""
                            }
                        }
                ) { Text("Rename") }
            },
            dismissButton = {
                OutlinedButton(onClick = { renameText = ""; viewModel.dismissRenameDialog() }) { Text("Cancel") }
            }
        )
    }

    if (uiState.showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissCreateFolderDialog,
            title = { Text("Create Folder") },
            text = {
                OutlinedTextField(
                    value = createFolderText,
                    onValueChange = { createFolderText = it },
                    label = { Text("Folder name") },
                    singleLine = true,
                    placeholder = { Text("New Folder") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createFolder(createFolderText.ifEmpty { "New Folder" })
                        createFolderText = ""
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                OutlinedButton(onClick = { createFolderText = ""; viewModel.dismissCreateFolderDialog() }) { Text("Cancel") }
            }
        )
    }

    if (uiState.showCreateFileDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissCreateFileDialog,
            title = { Text("Create File") },
            text = {
                OutlinedTextField(
                    value = createFileText,
                    onValueChange = { createFileText = it },
                    label = { Text("File name") },
                    singleLine = true,
                    placeholder = { Text("New File.txt") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createFile(createFileText.ifEmpty { "New File.txt" })
                        createFileText = ""
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                OutlinedButton(onClick = { createFileText = ""; viewModel.dismissCreateFileDialog() }) { Text("Cancel") }
            }
        )
    }

    // Properties Dialog
    uiState.propertiesTarget?.let { fileInfo ->
        PropertiesDialog(
            fileInfo = fileInfo,
            onDismiss = viewModel::dismissPropertiesDialog
        )
    }

    // ── Main Content ──

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (uiState.selectionMode) {
                // Selection mode top bar
                TopAppBar(
                    title = {
                        Text(
                            "${uiState.selectedFiles.size} selected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = viewModel::exitSelectionMode) {
                            Icon(Icons.Filled.Close, "Exit selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            uiState.selectedFiles.firstOrNull()?.let {
                                viewModel.showRenameDialog(it)
                            }
                        }) {
                            Icon(Icons.Filled.DriveFileRenameOutline, "Rename")
                        }
                        IconButton(onClick = {
                            viewModel.showDeleteConfirmation(uiState.selectedFiles)
                        }) {
                            Icon(Icons.Filled.Delete, "Delete")
                        }
                        IconButton(onClick = {
                            viewModel.copySelectedFiles(uiState.currentPath)
                        }) {
                            Icon(Icons.Filled.ContentCopy, "Copy here")
                        }
                        IconButton(onClick = {
                            viewModel.moveSelectedFiles(uiState.currentPath)
                        }) {
                            Icon(Icons.Filled.ContentCut, "Move here")
                        }
                        IconButton(onClick = {
                            uiState.selectedFiles.firstOrNull()?.let {
                                viewModel.shareFile(
                                    uiState.files.first { f -> f.path == it }
                                )
                            }
                        }) {
                            Icon(Icons.Filled.Share, "Share")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = uiState.currentPath.substringAfterLast('/').ifEmpty { "Internal Storage" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (uiState.files.isNotEmpty()) {
                                Text(
                                    text = "${uiState.files.size} items",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        if (uiState.currentPath != "/") {
                            IconButton(onClick = viewModel::navigateUp) {
                                Icon(Icons.Filled.OpenInNew, "Go up", modifier = Modifier.size(20.dp))
                            }
                        }
                        IconButton(onClick = viewModel::refresh) {
                            Icon(Icons.Filled.Refresh, "Refresh")
                        }
                        var showMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Filled.MoreVert, "More options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Create Folder") },
                                onClick = {
                                    showMenu = false
                                    viewModel.showCreateFolderDialog()
                                },
                                leadingIcon = { Icon(Icons.Filled.CreateNewFolder, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Create File") },
                                onClick = {
                                    showMenu = false
                                    viewModel.showCreateFileDialog()
                                },
                                leadingIcon = { Icon(Icons.Filled.NoteAdd, null) }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Sort by Name") },
                                onClick = { showMenu = false; viewModel.setSortMode(SortMode.NAME) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort by Date") },
                                onClick = { showMenu = false; viewModel.setSortMode(SortMode.DATE) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort by Size") },
                                onClick = { showMenu = false; viewModel.setSortMode(SortMode.SIZE) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort by Extension") },
                                onClick = { showMenu = false; viewModel.setSortMode(SortMode.EXTENSION) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, null) }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
    ) { paddingValues ->
        // Progress indicator for operations
        if (uiState.operationInProgress) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                uiState.operationMessage?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.files.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "This folder is empty",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Content with drag-and-drop support
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp)
                        .onGloballyPositioned { coords ->
                            val pos = coords.positionInRoot()
                            listTop = pos.y
                            listBottom = pos.y + coords.size.height.toFloat()
                        },
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                        items = uiState.files,
                        key = { it.path }
                    ) { fileInfo ->
                        FileListItem(
                            fileInfo = fileInfo,
                            isSelected = fileInfo.path in uiState.selectedFiles,
                            isFavorite = fileInfo.path in uiState.favorites,
                            isDropTarget = fileInfo.path == dropTargetPath,
                            selectionMode = uiState.selectionMode,
                            onSingleClick = {
                                if (uiState.selectionMode) {
                                    viewModel.toggleSelection(fileInfo.path)
                                } else if (fileInfo.isDirectory) {
                                    onNavigateToFolder(fileInfo.path)
                                } else {
                                    viewModel.openFile(fileInfo)
                                }
                            },
                            onLongClick = {
                                if (!uiState.selectionMode) {
                                    viewModel.enterSelectionMode(fileInfo.path)
                                }
                            },
                            onFavoriteClick = {
                                viewModel.toggleFavorite(fileInfo.path)
                            },
                            onOptionsClick = { fileInfo ->
                                viewModel.showPropertiesDialog(fileInfo)
                            },
                            onItemPositioned = { path, x, top, bottom ->
                                itemPositions[path] = Pair(top, bottom)
                            },
                            onDragStart = { path, touchY ->
                                val pos = itemPositions[path]
                                if (pos != null && !uiState.selectionMode) {
                                    isDragging = true
                                    draggedFilePath = path
                                    touchStartScreenY = pos.first + touchY
                                    dragOffsetY = 0f
                                    dropTargetPath = null
                                }
                            },
                            onDrag = { dx, dy ->
                                dragOffsetY += dy

                                // Detect drop target by absolute finger Y position
                                val fingerY = touchStartScreenY + dragOffsetY
                                dropTargetPath = null
                                // Only folders can be drop targets
                                for (file in uiState.files) {
                                    if (!file.isDirectory) continue
                                    val positions = itemPositions[file.path] ?: continue
                                    if (fingerY in positions.first..positions.second) {
                                        if (file.path != draggedFilePath) {
                                            dropTargetPath = file.path
                                        }
                                        break
                                    }
                                }

                                // Auto-scroll logic: scroll when finger is near top/bottom edge
                                val scrollZonePx = with(density) { SCROLL_ZONE_DP.dp.toPx() }
                                val listHeight = listBottom - listTop
                                val fingerInList = fingerY - listTop

                                scrollSpeed = when {
                                    fingerInList < scrollZonePx && lazyListState.canScrollBackward -> {
                                        // Near top edge: scroll up
                                        // Speed increases as finger gets closer to top
                                        val proximity = 1f - (fingerInList / scrollZonePx)
                                        -(proximity * MAX_SCROLL_SPEED).toInt().coerceAtMost(MAX_SCROLL_SPEED)
                                    }
                                    fingerInList > listHeight - scrollZonePx -> {
                                        // Near bottom edge: scroll down
                                        val distanceFromBottom = fingerInList - (listHeight - scrollZonePx)
                                        val proximity = (distanceFromBottom / scrollZonePx).coerceAtMost(1f)
                                        (proximity * MAX_SCROLL_SPEED).toInt().coerceAtMost(MAX_SCROLL_SPEED)
                                    }
                                    else -> 0
                                }
                            },
                            onDragEnd = {
                                // Handle drop only if a valid target folder is found
                                val sourcePath = draggedFilePath
                                val targetFolder = dropTargetPath
                                if (sourcePath != null && targetFolder != null && targetFolder != sourcePath) {
                                    viewModel.moveFileToFolder(sourcePath, targetFolder)
                                }
                                // Reset all drag state
                                isDragging = false
                                draggedFilePath = null
                                dragOffsetY = 0f
                                dropTargetPath = null
                                scrollSpeed = 0
                            },
                            onDragCancel = {
                                isDragging = false
                                draggedFilePath = null
                                dragOffsetY = 0f
                                dropTargetPath = null
                                scrollSpeed = 0
                            }
                        )
                    }

                    item { Spacer(Modifier.height(80.dp)) }
                }

                // Drag overlay - shows a floating card following the finger
                if (isDragging && draggedFile != null) {
                    DragOverlay(
                        fileName = draggedFile.name,
                        offsetY = dragOffsetY
                    )
                }
            }
        }
    }
}

@Composable
private fun NoPermissionScreen(
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Filled.Info,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "Storage Permission Required",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "OmniFiles needs storage access to browse and manage your files.\n\n" +
                        "On Android 11+, tap \"Grant Permission\" and enable \"Allow access to manage all files\".",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(32.dp))
            Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.FileCopy, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Grant Permission")
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Retry")
            }
        }
    }
}

@Composable
private fun PropertiesDialog(
    fileInfo: FileInfo,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Properties", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PropRow("Name", fileInfo.name)
                PropRow("Path", fileInfo.path)
                PropRow("Type", when (fileInfo.fileType) {
                    FileType.FOLDER -> "Folder"
                    FileType.IMAGE -> "Image"
                    FileType.VIDEO -> "Video"
                    FileType.AUDIO -> "Audio"
                    FileType.ARCHIVE -> "Archive"
                    FileType.APK -> "APK Package"
                    FileType.DOCUMENT -> "Document"
                    FileType.OTHER -> if (fileInfo.extension.isNotEmpty()) "${fileInfo.extension.uppercase()} File" else "Unknown"
                })
                if (!fileInfo.isDirectory) {
                    PropRow("Size", formatFileSize(fileInfo.size))
                    PropRow("Extension", fileInfo.extension.uppercase())
                } else {
                    PropRow("Contents", "${java.io.File(fileInfo.path).listFiles()?.size ?: 0} items")
                }
                PropRow("Modified", formatDate(fileInfo.lastModified))
                PropRow("Hidden", if (fileInfo.isHidden) "Yes" else "No")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun PropRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Floating card overlay shown during drag operations.
 * Follows the user's finger to show what file is being dragged.
 */
@Composable
private fun DragOverlay(
    fileName: String,
    offsetY: Float
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, offsetY.toInt().coerceAtLeast(0)) }
                .shadow(elevation = 12.dp, shape = RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MoveDown,
                    contentDescription = "Moving",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
