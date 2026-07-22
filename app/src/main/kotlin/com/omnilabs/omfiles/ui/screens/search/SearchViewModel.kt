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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<FileInfo> = emptyList(),
    val suggestions: List<FileInfo> = emptyList(),
    val isSearching: Boolean = false,
    val isIndexed: Boolean = false,
    val isIndexing: Boolean = false,
    val indexingProgress: Int = 0,
    val indexingTotal: Int = 0,
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
    private var suggestJob: Job? = null

    init {
        // Check if index exists on creation
        viewModelScope.launch {
            val status = searchRepository.getIndexStatus()
            _uiState.value = _uiState.value.copy(
                isIndexed = status.indexedFilesCount > 0,
                indexedCount = status.indexedFilesCount
            )

            // Auto-index on first launch (small delay so UI loads first)
            if (searchRepository.isIndexNeeded()) {
                delay(500)
                indexStorage()
            }
        }
    }

    fun onQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        searchJob?.cancel()
        suggestJob?.cancel()

        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(
                results = emptyList(),
                suggestions = emptyList(),
                isSearching = false
            )
            return
        }

        // Show suggestions immediately (prefix match, < 10 ms)
        suggestJob = viewModelScope.launch {
            searchRepository.suggest(query).collectLatest { suggestions ->
                _uiState.value = _uiState.value.copy(suggestions = suggestions)
            }
        }

        // Full search with 300ms debounce
        searchJob = viewModelScope.launch {
            delay(300)
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
            _uiState.value = _uiState.value.copy(isIndexing = true, isSearching = true)
            val result = searchRepository.indexFolder(
                path = "/storage/emulated/0",
                onProgress = { indexed, total ->
                    _uiState.value = _uiState.value.copy(
                        isIndexing = true,
                        indexingProgress = indexed,
                        indexingTotal = total
                    )
                }
            )
            when (result) {
                is OperationResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isIndexed = true,
                        indexedCount = result.data,
                        isIndexing = false,
                        isSearching = false
                    )
                }
                is OperationResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.message,
                        isIndexing = false,
                        isSearching = false
                    )
                }
            }
        }
    }

    fun clearSearch() {
        _uiState.value = SearchUiState(
            isIndexed = _uiState.value.isIndexed,
            indexedCount = _uiState.value.indexedCount
        )
        searchJob?.cancel()
        suggestJob?.cancel()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun dismissSuggestions() {
        _uiState.value = _uiState.value.copy(suggestions = emptyList())
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

        searchRepository.search(filters).collectLatest { results ->
            _uiState.value = _uiState.value.copy(
                results = results,
                isSearching = false
            )
        }
    }
}
