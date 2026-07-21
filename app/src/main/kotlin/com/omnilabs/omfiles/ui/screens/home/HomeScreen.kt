package com.omnilabs.omfiles.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.omnilabs.omfiles.domain.model.FileInfo
import com.omnilabs.omfiles.domain.model.StorageInfo
import com.omnilabs.omfiles.domain.model.StorageType
import com.omnilabs.omfiles.ui.components.FileListItem
import com.omnilabs.omfiles.utils.formatFileSize
import com.omnilabs.omfiles.utils.formatPercentage
import com.omnilabs.omfiles.utils.formatStorageSize

data class QuickAccessItem(
    val label: String,
    val icon: ImageVector,
    val color: androidx.compose.ui.graphics.Color,
    val path: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToFiles: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val error by viewModel.error.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "OmniFiles",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Storage cards
                item {
                    Text(
                        "Storage",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                items(uiState.storageVolumes) { storage ->
                    StorageCard(
                        storage = storage,
                        onClick = { onNavigateToFiles(storage.path) }
                    )
                }

                // Quick access grid
                item {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Quick Access",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                item {
                    QuickAccessGrid(
                        onItemClick = onNavigateToFiles
                    )
                }

                // Favorites
                if (uiState.favorites.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Outlined.Star,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Favorites",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    items(uiState.favorites.take(5)) { file ->
                        FileListItem(
                            fileInfo = file,
                            isFavorite = true,
                            onSingleClick = {
                                if (file.isDirectory) onNavigateToFiles(file.path)
                            }
                        )
                    }
                }

                // Recent files
                item {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Outlined.History,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Recent Files",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (uiState.recentFiles.isEmpty()) {
                    item {
                        Text(
                            "No recent files",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                } else {
                    items(uiState.recentFiles.take(10)) { file ->
                        FileListItem(
                            fileInfo = file,
                            onSingleClick = {
                                if (file.isDirectory) onNavigateToFiles(file.path)
                            }
                        )
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun StorageCard(
    storage: StorageInfo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when (storage.type) {
                            StorageType.INTERNAL -> MaterialTheme.colorScheme.primary
                            StorageType.SD_CARD -> MaterialTheme.colorScheme.tertiary
                            StorageType.USB_OTG -> MaterialTheme.colorScheme.secondary
                        }.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (storage.type) {
                        StorageType.INTERNAL -> Icons.Filled.Storage
                        StorageType.SD_CARD -> Icons.Filled.SdCard
                        StorageType.USB_OTG -> Icons.Filled.Usb
                    },
                    contentDescription = null,
                    tint = when (storage.type) {
                        StorageType.INTERNAL -> MaterialTheme.colorScheme.primary
                        StorageType.SD_CARD -> MaterialTheme.colorScheme.tertiary
                        StorageType.USB_OTG -> MaterialTheme.colorScheme.secondary
                    },
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = storage.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { storage.usedPercentage / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = when {
                        storage.usedPercentage > 90f -> MaterialTheme.colorScheme.error
                        storage.usedPercentage > 70f -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${formatStorageSize(storage.usedSpace)} used • ${formatStorageSize(storage.freeSpace)} free (${formatPercentage(storage.usedPercentage)}%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun QuickAccessGrid(
    onItemClick: (String) -> Unit
) {
    val items = listOf(
        QuickAccessItem("Downloads", Icons.Outlined.Download, MaterialTheme.colorScheme.primary, "/storage/emulated/0/Download"),
        QuickAccessItem("Documents", Icons.Filled.Description, MaterialTheme.colorScheme.secondary, "/storage/emulated/0/Documents"),
        QuickAccessItem("Images", Icons.Filled.Image, MaterialTheme.colorScheme.tertiary, "/storage/emulated/0/DCIM"),
        QuickAccessItem("Videos", Icons.Filled.Videocam, MaterialTheme.colorScheme.error, "/storage/emulated/0/Movies"),
        QuickAccessItem("Audio", Icons.Filled.AudioFile, MaterialTheme.colorScheme.tertiary, "/storage/emulated/0/Music"),
        QuickAccessItem("APKs", Icons.Outlined.Android, MaterialTheme.colorScheme.primary, "/storage/emulated/0/Download"),
        QuickAccessItem("Archives", Icons.Outlined.Archive, MaterialTheme.colorScheme.secondary, "/storage/emulated/0/Download"),
        QuickAccessItem("Internal", Icons.Filled.Folder, MaterialTheme.colorScheme.tertiary, "/storage/emulated/0"),
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        userScrollEnabled = false
    ) {
        items(items) { item ->
            QuickAccessTile(
                item = item,
                onClick = { onItemClick(item.path) }
            )
        }
    }
}

@Composable
private fun QuickAccessTile(
    item: QuickAccessItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(item.color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = item.color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
