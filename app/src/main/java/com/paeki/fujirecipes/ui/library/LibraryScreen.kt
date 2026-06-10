package com.paeki.fujirecipes.ui.library

import android.animation.ValueAnimator
import android.graphics.BitmapFactory
import android.net.Uri
import com.paeki.fujirecipes.ui.overlay.OverlayLayer
import com.paeki.fujirecipes.ui.overlay.BackHandler
import com.paeki.fujirecipes.ui.overlay.overlayStackOf
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.paeki.fujirecipes.ui.components.DeleteConfirmDialog
import com.paeki.fujirecipes.ui.components.IconCheck
import com.paeki.fujirecipes.ui.components.IconChevronRight
import com.paeki.fujirecipes.ui.components.IconClose
import com.paeki.fujirecipes.ui.components.IconEdit
import com.paeki.fujirecipes.ui.components.IconFilter
import com.paeki.fujirecipes.ui.components.IconFolder
import com.paeki.fujirecipes.ui.components.IconMoreVertical
import com.paeki.fujirecipes.ui.components.IconPlus
import com.paeki.fujirecipes.ui.components.IconSearch
import com.paeki.fujirecipes.ui.components.IconStar
import com.paeki.fujirecipes.ui.components.IconStarFilled
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.emoji2.emojipicker.EmojiPickerView
import com.paeki.fujirecipes.ui.components.IconMoreVertical
import com.paeki.fujirecipes.ui.components.Pill
import com.paeki.fujirecipes.ui.components.Wordmark
import com.paeki.fujirecipes.ui.components.BlurredImagePlaceholder
import com.paeki.fujirecipes.ui.components.ImageLoadState
import com.paeki.fujirecipes.ui.components.decodeSampledBitmap
import com.paeki.fujirecipes.ui.components.shimmerBrush
import com.paeki.fujirecipes.ui.model.LibraryGroupUiModel
import com.paeki.fujirecipes.ui.model.LibraryGroupStyle
import com.paeki.fujirecipes.ui.model.LibraryRecipeUiModel
import com.paeki.fujirecipes.ui.model.sourceCameraDisplayName
import com.paeki.fujirecipes.ui.haptics.FujiHapticEffect
import com.paeki.fujirecipes.ui.haptics.FujiHaptics
import com.paeki.fujirecipes.ui.theme.Bg
import com.paeki.fujirecipes.ui.theme.Border
import com.paeki.fujirecipes.ui.theme.Gold
import com.paeki.fujirecipes.ui.theme.MonoFamily
import com.paeki.fujirecipes.ui.theme.PanelLow
import com.paeki.fujirecipes.ui.theme.SansFamily
import com.paeki.fujirecipes.ui.theme.TextDim
import com.paeki.fujirecipes.ui.theme.TextMuted
import com.paeki.fujirecipes.ui.theme.TextPrimary
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibraryScreen(
    showImages: Boolean = false,
    scrollToTopSignal: Boolean = false,
    onOpenItem: (LibraryRecipeUiModel) -> Unit,
    onCreateRecipe: () -> Unit,
    onAddGroupImage: (String) -> Unit,
    onImportFromPhoto: () -> Unit = {},
    onImportFromScreenshot: () -> Unit = {},
    onImportFromQr: () -> Unit = {},
    onScanTileGuide: () -> Unit = {},
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
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { enteredGroupIds.clear() } }
    var pullDistance by remember { mutableStateOf(0f) }
    var showAddDrawer by remember { mutableStateOf(false) }
    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()

    LaunchedEffect(scrollToTopSignal) {
        if (scrollToTopSignal) {
            vm.resetToTop()
            lazyListState.animateScrollToItem(0)
        }
    }

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
    val headerEnterSpec = tween<Float>(durationMillis = if (headerMotionEnabled) 210 else 0, easing = FastOutSlowInEasing)
    val headerExitSpec = tween<Float>(durationMillis = if (headerMotionEnabled) 150 else 0, easing = FastOutSlowInEasing)
    val headerOffsetEnterSpec = tween<IntOffset>(durationMillis = if (headerMotionEnabled) 210 else 0, easing = FastOutSlowInEasing)
    val headerOffsetExitSpec = tween<IntOffset>(durationMillis = if (headerMotionEnabled) 150 else 0, easing = FastOutSlowInEasing)
    val headerSizeSpec = tween<IntSize>(durationMillis = if (headerMotionEnabled) 240 else 0, easing = FastOutSlowInEasing)

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
                modifier = Modifier.fillMaxSize(),
            ) {
                if (state.sorted.isEmpty()) {
                    item(key = "library-empty-search") {
                        LibraryEmptySearchState(searching = state.searchQuery.isNotBlank())
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

@Composable
private fun LibrarySearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(PanelGroupBg)
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(IconSearch, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = SansFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = TextPrimary,
            ),
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                if (value.isBlank()) {
                    Text(
                        text = "Search name, group, or film simulation",
                        fontFamily = SansFamily,
                        fontSize = 13.sp,
                        color = TextDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                innerTextField()
            },
        )
    }
}

@Composable
private fun LibraryEmptySearchState(searching: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 42.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (searching) "NO MATCHES" else "NO RECIPES",
            fontFamily = SansFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            letterSpacing = 1.2.sp,
            color = TextPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (searching) "Try another name, group, or film simulation." else "Saved recipes will appear here.",
            fontFamily = SansFamily,
            fontSize = 13.sp,
            color = TextDim,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LibraryGroupToggleButton(
    groupedView: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(if (groupedView) Gold else Color.Transparent)
            .border(1.dp, if (groupedView) Gold else Border, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            IconFolder,
            contentDescription = "Grouped view",
            tint = if (groupedView) Bg else TextPrimary,
            modifier = Modifier.size(17.dp),
        )
    }
}

@Composable
private fun LibraryModeButton(
    groupedView: Boolean,
    organizing: Boolean,
    openGroup: String?,
    onClick: () -> Unit,
) {
    val label = when {
        groupedView && openGroup == null -> "New"
        organizing -> "Done"
        else -> "Edit"
    }
    val active = organizing

    Text(
        text = label.uppercase(),
        fontFamily = SansFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        letterSpacing = 1.1.sp,
        color = if (active) Bg else Gold,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .width(52.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (active) Gold else Color.Transparent)
            .border(1.dp, Gold, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    )
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun LibraryFilterDialog(
    sortBy: String,
    onChangeSort: (String) -> Unit,
    filterFavorites: Boolean,
    onToggleFilterFavorites: () -> Unit,
    photoFilter: LibraryPhotoFilter,
    onChangePhotoFilter: (LibraryPhotoFilter) -> Unit,
    groups: List<LibraryGroupUiModel>,
    groupCounts: Map<String, Int>,
    selectedGroupId: String?,
    onSelectGroup: (String?) -> Unit,
    sourceOptions: List<Pair<String, String>>,
    selectedSourceKey: String?,
    onSelectSource: (String?) -> Unit,
    filmSimOptions: List<String>,
    selectedFilmSim: String?,
    onSelectFilmSim: (String?) -> Unit,
    activeFilterCount: Int,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(PanelGroupBg)
                .border(1.dp, Border, RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) {
            Text(
                text = "FILTER",
                fontFamily = MonoFamily,
                fontSize = 10.sp,
                letterSpacing = 1.6.sp,
                color = TextMuted,
            )
            Spacer(Modifier.height(14.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterOptionRow(
                    label = "Favorite",
                    active = filterFavorites,
                    onClick = onToggleFilterFavorites,
                )
                FilterOptionRow(
                    label = "With photos",
                    active = photoFilter == LibraryPhotoFilter.WithPhotos,
                    onClick = {
                        onChangePhotoFilter(
                            if (photoFilter == LibraryPhotoFilter.WithPhotos) LibraryPhotoFilter.All else LibraryPhotoFilter.WithPhotos,
                        )
                    },
                )
                FilterOptionRow(
                    label = "No photos",
                    active = photoFilter == LibraryPhotoFilter.NoPhotos,
                    onClick = {
                        onChangePhotoFilter(
                            if (photoFilter == LibraryPhotoFilter.NoPhotos) LibraryPhotoFilter.All else LibraryPhotoFilter.NoPhotos,
                        )
                    },
                )
                FilterOptionRow(
                    label = "Ungrouped",
                    active = selectedGroupId == LibraryUngroupedFilterId,
                    onClick = {
                        onSelectGroup(if (selectedGroupId == LibraryUngroupedFilterId) null else LibraryUngroupedFilterId)
                    },
                )
            }

            if (groups.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                FilterSectionTitle("GROUP")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    groups.forEach { group ->
                        FilterOptionRow(
                            label = "${group.name} ${groupCounts[group.id] ?: 0}",
                            active = selectedGroupId == group.id,
                            onClick = { onSelectGroup(if (selectedGroupId == group.id) null else group.id) },
                        )
                    }
                }
            }

            if (filmSimOptions.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                FilterSectionTitle("FILM SIM")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    filmSimOptions.forEach { sim ->
                        FilterOptionRow(
                            label = sim,
                            active = selectedFilmSim == sim,
                            onClick = { onSelectFilmSim(if (selectedFilmSim == sim) null else sim) },
                        )
                    }
                }
            }

            if (sourceOptions.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                FilterSectionTitle("SOURCE")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    sourceOptions.forEach { (key, label) ->
                        FilterOptionRow(
                            label = label,
                            active = selectedSourceKey == key,
                            onClick = { onSelectSource(if (selectedSourceKey == key) null else key) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Border))
            Spacer(Modifier.height(16.dp))

            FilterSectionTitle("SORT")
            SortTabRow(sortBy = sortBy, onChangeSort = onChangeSort)
            if (activeFilterCount > 0) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "CLEAR FILTERS",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.4.sp,
                    color = Gold,
                    modifier = Modifier
                        .align(Alignment.End)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onClearFilters)
                        .padding(horizontal = 8.dp, vertical = 7.dp),
                )
            }
        }
    }
}

@Composable
private fun FilterSectionTitle(text: String) {
    Text(
        text = text,
        fontFamily = MonoFamily,
        fontSize = 9.sp,
        letterSpacing = 1.3.sp,
        color = TextDim,
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun FilterOptionRow(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = label.uppercase(),
        fontFamily = SansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        letterSpacing = 1.1.sp,
        color = if (active) Gold else TextPrimary,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) Gold.copy(alpha = 0.14f) else Color.Transparent)
            .border(1.dp, if (active) Gold.copy(alpha = 0.5f) else Border, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}


@Composable
private fun SortTabRow(sortBy: String, onChangeSort: (String) -> Unit) {
    val dateActive = sortBy == "NEWEST" || sortBy == "OLDEST"
    val nameActive = sortBy == "NAME_ASC" || sortBy == "NAME_DESC"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, Border, RoundedCornerShape(10.dp)),
    ) {
        SortTabItem(
            label = "DATE",
            active = dateActive,
            direction = if (sortBy == "OLDEST") "↑" else if (dateActive) "↓" else null,
            modifier = Modifier.weight(1f),
            onClick = {
                onChangeSort(if (sortBy == "NEWEST") "OLDEST" else "NEWEST")
            },
        )
        Box(Modifier.width(1.dp).fillMaxHeight().background(Border))
        SortTabItem(
            label = "NAME",
            active = nameActive,
            direction = if (sortBy == "NAME_DESC") "↓" else if (nameActive) "↑" else null,
            modifier = Modifier.weight(1f),
            onClick = {
                onChangeSort(if (sortBy == "NAME_ASC") "NAME_DESC" else "NAME_ASC")
            },
        )
    }
}

@Composable
private fun SortTabItem(
    label: String,
    active: Boolean,
    direction: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(if (active) Gold.copy(alpha = 0.14f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                fontFamily = SansFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                letterSpacing = 1.1.sp,
                color = if (active) Gold else TextPrimary,
            )
            if (direction != null) {
                Text(
                    text = direction,
                    fontSize = 12.sp,
                    color = Gold,
                )
            }
        }
    }
}

@Composable
private fun LibraryGroupCard(
    label: String,
    count: Int,
    style: LibraryGroupStyle,
    entranceIndex: Int,
    entranceRun: Int,
    alreadyEntered: Boolean = false,
    onEntered: () -> Unit = {},
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
    onOpenEditor: () -> Unit,
) {
    val context = LocalContext.current
    val imageState by produceState(ImageLoadState(null, null), style.imageUri) {
        val uri = style.imageUri?.let { Uri.parse(it) } ?: return@produceState
        val preview = withContext(Dispatchers.IO) { decodeSampledBitmap(context, uri, maxPx = 16) }
        value = ImageLoadState(preview = preview, full = null)
        val full = withContext(Dispatchers.IO) {
            runCatching {
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                val targetPx = (context.resources.displayMetrics.density * 320).toInt().coerceAtLeast(1)
                opts.inSampleSize = generateSequence(1) { it * 2 }
                    .first { it * targetPx >= opts.outWidth.coerceAtLeast(1) }
                opts.inJustDecodeBounds = false
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                    ?.asImageBitmap()
            }.getOrNull()
        }
        value = ImageLoadState(preview = preview, full = full)
    }
    val accent = groupAccent(style.color)
    val motionEnabled = ValueAnimator.areAnimatorsEnabled()
    var entered by remember(entranceRun) { mutableStateOf(!motionEnabled || alreadyEntered) }
    LaunchedEffect(entranceRun, motionEnabled, alreadyEntered) {
        if (!entered) {
            kotlinx.coroutines.delay((entranceIndex * 55L).coerceAtMost(220L))
            entered = true
        }
        onEntered()
    }
    val entranceProgress by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "library-folder-entrance",
    )

    Column(
        modifier = modifier
            .height(156.dp)
            .graphicsLayer {
                if (motionEnabled) {
                    alpha = entranceProgress
                    translationY = (1f - entranceProgress) * 22.dp.toPx()
                }
            }
            .clip(RoundedCornerShape(14.dp))
            .background(PanelGroupBg)
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .clickable(onClick = onOpen),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(82.dp)
                .background(accent.copy(alpha = 0.18f)),
        ) {
            if (style.imageUri != null) {
                BlurredImagePlaceholder(
                    state = imageState,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                // dim overlay so text below stays readable
                val overlayAlpha by animateFloatAsState(
                    targetValue = if (imageState.full != null) 0.28f else 0f,
                    animationSpec = tween(380, easing = FastOutSlowInEasing),
                    label = "group-overlay-alpha",
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = overlayAlpha)),
                )
            } else {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Bg.copy(alpha = 0.72f))
                        .border(1.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = style.icon, fontSize = 22.sp, lineHeight = 22.sp)
                }
            }
        }
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label.uppercase(),
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.2.sp,
                    color = TextPrimary,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    IconMoreVertical,
                    contentDescription = "Edit group",
                    tint = TextMuted,
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onOpenEditor)
                        .padding(4.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "$count RECIPES",
                fontFamily = MonoFamily,
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                color = TextDim,
            )
        }
    }
}

@Composable
private fun CreateLibraryGroupDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var draft by remember { mutableStateOf("") }
    val context = LocalContext.current
    val view = LocalView.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 238.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(PanelGroupBg)
                .border(1.dp, Border, RoundedCornerShape(20.dp))
                .imePadding()
                .padding(22.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "NEW FOLDER",
                            fontFamily = MonoFamily,
                            fontSize = 10.sp,
                            letterSpacing = 1.5.sp,
                            color = TextDim,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "CREATE FOLDER",
                            fontFamily = SansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 21.sp,
                            letterSpacing = 0.6.sp,
                            color = TextPrimary,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Gold.copy(alpha = 0.14f))
                            .border(1.dp, Gold.copy(alpha = 0.48f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(IconPlus, contentDescription = null, tint = Gold, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(Modifier.height(28.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Bg)
                        .border(1.dp, Border, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    BasicTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = SansFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = TextPrimary,
                        ),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (draft.isBlank()) {
                                Text(
                                    text = "Folder name",
                                    fontFamily = SansFamily,
                                    fontSize = 15.sp,
                                    color = TextDim,
                                )
                            }
                            innerTextField()
                        },
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "CANCEL",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    letterSpacing = 1.3.sp,
                    color = TextDim,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable {
                            FujiHaptics.perform(context, view, FujiHapticEffect.SheetDismiss)
                            onDismiss()
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "CREATE",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    letterSpacing = 1.3.sp,
                    color = if (draft.isBlank()) TextDim else Gold,
                    modifier = Modifier.clickable(enabled = draft.isNotBlank()) {
                        FujiHaptics.perform(context, view, FujiHapticEffect.Confirm)
                        onCreate(draft)
                    },
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun LibraryGroupEditorSheet(
    group: String,
    count: Int,
    style: LibraryGroupStyle,
    canRename: Boolean = true,
    canDelete: Boolean = true,
    onChangeStyle: (LibraryGroupStyle) -> Unit,
    onRename: (String) -> Unit,
    onAddImage: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val accent = groupAccent(style.color)
    val motionEnabled = ValueAnimator.areAnimatorsEnabled()
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(!motionEnabled) }
    var nameDraft by remember(group) { mutableStateOf(group) }
    var pendingDeleteGroup by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val view = LocalView.current

    fun dismissWithMotion() {
        FujiHaptics.perform(context, view, FujiHapticEffect.SheetDismiss)
        if (!motionEnabled) {
            onDismiss()
            return
        }
        scope.launch {
            visible = false
            delay(180)
            onDismiss()
        }
    }

    LaunchedEffect(motionEnabled) {
        visible = true
        FujiHaptics.perform(context, view, FujiHapticEffect.SheetOpen)
    }

    val overlayTransition = updateTransition(targetState = visible, label = "library-editor-overlay")
    val overlayAlpha by overlayTransition.animateFloat(
        transitionSpec = { tween(durationMillis = if (targetState) 170 else 120, easing = FastOutSlowInEasing) },
        label = "library-editor-overlay-alpha",
    ) { isVisible -> if (isVisible) 1f else 0f }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LibrarySheetOverlay.copy(alpha = 0.88f * overlayAlpha))
            .clickable(onClick = ::dismissWithMotion),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(150, easing = FastOutSlowInEasing)) + slideInVertically(
                animationSpec = tween(280, easing = FastOutSlowInEasing),
                initialOffsetY = { it / 3 },
            ),
            exit = fadeOut(tween(105, easing = FastOutSlowInEasing)) + slideOutVertically(
                animationSpec = tween(180, easing = FastOutSlowInEasing),
                targetOffsetY = { it / 4 },
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 640.dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(LibrarySheetBg)
                    .border(1.dp, LibrarySheetBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .clickable(onClick = {})
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                // Pinned drag handle — outside the scroll
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 14.dp)
                        .width(38.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(TextDim.copy(alpha = 0.55f)),
                )
                // Scrollable content below
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(horizontal = 20.dp)
                        .padding(top = 18.dp, bottom = 14.dp),
                ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column {
                        Text(
                            text = group.uppercase(),
                            fontFamily = SansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            letterSpacing = 1.1.sp,
                            color = TextPrimary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "$count RECIPES",
                            fontFamily = MonoFamily,
                            fontSize = 10.sp,
                            letterSpacing = 1.2.sp,
                            color = TextDim,
                        )
                    }
                    Text(
                        text = "DONE",
                        fontFamily = SansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.3.sp,
                        color = Gold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable(onClick = ::dismissWithMotion)
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                    )
                }
                if (canRename) {
                    Spacer(Modifier.height(20.dp))
                    SheetSectionLabel("NAME")
                    SheetNameRow(
                        value = nameDraft,
                        originalValue = group,
                        color = accent,
                        onValueChange = { nameDraft = it },
                        onSave = {
                            FujiHaptics.perform(context, view, FujiHapticEffect.Confirm)
                            onRename(nameDraft)
                        },
                    )
                    Spacer(Modifier.height(18.dp))
                } else {
                    Spacer(Modifier.height(20.dp))
                }
                SheetSectionLabel("IDENTITY")
                SheetIdentityRow(
                    icon = style.icon,
                    hasCover = style.imageUri != null,
                    color = accent,
                    onIconChange = { if (it.isNotEmpty()) onChangeStyle(style.copy(icon = it)) },
                    onChooseImage = onAddImage,
                    onRemoveImage = {
                        if (style.imageUri != null) onChangeStyle(style.copy(imageUri = null))
                    },
                )
                Spacer(Modifier.height(18.dp))
                SheetSectionLabel("ACCENT")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    GroupColor.entries.forEach { color ->
                        SheetColorChoice(
                            color = color.value,
                            active = style.color == color.name,
                            onClick = {
                                FujiHaptics.perform(context, view, FujiHapticEffect.Selection)
                                onChangeStyle(style.copy(color = color.name))
                            },
                        )
                    }
                }
                if (canDelete) {
                    Spacer(Modifier.height(22.dp))
                    Text(
                        text = "DELETE GROUP",
                        fontFamily = SansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.4.sp,
                        color = Color(0xFFD45B4A),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(LibrarySheetControlBg)
                            .border(1.dp, Color(0xFFD45B4A).copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                            .clickable {
                                FujiHaptics.perform(context, view, FujiHapticEffect.Reject)
                                pendingDeleteGroup = true
                            }
                            .padding(vertical = 13.dp),
                    )
                }
                } // inner scrollable Column
            }
        }
    }

    if (pendingDeleteGroup) {
        DeleteConfirmDialog(
            title = group,
            body = "This group will be deleted. Recipes inside will not be removed from your library.",
            confirmLabel = "Delete Group",
            onConfirm = onDelete,
            onDismiss = { pendingDeleteGroup = false },
        )
    }
}

@Composable
private fun SheetSectionLabel(label: String) {
    Text(
        text = label,
        fontFamily = MonoFamily,
        fontSize = 10.sp,
        letterSpacing = 1.5.sp,
        color = TextMuted,
        modifier = Modifier.padding(bottom = 9.dp),
    )
}


@Composable
private fun SheetNameRow(
    value: String,
    originalValue: String,
    color: Color,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    val normalized = value.trim()
    val canSave = normalized.isNotBlank() && normalized != originalValue

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(LibrarySheetControlBg)
            .border(1.dp, if (canSave) color.copy(alpha = 0.72f) else LibrarySheetBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = SansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 0.4.sp,
                color = TextPrimary,
            ),
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                if (value.isBlank()) {
                    Text(
                        text = "Group name",
                        fontFamily = SansFamily,
                        fontSize = 14.sp,
                        color = TextDim,
                    )
                }
                innerTextField()
            },
        )
        Text(
            text = "SAVE",
            fontFamily = SansFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 10.5.sp,
            letterSpacing = 1.3.sp,
            color = if (canSave) color else TextDim,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable(enabled = canSave, onClick = onSave)
                .padding(horizontal = 2.dp, vertical = 6.dp),
        )
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SheetIdentityRow(
    icon: String,
    hasCover: Boolean,
    color: Color,
    onIconChange: (String) -> Unit,
    onChooseImage: () -> Unit,
    onRemoveImage: () -> Unit,
) {
    var pickerOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val view = LocalView.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LibrarySheetControlBg)
            .border(1.dp, LibrarySheetBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (hasCover) Bg.copy(alpha = 0.52f) else color.copy(alpha = 0.18f))
                    .border(
                        1.dp,
                        if (pickerOpen) color else if (hasCover) LibrarySheetBorder else color.copy(alpha = 0.62f),
                        RoundedCornerShape(14.dp),
                    )
                    .clickable {
                        FujiHaptics.perform(context, view, FujiHapticEffect.Selection)
                        pickerOpen = !pickerOpen
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(text = icon, fontSize = 27.sp, textAlign = TextAlign.Center)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (hasCover) "Cover image is active" else "Emoji is active",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextPrimary,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (hasCover) {
                        "Emoji is saved, but hidden on the group card while a cover image is set."
                    } else {
                        "Tap the square to pick an emoji, or add a cover image."
                    },
                    fontFamily = SansFamily,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = TextDim,
                )
            }
        }

        if (pickerOpen) {
            Dialog(onDismissRequest = { pickerOpen = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(LibrarySheetBg)
                        .navigationBarsPadding(),
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 12.dp, bottom = 8.dp)
                            .width(36.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(TextDim.copy(alpha = 0.55f)),
                    )
                    AndroidView(
                        factory = { ctx ->
                            EmojiPickerView(ctx).apply {
                                setOnEmojiPickedListener { item ->
                                    FujiHaptics.perform(context, view, FujiHapticEffect.Confirm)
                                    onIconChange(item.emoji)
                                    pickerOpen = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(13.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SheetMiniAction(
                label = if (hasCover) "Change Cover" else "Add Cover",
                color = color,
                modifier = Modifier.weight(1f),
                onClick = {
                    FujiHaptics.perform(context, view, FujiHapticEffect.Selection)
                    onChooseImage()
                },
            )
            if (hasCover) {
                SheetMiniAction(
                    label = "Remove",
                    color = TextDim,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        FujiHaptics.perform(context, view, FujiHapticEffect.Reject)
                        onRemoveImage()
                    },
                )
            }
        }
    }
}

@Composable
private fun SheetMiniAction(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Bg.copy(alpha = 0.62f))
            .border(1.dp, LibrarySheetBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.uppercase(),
            fontFamily = SansFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 10.5.sp,
            letterSpacing = 1.2.sp,
            color = color,
        )
    }
}

@Composable
private fun SheetActionRow(
    label: String,
    value: String,
    color: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(LibrarySheetControlBg)
            .border(1.dp, LibrarySheetBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label.uppercase(),
            fontFamily = SansFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 1.1.sp,
            color = TextPrimary,
        )
        Text(
            text = value.uppercase(),
            fontFamily = MonoFamily,
            fontSize = 10.sp,
            letterSpacing = 1.1.sp,
            color = color,
        )
    }
}

@Composable
private fun SheetColorChoice(
    color: Color,
    active: Boolean,
    onClick: () -> Unit,
) {
    val ringScale by animateFloatAsState(
        targetValue = if (active && ValueAnimator.areAnimatorsEnabled()) 1.08f else 1f,
        animationSpec = tween(160, easing = FastOutSlowInEasing),
        label = "library-sheet-color-ring",
    )

    Box(
        modifier = Modifier
            .size(34.dp)
            .graphicsLayer {
                scaleX = ringScale
                scaleY = ringScale
            }
            .clip(CircleShape)
            .background(color)
            .border(2.dp, if (active) TextPrimary else LibrarySheetBorder, CircleShape)
            .clickable(onClick = onClick),
    )
}

@Composable
private fun GroupHeader(label: String, count: Int, onBack: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Bg)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = if (onBack != null) {
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onBack)
                    .padding(end = 8.dp)
            } else {
                Modifier
            },
        ) {
            if (onBack != null) {
                Text(
                    text = "‹",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = Gold,
                    modifier = Modifier.offset(y = (-1).dp),
                )
            }
            Text(
                text = label.uppercase(),
                fontFamily = SansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 1.5.sp,
                color = Gold,
                modifier = Modifier.offset(y = 1.dp),
            )
        }
        Text(
            text = "$count",
            fontFamily = MonoFamily,
            fontSize = 11.sp,
            color = TextDim,
        )
    }
    BottomDivider()
}

private sealed class ReferenceThumbnailState {
    object Loading : ReferenceThumbnailState()
    object Failed : ReferenceThumbnailState()
    data class Ready(val bitmap: ImageBitmap) : ReferenceThumbnailState()
}

@Composable
private fun LibraryReferenceImageSkeleton() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PanelGroupBg),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            PanelGroupBg,
                            Color(0xFF211E19),
                            PanelGroupBg,
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 18.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(132.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(TextDim.copy(alpha = 0.18f)),
            )
            Box(
                modifier = Modifier
                    .width(84.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(TextDim.copy(alpha = 0.12f)),
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
private fun LibraryRecipeRow(
    recipe: LibraryRecipeUiModel,
    groups: List<LibraryGroupUiModel>,
    groupCounts: Map<String, Int>,
    organizing: Boolean,
    showImages: Boolean = false,
    customGroupRecipeId: String?,
    customGroupDraft: String,
    onCustomGroupDraftChange: (String) -> Unit,
    onStartCustomGroup: () -> Unit,
    onCloseGroupPicker: () -> Unit,
    onSaveCustomGroup: () -> Unit,
    onChangeGroup: (String) -> Unit,
    selected: Boolean,
    selectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onStartSelection: () -> Unit,
    onOpenItem: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val context = LocalContext.current
    val thumbnailUri = recipe.referenceImageUris.firstOrNull()
    val useCardLayout = showImages && thumbnailUri != null && !organizing
    val thumbnailState by produceState<ReferenceThumbnailState>(
        initialValue = ReferenceThumbnailState.Loading,
        key1 = thumbnailUri,
    ) {
        value = if (thumbnailUri == null) {
            ReferenceThumbnailState.Failed
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    val uri = Uri.parse(thumbnailUri)
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                    val targetPx = (context.resources.displayMetrics.density * 320).toInt().coerceAtLeast(1)
                    opts.inSampleSize = generateSequence(1) { it * 2 }
                        .first { it * targetPx >= opts.outWidth.coerceAtLeast(1) }
                    opts.inJustDecodeBounds = false
                    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                        ?.asImageBitmap()
                        ?.let { ReferenceThumbnailState.Ready(it) }
                        ?: ReferenceThumbnailState.Failed
                }.getOrElse { ReferenceThumbnailState.Failed }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) Gold.copy(alpha = 0.08f) else Color.Transparent)
            .combinedClickable(
                enabled = !organizing,
                onClick = {
                    if (selectionMode) onToggleSelection() else onOpenItem()
                },
                onLongClick = onStartSelection,
            ),
    ) {
        // ── Image strip (card layout) ──────────────────────────────────
        if (useCardLayout) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
            ) {
                when (val state = thumbnailState) {
                    is ReferenceThumbnailState.Ready -> {
                        Image(
                            bitmap = state.bitmap,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    ReferenceThumbnailState.Loading -> LibraryReferenceImageSkeleton()
                    ReferenceThumbnailState.Failed -> Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(PanelGroupBg),
                    )
                }
                // Sim label pinned top-left, selection/star top-right
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Bg.copy(alpha = 0.72f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = recipe.sim.uppercase(),
                            fontFamily = MonoFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 9.sp,
                            letterSpacing = 1.4.sp,
                            color = TextMuted,
                        )
                    }
                    if (selectionMode) {
                        Box(
                            modifier = Modifier.size(30.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            SelectionMark(selected = selected)
                        }
                    } else {
                        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(30.dp)) {
                            Icon(
                                if (recipe.favorite) IconStarFilled else IconStar,
                                contentDescription = if (recipe.favorite) "Remove favorite" else "Add favorite",
                                tint = if (recipe.favorite) Gold else TextPrimary.copy(alpha = 0.72f),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }

        // ── Text + meta + pills ────────────────────────────────────────
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                if (!useCardLayout) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = recipe.sim.uppercase(),
                            fontFamily = MonoFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp,
                            letterSpacing = 1.4.sp,
                            color = TextMuted,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "SAVED ${recipe.saved.uppercase()}",
                                fontFamily = MonoFamily,
                                fontSize = 10.5.sp,
                                letterSpacing = 1.sp,
                                color = TextDim,
                            )
                            if (selectionMode) {
                                Box(
                                    modifier = Modifier.size(26.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    SelectionMark(selected = selected)
                                }
                            } else {
                                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(26.dp)) {
                                    Icon(
                                        if (recipe.favorite) IconStarFilled else IconStar,
                                        contentDescription = if (recipe.favorite) "Remove favorite" else "Add favorite",
                                        tint = if (recipe.favorite) Gold else TextDim,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                } else {
                    // Card layout: saved date sits above the name as a caption
                    Text(
                        text = "SAVED ${recipe.saved.uppercase()}",
                        fontFamily = MonoFamily,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        color = TextDim,
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    text = recipe.name,
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    letterSpacing = 0.1.sp,
                    color = TextPrimary,
                )
                val cameraSource = recipe.sourceCameraDisplayName()
                if (cameraSource != null && useCardLayout) {
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = "FROM ${cameraSource.uppercase()}",
                        fontFamily = MonoFamily,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        color = TextDim,
                    )
                }
                if (recipe.description.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = recipe.description,
                        fontFamily = SansFamily,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = TextDim,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (organizing) {
                    Spacer(Modifier.height(12.dp))
                    GroupAssignmentRow(
                        recipe = recipe,
                        groups = groups,
                        groupCounts = groupCounts,
                        customGroupRecipeId = customGroupRecipeId,
                        customGroupDraft = customGroupDraft,
                        onCustomGroupDraftChange = onCustomGroupDraftChange,
                        onStartCustomGroup = onStartCustomGroup,
                        onCloseGroupPicker = onCloseGroupPicker,
                        onSaveCustomGroup = onSaveCustomGroup,
                        onChangeGroup = onChangeGroup,
                    )
                }
                if (!organizing) {
                    Spacer(Modifier.height(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        recipe.pills.forEach { Pill(text = it) }
                    }
                }
            }
        }
        BottomDivider()
    }
}

@Composable
private fun SelectionMark(
    selected: Boolean,
    modifier: Modifier = Modifier,
    ) {
    val scale by animateFloatAsState(
        targetValue = if (selected && ValueAnimator.areAnimatorsEnabled()) 1.03f else 1f,
        animationSpec = tween(120, easing = FastOutSlowInEasing),
        label = "library-selection-mark-scale",
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .size(20.dp)
            .clip(CircleShape)
            .background(if (selected) Gold.copy(alpha = 0.95f) else Color.Transparent)
            .border(1.dp, if (selected) Gold else TextDim.copy(alpha = 0.42f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                IconCheck,
                contentDescription = null,
                tint = Bg,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

@Composable
private fun LibrarySelectionBar(
    selectedCount: Int,
    onClear: () -> Unit,
    onAddToGroup: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(PanelLow)
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .padding(start = 14.dp, end = 8.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "SELECTED",
                fontFamily = MonoFamily,
                fontSize = 9.sp,
                letterSpacing = 1.4.sp,
                color = TextDim,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (selectedCount == 1) "1 RECIPE" else "$selectedCount RECIPES",
                fontFamily = SansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 0.8.sp,
                color = TextPrimary,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            SelectionAction(text = "GROUP", onClick = onAddToGroup)
            SelectionAction(text = "DELETE", danger = true, onClick = onDelete)
            SelectionAction(text = "DONE", onClick = onClear)
        }
    }
}

@Composable
private fun SelectionAction(
    text: String,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        fontFamily = SansFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        letterSpacing = 1.25.sp,
        color = if (danger) Color(0xFFE0684D) else Gold,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
    )
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun BulkGroupDialog(
    selectedCount: Int,
    groups: List<LibraryGroupUiModel>,
    onDismiss: () -> Unit,
    onSelectGroup: (String) -> Unit,
    onCreateGroup: (String) -> Unit,
) {
    var customGroupDraft by remember { mutableStateOf("") }
    val context = LocalContext.current
    val view = LocalView.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(PanelGroupBg)
                .border(1.dp, Border, RoundedCornerShape(20.dp))
                .imePadding()
                .padding(18.dp),
        ) {
            Text(
                text = "ADD $selectedCount TO GROUP",
                fontFamily = MonoFamily,
                fontSize = 10.sp,
                letterSpacing = 1.5.sp,
                color = TextDim,
            )
            Spacer(Modifier.height(14.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LibraryGroupChip(
                    label = "No groups",
                    count = 0,
                    active = false,
                    onClick = {
                        FujiHaptics.perform(context, view, FujiHapticEffect.Confirm)
                        onSelectGroup("")
                    },
                )
                groups.forEach { group ->
                    LibraryGroupChip(
                        label = group.name,
                        count = 0,
                        active = false,
                        onClick = {
                            FujiHaptics.perform(context, view, FujiHapticEffect.Confirm)
                            onSelectGroup(group.id)
                        },
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Bg)
                    .border(1.dp, Border, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BasicTextField(
                    value = customGroupDraft,
                    onValueChange = { customGroupDraft = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = SansFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = TextPrimary,
                    ),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (customGroupDraft.isBlank()) {
                            Text(
                                text = "New group",
                                fontFamily = SansFamily,
                                fontSize = 13.sp,
                                color = TextDim,
                            )
                        }
                        innerTextField()
                    },
                )
                Text(
                    text = "CREATE",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    letterSpacing = 1.3.sp,
                    color = if (customGroupDraft.isBlank()) TextDim else Gold,
                    modifier = Modifier.clickable(enabled = customGroupDraft.isNotBlank()) {
                        FujiHaptics.perform(context, view, FujiHapticEffect.Confirm)
                        onCreateGroup(customGroupDraft)
                    },
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun GroupAssignmentRow(
    recipe: LibraryRecipeUiModel,
    groups: List<LibraryGroupUiModel>,
    groupCounts: Map<String, Int>,
    customGroupRecipeId: String?,
    customGroupDraft: String,
    onCustomGroupDraftChange: (String) -> Unit,
    onStartCustomGroup: () -> Unit,
    onCloseGroupPicker: () -> Unit,
    onSaveCustomGroup: () -> Unit,
    onChangeGroup: (String) -> Unit,
) {
    val assignedGroups = groups.filter { it.id in recipe.groupIds }
    val groupSummary = assignedGroups
        .joinToString(" / ") { it.name.uppercase() }
        .ifBlank { "NO GROUPS" }
    val context = LocalContext.current
    val view = LocalView.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(PanelGroupBg)
                .border(1.dp, Border, RoundedCornerShape(12.dp))
                .clickable {
                    FujiHaptics.perform(context, view, FujiHapticEffect.SheetOpen)
                    onStartCustomGroup()
                }
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "GROUPS",
                fontFamily = MonoFamily,
                fontSize = 10.sp,
                letterSpacing = 1.4.sp,
                color = TextDim,
            )
            Text(
                text = groupSummary,
                fontFamily = SansFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "EDIT",
                fontFamily = SansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.5.sp,
                letterSpacing = 1.3.sp,
                color = Gold,
            )
        }

        if (customGroupRecipeId == recipe.id) {
            Dialog(onDismissRequest = onCloseGroupPicker) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(PanelGroupBg)
                        .border(1.dp, Border, RoundedCornerShape(20.dp))
                        .imePadding()
                        .padding(18.dp),
                ) {
                    Text(
                        text = "EDIT GROUPS",
                        fontFamily = MonoFamily,
                        fontSize = 10.sp,
                        letterSpacing = 1.5.sp,
                        color = TextDim,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = recipe.name,
                        fontFamily = SansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.height(14.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        LibraryGroupChip(
                            label = "No groups",
                            count = 0,
                            active = recipe.groupIds.isEmpty(),
                            onClick = {
                                FujiHaptics.perform(context, view, FujiHapticEffect.Selection)
                                onChangeGroup("")
                            },
                        )
                        groups.forEach { group ->
                            LibraryGroupChip(
                                label = group.name,
                                count = groupCounts[group.id] ?: 0,
                                active = group.id in recipe.groupIds,
                                onClick = {
                                    FujiHaptics.perform(context, view, FujiHapticEffect.Selection)
                                    onChangeGroup(group.id)
                                },
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Bg)
                            .border(1.dp, Border, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        BasicTextField(
                            value = customGroupDraft,
                            onValueChange = onCustomGroupDraftChange,
                            singleLine = true,
                            textStyle = TextStyle(
                                fontFamily = SansFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = TextPrimary,
                            ),
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                if (customGroupDraft.isBlank()) {
                                    Text(
                                        text = "New group",
                                        fontFamily = SansFamily,
                                        fontSize = 13.sp,
                                        color = TextDim,
                                    )
                                }
                                innerTextField()
                            },
                        )
                        Text(
                            text = "CREATE",
                            fontFamily = SansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp,
                            letterSpacing = 1.3.sp,
                            color = if (customGroupDraft.isBlank()) TextDim else Gold,
                            modifier = Modifier.clickable(enabled = customGroupDraft.isNotBlank()) {
                                FujiHaptics.perform(context, view, FujiHapticEffect.Confirm)
                                onSaveCustomGroup()
                            },
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "DONE",
                        fontFamily = SansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.4.sp,
                        color = Gold,
                        modifier = Modifier
                            .align(Alignment.End)
                            .clickable {
                                FujiHaptics.perform(context, view, FujiHapticEffect.SheetDismiss)
                                onCloseGroupPicker()
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryGroupChip(
    label: String,
    count: Int,
    active: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (active) Gold else PanelGroupBg)
            .border(1.dp, if (active) Gold else Border, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(
            text = label.uppercase(),
            fontFamily = SansFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            letterSpacing = 1.2.sp,
            color = if (active) Bg else TextPrimary,
        )
        Text(
            text = count.toString(),
            fontFamily = MonoFamily,
            fontSize = 10.sp,
            color = if (active) Bg.copy(alpha = 0.72f) else TextDim,
        )
    }
}

private val PanelGroupBg = androidx.compose.ui.graphics.Color(0xFF171512)
private val LibrarySheetOverlay = androidx.compose.ui.graphics.Color(0xFF0A0A09)
private val LibrarySheetBg = androidx.compose.ui.graphics.Color(0xFF11100E)
private val LibrarySheetControlBg = androidx.compose.ui.graphics.Color(0xFF161411)
private val LibrarySheetBorder = androidx.compose.ui.graphics.Color(0xFF26221C)
private data class LibraryFolderGridItem(
    val group: LibraryGroupUiModel,
    val recipes: List<LibraryRecipeUiModel> = emptyList(),
    val count: Int = recipes.size,
)

private fun String.isGeneratedGroupId(): Boolean =
    removePrefix("group-").matches(
        Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"),
    )

private enum class GroupColor(val value: Color) {
    Gold(Color(0xFFC99A4E)),
    Brass(Color(0xFFB88746)),
    Amber(Color(0xFFD2A136)),
    Cream(Color(0xFFE9D9B8)),
    Clay(Color(0xFF9F6A4E)),
    Coral(Color(0xFFC56A55)),
    Rose(Color(0xFFB45C69)),
    Plum(Color(0xFF80607E)),
    Indigo(Color(0xFF5F6F96)),
    Blue(Color(0xFF5F7F98)),
    Teal(Color(0xFF5F8B87)),
    Green(Color(0xFF6F8B63)),
    Olive(Color(0xFF83855A)),
    Silver(Color(0xFFA8A49A)),
    Red(Color(0xFFA94B35)),
}

private fun groupAccent(name: String): Color =
    GroupColor.entries.firstOrNull { it.name == name }?.value ?: GroupColor.Gold.value

@Composable
private fun BottomDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Border),
    )
}

@Composable
private fun AddRecipeDrawer(
    onCreateRecipe: () -> Unit,
    onImportFromPhoto: () -> Unit,
    onImportFromScreenshot: () -> Unit,
    onImportFromQr: () -> Unit,
    onScanTileGuide: () -> Unit,
    onDismiss: () -> Unit,
) {
    val motionEnabled = android.animation.ValueAnimator.areAnimatorsEnabled()
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(!motionEnabled) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val context = LocalContext.current
    val view = LocalView.current

    fun dismissWithMotion() {
        FujiHaptics.perform(context, view, FujiHapticEffect.DrawerSwooshDismiss)
        if (!motionEnabled) { onDismiss(); return }
        scope.launch { visible = false; delay(180); onDismiss() }
    }

    LaunchedEffect(motionEnabled) {
        visible = true
        FujiHaptics.perform(context, view, FujiHapticEffect.DrawerSwooshOpen)
    }

    val overlayTransition = androidx.compose.animation.core.updateTransition(targetState = visible, label = "add-drawer-overlay")
    val overlayAlpha by overlayTransition.animateFloat(
        transitionSpec = { tween(if (targetState) 170 else 120, easing = FastOutSlowInEasing) },
        label = "add-drawer-overlay-alpha",
    ) { if (it) 1f else 0f }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LibrarySheetOverlay.copy(alpha = 0.88f * overlayAlpha))
            .clickable(onClick = ::dismissWithMotion),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(150, easing = FastOutSlowInEasing)) + slideInVertically(
                animationSpec = tween(280, easing = FastOutSlowInEasing),
                initialOffsetY = { it / 3 },
            ),
            exit = fadeOut(tween(105, easing = FastOutSlowInEasing)) + slideOutVertically(
                animationSpec = tween(180, easing = FastOutSlowInEasing),
                targetOffsetY = { it / 4 },
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(0, dragOffset.roundToInt().coerceAtLeast(0)) }
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(LibrarySheetBg)
                    .border(1.dp, LibrarySheetBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .clickable(onClick = {})
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (dragOffset > 100.dp.toPx()) dismissWithMotion()
                                else dragOffset = 0f
                            },
                            onDragCancel = { dragOffset = 0f },
                        ) { _, amount -> dragOffset = (dragOffset + amount).coerceAtLeast(0f) }
                    }
                    .navigationBarsPadding(),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 14.dp)
                        .width(38.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(TextDim.copy(alpha = 0.55f)),
                )
                Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 16.dp)) {
                    Text(
                        text = "ADD RECIPE",
                        fontFamily = SansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        letterSpacing = 0.4.sp,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Add a new recipe to your library.",
                        fontFamily = SansFamily,
                        fontSize = 13.sp,
                        color = TextDim,
                    )
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(LibrarySheetBorder))
                DrawerOptionRow("New recipe", "Build a clean preset from scratch", icon = IconEdit, onClick = onCreateRecipe)
                Box(Modifier.fillMaxWidth().height(1.dp).background(LibrarySheetBorder))
                DrawerOptionRow("From JPEG", "Read EXIF from a camera photo", icon = com.paeki.fujirecipes.ui.components.IconCamera, onClick = onImportFromPhoto)
                Box(Modifier.fillMaxWidth().height(1.dp).background(LibrarySheetBorder))
                DrawerOptionRow("From screenshot", "Extract settings from recipe image", icon = com.paeki.fujirecipes.ui.components.IconImage, onClick = onImportFromScreenshot)
                Box(Modifier.fillMaxWidth().height(1.dp).background(LibrarySheetBorder))
                DrawerOptionRow("QR code", "Import a shared FujiSync recipe", icon = com.paeki.fujirecipes.ui.components.IconQrCode, onClick = onImportFromQr)
                Box(Modifier.fillMaxWidth().height(1.dp).background(LibrarySheetBorder))
                DrawerOptionRow("Scan tile", "Capture from app recipe tile", icon = com.paeki.fujirecipes.ui.components.IconScan, onClick = onScanTileGuide)
            }
        }
    }
}


@Composable
private fun DrawerOptionRow(
    label: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 22.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) TextMuted else TextDim,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontFamily = SansFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = if (enabled) TextPrimary else TextDim,
            )
            Text(
                text = subtitle,
                fontFamily = SansFamily,
                fontSize = 12.sp,
                color = TextDim,
            )
        }
        if (!enabled) {
            Text(
                text = "SOON",
                fontFamily = MonoFamily,
                fontSize = 9.sp,
                letterSpacing = 1.4.sp,
                color = TextDim,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(LibrarySheetControlBg)
                    .border(1.dp, LibrarySheetBorder, RoundedCornerShape(999.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

