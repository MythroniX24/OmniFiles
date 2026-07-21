package com.omnilabs.omfiles.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "search_index",
    indices = [
        Index(value = ["name"]),
        Index(value = ["extension"]),
        Index(value = ["parent_path"])
    ]
)
data class SearchIndexEntity(
    @PrimaryKey
    val path: String,
    val name: String,
    val extension: String,
    @ColumnInfo(name = "is_directory")
    val isDirectory: Boolean,
    val size: Long,
    @ColumnInfo(name = "last_modified")
    val lastModified: Long,
    @ColumnInfo(name = "parent_path")
    val parentPath: String
)
