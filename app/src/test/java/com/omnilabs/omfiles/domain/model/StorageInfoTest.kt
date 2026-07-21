package com.omnilabs.omfiles.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StorageInfoTest {

    @Test
    fun usedPercentage_calculatesCorrectly() {
        val storage = StorageInfo(
            path = "/storage",
            label = "Internal",
            type = StorageType.INTERNAL,
            totalSpace = 100_000,
            freeSpace = 25_000,
            usedSpace = 75_000,
            isAvailable = true,
            isPrimary = true
        )
        assertThat(storage.usedPercentage).isWithin(0.01f).of(75f)
    }

    @Test
    fun usedPercentage_zeroTotal_returnsZero() {
        val storage = StorageInfo(
            path = "/storage",
            label = "Internal",
            type = StorageType.INTERNAL,
            totalSpace = 0,
            freeSpace = 0,
            usedSpace = 0,
            isAvailable = true,
            isPrimary = true
        )
        assertThat(storage.usedPercentage).isEqualTo(0f)
    }

    @Test
    fun usedPercentage_fullDisk_returnsHundred() {
        val storage = StorageInfo(
            path = "/storage",
            label = "Internal",
            type = StorageType.INTERNAL,
            totalSpace = 100_000,
            freeSpace = 0,
            usedSpace = 100_000,
            isAvailable = true,
            isPrimary = true
        )
        assertThat(storage.usedPercentage).isWithin(0.01f).of(100f)
    }
}
