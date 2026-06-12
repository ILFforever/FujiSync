package com.paeki.fujirecipes.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paeki.fujirecipes.domain.model.canonicalFilmSimLabel
import com.paeki.fujirecipes.ui.LibraryUiState
import com.paeki.fujirecipes.ui.model.LibraryGroupStyle
import com.paeki.fujirecipes.ui.model.LibraryGroupUiModel
import com.paeki.fujirecipes.ui.model.LibraryRecipeUiModel
import com.paeki.fujirecipes.ui.model.sourceCameraDisplayName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

enum class LibraryPhotoFilter { All, WithPhotos, NoPhotos }

const val LibraryUngroupedFilterId = "__ungrouped__"

data class LibraryScreenState(
    // Data from LibraryStateHolder
    val data: LibraryUiState = LibraryUiState(),
    // Sort+filter+search → visible recipes
    val sorted: List<LibraryRecipeUiModel> = emptyList(),
    val visibleGroups: List<LibraryGroupUiModel> = emptyList(),
    val groupNamesById: Map<String, String> = emptyMap(),
    val filmSimOptions: List<String> = emptyList(),
    val sourceOptions: List<Pair<String, String>> = emptyList(),
    val activeFilterCount: Int = 0,
    // UI state
    val groupedView: Boolean = false,
    val organizing: Boolean = false,
    val openGroup: String? = null,
    val filterOpen: Boolean = false,
    val filterFavorites: Boolean = false,
    val filterPhotoState: LibraryPhotoFilter = LibraryPhotoFilter.All,
    val filterGroupId: String? = null,
    val filterSourceKey: String? = null,
    val filterFilmSim: String? = null,
    val editingGroup: String? = null,
    val createGroupOpen: Boolean = false,
    val selectedRecipeIds: Set<String> = emptySet(),
    val bulkGroupOpen: Boolean = false,
    val searchOpen: Boolean = false,
    val searchQuery: String = "",
    val pendingBatchDelete: Boolean = false,
    val customGroupRecipeId: String? = null,
    val customGroupDraft: String = "",
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    val holder: LibraryStateHolder,
) : ViewModel() {

    private val _ui = MutableStateFlow(LibraryScreenUiState())

    val screenState: StateFlow<LibraryScreenState> = combine(holder.state, _ui) { data, ui ->
        val visibleGroups = buildVisibleGroups(data.recipes, data.groups)
        val groupNamesById = visibleGroups.associate { it.id to it.name }
        val filmSimOptions = data.recipes.map { it.sim.canonicalFilmSimLabel() }.filter { it.isNotBlank() }.distinct().sorted()
        val sourceOptions = data.recipes
            .mapNotNull { r -> r.sourceFilterKey()?.let { it to r.sourceFilterLabel() } }
            .distinctBy { it.first }
            .sortedBy { it.second }

        val filtered = applyFilters(data.recipes, ui, groupNamesById)
        val sorted = applySort(filtered, data.sort, ui.favoritesOnTop)
        val activeFilterCount = listOf(
            ui.filterFavorites,
            ui.filterPhotoState != LibraryPhotoFilter.All,
            ui.filterGroupId != null,
            ui.filterSourceKey != null,
            ui.filterFilmSim != null,
        ).count { it }

        LibraryScreenState(
            data = data,
            sorted = sorted,
            visibleGroups = visibleGroups,
            groupNamesById = groupNamesById,
            filmSimOptions = filmSimOptions,
            sourceOptions = sourceOptions,
            activeFilterCount = activeFilterCount,
            groupedView = ui.groupedView,
            organizing = ui.organizing,
            openGroup = ui.openGroup,
            filterOpen = ui.filterOpen,
            filterFavorites = ui.filterFavorites,
            filterPhotoState = ui.filterPhotoState,
            filterGroupId = ui.filterGroupId,
            filterSourceKey = ui.filterSourceKey,
            filterFilmSim = ui.filterFilmSim,
            editingGroup = ui.editingGroup,
            createGroupOpen = ui.createGroupOpen,
            selectedRecipeIds = ui.selectedRecipeIds,
            bulkGroupOpen = ui.bulkGroupOpen,
            searchOpen = ui.searchOpen,
            searchQuery = ui.searchQuery,
            pendingBatchDelete = ui.pendingBatchDelete,
            customGroupRecipeId = ui.customGroupRecipeId,
            customGroupDraft = ui.customGroupDraft,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LibraryScreenState())

    // ── UI state mutations ────────────────────────────────────────────

    fun setGroupedView(on: Boolean) = _ui.update {
        it.copy(groupedView = on, openGroup = null, organizing = false, selectedRecipeIds = emptySet())
    }
    fun setOrganizing(on: Boolean) = _ui.update { it.copy(organizing = on, selectedRecipeIds = emptySet()) }
    fun setOpenGroup(groupId: String?) = _ui.update { it.copy(openGroup = groupId) }
    fun setFilterOpen(open: Boolean) = _ui.update { it.copy(filterOpen = open) }
    fun setFilterFavorites(on: Boolean) = _ui.update { it.copy(filterFavorites = on) }
    fun setFilterPhotoState(f: LibraryPhotoFilter) = _ui.update { it.copy(filterPhotoState = f) }
    fun setFilterGroupId(id: String?) = _ui.update { it.copy(filterGroupId = id) }
    fun setFilterSourceKey(key: String?) = _ui.update { it.copy(filterSourceKey = key) }
    fun setFilterFilmSim(sim: String?) = _ui.update { it.copy(filterFilmSim = sim) }
    fun clearFilters() = _ui.update {
        it.copy(
            filterFavorites = false,
            filterPhotoState = LibraryPhotoFilter.All,
            filterGroupId = null,
            filterSourceKey = null,
            filterFilmSim = null,
        )
    }
    fun setEditingGroup(groupId: String?) = _ui.update { it.copy(editingGroup = groupId) }
    fun setCreateGroupOpen(open: Boolean) = _ui.update { it.copy(createGroupOpen = open) }
    fun setSelectedRecipeIds(ids: Set<String>) = _ui.update { it.copy(selectedRecipeIds = ids) }
    fun toggleSelection(recipeId: String) = _ui.update {
        it.copy(selectedRecipeIds = if (recipeId in it.selectedRecipeIds) it.selectedRecipeIds - recipeId else it.selectedRecipeIds + recipeId)
    }
    fun startSelection(recipeId: String) = _ui.update { it.copy(selectedRecipeIds = it.selectedRecipeIds + recipeId) }
    fun clearSelection() = _ui.update { it.copy(selectedRecipeIds = emptySet(), bulkGroupOpen = false) }
    fun setBulkGroupOpen(open: Boolean) = _ui.update { it.copy(bulkGroupOpen = open) }
    fun setSearchOpen(open: Boolean) = _ui.update { it.copy(searchOpen = open) }
    fun setSearchQuery(query: String) = _ui.update { it.copy(searchQuery = query) }
    fun setPendingBatchDelete(pending: Boolean) = _ui.update { it.copy(pendingBatchDelete = pending) }
    fun setCustomGroupRecipeId(id: String?) = _ui.update { it.copy(customGroupRecipeId = id, customGroupDraft = "") }
    fun setCustomGroupDraft(draft: String) = _ui.update { it.copy(customGroupDraft = draft) }
    fun setFavoritesOnTop(on: Boolean) = _ui.update { it.copy(favoritesOnTop = on) }

    fun resetToTop() {
        _ui.update {
            it.copy(
                groupedView = false,
                openGroup = null,
                organizing = false,
                selectedRecipeIds = emptySet(),
                searchOpen = false,
                searchQuery = "",
                filterFavorites = false,
                filterPhotoState = LibraryPhotoFilter.All,
                filterGroupId = null,
                filterSourceKey = null,
                filterFilmSim = null,
            )
        }
    }

    // ── Data mutations (delegate to holder) ──────────────────────────

    fun changeRecipeGroup(recipeId: String, groupId: String) = holder.changeRecipeGroup(recipeId, groupId)
    fun changeRecipesGroup(ids: Set<String>, groupId: String) {
        holder.changeRecipesGroup(ids, groupId)
        clearSelection()
    }
    fun createGroupForRecipe(recipeId: String, name: String) = holder.createGroupForRecipe(recipeId, name)
    fun createGroup(name: String) = holder.createGroup(name)
    fun createGroupForRecipes(ids: Set<String>, name: String) {
        holder.createGroupForRecipes(ids, name)
        clearSelection()
    }
    fun deleteRecipes(ids: Set<String>) {
        holder.deleteRecipes(ids)
        clearSelection()
    }
    fun changeGroupStyle(groupId: String, style: LibraryGroupStyle) = holder.changeGroupStyle(groupId, style)
    fun renameGroup(groupId: String, name: String) = holder.renameGroup(groupId, name)
    fun deleteGroup(groupId: String) = holder.deleteGroup(groupId)
    fun toggleFavorite(recipeId: String) = holder.toggleFavorite(recipeId)
    fun setSort(sort: String) = holder.setSort(sort)

    // ── Helpers ───────────────────────────────────────────────────────

    private fun buildVisibleGroups(
        recipes: List<LibraryRecipeUiModel>,
        groups: List<LibraryGroupUiModel>,
    ): List<LibraryGroupUiModel> {
        val configured = groups.associateBy { it.id }
        val orphanGroups = recipes
            .flatMap { it.groupIds }
            .distinct()
            .filterNot { it in configured }
            .map { groupId -> LibraryGroupUiModel(id = groupId, name = "Library") }
        return groups + orphanGroups
    }

    private fun applyFilters(
        recipes: List<LibraryRecipeUiModel>,
        ui: LibraryScreenUiState,
        groupNamesById: Map<String, String>,
    ): List<LibraryRecipeUiModel> {
        val query = ui.searchQuery.trim()
        return recipes.filter { recipe ->
            val favoriteMatch = !ui.filterFavorites || recipe.favorite
            val photoMatch = when (ui.filterPhotoState) {
                LibraryPhotoFilter.All -> true
                LibraryPhotoFilter.WithPhotos -> recipe.referenceImageUris.isNotEmpty()
                LibraryPhotoFilter.NoPhotos -> recipe.referenceImageUris.isEmpty()
            }
            val groupMatch = when (ui.filterGroupId) {
                null -> true
                LibraryUngroupedFilterId -> recipe.groupIds.isEmpty()
                else -> recipe.groupIds.any { it == ui.filterGroupId }
            }
            val sourceMatch = ui.filterSourceKey == null || recipe.sourceFilterKey() == ui.filterSourceKey
            val filmSimMatch = ui.filterFilmSim == null || recipe.sim.canonicalFilmSimLabel() == ui.filterFilmSim
            val searchMatch = query.isBlank() ||
                recipe.name.contains(query, ignoreCase = true) ||
                recipe.sim.contains(query, ignoreCase = true) ||
                recipe.groupIds.any { groupId ->
                    groupNamesById[groupId]?.contains(query, ignoreCase = true) == true
                }
            favoriteMatch && photoMatch && groupMatch && sourceMatch && filmSimMatch && searchMatch
        }
    }

    private fun applySort(recipes: List<LibraryRecipeUiModel>, sortBy: String, favoritesOnTop: Boolean): List<LibraryRecipeUiModel> {
        val sorted = when (sortBy) {
            "OLDEST" -> recipes.reversed()
            "NAME_ASC" -> recipes.sortedBy { it.name.lowercase() }
            "NAME_DESC" -> recipes.sortedByDescending { it.name.lowercase() }
            else -> recipes
        }
        return if (favoritesOnTop) sorted.sortedByDescending { it.favorite } else sorted
    }
}

internal fun LibraryRecipeUiModel.sourceFilterKey(): String? {
    val name = sourceCameraName?.trim().orEmpty()
    val model = sourceCameraModel?.trim().orEmpty()
    return listOf(name, model).filter { it.isNotBlank() }.joinToString("|").ifBlank { null }
}

internal fun LibraryRecipeUiModel.sourceFilterLabel(): String =
    sourceCameraDisplayName() ?: "Unknown camera"

private data class LibraryScreenUiState(
    val groupedView: Boolean = false,
    val organizing: Boolean = false,
    val openGroup: String? = null,
    val filterOpen: Boolean = false,
    val filterFavorites: Boolean = false,
    val filterPhotoState: LibraryPhotoFilter = LibraryPhotoFilter.All,
    val filterGroupId: String? = null,
    val filterSourceKey: String? = null,
    val filterFilmSim: String? = null,
    val editingGroup: String? = null,
    val createGroupOpen: Boolean = false,
    val selectedRecipeIds: Set<String> = emptySet(),
    val bulkGroupOpen: Boolean = false,
    val searchOpen: Boolean = false,
    val searchQuery: String = "",
    val pendingBatchDelete: Boolean = false,
    val customGroupRecipeId: String? = null,
    val customGroupDraft: String = "",
    val favoritesOnTop: Boolean = false,
)
