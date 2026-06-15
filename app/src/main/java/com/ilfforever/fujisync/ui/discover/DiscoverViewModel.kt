package com.ilfforever.fujisync.ui.discover

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilfforever.fujisync.data.remote.FxwApi
import com.ilfforever.fujisync.data.remote.FxwRecipe
import com.ilfforever.fujisync.data.remote.FxwRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiscoverUiState(
    val recipes: List<FxwRecipe> = emptyList(),
    val initialLoading: Boolean = true,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 0,
    val hasMore: Boolean = true,
    val source: FxwRepository.Source? = null,
    val searchQuery: String = "",
    val committedQuery: String = "",
    val searchResults: List<FxwRecipe> = emptyList(),
    val searchLoading: Boolean = false,
    val searchError: String? = null,
)

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
) : ViewModel() {
    var state by mutableStateOf(DiscoverUiState())
        private set

    private var searchJob: Job? = null

    init {
        loadPage(1)
    }

    fun setSearchQuery(query: String) {
        state = state.copy(searchQuery = query, searchError = null)
        if (query.isBlank()) state = state.copy(searchResults = emptyList(), searchLoading = false, committedQuery = "")
    }

    fun submitSearch() {
        val query = state.searchQuery.trim()
        if (query.isBlank()) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            state = state.copy(searchLoading = true, committedQuery = query)
            runCatching { FxwApi.fetchRecipes(search = query, perPage = 20) }
                .onSuccess { state = state.copy(searchResults = it.recipes, searchLoading = false) }
                .onFailure { state = state.copy(searchLoading = false, searchError = it.localizedMessage ?: "Search failed") }
        }
    }

    fun refresh() {
        loadPage(page = 1, forceRefresh = true)
    }

    fun loadNextPage() {
        val current = state
        if (current.initialLoading || current.loadingMore || !current.hasMore) return
        loadPage(current.currentPage + 1)
    }

    private fun loadPage(page: Int, forceRefresh: Boolean = false) {
        val current = state
        if (current.initialLoading && page != 1) return
        if (current.loadingMore) return
        if (page == 1 && current.initialLoading && current.currentPage == 1) return

        viewModelScope.launch {
            state = if (page == 1) {
                current.copy(initialLoading = true, error = null)
            } else {
                current.copy(loadingMore = true, error = null)
            }

            runCatching {
                FxwRepository.loadPage(
                    context = appContext,
                    page = page,
                    forceRefresh = forceRefresh,
                )
            }.fold(
                onSuccess = { result ->
                    state = state.copy(
                        recipes = if (page == 1) result.recipes else state.recipes + result.recipes,
                        hasMore = result.hasMore,
                        currentPage = page,
                        source = result.source,
                        initialLoading = false,
                        loadingMore = false,
                        error = null,
                    )
                },
                onFailure = { error ->
                    state = state.copy(
                        initialLoading = false,
                        loadingMore = false,
                        error = error.localizedMessage ?: "Network error",
                    )
                },
            )
        }
    }
}
