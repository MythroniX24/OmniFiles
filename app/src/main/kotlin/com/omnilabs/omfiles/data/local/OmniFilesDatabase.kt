package com.omnilabs.omfiles.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.omnilabs.omfiles.data.local.dao.FavoriteDao
import com.omnilabs.omfiles.data.local.dao.RecentFileDao
import com.omnilabs.omfiles.data.local.dao.SearchIndexDao
import com.omnilabs.omfiles.data.local.entity.FavoriteEntity
import com.omnilabs.omfiles.data.local.entity.RecentFileEntity
import com.omnilabs.omfiles.data.local.entity.SearchIndexEntity

@Database(
    entities = [
        FavoriteEntity::class,
        RecentFileEntity::class,
        SearchIndexEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class OmniFilesDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun recentFileDao(): RecentFileDao
    abstract fun searchIndexDao(): SearchIndexDao

    companion object {
        const val DATABASE_NAME = "omnifiles.db"
    }
}
