package com.omnilabs.omfiles.di

import com.omnilabs.omfiles.data.storage.StorageEngineProviderImpl
import com.omnilabs.omfiles.data.storage.engine.LocalStorageEngine
import com.omnilabs.omfiles.data.storage.engine.MediaStoreStorageEngine
import com.omnilabs.omfiles.data.storage.engine.SafStorageEngine
import com.omnilabs.omfiles.domain.storage.StorageEngineProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StorageModule {

    @Binds
    @Singleton
    abstract fun bindStorageEngineProvider(impl: StorageEngineProviderImpl): StorageEngineProvider
}
