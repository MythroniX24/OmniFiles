package com.omnilabs.omfiles.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omnilabs.omfiles.core.theme.fileTypeApk
import com.omnilabs.omfiles.core.theme.fileTypeArchive
import com.omnilabs.omfiles.core.theme.fileTypeAudio
import com.omnilabs.omfiles.core.theme.fileTypeDefault
import com.omnilabs.omfiles.core.theme.fileTypeDocument
import com.omnilabs.omfiles.core.theme.fileTypeFolder
import com.omnilabs.omfiles.core.theme.fileTypeImage
import com.omnilabs.omfiles.core.theme.fileTypeVideo
import com.omnilabs.omfiles.domain.model.FileInfo
import com.omnilabs.omfiles.domain.model.FileType
import com.omnilabs.omfiles.utils.formatFileSize
import com.omnilabs.omfiles.utils.formatDate

/**
 * A file list item that supports tap, long-press (selection), and long-press drag-and-drop.
 *
 * Gesture behavior (when not in selection mode):
 * - Tap → [onSingleClick] (open file / navigate folder / toggle selection)
 * - Long-press (hold still) → [onLongClick] (enter selection mode)
 * - Long-press + drag → initiates drag-and-drop via [onDragStart], [onDrag], [onDragEnd]
 *
 * When in selection mode, only tap is active (toggles selection checkbox).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListItem(
    fileInfo: FileInfo,
    isSelected: Boolean = false,
    isFavorite: Boolean = false,
    isDropTarget: Boolean = false,
    selectionMode: Boolean = false,
    onSingleClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onFavoriteClick: () -> Unit = {},
    onOptionsClick: (FileInfo) -> Unit = {},
    onItemPositioned: (String, Float, Float, Float) -> Unit = { _, _, _, _ -> },
    onDragStart: (String, Float) -> Unit = { _, _ -> },
    onDrag: (Float, Float) -> Unit = { _, _ -> },
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = when {
            isDropTarget && fileInfo.isDirectory -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else -> Color.Transparent
        },
        label = "itemBg"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isDropTarget && fileInfo.isDirectory -> MaterialTheme.colorScheme.primary
            else -> Color.Transparent
        },
        label = "itemBorder"
    )

    // Build the base modifier with position tracking and click handling
    val baseModifier = modifier
        .fillMaxWidth()
        .onGloballyPositioned { coordinates ->
            val pos = coordinates.positionInRoot()
            val size = coordinates.size
            onItemPositioned(
                fileInfo.path,
                pos.x,
                pos.y,
                pos.y + size.height.toFloat()
            )
        }

    // Apply click and drag gesture modifiers conditionally
    val finalModifier = if (selectionMode) {
        // In selection mode: only tap to toggle selection
        baseModifier.combinedClickable(
            onClick = onSingleClick,
            onLongClick = null
        )
    } else {
        // Normal mode: tap, long-press, and drag gestures
        // combinedClickable handles tap and long-press (no drag movement)
        // detectDragGesturesAfterLongPress handles long-press + drag
        baseModifier
            .combinedClickable(
                onClick = onSingleClick,
                onLongClick = onLongClick
            )
            .pointerInput(fileInfo.path) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        onDragStart(fileInfo.path, offset.y)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x, dragAmount.y)
                    },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragCancel
                )
            }
    }

    Surface(
        modifier = finalModifier,
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        tonalElevation = if (isSelected) 2.dp else 0.dp,
        border = if (borderColor != Color.Transparent) {
            androidx.compose.foundation.BorderStroke(2.dp, borderColor)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection checkbox
            if (selectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSingleClick() },
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
            }

            // File type icon (with thumbnail for images/videos)
            FileTypeIcon(fileInfo.fileType, fileInfo.extension, fileInfo.path)

            Spacer(Modifier.width(12.dp))

            // File details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileInfo.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (fileInfo.isDirectory) {
                        Text(
                            text = if (fileInfo.itemCount >= 0) "${fileInfo.itemCount} items" else "Folder",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = formatFileSize(fileInfo.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "\u00b7",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatDate(fileInfo.lastModified),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Action buttons (info + favorite)
            if (!selectionMode) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Info/Properties button — reusable icon button wrapper
                    IconButton(
                        onClick = { onOptionsClick(fileInfo) },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = "Properties",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    // Favorite star button
                    IconButton(
                        onClick = { onFavoriteClick() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }

        // Drop target indicator line at the top
        if (isDropTarget && fileInfo.isDirectory) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
fun FileTypeIcon(fileType: FileType, extension: String, path: String = "", modifier: Modifier = Modifier) {
    if (path.isNotEmpty() && fileType in listOf(FileType.IMAGE, FileType.VIDEO)) {
        FileThumbnail(
            filePath = path,
            fileType = fileType,
            modifier = modifier
        )
    } else {
        val (icon, color) = when (fileType) {
            FileType.FOLDER -> Icons.Filled.Folder to fileTypeFolder
            FileType.IMAGE -> Icons.Filled.Image to fileTypeImage
            FileType.VIDEO -> Icons.Filled.Videocam to fileTypeVideo
            FileType.AUDIO -> Icons.Filled.AudioFile to fileTypeAudio
            FileType.ARCHIVE -> Icons.Outlined.Archive to fileTypeArchive
            FileType.APK -> Icons.Outlined.Android to fileTypeApk
            FileType.DOCUMENT -> Icons.Filled.Description to fileTypeDocument
            FileType.OTHER -> Icons.Filled.Extension to fileTypeDefault
        }

        Surface(
            modifier = modifier.size(44.dp),
            shape = RoundedCornerShape(10.dp),
            color = color.copy(alpha = 0.15f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = fileType.name,
                tint = color,
                modifier = Modifier
                    .padding(8.dp)
                    .size(28.dp)
            )
        }
    }
}
