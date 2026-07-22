package com.omnilabs.omfiles.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnilabs.omfiles.domain.model.FileInfo
import com.omnilabs.omfiles.domain.repository.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<FileInfo> = emptyList(),
    val isSearching: Boolean = false,
    val error: String? = null
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

        if (query.trim().length < 1) {
            _uiState.value = _uiState.value.copy(results = emptyList(), isSearching = false)
            return
        }

        // Debounce 200ms then search directly
        searchJob = viewModelScope.launch {
            delay(200)
            performSearch()
        }
    }

    fun clearSearch() {
        _uiState.value = SearchUiState()
        searchJob?.cancel()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private suspend fun performSearch() {
        val state = _uiState.value
        if (state.query.isBlank()) {
            _uiState.value = state.copy(results = emptyList(), isSearching = false)
            return
        }

        _uiState.value = state.copy(isSearching = true)

        try {
            searchRepository.search(state.query).collectLatest { results ->
                _uiState.value = _uiState.value.copy(
                    results = results,
                    isSearching = false
                )
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                error = "Search failed: ${e.message}",
                isSearching = false
            )
        }
    }
}
