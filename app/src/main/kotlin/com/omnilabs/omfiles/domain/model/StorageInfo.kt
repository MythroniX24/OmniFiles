package com.omnilabs.omfiles.domain.model

data class StorageInfo(
    val path: String,
    val label: String,
    val type: StorageType,
    val totalSpace: Long,
    val freeSpace: Long,
    val usedSpace: Long,
    val isAvailable: Boolean,
    val isPrimary: Boolean
) {
    val usedPercentage: Float
        get() = if (totalSpace > 0) (usedSpace.toFloat() / totalSpace.toFloat()) * 100f else 0f
}

enum class StorageType {
    INTERNAL,
    SD_CARD,
    USB_OTG
}
