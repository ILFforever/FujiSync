package com.ilfforever.fujirecipes.ui.discover

import com.ilfforever.fujirecipes.R
import com.ilfforever.fujirecipes.ui.library.normalizedFxwParams
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import com.ilfforever.fujirecipes.ui.haptics.FujiHapticEffect
import com.ilfforever.fujirecipes.ui.haptics.FujiHaptics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ilfforever.fujirecipes.data.ptp.CameraPresetName
import com.ilfforever.fujirecipes.data.remote.FxwRecipe
import com.ilfforever.fujirecipes.data.remote.FxwRepository
import com.ilfforever.fujirecipes.ui.MainViewModel
import com.ilfforever.fujirecipes.ui.components.FilmSimBadgeImage
import com.ilfforever.fujirecipes.ui.components.FilmSimInlineLabel
import com.ilfforever.fujirecipes.ui.components.FilmSimLabel
import com.ilfforever.fujirecipes.ui.components.IconCheck
import com.ilfforever.fujirecipes.ui.components.IconClose
import com.ilfforever.fujirecipes.ui.components.IconGlobe
import com.ilfforever.fujirecipes.ui.components.IconSearch
import com.ilfforever.fujirecipes.ui.components.Pill
import com.ilfforever.fujirecipes.ui.components.PrimaryCTA
import com.ilfforever.fujirecipes.ui.components.PropRow
import com.ilfforever.fujirecipes.ui.components.recipePropertyRows
import com.ilfforever.fujirecipes.ui.components.SectionLabel
import com.ilfforever.fujirecipes.ui.components.Wordmark
import com.ilfforever.fujirecipes.ui.theme.Bg
import com.ilfforever.fujirecipes.ui.theme.Border
import com.ilfforever.fujirecipes.ui.theme.Gold
import com.ilfforever.fujirecipes.ui.theme.MonoFamily
import com.ilfforever.fujirecipes.ui.theme.PanelHigh
import com.ilfforever.fujirecipes.ui.theme.PanelLow
import com.ilfforever.fujirecipes.ui.theme.SansFamily
import com.ilfforever.fujirecipes.ui.theme.TextDim
import com.ilfforever.fujirecipes.ui.theme.TextMuted
import com.ilfforever.fujirecipes.ui.theme.TextPrimary
import androidx.hilt.navigation.compose.hiltViewModel

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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun FxwRecipeDetailScreen(
    recipe: FxwRecipe?,
    maxReferenceImages: Int,
    onClose: () -> Unit,
    onSaveToLibrary: suspend (FxwRecipe, String, Boolean) -> Unit,
    onAfterSave: () -> Unit,
) {
    // Keep last recipe alive so exit animation has content to animate out
    var displayRecipe by remember { mutableStateOf(recipe) }
    LaunchedEffect(recipe) { if (recipe != null) displayRecipe = recipe }
    var showSaveSheet by remember { mutableStateOf(false) }
    var saveName by remember { mutableStateOf("") }
    LaunchedEffect(displayRecipe) { saveName = displayRecipe?.title.orEmpty() }
    val context = LocalContext.current
    val view = LocalView.current
    fun haptic(effect: FujiHapticEffect) = FujiHaptics.perform(context, view, effect)

    AnimatedVisibility(
        visible = recipe != null,
        enter = slideInVertically(initialOffsetY = { it / 3 }, animationSpec = tween(300)) + fadeIn(tween(200)),
        exit = slideOutVertically(targetOffsetY = { it / 3 }, animationSpec = tween(250)) + fadeOut(tween(150)),
    ) {
        val r = displayRecipe ?: return@AnimatedVisibility

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Bg),
        ) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                // Top nav
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier
                            .clickable(onClick = onClose)
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(IconClose, contentDescription = "Close", tint = TextPrimary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "CLOSE",
                            fontFamily = SansFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            letterSpacing = 1.8.sp,
                            color = TextPrimary,
                        )
                    }
                    Text(
                        text = r.date,
                        fontFamily = MonoFamily,
                        fontSize = 10.5.sp,
                        letterSpacing = 1.sp,
                        color = TextDim,
                    )
                }

                Divider()

                // Scrollable body — LazyColumn so HorizontalPager scroll axes don't conflict
                val pagerState = rememberPagerState { r.imageUrls.size }
                var selectedVariantIdx by remember(r.id) { mutableIntStateOf(0) }
                val activeParams = if (r.variants.isNotEmpty()) r.variants[selectedVariantIdx].params else r.params
                val paramSections = r.discoverParamSections(activeParams)

                Box(modifier = Modifier.weight(1f)) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    // Image carousel
                    if (r.imageUrls.isNotEmpty()) {
                        item(key = "carousel") {
                            Box {
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(280.dp),
                                ) { page ->
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(r.imageUrls[page])
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(PanelLow),
                                    )
                                }
                                // Page counter pill — only shown when more than 1 image
                                if (r.imageUrls.size > 1) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(10.dp)
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(Color.Black.copy(alpha = 0.55f))
                                            .padding(horizontal = 10.dp, vertical = 5.dp),
                                    ) {
                                        Text(
                                            text = "${pagerState.currentPage + 1} / ${r.imageUrls.size}",
                                            fontFamily = MonoFamily,
                                            fontSize = 10.sp,
                                            letterSpacing = 0.8.sp,
                                            color = Color.White.copy(alpha = 0.9f),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Hero card
                    item(key = "hero") {
                        var articleExpanded by remember { mutableStateOf(false) }
                        val hasArticle = r.articleText.isNotBlank()
                        val cardShape = RoundedCornerShape(16.dp)

                        Column(
                            modifier = Modifier
                                .padding(14.dp)
                                .clip(cardShape)
                                .background(PanelLow)
                                .border(1.dp, Border, cardShape),
                        ) {
                            // Header section with horizontal padding
                            Column(
                                modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp),
                            ) {
                                if (r.filmSim.isNotBlank()) {
                                    FilmSimLabel(sim = r.filmSim, imageSize = 28.dp)
                                    Spacer(Modifier.height(12.dp))
                                }
                                Text(
                                    text = r.title,
                                    fontFamily = SansFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 32.sp,
                                    letterSpacing = 0.1.sp,
                                    lineHeight = 34.sp,
                                    color = TextPrimary,
                                )
                                // Article text right under the name, clipped when collapsed
                                if (hasArticle) {
                                    Spacer(Modifier.height(12.dp))
                                    if (articleExpanded) {
                                        Text(
                                            text = r.articleText,
                                            fontFamily = SansFamily,
                                            fontSize = 13.sp,
                                            lineHeight = 21.sp,
                                            color = TextMuted,
                                        )
                                    } else {
                                        Box {
                                            Text(
                                                text = r.articleText,
                                                fontFamily = SansFamily,
                                                fontSize = 13.sp,
                                                lineHeight = 21.sp,
                                                color = TextMuted,
                                                maxLines = 4,
                                                overflow = TextOverflow.Clip,
                                                modifier = Modifier.height(84.dp),
                                            )
                                            // Fade scrim
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(48.dp)
                                                    .align(Alignment.BottomCenter)
                                                    .background(
                                                        Brush.verticalGradient(
                                                            listOf(Color.Transparent, PanelLow)
                                                        )
                                                    ),
                                            )
                                        }
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Border),
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { haptic(FujiHapticEffect.SoftConfirm); openOriginalRecipe(context, r) }
                                    .padding(horizontal = 22.dp, vertical = 13.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "SOURCE · FUJI X WEEKLY",
                                    fontFamily = MonoFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.4.sp,
                                    color = TextDim,
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                                ) {
                                    Icon(IconGlobe, contentDescription = null, tint = Gold, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = "OPEN ORIGINAL",
                                        fontFamily = SansFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.sp,
                                        letterSpacing = 1.7.sp,
                                        color = Gold,
                                    )
                                }
                            }

                            // Expand / collapse footer row
                            if (hasArticle) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(Border),
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { haptic(FujiHapticEffect.DrawerSwooshDismiss); articleExpanded = !articleExpanded }
                                        .padding(horizontal = 22.dp, vertical = 13.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = if (articleExpanded) "COLLAPSE" else "EXPAND TO READ",
                                        fontFamily = SansFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.sp,
                                        letterSpacing = 2.sp,
                                        color = Gold,
                                    )
                                    Text(
                                        text = if (articleExpanded) "▲" else "▼",
                                        fontSize = 9.sp,
                                        color = Gold,
                                    )
                                }
                            }
                        }
                    }

                    // Variant toggle (only shown when post has multiple recipe variants)
                    if (r.variants.size > 1) {
                        item(key = "variants") {
                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    r.variants.forEachIndexed { idx, variant ->
                                        val active = idx == selectedVariantIdx
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (active) Gold.copy(alpha = 0.15f) else PanelHigh)
                                                .border(1.5.dp, if (active) Gold.copy(alpha = 0.7f) else Border, RoundedCornerShape(10.dp))
                                                .clickable {
                                                    haptic(FujiHapticEffect.Selection)
                                                    selectedVariantIdx = idx
                                                }
                                                .padding(horizontal = 10.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                                        ) {
                                            FilmSimBadgeImage(sim = variant.label, size = 22.dp)
                                            Text(
                                                text = "Recipe ${idx + 1}",
                                                fontFamily = MonoFamily,
                                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                                fontSize = 10.sp,
                                                letterSpacing = 0.6.sp,
                                                color = if (active) Gold else TextMuted,
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "This post contains ${r.variants.size} recipes. Settings and camera compatibility differ per recipe — check the original post to see which cameras each one supports.",
                                    fontFamily = SansFamily,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    color = TextDim,
                                    modifier = Modifier.padding(horizontal = 2.dp),
                                )
                            }
                        }
                    }

                    // Params sections
                    paramSections.forEach { section ->
                        item(key = "params-${section.label}") {
                            DiscoverParamSection(label = section.label, data = section.data)
                        }
                    }

                    // Compatibility section
                    if (r.variants.isEmpty()) {
                        // Single recipe: show detected generation from title/body
                        if (r.xTransGen != null) {
                            item(key = "compat") {
                                Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                                    Spacer(Modifier.height(8.dp))
                                    SectionLabel(text = "Compatibility", modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp))
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(PanelLow)
                                            .border(1.dp, Border, RoundedCornerShape(14.dp)),
                                    ) {
                                        PropRow(label = "Sensors", value = r.xTransGen, isLast = true)
                                    }
                                    Spacer(Modifier.height(14.dp))
                                }
                            }
                        }
                    }

                    item(key = "spacer") { Spacer(Modifier.height(100.dp)) }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Bg))),
                )
                } // Box

                // Sticky CTA
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Bg)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Border))
                    Spacer(Modifier.height(12.dp))
                    PrimaryCTA(
                        label = "Add to Library",
                        onClick = {
                            haptic(FujiHapticEffect.Selection)
                            saveName = r.title
                            showSaveSheet = true
                        },
                    )
                }
            }
        }
    }

    if (showSaveSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var isSaving by remember { mutableStateOf(false) }
        val hasImages = (displayRecipe?.imageUrls?.size ?: 0) > 0
        var includePhotos by remember { mutableStateOf(true) }
        val nameError = CameraPresetName.validate(saveName)
        val scope = rememberCoroutineScope()
        ModalBottomSheet(
            onDismissRequest = { if (!isSaving) showSaveSheet = false },
            sheetState = sheetState,
            containerColor = PanelLow,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
            ) {
                Text(
                    text = "SAVE TO LIBRARY",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    color = TextDim,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "NAME",
                    fontFamily = MonoFamily,
                    fontSize = 9.5.sp,
                    letterSpacing = 1.6.sp,
                    color = TextDim,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Bg)
                        .border(1.dp, if (nameError != null && saveName.isNotBlank()) Color(0xFFA0522D).copy(alpha = 0.6f) else Border, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicTextField(
                        value = saveName,
                        onValueChange = { if (!isSaving) saveName = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = SansFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = TextPrimary,
                        ),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (saveName.isBlank()) {
                                Text(
                                    text = "Recipe name",
                                    fontFamily = SansFamily,
                                    fontSize = 15.sp,
                                    color = TextDim,
                                )
                            }
                            innerTextField()
                        },
                    )
                    if (saveName.isNotBlank() && !isSaving) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(Border)
                                .clickable { saveName = "" },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(IconClose, contentDescription = "Clear", tint = TextDim, modifier = Modifier.size(10.dp))
                        }
                    }
                }
                if (nameError != null && saveName.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = nameError,
                        fontFamily = SansFamily,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = Color(0xFFA0522D),
                        modifier = Modifier.padding(horizontal = 2.dp),
                    )
                }
                if (hasImages) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Bg)
                            .border(1.dp, if (includePhotos) Gold.copy(alpha = 0.4f) else Border, RoundedCornerShape(10.dp))
                            .clickable(enabled = !isSaving) { haptic(FujiHapticEffect.SoftSelection); includePhotos = !includePhotos }
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "Include photos as reference",
                                fontFamily = SansFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = if (includePhotos) TextPrimary else TextDim,
                            )
                            Text(
                                text = "${minOf(displayRecipe!!.imageUrls.size, maxReferenceImages)} photo${if (minOf(displayRecipe!!.imageUrls.size, maxReferenceImages) != 1) "s" else ""} from this post",
                                fontFamily = SansFamily,
                                fontSize = 11.sp,
                                color = TextMuted,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(if (includePhotos) Gold else Color.Transparent)
                                .border(1.5.dp, if (includePhotos) Gold else Border, androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (includePhotos) {
                                Icon(
                                    IconCheck,
                                    contentDescription = null,
                                    tint = Bg,
                                    modifier = Modifier.size(12.dp),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                PrimaryCTA(
                    label = if (isSaving) "Saving…" else "Save to Library",
                    busy = isSaving,
                    enabled = saveName.isNotBlank() && nameError == null && !isSaving,
                    onClick = {
                        haptic(FujiHapticEffect.SuccessPause)
                        scope.launch {
                            isSaving = true
                            onSaveToLibrary(displayRecipe!!, saveName, includePhotos)
                            showSaveSheet = false
                            onAfterSave()
                        }
                    },
                )
            }
        }
    }
}


private fun openOriginalRecipe(context: android.content.Context, recipe: FxwRecipe) {
    val toolbarColor = android.graphics.Color.parseColor("#0D0D0D")
    val intent = androidx.browser.customtabs.CustomTabsIntent.Builder()
        .setShowTitle(true)
        .setToolbarColor(toolbarColor)
        .setNavigationBarColor(toolbarColor)
        .setNavigationBarDividerColor(android.graphics.Color.parseColor("#1AFFFFFF"))
        .setColorScheme(androidx.browser.customtabs.CustomTabsIntent.COLOR_SCHEME_DARK)
        .setStartAnimations(context, R.anim.slide_in_right, R.anim.slide_out_left)
        .setExitAnimations(context, R.anim.slide_in_left, R.anim.slide_out_right)
        .setShareState(androidx.browser.customtabs.CustomTabsIntent.SHARE_STATE_ON)
        .build()
    intent.launchUrl(context, android.net.Uri.parse(recipe.postUrl))
}

private data class DiscoverParamSectionData(
    val label: String,
    val data: Map<String, String>,
)

private fun FxwRecipe.discoverParamSections(overrideParams: Map<String, String>? = null): List<DiscoverParamSectionData> {
    val normalized = (overrideParams ?: params).normalizedFxwParams()
    val used = mutableSetOf<String>()

    fun section(label: String, keys: List<String>): DiscoverParamSectionData? {
        val data = buildMap {
            keys.forEach { key ->
                val value = normalized[key].orEmpty()
                if (value.isNotBlank()) {
                    put(key, value)
                    used += key
                }
            }
        }
        return data.takeIf { it.isNotEmpty() }?.let { DiscoverParamSectionData(label, it) }
    }

    return buildList {
        section(
            label = "Effects",
            keys = listOf("Dynamic Range", "D Range Priority", "Grain Effect", "Color Chrome", "Color Chrome FX Blue", "Smooth Skin"),
        )?.let(::add)
        section(
            label = "White Balance",
            keys = listOf("White Balance", "WB Shift R", "WB Shift B"),
        )?.let(::add)
        section(
            label = "Tone",
            keys = listOf("Highlight Tone", "Shadow Tone", "Color", "Sharpness", "High ISO NR", "Clarity"),
        )?.let(::add)

        val other = normalized
            .filterKeys { it != "Film Simulation" && it !in used }
            .filterValues { it.isNotBlank() }
        if (other.isNotEmpty()) add(DiscoverParamSectionData("Other", other))
    }
}


@Composable
private fun DiscoverParamSection(label: String, data: Map<String, String>) {
    if (data.isEmpty()) return
    val entries = recipePropertyRows(data)
    if (entries.isEmpty()) return
    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 0.dp)) {
        Spacer(Modifier.height(8.dp))
        SectionLabel(text = label, modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(PanelLow)
                .border(1.dp, Border, RoundedCornerShape(14.dp)),
        ) {
            entries.forEachIndexed { idx, (key, value) ->
                PropRow(
                    label = key,
                    value = value,
                    isLast = idx == entries.lastIndex,
                )
                if (idx < entries.lastIndex) Divider()
            }
        }
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Border),
    )
}
