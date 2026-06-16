package com.ilfforever.fujisync.ui.library

import android.animation.ValueAnimator
import com.ilfforever.fujisync.ui.overlay.OverlayLayer
import com.ilfforever.fujisync.ui.overlay.BackHandler
import com.ilfforever.fujisync.ui.overlay.overlayStackOf
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujisync.ui.components.DeleteConfirmDialog
import com.ilfforever.fujisync.ui.components.IconClose
import com.ilfforever.fujisync.ui.components.IconFilter
import com.ilfforever.fujisync.ui.components.IconPlus
import com.ilfforever.fujisync.ui.components.IconSearch
import com.ilfforever.fujisync.ui.components.Wordmark
import com.ilfforever.fujisync.ui.haptics.FujiHapticEffect
import com.ilfforever.fujisync.ui.haptics.FujiHaptics
import com.ilfforever.fujisync.ui.library.components.AddRecipeDrawer
import com.ilfforever.fujisync.ui.library.components.BottomDivider
import com.ilfforever.fujisync.ui.library.components.BulkGroupDialog
import com.ilfforever.fujisync.ui.library.components.CreateLibraryGroupDialog
import com.ilfforever.fujisync.ui.library.components.GroupHeader
import com.ilfforever.fujisync.ui.library.components.LibraryEmptySearchState
import com.ilfforever.fujisync.ui.library.components.LibraryFilterDialog
import com.ilfforever.fujisync.ui.library.components.LibraryFolderGridItem
import com.ilfforever.fujisync.ui.library.components.LibraryGroupCard
import com.ilfforever.fujisync.ui.library.components.LibraryGroupEditorSheet
import com.ilfforever.fujisync.ui.library.components.LibraryGroupToggleButton
import com.ilfforever.fujisync.ui.library.components.LibraryModeButton
import com.ilfforever.fujisync.ui.library.components.LibraryRecipeRow
import com.ilfforever.fujisync.ui.library.components.LibrarySearchField
import com.ilfforever.fujisync.ui.library.components.LibrarySelectionBar
import com.ilfforever.fujisync.ui.library.components.isGeneratedGroupId
import com.ilfforever.fujisync.ui.model.LibraryGroupStyle
import com.ilfforever.fujisync.ui.model.LibraryGroupUiModel
import com.ilfforever.fujisync.ui.model.LibraryRecipeUiModel
import com.ilfforever.fujisync.ui.theme.Bg
import com.ilfforever.fujisync.ui.theme.Gold
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.SansFamily
import com.ilfforever.fujisync.ui.theme.TextMuted
import com.ilfforever.fujisync.ui.theme.TextPrimary

@Composable
fun LibraryScreen(
    showImages: Boolean = false,
    showCardImageCount: Boolean = true,
    favoritesOnTop: Boolean = false,
    scrollToTopSignal: Boolean = false,
    onOpenItem: (LibraryRecipeUiModel) -> Unit,
    onCreateRecipe: () -> Unit,
    onAddGroupImage: (String) -> Unit,
    onImportFromPhoto: () -> Unit = {},
    onImportFromScreenshot: () -> Unit = {},
    onImportFromQr: () -> Unit = {},
    onScanTileGuide: () -> Unit = {},
    onComposeSet: () -> Unit = {},
) {
    val vm: LibraryViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val state by vm.screenState.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current
    fun haptic(effect: FujiHapticEffect) {
        FujiHaptics.perform(context, view, effect)
    }

    val enteredGroupIds = remember { mutableSetOf<String>() }
    var groupEntranceRun by remember { mutableStateOf(0) }
    DisposableEffect(Unit) { onDispose { enteredGroupIds.clear() } }
    var pullDistance by remember { mutableStateOf(0f) }
    var showAddDrawer by remember { mutableStateOf(false) }
    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()

    LaunchedEffect(scrollToTopSignal) {
        if (scrollToTopSignal) {
            vm.resetToTop()
            lazyListState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(favoritesOnTop) { vm.setFavoritesOnTop(favoritesOnTop) }

    LaunchedEffect(state.groupedView, state.openGroup) {
        if (state.groupedView && state.openGroup == null) {
            enteredGroupIds.clear()
            groupEntranceRun += 1
        }
    }

    val nestedScrollConnection = remember(state.openGroup) {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPreScroll(
                available: androidx.compose.ui.geometry.Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource,
            ): androidx.compose.ui.geometry.Offset {
                if (state.openGroup == null) return androidx.compose.ui.geometry.Offset.Zero
                if (available.y > 0 && lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0) {
                    pullDistance += available.y
                    return available
                }
                if (available.y < 0 && pullDistance > 0) {
                    val consumed = available.y.coerceAtLeast(-pullDistance)
                    pullDistance += consumed
                    return androidx.compose.ui.geometry.Offset(0f, consumed)
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }

            override suspend fun onPreFling(available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity {
                if (pullDistance > 450f) vm.setOpenGroup(null)
                pullDistance = 0f
                return androidx.compose.ui.unit.Velocity.Zero
            }
        }
    }

    val groupById = state.visibleGroups.associateBy { it.id }
    val visibleGrouped = remember(state.sorted, state.visibleGroups) {
        state.sorted
            .flatMap { recipe ->
                if (recipe.groupIds.isEmpty()) listOf(LibraryUngroupedFilterId to recipe)
                else recipe.groupIds.map { groupId -> groupId to recipe }
            }
            .groupBy({ it.first }, { it.second })
    }
    val allGrouped = remember(state.data.recipes, state.visibleGroups) {
        state.data.recipes
            .flatMap { recipe ->
                if (recipe.groupIds.isEmpty()) listOf(LibraryUngroupedFilterId to recipe)
                else recipe.groupIds.map { groupId -> groupId to recipe }
            }
            .groupBy({ it.first }, { it.second })
    }
    val groupCounts = remember(allGrouped) { allGrouped.mapValues { (_, r) -> r.size } }
    val searching = state.searchOpen || state.searchQuery.isNotBlank()
    val headerMotionEnabled = ValueAnimator.areAnimatorsEnabled()
    val headerEnterSpec = tween<Float>(durationMillis = if (headerMotionEnabled) 260 else 0, easing = FastOutSlowInEasing)
    val headerExitSpec = tween<Float>(durationMillis = if (headerMotionEnabled) 190 else 0, easing = FastOutSlowInEasing)
    val headerOffsetEnterSpec = if (headerMotionEnabled) {
        spring<IntOffset>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntOffset.VisibilityThreshold)
    } else tween(0)
    val headerOffsetExitSpec = if (headerMotionEnabled) {
        spring<IntOffset>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium, visibilityThreshold = IntOffset.VisibilityThreshold)
    } else tween(0)
    val headerSizeSpec = if (headerMotionEnabled) {
        spring<IntSize>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntSize.VisibilityThreshold)
    } else tween(0)

    overlayStackOf(
        OverlayLayer(state.openGroup != null) { vm.setOpenGroup(null) },
        OverlayLayer(state.editingGroup != null) { vm.setEditingGroup(null) },
        OverlayLayer(state.selectedRecipeIds.isNotEmpty()) { vm.clearSelection() },
        OverlayLayer(showAddDrawer) { showAddDrawer = false },
    ).BackHandler()

    Box(modifier = Modifier.fillMaxSize().background(Bg)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Bg)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.height(28.dp), contentAlignment = Alignment.CenterStart) {
                    Wordmark()
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(animationSpec = headerSizeSpec)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .animateContentSize(animationSpec = headerSizeSpec),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = !searching,
                        enter = fadeIn(headerEnterSpec) + slideInHorizontally(animationSpec = headerOffsetEnterSpec, initialOffsetX = { -it / 8 }),
                        exit = fadeOut(headerExitSpec) + slideOutHorizontally(animationSpec = headerOffsetExitSpec, targetOffsetX = { -it / 8 }),
                    ) {
                        Text("LIBRARY", fontFamily = SansFamily, fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = 0.4.sp, color = TextPrimary)
                    }
                    androidx.compose.animation.AnimatedVisibility(
                        visible = searching,
                        enter = fadeIn(headerEnterSpec) + slideInHorizontally(animationSpec = headerOffsetEnterSpec, initialOffsetX = { it / 10 }),
                        exit = fadeOut(headerExitSpec) + slideOutHorizontally(animationSpec = headerOffsetExitSpec, targetOffsetX = { it / 10 }),
                    ) {
                        LibrarySearchField(
                            value = state.searchQuery,
                            onValueChange = vm::setSearchQuery,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.height(36.dp),
                ) {
                    // Search / Close — always in the same slot, crossfade in-place
                    Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = !searching,
                            enter = fadeIn(headerEnterSpec),
                            exit = fadeOut(headerExitSpec),
                        ) {
                            IconButton(
                                onClick = {
                                    vm.setSearchOpen(true)
                                },
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(IconSearch, contentDescription = "Search", tint = TextPrimary, modifier = Modifier.size(20.dp))
                            }
                        }
                        androidx.compose.animation.AnimatedVisibility(
                            visible = searching,
                            enter = fadeIn(headerEnterSpec),
                            exit = fadeOut(headerExitSpec),
                        ) {
                            IconButton(
                                onClick = {
                                    if (state.searchQuery.isBlank()) vm.setSearchOpen(false) else vm.setSearchQuery("")
                                },
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(IconClose, contentDescription = "Close search", tint = TextPrimary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    // Filter — never moves
                    IconButton(
                        onClick = {
                            haptic(FujiHapticEffect.SheetOpen)
                            vm.setFilterOpen(true)
                        },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(IconFilter, contentDescription = "Filter and sort", tint = TextPrimary, modifier = Modifier.size(20.dp))
                    }
                    // + — fades out when search is active
                    androidx.compose.animation.AnimatedVisibility(
                        visible = !searching,
                        enter = fadeIn(headerEnterSpec),
                        exit = fadeOut(headerExitSpec),
                    ) {
                        Box(
                            modifier = Modifier.size(28.dp).clip(CircleShape).background(Gold).clickable(onClick = {
                                showAddDrawer = true
                            }),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(IconPlus, contentDescription = "Add", tint = Bg, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Count + sort row
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (state.selectedRecipeIds.isNotEmpty()) {
                            "${state.selectedRecipeIds.size} SELECTED"
                        } else if (state.activeFilterCount > 0 || state.searchQuery.isNotBlank()) {
                            "${state.sorted.size} OF ${state.data.recipes.size} RECIPES"
                        } else {
                            "${state.sorted.size} RECIPES"
                        },
                        fontFamily = MonoFamily,
                        fontSize = 11.sp,
                        letterSpacing = 1.4.sp,
                        color = if (state.selectedRecipeIds.isEmpty()) TextMuted else Gold,
                    )
                    Row(
                        modifier = Modifier.padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        LibraryGroupToggleButton(
                            groupedView = state.groupedView,
                            onClick = {
                                haptic(FujiHapticEffect.Confirm)
                                vm.setGroupedView(!state.groupedView)
                            },
                        )
                        LibraryModeButton(
                            groupedView = state.groupedView,
                            organizing = state.organizing,
                            openGroup = state.openGroup,
                            onClick = {
                                if (state.groupedView && state.openGroup == null) {
                                    vm.setCreateGroupOpen(true)
                                } else {
                                    vm.setOrganizing(!state.organizing)
                                }
                            },
                        )
                    }
                }
                BottomDivider()
            }

            // Recipe list
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize().nestedScroll(nestedScrollConnection),
            ) {
                if (state.sorted.isEmpty()) {
                    item(key = "library-empty-search") {
                        LibraryEmptySearchState(
                            searching = state.searchQuery.isNotBlank(),
                            loadError = state.data.loadError,
                        )
                    }
                }
                if (state.groupedView) {
                    if (state.openGroup == null) {
                        val groupEntries = state.visibleGroups
                            .mapNotNull { group ->
                                val groupRecipes = visibleGrouped[group.id].orEmpty()
                                if (groupRecipes.isNotEmpty() || group.id.isGeneratedGroupId()) {
                                    LibraryFolderGridItem(group = group, recipes = groupRecipes, count = groupCounts[group.id] ?: 0)
                                } else {
                                    null
                                }
                            }
                            .let { entries ->
                                val ungroupedRecipes = visibleGrouped[LibraryUngroupedFilterId].orEmpty()
                                if (ungroupedRecipes.isEmpty()) entries
                                else listOf(
                                    LibraryFolderGridItem(
                                        group = LibraryGroupUiModel(LibraryUngroupedFilterId, "Ungrouped"),
                                        recipes = ungroupedRecipes,
                                        count = groupCounts[LibraryUngroupedFilterId] ?: 0,
                                    ),
                                ) + entries
                            }
                        items(groupEntries.chunked(2).size, key = { rowIdx -> "group-card-row-$rowIdx" }) { rowIdx ->
                            val rowGroups = groupEntries.chunked(2)[rowIdx]
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                rowGroups.forEachIndexed { groupIdx, item ->
                                    val group = item.group
                                    androidx.compose.runtime.key(group.id, groupEntranceRun) {
                                        LibraryGroupCard(
                                            label = group.name,
                                            count = item.count,
                                            style = state.data.groupStyles[group.id] ?: LibraryGroupStyle(),
                                            entranceIndex = rowIdx * 2 + groupIdx,
                                            entranceRun = groupEntranceRun,
                                            alreadyEntered = group.id in enteredGroupIds,
                                            onEntered = { enteredGroupIds.add(group.id) },
                                            modifier = Modifier.weight(1f),
                                            onOpen = {
                                                haptic(FujiHapticEffect.SoftSelection)
                                                vm.setOpenGroup(group.id)
                                            },
                                            onOpenEditor = {
                                                if (state.editingGroup == null) {
                                                    haptic(FujiHapticEffect.SheetOpen)
                                                    vm.setEditingGroup(group.id)
                                                }
                                            },
                                        )
                                    }
                                }
                                if (rowGroups.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    } else {
                        val groupId = state.openGroup.orEmpty()
                        val group = if (groupId == LibraryUngroupedFilterId) {
                            LibraryGroupUiModel(groupId, "Ungrouped")
                        } else {
                            groupById[groupId] ?: LibraryGroupUiModel(groupId, "Library")
                        }
                        val groupRecipes = visibleGrouped[groupId].orEmpty()
                        item(key = "open-group-$groupId") {
                            GroupHeader(label = group.name, count = groupCounts[groupId] ?: 0, onBack = { vm.setOpenGroup(null) })
                        }
                        items(groupRecipes.size, key = { idx -> groupRecipes[idx].id }) { idx ->
                            val recipe = groupRecipes[idx]
                            LibraryRecipeRow(
                                recipe = recipe,
                                groups = state.visibleGroups,
                                groupCounts = groupCounts,
                                organizing = state.organizing,
                                showImages = showImages,
                                showCardImageCount = showCardImageCount,
                                customGroupRecipeId = state.customGroupRecipeId,
                                customGroupDraft = state.customGroupDraft,
                                onCustomGroupDraftChange = vm::setCustomGroupDraft,
                                onStartCustomGroup = { vm.setCustomGroupRecipeId(recipe.id) },
                                onCloseGroupPicker = { vm.setCustomGroupRecipeId(null) },
                                onSaveCustomGroup = {
                                    vm.createGroupForRecipe(recipe.id, state.customGroupDraft)
                                    vm.setCustomGroupRecipeId(null)
                                },
                                onChangeGroup = { vm.changeRecipeGroup(recipe.id, it) },
                                selected = recipe.id in state.selectedRecipeIds,
                                selectionMode = state.selectedRecipeIds.isNotEmpty(),
                                onToggleSelection = {
                                    vm.toggleSelection(recipe.id)
                                },
                                onStartSelection = {
                                    haptic(FujiHapticEffect.DragStart)
                                    vm.startSelection(recipe.id)
                                },
                                onOpenItem = {
                                    onOpenItem(recipe)
                                },
                                onToggleFavorite = {
                                    haptic(FujiHapticEffect.SoftSelection)
                                    vm.toggleFavorite(recipe.id)
                                },
                            )
                        }
                    }
                } else {
                    items(state.sorted.size, key = { idx -> state.sorted[idx].id }) { idx ->
                        val recipe = state.sorted[idx]
                        LibraryRecipeRow(
                            recipe = recipe,
                            groups = state.visibleGroups,
                            groupCounts = groupCounts,
                            organizing = state.organizing,
                            showImages = showImages,
                            showCardImageCount = showCardImageCount,
                            customGroupRecipeId = state.customGroupRecipeId,
                            customGroupDraft = state.customGroupDraft,
                            onCustomGroupDraftChange = vm::setCustomGroupDraft,
                            onStartCustomGroup = { vm.setCustomGroupRecipeId(recipe.id) },
                            onCloseGroupPicker = { vm.setCustomGroupRecipeId(null) },
                            onSaveCustomGroup = {
                                vm.createGroupForRecipe(recipe.id, state.customGroupDraft)
                                vm.setCustomGroupRecipeId(null)
                            },
                            onChangeGroup = { vm.changeRecipeGroup(recipe.id, it) },
                            selected = recipe.id in state.selectedRecipeIds,
                            selectionMode = state.selectedRecipeIds.isNotEmpty(),
                            onToggleSelection = {
                                vm.toggleSelection(recipe.id)
                            },
                            onStartSelection = {
                                haptic(FujiHapticEffect.DragStart)
                                vm.startSelection(recipe.id)
                            },
                            onOpenItem = {
                                onOpenItem(recipe)
                            },
                            onToggleFavorite = {
                                haptic(FujiHapticEffect.SoftSelection)
                                vm.toggleFavorite(recipe.id)
                            },
                        )
                    }
                }
                item { Spacer(Modifier.height(if (state.selectedRecipeIds.isEmpty()) 24.dp else 104.dp)) }
            }
        }

        state.editingGroup?.let { groupId ->
            val group = if (groupId == LibraryUngroupedFilterId) {
                LibraryGroupUiModel(groupId, "Ungrouped")
            } else {
                groupById[groupId] ?: LibraryGroupUiModel(groupId, "Library")
            }
            val isSystemGroup = groupId == LibraryUngroupedFilterId
            LibraryGroupEditorSheet(
                group = group.name,
                count = groupCounts[groupId] ?: 0,
                style = state.data.groupStyles[groupId] ?: LibraryGroupStyle(),
                canRename = !isSystemGroup,
                canDelete = !isSystemGroup,
                onChangeStyle = { vm.changeGroupStyle(groupId, it) },
                onRename = { nextName ->
                    val renamed = nextName.trim()
                    if (renamed.isNotBlank() && renamed != group.name) vm.renameGroup(groupId, renamed)
                },
                onAddImage = { onAddGroupImage(groupId) },
                onDelete = {
                    vm.setEditingGroup(null)
                    vm.deleteGroup(groupId)
                },
                onDismiss = { vm.setEditingGroup(null) },
            )
        }

        if (state.createGroupOpen) {
            CreateLibraryGroupDialog(
                onDismiss = { vm.setCreateGroupOpen(false) },
                onCreate = { name ->
                    vm.createGroup(name)
                    vm.setCreateGroupOpen(false)
                },
            )
        }

        AnimatedVisibility(
            visible = state.selectedRecipeIds.isNotEmpty(),
            enter = fadeIn(tween(120)) + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut(tween(120)) + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            LibrarySelectionBar(
                selectedCount = state.selectedRecipeIds.size,
                onClear = {
                    haptic(FujiHapticEffect.SheetDismiss)
                    vm.clearSelection()
                },
                onAddToGroup = {
                    haptic(FujiHapticEffect.SheetOpen)
                    vm.setBulkGroupOpen(true)
                },
                onDelete = {
                    haptic(FujiHapticEffect.Reject)
                    vm.setPendingBatchDelete(true)
                },
            )
        }

        if (state.bulkGroupOpen) {
            BulkGroupDialog(
                selectedCount = state.selectedRecipeIds.size,
                groups = state.visibleGroups,
                onDismiss = { vm.setBulkGroupOpen(false) },
                onSelectGroup = { groupId ->
                    haptic(FujiHapticEffect.Confirm)
                    vm.changeRecipesGroup(state.selectedRecipeIds, groupId)
                    vm.setBulkGroupOpen(false)
                },
                onCreateGroup = { name ->
                    haptic(FujiHapticEffect.Confirm)
                    vm.createGroupForRecipes(state.selectedRecipeIds, name)
                    vm.setBulkGroupOpen(false)
                },
            )
        }

        if (showAddDrawer) {
                AddRecipeDrawer(
                    onCreateRecipe = { haptic(FujiHapticEffect.Selection); showAddDrawer = false; onCreateRecipe() },
                    onImportFromPhoto = { haptic(FujiHapticEffect.Selection); showAddDrawer = false; onImportFromPhoto() },
                    onImportFromScreenshot = { haptic(FujiHapticEffect.Selection); showAddDrawer = false; onImportFromScreenshot() },
                    onImportFromQr = { haptic(FujiHapticEffect.Selection); showAddDrawer = false; onImportFromQr() },
                    onScanTileGuide = { haptic(FujiHapticEffect.Selection); showAddDrawer = false; onScanTileGuide() },
                    onComposeSet = { haptic(FujiHapticEffect.Selection); showAddDrawer = false; onComposeSet() },
                    onDismiss = { showAddDrawer = false },
                )
        }
    }

    if (state.filterOpen) {
        LibraryFilterDialog(
            sortBy = state.data.sort,
            onChangeSort = { vm.setSort(it) },
            filterFavorites = state.filterFavorites,
            onToggleFilterFavorites = { vm.setFilterFavorites(!state.filterFavorites) },
            photoFilter = state.filterPhotoState,
            onChangePhotoFilter = { vm.setFilterPhotoState(it) },
            groups = state.visibleGroups,
            groupCounts = groupCounts,
            selectedGroupId = state.filterGroupId,
            onSelectGroup = { vm.setFilterGroupId(it) },
            sourceOptions = state.sourceOptions,
            selectedSourceKey = state.filterSourceKey,
            onSelectSource = { vm.setFilterSourceKey(it) },
            filmSimOptions = state.filmSimOptions,
            selectedFilmSim = state.filterFilmSim,
            onSelectFilmSim = { vm.setFilterFilmSim(it) },
            activeFilterCount = state.activeFilterCount,
            onClearFilters = {
                haptic(FujiHapticEffect.SheetDismiss)
                vm.clearFilters()
            },
            onDismiss = {
                haptic(FujiHapticEffect.SheetDismiss)
                vm.setFilterOpen(false)
            },
        )
    }

    if (state.pendingBatchDelete) {
        val count = state.selectedRecipeIds.size
        DeleteConfirmDialog(
            title = if (count == 1) "1 Recipe" else "$count Recipes",
            body = "These recipes will be permanently removed from your library.",
            confirmLabel = "Delete",
            onConfirm = {
                vm.deleteRecipes(state.selectedRecipeIds)
                vm.setPendingBatchDelete(false)
            },
            onDismiss = { vm.setPendingBatchDelete(false) },
        )
    }
}
