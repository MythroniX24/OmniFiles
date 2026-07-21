package com.omnilabs.omfiles.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.omnilabs.omfiles.domain.model.SortMode
import com.omnilabs.omfiles.domain.model.SortOrder
import com.omnilabs.omfiles.domain.model.ThemeMode
import com.omnilabs.omfiles.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    companion object {
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
        private val SHOW_HIDDEN = booleanPreferencesKey("show_hidden")
        private val SORT_MODE = stringPreferencesKey("sort_mode")
        private val SORT_ORDER = stringPreferencesKey("sort_order")
        private val FOLDERS_FIRST = booleanPreferencesKey("folders_first")
    }

    override val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        prefs[THEME_MODE]?.let { mode ->
            try { ThemeMode.valueOf(mode) } catch (_: Exception) { ThemeMode.SYSTEM }
        } ?: ThemeMode.SYSTEM
    }

    override val dynamicColors: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[DYNAMIC_COLORS] ?: true
    }

    override val showHiddenFiles: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SHOW_HIDDEN] ?: false
    }

    override val sortMode: Flow<SortMode> = context.dataStore.data.map { prefs ->
        prefs[SORT_MODE]?.let { mode ->
            try { SortMode.valueOf(mode) } catch (_: Exception) { SortMode.NAME }
        } ?: SortMode.NAME
    }

    override val sortOrder: Flow<SortOrder> = context.dataStore.data.map { prefs ->
        prefs[SORT_ORDER]?.let { order ->
            try { SortOrder.valueOf(order) } catch (_: Exception) { SortOrder.ASCENDING }
        } ?: SortOrder.ASCENDING
    }

    override val foldersFirst: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[FOLDERS_FIRST] ?: true
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs -> prefs[THEME_MODE] = mode.name }
    }

    override suspend fun setDynamicColors(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[DYNAMIC_COLORS] = enabled }
    }

    override suspend fun setShowHiddenFiles(show: Boolean) {
        context.dataStore.edit { prefs -> prefs[SHOW_HIDDEN] = show }
    }

    override suspend fun setSortMode(mode: SortMode) {
        context.dataStore.edit { prefs -> prefs[SORT_MODE] = mode.name }
    }

    override suspend fun setSortOrder(order: SortOrder) {
        context.dataStore.edit { prefs -> prefs[SORT_ORDER] = order.name }
    }

    override suspend fun setFoldersFirst(foldersFirst: Boolean) {
        context.dataStore.edit { prefs -> prefs[FOLDERS_FIRST] = foldersFirst }
    }
}
