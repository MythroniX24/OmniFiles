package com.omnilabs.omfiles.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions

/**
 * FTS4 virtual table for ultra-fast full-text search on file names.
 *
 * Room auto-creates triggers that keep this FTS index in sync with the
 * [SearchIndexEntity] content table whenever insert/update/delete happens.
 * FTS4 MATCH queries are ~100x faster than SQLite LIKE '%query%' because
 * they use a segmented inverted index rather than full table scans.
 *
 * The unicode61 tokenizer handles Unicode correctly (Hindi, Chinese, etc.)
 * and splits on word boundaries so "myfile" matches "myfile.txt" with MATCH 'myfile*'.
 */
@Fts4(
    contentEntity = SearchIndexEntity::class,
    tokenizer = FtsOptions.TOKENIZER_UNICODE61,
    tokenizerArgs = ["tokenchars=1"]
)
@Entity(tableName = "search_index_fts")
data class SearchIndexFts(
    /** Lowercased file/folder name — the primary searchable field */
    @ColumnInfo(name = "name")
    val name: String,

    /** Parent path for scope-limited searches */
    @ColumnInfo(name = "parent_path")
    val parentPath: String
)
