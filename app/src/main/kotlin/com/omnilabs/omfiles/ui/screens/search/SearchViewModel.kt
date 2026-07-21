package com.omnilabs.omfiles.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnilabs.omfiles.domain.model.FileInfo
import com.omnilabs.omfiles.domain.model.OperationResult
import com.omnilabs.omfiles.domain.repository.SearchFilters
import com.omnilabs.omfiles.domain.repository.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<FileInfo> = emptyList(),
    val isSearching: Boolean = false,
    val isIndexed: Boolean = false,
    val indexedCount: Int = 0,
    val error: String? = null,
    val extension: String? = null,
    val minSize: Long? = null,
    val maxSize: Long? = null
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // debounce
            performSearch()
        }
    }

    fun onExtensionFilter(extension: String?) {
        _uiState.value = _uiState.value.copy(extension = extension)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(100)
            performSearch()
        }
    }

    fun onSizeFilter(minSize: Long?, maxSize: Long?) {
        _uiState.value = _uiState.value.copy(minSize = minSize, maxSize = maxSize)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(100)
            performSearch()
        }
    }

    fun indexStorage() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true)
            val result = searchRepository.indexFolder("/storage/emulated/0")
            when (result) {
                is OperationResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isIndexed = true,
                        indexedCount = result.data,
                        isSearching = false
                    )
                }
                is OperationResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.message,
                        isSearching = false
                    )
                }
            }
        }
    }

    fun clearSearch() {
        _uiState.value = SearchUiState()
        searchJob?.cancel()
    }

    private suspend fun performSearch() {
        val state = _uiState.value
        if (state.query.isBlank()) {
            _uiState.value = state.copy(results = emptyList(), isSearching = false)
            return
        }

        _uiState.value = state.copy(isSearching = true)

        val filters = SearchFilters(
            query = state.query,
            extension = state.extension,
            minSize = state.minSize,
            maxSize = state.maxSize
        )

        searchRepository.search(filters).collect { results ->
            _uiState.value = _uiState.value.copy(
                results = results,
                isSearching = false
            )
        }
    }
}
