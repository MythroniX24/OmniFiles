package com.omnilabs.omfiles.ui.screens.recycle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnilabs.omfiles.domain.model.OperationResult
import com.omnilabs.omfiles.domain.repository.RecycleBinItem
import com.omnilabs.omfiles.domain.repository.RecycleBinRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecycleBinUiState(
    val items: List<RecycleBinItem> = emptyList(),
    val isLoading: Boolean = true,
    val totalSize: Long = 0L,
    val itemCount: Int = 0,
    val error: String? = null,
    val showEmptyConfirm: Boolean = false,
    val operationInProgress: Boolean = false
)

@HiltViewModel
class RecycleBinViewModel @Inject constructor(
    private val recycleBinRepository: RecycleBinRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecycleBinUiState())
    val uiState: StateFlow<RecycleBinUiState> = _uiState.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadItems()
    }

    fun loadItems() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val items = recycleBinRepository.getAllItems()
                val totalSize = recycleBinRepository.getTotalSize()
                val count = recycleBinRepository.getItemCount()
                _uiState.value = _uiState.value.copy(
                    items = items,
                    totalSize = totalSize,
                    itemCount = count,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                _error.value = "Failed to load recycle bin: ${e.message}"
            }
        }
    }

    fun restoreItem(id: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(operationInProgress = true)
            when (val result = recycleBinRepository.restoreItem(id)) {
                is OperationResult.Success -> {
                    _error.value = "Restored: ${result.data}"
                    loadItems()
                }
                is OperationResult.Error -> {
                    _uiState.value = _uiState.value.copy(operationInProgress = false)
                    _error.value = result.message
                }
            }
        }
    }

    fun permanentlyDelete(id: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(operationInProgress = true)
            when (val result = recycleBinRepository.permanentDelete(id)) {
                is OperationResult.Success -> {
                    _error.value = "Item permanently deleted"
                    loadItems()
                }
                is OperationResult.Error -> {
                    _uiState.value = _uiState.value.copy(operationInProgress = false)
                    _error.value = result.message
                }
            }
        }
    }

    fun showEmptyConfirm() {
        _uiState.value = _uiState.value.copy(showEmptyConfirm = true)
    }

    fun dismissEmptyConfirm() {
        _uiState.value = _uiState.value.copy(showEmptyConfirm = false)
    }

    fun emptyTrash() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(showEmptyConfirm = false, operationInProgress = true)
            when (val result = recycleBinRepository.emptyTrash()) {
                is OperationResult.Success -> {
                    _error.value = "Trash emptied"
                    loadItems()
                }
                is OperationResult.Error -> {
                    _uiState.value = _uiState.value.copy(operationInProgress = false)
                    _error.value = result.message
                }
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
