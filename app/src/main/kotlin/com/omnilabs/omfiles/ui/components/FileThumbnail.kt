package com.omnilabs.omfiles.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.os.Build
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.omnilabs.omfiles.core.theme.fileTypeApk
import com.omnilabs.omfiles.core.theme.fileTypeArchive
import com.omnilabs.omfiles.core.theme.fileTypeAudio
import com.omnilabs.omfiles.core.theme.fileTypeDefault
import com.omnilabs.omfiles.core.theme.fileTypeDocument
import com.omnilabs.omfiles.core.theme.fileTypeFolder
import com.omnilabs.omfiles.core.theme.fileTypeImage
import com.omnilabs.omfiles.core.theme.fileTypeVideo
import com.omnilabs.omfiles.domain.model.FileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Composable that displays a file thumbnail (for images/videos) or a file type icon.
 * Thumbnails are generated asynchronously on a background thread.
 */
@Composable
fun FileThumbnail(
    filePath: String,
    fileType: FileType,
    modifier: Modifier = Modifier.size(44.dp)
) {
    val context = LocalContext.current
    var thumbnail by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(filePath) {
        isLoading = true
        thumbnail = loadThumbnail(context, filePath, fileType)
        isLoading = false
    }

    Surface(
        modifier = modifier.clip(RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        color = when {
            thumbnail != null -> Color.Transparent
            else -> getFileTypeColor(fileType).copy(alpha = 0.15f)
        }
    ) {
        Box(
            modifier = Modifier.size(44.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                thumbnail != null -> {
                    Image(
                        bitmap = thumbnail!!.asImageBitmap(),
                        contentDescription = "Thumbnail",
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                else -> {
                    Icon(
                        imageVector = getFileTypeIcon(fileType),
                        contentDescription = fileType.name,
                        tint = getFileTypeColor(fileType),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Loading indicator
            if (isLoading && fileType in listOf(FileType.IMAGE, FileType.VIDEO)) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = getFileTypeIcon(fileType),
                            contentDescription = null,
                            tint = getFileTypeColor(fileType).copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Load a thumbnail for the given file. Works for images and videos.
 * Runs on IO dispatcher to avoid blocking the UI thread.
 */
private suspend fun loadThumbnail(
    context: Context,
    filePath: String,
    fileType: FileType
): Bitmap? = withContext(Dispatchers.IO) {
    try {
        val file = File(filePath)
        if (!file.exists()) return@withContext null

        when (fileType) {
            FileType.IMAGE -> {
                // Load a scaled-down version of the image
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                    BitmapFactory.decodeFile(filePath, this)
                    // Calculate sample size to get a thumbnail-sized bitmap
                    inSampleSize = calculateInSampleSize(this, 150, 150)
                    inJustDecodeBounds = false
                }
                BitmapFactory.decodeFile(filePath, options)
            }
            FileType.VIDEO -> {
                // Extract a frame from the video
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Video.Thumbnails.getThumbnail(
                        context.contentResolver,
                        filePath.hashCode().toLong(),
                        MediaStore.Video.Thumbnails.MINI_KIND,
                        null
                    )
                } else {
                    @Suppress("DEPRECATION")
                    ThumbnailUtils.createVideoThumbnail(
                        filePath,
                        MediaStore.Video.Thumbnails.MINI_KIND
                    )
                }
            }
            FileType.APK -> {
                // Extract APK icon
                try {
                    val pm = context.packageManager
                    val pkgInfo = pm.getPackageArchiveInfo(filePath, 0)
                    pkgInfo?.applicationInfo?.let { info ->
                        info.sourceDir = filePath
                        info.publicSourceDir = filePath
                        pm.getApplicationIcon(info).let { drawable ->
                            val bitmap = Bitmap.createBitmap(
                                drawable.intrinsicWidth.coerceAtLeast(1),
                                drawable.intrinsicHeight.coerceAtLeast(1),
                                Bitmap.Config.ARGB_8888
                            )
                            // Simple icon extraction may need Canvas drawing
                            null // Fall back to icon for now
                        }
                    }
                } catch (_: Exception) { }
                null
            }
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}

/**
 * Calculate the sample size to scale down large images.
 */
private fun calculateInSampleSize(
    options: BitmapFactory.Options,
    reqWidth: Int,
    reqHeight: Int
): Int {
    val (height: Int, width: Int) = options.outHeight to options.outWidth
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2

        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

/**
 * Get the icon for a file type.
 */
private fun getFileTypeIcon(type: FileType) = when (type) {
    FileType.FOLDER -> Icons.Filled.Folder
    FileType.IMAGE -> Icons.Filled.Image
    FileType.VIDEO -> Icons.Filled.Videocam
    FileType.AUDIO -> Icons.Filled.AudioFile
    FileType.ARCHIVE -> Icons.Outlined.Archive
    FileType.APK -> Icons.Outlined.Android
    FileType.DOCUMENT -> Icons.Filled.Description
    FileType.OTHER -> Icons.Filled.Extension
}

/**
 * Get the color associated with a file type.
 */
private fun getFileTypeColor(type: FileType) = when (type) {
    FileType.FOLDER -> fileTypeFolder
    FileType.IMAGE -> fileTypeImage
    FileType.VIDEO -> fileTypeVideo
    FileType.AUDIO -> fileTypeAudio
    FileType.ARCHIVE -> fileTypeArchive
    FileType.APK -> fileTypeApk
    FileType.DOCUMENT -> fileTypeDocument
    FileType.OTHER -> fileTypeDefault
}
