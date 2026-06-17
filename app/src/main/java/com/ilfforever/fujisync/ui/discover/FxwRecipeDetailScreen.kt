package com.ilfforever.fujisync.ui.discover

import com.ilfforever.fujisync.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ilfforever.fujisync.data.ptp.CameraPresetName
import com.ilfforever.fujisync.data.remote.FxwRecipe
import com.ilfforever.fujisync.ui.components.FilmSimBadgeImage
import com.ilfforever.fujisync.ui.components.FilmSimLabel
import com.ilfforever.fujisync.ui.components.IconCheck
import com.ilfforever.fujisync.ui.components.IconClose
import com.ilfforever.fujisync.ui.components.IconGlobe
import com.ilfforever.fujisync.ui.components.PrimaryCTA
import com.ilfforever.fujisync.ui.components.PropRow
import com.ilfforever.fujisync.ui.components.SectionLabel
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun FxwRecipeDetailScreen(
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
                        cursorBrush = SolidColor(TextPrimary),
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
                                .clip(CircleShape)
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
                                .clip(CircleShape)
                                .background(if (includePhotos) Gold else Color.Transparent)
                                .border(1.5.dp, if (includePhotos) Gold else Border, CircleShape),
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
