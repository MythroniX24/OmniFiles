package com.omnilabs.omfiles.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnilabs.omfiles.domain.model.FileInfo
import com.omnilabs.omfiles.domain.model.StorageInfo
import com.omnilabs.omfiles.domain.repository.FavoriteRepository
import com.omnilabs.omfiles.domain.repository.FileRepository
import com.omnilabs.omfiles.domain.repository.RecentFilesRepository
import com.omnilabs.omfiles.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val storageVolumes: List<StorageInfo> = emptyList(),
    val recentFiles: List<FileInfo> = emptyList(),
    val favorites: List<FileInfo> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val showHidden: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val fileRepository: FileRepository,
    private val recentFilesRepository: RecentFilesRepository,
    private val favoriteRepository: FavoriteRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isLoading = MutableStateFlow(true)

    private val _storageVolumes = MutableStateFlow<List<StorageInfo>>(emptyList())

    val uiState: StateFlow<HomeUiState> = combine(
        _storageVolumes,
        recentFilesRepository.getRecentFiles(),
        favoriteRepository.getFavorites(),
        settingsRepository.showHiddenFiles,
        _isLoading
    ) { volumes, recent, favorites, showHidden, loading ->
        HomeUiState(
            storageVolumes = volumes,
            recentFiles = recent,
            favorites = favorites,
            isLoading = loading,
            showHidden = showHidden
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _storageVolumes.value = fileRepository.getStorageVolumes()
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Failed to load data: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                // Storage volumes are loaded reactively
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Failed to load data: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun refresh() {
        loadData()
    }
}
