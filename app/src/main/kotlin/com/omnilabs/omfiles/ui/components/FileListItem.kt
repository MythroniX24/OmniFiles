package com.omnilabs.omfiles.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnilabs.omfiles.domain.model.FileInfo
import com.omnilabs.omfiles.domain.model.FileType
import com.omnilabs.omfiles.utils.formatDate
import com.omnilabs.omfiles.utils.formatFileSize

/**
 * A file list item that matches the premium HTML explorer design.
 *
 * Gesture behavior (when not in selection mode):
 * - Tap → [onSingleClick]
 * - Long-press (hold still) → [onLongClick] (enter selection mode)
 * - Long-press + drag → drag-and-drop via [onDragStart], [onDrag], [onDragEnd]
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListItem(
    fileInfo: FileInfo,
    isSelected: Boolean = false,
    isDropTarget: Boolean = false,
    selectionMode: Boolean = false,
    onSingleClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onOptionsClick: (FileInfo) -> Unit = {},
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
        targetValue = if (isDropTarget && fileInfo.isDirectory) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "itemBorder"
    )

    val finalModifier = if (selectionMode) {
        modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(0.dp))
            .combinedClickable(
                onClick = onSingleClick,
                onLongClick = null
            )
    } else {
        modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(0.dp))
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

    Row(
        modifier = finalModifier
            .background(bgColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Selection checkbox
        if (selectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onSingleClick() },
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
        }

        // File type icon (48x48 area)
        FileTypeIcon(fileInfo = fileInfo)

        Spacer(Modifier.width(16.dp))

        // File details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fileInfo.name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (fileInfo.isDirectory) {
                        if (fileInfo.itemCount >= 0) "${fileInfo.itemCount} items" else "Folder"
                    } else {
                        formatFileSize(fileInfo.size)
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        letterSpacing = (12.sp.value * 0.02).sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "\u00b7",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Text(
                    text = formatDate(fileInfo.lastModified),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        letterSpacing = (12.sp.value * 0.02).sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // More options
        if (!selectionMode) {
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = { onOptionsClick(fileInfo) },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = "More",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

    // Drop target top indicator
    if (isDropTarget && fileInfo.isDirectory) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
private fun FileTypeIcon(fileInfo: FileInfo, modifier: Modifier = Modifier) {
    val (primaryIcon, overlayIcon, iconColor) = iconStyleFor(fileInfo)

    Box(
        modifier = modifier
            .size(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = primaryIcon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(40.dp)
        )

        // Overlay icon for folders
        if (overlayIcon != null) {
            Icon(
                imageVector = overlayIcon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(14.dp)
                    .align(Alignment.Center)
                    .offset(0.dp, (-2).dp)
            )
        }
    }
}

private data class FileIconStyle(
    val primary: ImageVector,
    val overlay: ImageVector?,
    val color: Color
)

@Composable
private fun iconStyleFor(fileInfo: FileInfo): FileIconStyle {
    return when (fileInfo.fileType) {
        FileType.FOLDER -> FileIconStyle(
            primary = Icons.Filled.Folder,
            overlay = folderOverlayIcon(fileInfo.name),
            color = Color(0xFFFF9800)
        )
        FileType.IMAGE -> FileIconStyle(Icons.Filled.Image, null, Color(0xFF4CAF50))
        FileType.VIDEO -> FileIconStyle(Icons.Filled.VideoFile, null, Color(0xFFF44336))
        FileType.AUDIO -> FileIconStyle(Icons.Filled.AudioFile, null, Color(0xFF9C27B0))
        FileType.ARCHIVE -> FileIconStyle(Icons.Outlined.Inventory2, null, Color(0xFFFF9800))
        FileType.APK -> FileIconStyle(Icons.Filled.Android, null, Color(0xFF00BCD4))
        FileType.DOCUMENT -> when (fileInfo.extension.lowercase()) {
            "pdf" -> FileIconStyle(Icons.Outlined.PictureAsPdf, null, Color(0xFFE53935))
            else -> FileIconStyle(Icons.Filled.Description, null, MaterialTheme.colorScheme.primary)
        }
        FileType.OTHER -> FileIconStyle(Icons.Filled.Extension, null, Color(0xFF757575))
    }
}

private fun folderOverlayIcon(name: String): ImageVector? {
    return when (name.lowercase()) {
        "android" -> Icons.Filled.Android
        "dcim", "camera" -> Icons.Filled.Image
        "download", "downloads" -> Icons.Outlined.Archive
        "music", "audio", "sounds" -> Icons.Filled.MusicNote
        "movies", "videos" -> Icons.Filled.VideoFile
        "documents", "docs" -> Icons.Filled.Description
        else -> null
    }
}
