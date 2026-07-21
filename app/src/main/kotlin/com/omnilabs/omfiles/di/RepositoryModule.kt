package com.omnilabs.omfiles.di

import com.omnilabs.omfiles.data.repository.ArchiveRepositoryImpl
import com.omnilabs.omfiles.data.repository.FavoriteRepositoryImpl
import com.omnilabs.omfiles.data.repository.FileRepositoryImpl
import com.omnilabs.omfiles.data.repository.RecentFilesRepositoryImpl
import com.omnilabs.omfiles.data.repository.SearchRepositoryImpl
import com.omnilabs.omfiles.data.repository.SettingsRepositoryImpl
import com.omnilabs.omfiles.domain.repository.ArchiveRepository
import com.omnilabs.omfiles.domain.repository.FavoriteRepository
import com.omnilabs.omfiles.domain.repository.FileRepository
import com.omnilabs.omfiles.domain.repository.RecentFilesRepository
import com.omnilabs.omfiles.domain.repository.SearchRepository
import com.omnilabs.omfiles.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFileRepository(impl: FileRepositoryImpl): FileRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindFavoriteRepository(impl: FavoriteRepositoryImpl): FavoriteRepository

    @Binds
    @Singleton
    abstract fun bindRecentFilesRepository(impl: RecentFilesRepositoryImpl): RecentFilesRepository

    @Binds
    @Singleton
    abstract fun bindArchiveRepository(impl: ArchiveRepositoryImpl): ArchiveRepository

    @Binds
    @Singleton
    abstract fun bindSearchRepository(impl: SearchRepositoryImpl): SearchRepository
}
