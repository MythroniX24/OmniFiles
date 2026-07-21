package com.omnilabs.omfiles.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recycle_bin")
data class RecycleBinEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalPath: String,
    val trashPath: String,
    val name: String,
    val extension: String,
    @ColumnInfo(name = "is_directory")
    val isDirectory: Boolean,
    val size: Long,
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long,
    @ColumnInfo(name = "original_parent")
    val originalParent: String
)
