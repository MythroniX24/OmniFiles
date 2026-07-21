package com.omnilabs.omfiles.di

import android.content.Context
import androidx.room.Room
import com.omnilabs.omfiles.data.local.OmniFilesDatabase
import com.omnilabs.omfiles.data.local.dao.FavoriteDao
import com.omnilabs.omfiles.data.local.dao.RecentFileDao
import com.omnilabs.omfiles.data.local.dao.SearchIndexDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): OmniFilesDatabase {
        return Room.databaseBuilder(
            context,
            OmniFilesDatabase::class.java,
            OmniFilesDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideFavoriteDao(database: OmniFilesDatabase): FavoriteDao {
        return database.favoriteDao()
    }

    @Provides
    fun provideRecentFileDao(database: OmniFilesDatabase): RecentFileDao {
        return database.recentFileDao()
    }

    @Provides
    fun provideSearchIndexDao(database: OmniFilesDatabase): SearchIndexDao {
        return database.searchIndexDao()
    }
}
