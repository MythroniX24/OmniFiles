package com.omnilabs.omfiles.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.omnilabs.omfiles.data.local.dao.FavoriteDao
import com.omnilabs.omfiles.data.local.dao.RecentFileDao
import com.omnilabs.omfiles.data.local.dao.RecycleBinDao
import com.omnilabs.omfiles.data.local.dao.SearchIndexDao
import com.omnilabs.omfiles.data.local.entity.FavoriteEntity
import com.omnilabs.omfiles.data.local.entity.RecentFileEntity
import com.omnilabs.omfiles.data.local.entity.RecycleBinEntity
import com.omnilabs.omfiles.data.local.entity.SearchIndexEntity
import com.omnilabs.omfiles.data.local.entity.SearchIndexFts

@Database(
    entities = [
        FavoriteEntity::class,
        RecentFileEntity::class,
        SearchIndexEntity::class,
        SearchIndexFts::class,
        RecycleBinEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class OmniFilesDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun recentFileDao(): RecentFileDao
    abstract fun searchIndexDao(): SearchIndexDao
    abstract fun recycleBinDao(): RecycleBinDao

    companion object {
        const val DATABASE_NAME = "omnifiles.db"
    }
}
