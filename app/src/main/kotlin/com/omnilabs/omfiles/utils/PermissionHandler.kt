package com.omnilabs.omfiles.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * Handles storage permission requests for all Android versions.
 * On Android 11+: Uses MANAGE_EXTERNAL_STORAGE (All files access).
 * On Android 10 and below: Uses READ/WRITE_EXTERNAL_STORAGE.
 */
object PermissionHandler {

    /**
     * Check if the app has the necessary storage permissions.
     */
    fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Get the intent to request storage permission.
     * For Android 11+ this opens system settings for MANAGE_EXTERNAL_STORAGE.
     * For older versions, returns null (use runtime permission request instead).
     */
    fun getStoragePermissionIntent(context: Context): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            null // Use runtime permission request
        }
    }

    /**
     * Get the permissions array for runtime permission request (pre-Android 11).
     */
    fun getLegacyStoragePermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses granular media permissions
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    /**
     * Check if legacy storage permissions are granted.
     */
    fun hasLegacyStoragePermission(context: Context): Boolean {
        return getLegacyStoragePermissions().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Show a rationale dialog explaining why storage permission is needed.
     */
    fun shouldShowPermissionRationale(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            !Environment.isExternalStorageManager()
        } else {
            getLegacyStoragePermissions().any {
                activity.shouldShowRequestPermissionRationale(it)
            }
        }
    }
}
