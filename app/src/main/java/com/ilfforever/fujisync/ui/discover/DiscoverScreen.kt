package com.ilfforever.fujisync.ui.discover

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ilfforever.fujisync.data.remote.FxwRecipe
import com.ilfforever.fujisync.ui.MainViewModel
import com.ilfforever.fujisync.ui.components.FilmSimInlineLabel
import com.ilfforever.fujisync.ui.components.IconClose
import com.ilfforever.fujisync.ui.components.IconSearch
import com.ilfforever.fujisync.ui.components.Pill
import com.ilfforever.fujisync.ui.components.Wordmark
import com.ilfforever.fujisync.ui.haptics.FujiHapticEffect
import com.ilfforever.fujisync.ui.haptics.FujiHaptics
import com.ilfforever.fujisync.ui.theme.Bg
import com.ilfforever.fujisync.ui.theme.Border
import com.ilfforever.fujisync.ui.theme.Gold
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.PanelHigh
import com.ilfforever.fujisync.ui.theme.PanelLow
import com.ilfforever.fujisync.ui.theme.SansFamily
import com.ilfforever.fujisync.ui.theme.TextDim
import com.ilfforever.fujisync.ui.theme.TextMuted
import com.ilfforever.fujisync.ui.theme.TextPrimary

@Composable
fun DiscoverScreen(
    viewModel: DiscoverViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel(),
) {
    val state = viewModel.state
    val mainState by mainViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current
    val focusManager = LocalFocusManager.current
    var selectedRecipe by remember { mutableStateOf<FxwRecipe?>(null) }
    var searchOpen by remember { mutableStateOf(false) }
    LaunchedEffect(selectedRecipe) { if (selectedRecipe != null) focusManager.clearFocus() }
    BackHandler(enabled = selectedRecipe != null || searchOpen) {
        if (selectedRecipe != null) selectedRecipe = null
        else { searchOpen = false; viewModel.setSearchQuery("") }
    }

    LaunchedEffect(state.searchLoading) {
        if (state.searchLoading) {
            FujiHaptics.perform(context, view, FujiHapticEffect.SearchRunning)
        } else {
            FujiHaptics.cancel(context)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(Bg).statusBarsPadding()) {
            DiscoverHeader(
                searchOpen = searchOpen,
                searchQuery = state.searchQuery,
                searchLoading = state.searchLoading,
                onToggleSearch = {
                    FujiHaptics.perform(context, view, FujiHapticEffect.SearchTap)
                    searchOpen = !searchOpen
                    if (!searchOpen) viewModel.setSearchQuery("")
                },
                onSearchQueryChange = viewModel::setSearchQuery,
                onSearchSubmit = {
                    FujiHaptics.perform(context, view, FujiHapticEffect.SearchTap)
                    viewModel.submitSearch()
                },
                onSearchClose = { searchOpen = false; viewModel.setSearchQuery("") },
            )
            Divider()

            when {
                searchOpen && state.committedQuery.isBlank() -> SearchPromptState(typed = state.searchQuery)
                state.searchQuery.isNotBlank() -> when {
                    state.searchLoading -> LoadingState()
                    state.searchError != null -> ErrorState(message = state.searchError, onRetry = viewModel::submitSearch)
                    state.committedQuery.isNotBlank() && state.searchResults.isEmpty() -> ErrorState(message = "No results for \"${state.committedQuery}\"", onRetry = viewModel::submitSearch)
                    else -> RecipeList(
                        recipes = state.searchResults,
                        loadingMore = false,
                        hasMore = false,
                        onLoadMore = {},
                        onOpenRecipe = { selectedRecipe = it },
                    )
                }
                state.initialLoading -> LoadingState()
                state.error != null && state.recipes.isEmpty() -> ErrorState(
                    message = state.error,
                    onRetry = viewModel::refresh,
                )
                else -> RecipeList(
                    recipes = state.recipes,
                    loadingMore = state.loadingMore,
                    hasMore = state.hasMore,
                    onLoadMore = viewModel::loadNextPage,
                    onOpenRecipe = { selectedRecipe = it },
                )
            }
        }

        FxwRecipeDetailScreen(
            recipe = selectedRecipe,
            maxReferenceImages = mainState.settings.maxReferenceImages,
            onClose = { selectedRecipe = null },
            onSaveToLibrary = { recipe, name, includePhotos ->
                mainViewModel.handleSaveFromDiscover(recipe, name, includePhotos)
            },
            onAfterSave = { selectedRecipe = null },
        )
    }
}

@Composable
private fun DiscoverHeader(
    searchOpen: Boolean = false,
    searchQuery: String = "",
    searchLoading: Boolean = false,
    onToggleSearch: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    onSearchSubmit: () -> Unit = {},
    onSearchClose: () -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(searchOpen) {
        if (searchOpen) focusRequester.requestFocus()
    }
    val enterSpec = tween<Float>(210, easing = FastOutSlowInEasing)
    val exitSpec = tween<Float>(150, easing = FastOutSlowInEasing)
    val offsetEnter = tween<IntOffset>(210, easing = FastOutSlowInEasing)
    val offsetExit = tween<IntOffset>(150, easing = FastOutSlowInEasing)
    val sizeSpec = tween<androidx.compose.ui.unit.IntSize>(240, easing = FastOutSlowInEasing)

    Column(modifier = Modifier.fillMaxWidth()) {
        // Wordmark row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Bg)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.height(28.dp), contentAlignment = Alignment.CenterStart) {
                Wordmark()
            }
        }
        // Title / search field row — inline swap in the same slot
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(animationSpec = sizeSpec)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .animateContentSize(animationSpec = sizeSpec),
                contentAlignment = Alignment.CenterStart,
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = !searchOpen,
                    enter = fadeIn(enterSpec) + slideInHorizontally(offsetEnter) { -it / 8 },
                    exit = fadeOut(exitSpec) + slideOutHorizontally(offsetExit) { -it / 8 },
                ) {
                    Text(
                        text = "DISCOVER",
                        fontFamily = SansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        letterSpacing = 0.4.sp,
                        color = TextPrimary,
                    )
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = searchOpen,
                    enter = fadeIn(enterSpec) + slideInHorizontally(offsetEnter) { it / 10 },
                    exit = fadeOut(exitSpec) + slideOutHorizontally(offsetExit) { it / 10 },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(PanelHigh)
                            .border(1.dp, if (searchQuery.isNotBlank()) Gold.copy(alpha = 0.4f) else Border, RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            singleLine = true,
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(TextPrimary),
                            textStyle = TextStyle(fontFamily = SansFamily, fontSize = 15.sp, color = TextPrimary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { onSearchSubmit() }),
                            modifier = Modifier.weight(1f).focusRequester(focusRequester),
                            decorationBox = { inner ->
                                if (searchQuery.isEmpty()) Text("Search recipes…", fontFamily = SansFamily, fontSize = 15.sp, color = TextDim)
                                inner()
                            },
                        )
                        if (searchLoading) {
                            CircularProgressIndicator(color = Gold, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                        } else if (searchQuery.isNotBlank()) {
                            Icon(IconClose, contentDescription = "Clear", tint = TextDim, modifier = Modifier.size(14.dp).clickable { onSearchQueryChange("") })
                        }
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier.size(28.dp).clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = if (searchOpen) onSearchClose else onToggleSearch),
                contentAlignment = Alignment.Center,
            ) {
                Icon(IconSearch, contentDescription = "Search", tint = if (searchOpen) Gold else TextMuted, modifier = Modifier.size(20.dp))
            }
        }
        // Subtitle — collapses while searching
        androidx.compose.animation.AnimatedVisibility(
            visible = !searchOpen,
            enter = expandVertically(sizeSpec) + fadeIn(enterSpec),
            exit = shrinkVertically(sizeSpec) + fadeOut(exitSpec),
        ) {
            Text(
                text = "POWERED BY FUJI X WEEKLY",
                fontFamily = MonoFamily,
                fontSize = 10.sp,
                letterSpacing = 1.8.sp,
                color = TextDim,
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 6.dp),
            )
        }
    }
}

@Composable
private fun SearchPromptState(typed: String = "") {
    Box(Modifier.fillMaxSize().imePadding(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(PanelHigh)
                    .border(1.dp, if (typed.isNotBlank()) Gold.copy(alpha = 0.35f) else Border, RoundedCornerShape(999.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = IconSearch,
                    contentDescription = null,
                    tint = if (typed.isNotBlank()) Gold else TextDim,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = if (typed.isNotBlank()) "PRESS ENTER TO SEARCH" else "SEARCH RECIPES",
                fontFamily = SansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 2.sp,
                color = if (typed.isNotBlank()) TextPrimary else TextMuted,
            )
            if (typed.isNotBlank()) {
                Text(
                    text = "\"$typed\"",
                    fontFamily = MonoFamily,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp,
                    color = Gold,
                )
            } else {
                Text(
                    text = "film simulations, looks, creators",
                    fontFamily = SansFamily,
                    fontSize = 12.sp,
                    color = TextDim,
                )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Gold, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(message, fontFamily = MonoFamily, fontSize = 11.sp, letterSpacing = 0.8.sp, color = TextMuted)
            Text(
                text = "RETRY",
                fontFamily = SansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                color = Gold,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .border(1.dp, Gold, RoundedCornerShape(999.dp))
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun RecipeList(
    recipes: List<FxwRecipe>,
    loadingMore: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onOpenRecipe: (FxwRecipe) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp,
        ),
    ) {
        items(recipes.size, key = { recipes[it].id }) { idx ->
            RecipeRow(recipe = recipes[idx], onClick = { onOpenRecipe(recipes[idx]) })
        }
        if (hasMore) {
            item(key = "load-more") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (loadingMore) {
                        CircularProgressIndicator(color = Gold, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = "LOAD MORE",
                            fontFamily = SansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp,
                            letterSpacing = 2.2.sp,
                            color = Gold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .border(1.dp, Gold, RoundedCornerShape(999.dp))
                                .clickable(onClick = onLoadMore)
                                .padding(horizontal = 22.dp, vertical = 10.dp),
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipeRow(recipe: FxwRecipe, onClick: () -> Unit) {
    val context = LocalContext.current
    val cardShape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape),
    ) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PanelHigh)
            .border(1.dp, Border, cardShape)
            .clickable(onClick = onClick),
    ) {
        // Full-width banner image
        if (recipe.imageUrl != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(PanelLow),
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(recipe.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (recipe.filmSim.isNotBlank()) {
                    FilmSimInlineLabel(sim = recipe.filmSim)
                }
                Text(
                    text = recipe.date,
                    fontFamily = MonoFamily,
                    fontSize = 10.5.sp,
                    letterSpacing = 1.sp,
                    color = TextDim,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = recipe.title,
                fontFamily = SansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                letterSpacing = 0.1.sp,
                color = TextPrimary,
            )
            val pills = recipe.pillLabels()
            if (pills.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    pills.forEach { Pill(text = it) }
                }
            }
        }
    }
    // Bottom fade scrim
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .align(Alignment.BottomCenter)
            .background(
                Brush.verticalGradient(listOf(Color.Transparent, PanelHigh))
            ),
    )
    }
}
