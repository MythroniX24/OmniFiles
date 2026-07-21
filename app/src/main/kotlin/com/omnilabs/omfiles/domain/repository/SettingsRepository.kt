package com.omnilabs.omfiles.domain.repository

import com.omnilabs.omfiles.domain.model.SortMode
import com.omnilabs.omfiles.domain.model.SortOrder
import com.omnilabs.omfiles.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val themeMode: Flow<ThemeMode>
    val dynamicColors: Flow<Boolean>
    val showHiddenFiles: Flow<Boolean>
    val sortMode: Flow<SortMode>
    val sortOrder: Flow<SortOrder>
    val foldersFirst: Flow<Boolean>

    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setDynamicColors(enabled: Boolean)
    suspend fun setShowHiddenFiles(show: Boolean)
    suspend fun setSortMode(mode: SortMode)
    suspend fun setSortOrder(order: SortOrder)
    suspend fun setFoldersFirst(foldersFirst: Boolean)
}
