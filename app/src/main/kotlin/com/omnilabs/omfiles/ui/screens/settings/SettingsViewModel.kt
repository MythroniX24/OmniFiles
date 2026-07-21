package com.omnilabs.omfiles.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnilabs.omfiles.domain.model.SortMode
import com.omnilabs.omfiles.domain.model.SortOrder
import com.omnilabs.omfiles.domain.model.ThemeMode
import com.omnilabs.omfiles.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColors: Boolean = true,
    val showHiddenFiles: Boolean = false,
    val sortMode: SortMode = SortMode.NAME,
    val sortOrder: SortOrder = SortOrder.ASCENDING,
    val foldersFirst: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.themeMode.collect { mode ->
                _uiState.value = _uiState.value.copy(themeMode = mode)
            }
        }
        viewModelScope.launch {
            settingsRepository.dynamicColors.collect { enabled ->
                _uiState.value = _uiState.value.copy(dynamicColors = enabled)
            }
        }
        viewModelScope.launch {
            settingsRepository.showHiddenFiles.collect { show ->
                _uiState.value = _uiState.value.copy(showHiddenFiles = show)
            }
        }
        viewModelScope.launch {
            settingsRepository.sortMode.collect { mode ->
                _uiState.value = _uiState.value.copy(sortMode = mode)
            }
        }
        viewModelScope.launch {
            settingsRepository.sortOrder.collect { order ->
                _uiState.value = _uiState.value.copy(sortOrder = order)
            }
        }
        viewModelScope.launch {
            settingsRepository.foldersFirst.collect { foldersFirst ->
                _uiState.value = _uiState.value.copy(foldersFirst = foldersFirst)
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setDynamicColors(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDynamicColors(enabled) }
    }

    fun setShowHiddenFiles(show: Boolean) {
        viewModelScope.launch { settingsRepository.setShowHiddenFiles(show) }
    }

    fun setSortMode(mode: SortMode) {
        viewModelScope.launch { settingsRepository.setSortMode(mode) }
    }

    fun setSortOrder(order: SortOrder) {
        viewModelScope.launch { settingsRepository.setSortOrder(order) }
    }

    fun setFoldersFirst(foldersFirst: Boolean) {
        viewModelScope.launch { settingsRepository.setFoldersFirst(foldersFirst) }
    }
}
