package com.omnilabs.omfiles.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FormatUtilsTest {

    @Test
    fun formatFileSize_returnsBytes() {
        assertThat(formatFileSize(0)).isEqualTo("0 B")
        assertThat(formatFileSize(500)).isEqualTo("500 B")
        assertThat(formatFileSize(1023)).isEqualTo("1023 B")
    }

    @Test
    fun formatFileSize_returnsKB() {
        assertThat(formatFileSize(1024)).isEqualTo("1.0 KB")
        assertThat(formatFileSize(2048)).isEqualTo("2.0 KB")
        assertThat(formatFileSize(1536)).isEqualTo("1.5 KB")
    }

    @Test
    fun formatFileSize_returnsMB() {
        assertThat(formatFileSize(1024 * 1024)).isEqualTo("1.0 MB")
        assertThat(formatFileSize(5 * 1024 * 1024)).isEqualTo("5.0 MB")
    }

    @Test
    fun formatFileSize_returnsGB() {
        val gb = 1024L * 1024 * 1024
        assertThat(formatFileSize(gb)).isEqualTo("1.0 GB")
        assertThat(formatFileSize(2 * gb)).isEqualTo("2.0 GB")
    }

    @Test
    fun formatFileSize_returnsTB() {
        val tb = 1024L * 1024 * 1024 * 1024
        assertThat(formatFileSize(tb)).isEqualTo("1.0 TB")
    }

    @Test
    fun formatFileSize_negative_returnsUnknown() {
        assertThat(formatFileSize(-1)).isEqualTo("Unknown")
    }

    @Test
    fun formatStorageSize_returnsFormatted() {
        assertThat(formatStorageSize(0)).isEqualTo("0 B")
        assertThat(formatStorageSize(1024)).isEqualTo("1 KB")
        assertThat(formatStorageSize(1024 * 1024)).isEqualTo("1 MB")
        assertThat(formatStorageSize(1024 * 1024 * 1024)).isEqualTo("1 GB")
    }

    @Test
    fun formatPercentage_roundsCorrectly() {
        assertThat(formatPercentage(50.5f)).isEqualTo("50%")
        assertThat(formatPercentage(99.9f)).isEqualTo("99%")
        assertThat(formatPercentage(100f)).isEqualTo("100%")
    }
}
