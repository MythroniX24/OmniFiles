package com.omnilabs.omfiles.utils

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
private val sizeFormat = DecimalFormat("#,##0.#")

fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 0 -> "Unknown"
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> formatDecimal(bytes / 1024.0) + " KB"
        bytes < 1024 * 1024 * 1024 -> formatDecimal(bytes / (1024.0 * 1024.0)) + " MB"
        bytes < 1024L * 1024 * 1024 * 1024 -> formatDecimal(bytes / (1024.0 * 1024.0 * 1024.0)) + " GB"
        else -> formatDecimal(bytes / (1024.0 * 1024.0 * 1024.0 * 1024.0)) + " TB"
    }
}

fun formatStorageSize(bytes: Long): String {
    return when {
        bytes < 0 -> "0 B"
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        bytes < 1024L * 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024 * 1024)} GB"
        else -> "${bytes / (1024L * 1024 * 1024 * 1024)} TB"
    }
}

fun formatDate(timestamp: Long): String {
    return try {
        dateFormat.format(Date(timestamp))
    } catch (_: Exception) {
        "Unknown"
    }
}

fun formatPercentage(value: Float): String {
    return "${value.toInt()}%"
}

private fun formatDecimal(value: Double): String {
    return if (value < 10) {
        String.format("%.1f", value)
    } else {
        String.format("%.0f", value)
    }
}
