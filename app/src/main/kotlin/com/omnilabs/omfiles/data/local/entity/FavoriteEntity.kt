package com.omnilabs.omfiles.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val path: String,
    val name: String,
    val extension: String,
    @ColumnInfo(name = "is_directory")
    val isDirectory: Boolean,
    val size: Long,
    @ColumnInfo(name = "added_at")
    val addedAt: Long
)
