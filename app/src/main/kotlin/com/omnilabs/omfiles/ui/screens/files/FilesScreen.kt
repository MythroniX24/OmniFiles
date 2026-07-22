package com.omnilabs.omfiles.ui.screens.files

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MoveDown
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.omnilabs.omfiles.domain.model.FileInfo
import com.omnilabs.omfiles.domain.model.FileType
import com.omnilabs.omfiles.domain.model.SortMode
import com.omnilabs.omfiles.ui.components.FileListItem
import com.omnilabs.omfiles.utils.PermissionHandler
import com.omnilabs.omfiles.utils.formatDate
import com.omnilabs.omfiles.utils.formatFileSize
import kotlinx.coroutines.delay

private const val SCROLL_ZONE_DP = 120
private const val MAX_SCROLL_SPEED = 250

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    initialPath: String,
    onNavigateBack: () -> Unit,
    onNavigateToFolder: (String) -> Unit,
    onNavigateToSearch: () -> Unit = {},
    viewModel: FilesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val error by viewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var renameText by remember { mutableStateOf("") }
    var createFolderText by remember { mutableStateOf("") }
    var createFileText by remember { mutableStateOf("") }

    // Drag-and-drop state
    var isDragging by remember { mutableStateOf(false) }
    var draggedFilePath by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var dropTargetPath by remember { mutableStateOf<String?>(null) }
    var touchStartScreenY by remember { mutableFloatStateOf(0f) }

    val lazyListState = rememberLazyListState()
    var listScreenTop by remember { mutableFloatStateOf(0f) }
    var listBottom by remember { mutableFloatStateOf(0f) }
    var scrollSpeed by remember { mutableIntStateOf(0) }
    val density = androidx.compose.ui.platform.LocalDensity.current

    LaunchedEffect(scrollSpeed) {
        if (scrollSpeed != 0 && isDragging) {
            while (isDragging && scrollSpeed != 0) {
                val currentSpeed = scrollSpeed
                if (currentSpeed != 0) {
                    lazyListState.dispatchRawDelta(currentSpeed / 60f)
                }
                delay(16)
            }
        }
    }

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
                Button(onClick = {
                    val path = uiState.renameTarget
                    if (path != null) {
                        viewModel.renameFile(path, renameText.ifEmpty { targetName })
                        renameText = ""
                    }
                }) { Text("Rename") }
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
                Button(onClick = {
                    viewModel.createFolder(createFolderText.ifEmpty { "New Folder" })
                    createFolderText = ""
                }) { Text("Create") }
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
                Button(onClick = {
                    viewModel.createFile(createFileText.ifEmpty { "New File.txt" })
                    createFileText = ""
                }) { Text("Create") }
            },
            dismissButton = {
                OutlinedButton(onClick = { createFileText = ""; viewModel.dismissCreateFileDialog() }) { Text("Cancel") }
            }
        )
    }

    uiState.propertiesTarget?.let { fileInfo ->
        PropertiesDialog(fileInfo = fileInfo, onDismiss = viewModel::dismissPropertiesDialog)
    }

    // ── Main Content ──

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (uiState.selectionMode) {
                SelectionTopAppBar(
                    selectedCount = uiState.selectedFiles.size,
                    onExit = viewModel::exitSelectionMode,
                    onCopy = viewModel::copyToClipboard,
                    onCut = viewModel::cutToClipboard,
                    onRename = {
                        uiState.selectedFiles.firstOrNull()?.let { viewModel.showRenameDialog(it) }
                    },
                    onDelete = { viewModel.showDeleteConfirmation(uiState.selectedFiles) },
                    onShare = {
                        uiState.selectedFiles.firstOrNull()?.let { path ->
                            uiState.files.firstOrNull { it.path == path }?.let { viewModel.shareFile(it) }
                        }
                    }
                )
            } else {
                FilesTopAppBar(
                    onSearchClick = onNavigateToSearch,
                    onAccountClick = { /* TODO */ }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showCreateFolderDialog() },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add", modifier = Modifier.size(32.dp))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Breadcrumb bar
            if (!uiState.selectionMode) {
                BreadcrumbBar(path = uiState.currentPath)
            }

            if (uiState.operationInProgress) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
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

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.files.isEmpty() -> {
                    EmptyFolderView()
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier
                                .fillMaxSize()
                                .onGloballyPositioned { coordinates ->
                                    listScreenTop = coordinates.positionInRoot().y
                                    listBottom = listScreenTop + coordinates.size.height
                                },
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            items(items = uiState.files, key = { it.path }) { fileInfo ->
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                FileListItem(
                                    fileInfo = fileInfo,
                                    isSelected = fileInfo.path in uiState.selectedFiles,
                                    isDropTarget = fileInfo.path == dropTargetPath,
                                    selectionMode = uiState.selectionMode,
                                    onSingleClick = {
                                        if (uiState.selectionMode) viewModel.toggleSelection(fileInfo.path)
                                        else if (fileInfo.isDirectory) onNavigateToFolder(fileInfo.path)
                                        else viewModel.openFile(fileInfo)
                                    },
                                    onLongClick = {
                                        if (!uiState.selectionMode) viewModel.enterSelectionMode(fileInfo.path)
                                    },
                                    onOptionsClick = { viewModel.showPropertiesDialog(fileInfo) },
                                    onDragStart = { path, touchY ->
                                        if (uiState.selectionMode) return@FileListItem
                                        val index = uiState.files.indexOfFirst { it.path == path }
                                        val itemInfo = lazyListState.layoutInfo.visibleItemsInfo
                                            .find { it.index == index }
                                        if (itemInfo != null) {
                                            isDragging = true
                                            draggedFilePath = path
                                            touchStartScreenY = listScreenTop + itemInfo.offset + touchY
                                            dragOffsetY = 0f
                                            dropTargetPath = null
                                        }
                                    },
                                    onDrag = { _, dy ->
                                        dragOffsetY += dy
                                        val fingerScreenY = touchStartScreenY + dragOffsetY
                                        val fingerInList = fingerScreenY - listScreenTop
                                        dropTargetPath = null

                                        for (item in lazyListState.layoutInfo.visibleItemsInfo) {
                                            if (fingerInList >= item.offset && fingerInList <= item.offset + item.size) {
                                                val file = uiState.files.getOrNull(item.index)
                                                if (file?.isDirectory == true && file.path != draggedFilePath) {
                                                    dropTargetPath = file.path
                                                }
                                                break
                                            }
                                        }

                                        val scrollZonePx = with(density) { SCROLL_ZONE_DP.dp.toPx() }
                                        val listHeight = listBottom - listScreenTop
                                        scrollSpeed = when {
                                            fingerInList < scrollZonePx -> {
                                                val proximity = 1f - (fingerInList / scrollZonePx)
                                                -(proximity * MAX_SCROLL_SPEED).toInt().coerceAtMost(MAX_SCROLL_SPEED)
                                            }
                                            fingerInList > listHeight - scrollZonePx -> {
                                                val dist = fingerInList - (listHeight - scrollZonePx)
                                                val proximity = (dist / scrollZonePx).coerceAtMost(1f)
                                                (proximity * MAX_SCROLL_SPEED).toInt().coerceAtMost(MAX_SCROLL_SPEED)
                                            }
                                            else -> 0
                                        }
                                    },
                                    onDragEnd = {
                                        val sourcePath = draggedFilePath
                                        val targetFolder = dropTargetPath
                                        if (sourcePath != null && targetFolder != null && targetFolder != sourcePath) {
                                            viewModel.moveFileToFolder(sourcePath, targetFolder)
                                        }
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

                        if (isDragging && draggedFile != null) {
                            DragOverlay(fileName = draggedFile.name, offsetY = dragOffsetY)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilesTopAppBar(
    onSearchClick: () -> Unit,
    onAccountClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Omnifiles",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        actions = {
            IconButton(onClick = onAccountClick) {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = "Account",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun SelectionTopAppBar(
    selectedCount: Int,
    onExit: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                "$selectedCount selected",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        },
        navigationIcon = {
            IconButton(onClick = onExit) {
                Icon(Icons.Filled.Close, "Exit selection")
            }
        },
        actions = {
            IconButton(onClick = onCopy) { Icon(Icons.Filled.ContentCopy, "Copy") }
            IconButton(onClick = onCut) { Icon(Icons.Filled.ContentCut, "Cut") }
            IconButton(onClick = onRename) { Icon(Icons.Filled.DriveFileRenameOutline, "Rename") }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Delete") }
            IconButton(onClick = onShare) { Icon(Icons.Filled.Share, "Share") }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    )
}

@Composable
private fun BreadcrumbBar(path: String) {
    val segments = remember(path) {
        val parts = path.split('/').filter { it.isNotEmpty() }
        buildList {
            add("storage" to "storage")
            parts.drop(1).forEachIndexed { index, part ->
                add(part to if (index == parts.size - 2) "bold" else "normal")
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        segments.forEachIndexed { index, (label, style) ->
            if (index == 0) {
                Icon(
                    imageVector = Icons.Filled.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
            }

            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = if (style == "bold") FontWeight.Bold else FontWeight.Normal
                ),
                color = if (style == "bold") MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (index < segments.lastIndex) {
                Text(
                    text = "/",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyFolderView() {
    Box(
        modifier = Modifier.fillMaxSize(),
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
}

@Composable
private fun NoPermissionScreen(
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(
                Icons.Filled.Info,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(24.dp))
            Text("Storage Permission Required", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Text(
                "OmniFiles needs storage access to browse and manage your files.\n\n" +
                        "On Android 11+, tap \"Grant Permission\" and enable \"Allow access to manage all files\".",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(32.dp))
            Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.OpenInNew, contentDescription = null)
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
private fun PropertiesDialog(fileInfo: FileInfo, onDismiss: () -> Unit) {
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
                    PropRow("Contents", if (fileInfo.itemCount >= 0) "${fileInfo.itemCount} items" else "Folder")
                }
                PropRow("Modified", formatDate(fileInfo.lastModified))
                PropRow("Hidden", if (fileInfo.isHidden) "Yes" else "No")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
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

@Composable
private fun DragOverlay(fileName: String, offsetY: Float) {
    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp)) {
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
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.MoveDown, "Moving", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
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
