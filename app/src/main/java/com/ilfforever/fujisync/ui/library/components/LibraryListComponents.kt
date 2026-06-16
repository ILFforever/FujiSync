package com.ilfforever.fujisync.ui.library.components

import android.animation.ValueAnimator
import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.emoji2.emojipicker.EmojiPickerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ilfforever.fujisync.ui.components.IconCheck
import com.ilfforever.fujisync.ui.components.IconFolder
import com.ilfforever.fujisync.ui.components.IconSearch
import com.ilfforever.fujisync.ui.components.IconStar
import com.ilfforever.fujisync.ui.components.IconStarFilled
import com.ilfforever.fujisync.ui.components.Pill
import com.ilfforever.fujisync.ui.haptics.FujiHapticEffect
import com.ilfforever.fujisync.ui.haptics.FujiHaptics
import com.ilfforever.fujisync.ui.model.LibraryGroupUiModel
import com.ilfforever.fujisync.ui.model.LibraryRecipeUiModel
import com.ilfforever.fujisync.ui.theme.Bg
import com.ilfforever.fujisync.ui.theme.Border
import com.ilfforever.fujisync.ui.theme.Gold
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.PanelLow
import com.ilfforever.fujisync.ui.theme.SansFamily
import com.ilfforever.fujisync.ui.theme.TextDim
import com.ilfforever.fujisync.ui.theme.TextMuted
import com.ilfforever.fujisync.ui.theme.TextPrimary

@Composable
internal fun LibrarySearchField(
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
            cursorBrush = androidx.compose.ui.graphics.SolidColor(TextPrimary),
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
internal fun LibraryEmptySearchState(searching: Boolean, loadError: String? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 42.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = when {
                loadError != null -> "LIBRARY UNAVAILABLE"
                searching -> "NO MATCHES"
                else -> "NO RECIPES"
            },
            fontFamily = SansFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            letterSpacing = 1.2.sp,
            color = if (loadError != null) Gold else TextPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = loadError
                ?: if (searching) "Try another name, group, or film simulation." else "Saved recipes will appear here.",
            fontFamily = SansFamily,
            fontSize = 13.sp,
            color = TextDim,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun LibraryGroupToggleButton(
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
internal fun LibraryModeButton(
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
internal fun GroupHeader(label: String, count: Int, onBack: (() -> Unit)? = null) {
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

@Composable
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
internal fun LibraryRecipeRow(
    recipe: LibraryRecipeUiModel,
    groups: List<LibraryGroupUiModel>,
    groupCounts: Map<String, Int>,
    organizing: Boolean,
    showImages: Boolean = false,
    showCardImageCount: Boolean = true,
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
    val useCardLayout = showImages && recipe.referenceImageUris.isNotEmpty() && !organizing

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
        if (useCardLayout) {
            val count = recipe.referenceImageUris.size
            val pagerState = rememberPagerState { count }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clipToBounds(),
            ) {
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = count > 1,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(Uri.parse(recipe.referenceImageUris[page]))
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(PanelGroupBg),
                    )
                }
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
                if (count > 1 && showCardImageCount) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Bg.copy(alpha = 0.72f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1} / $count",
                            fontFamily = MonoFamily,
                            fontSize = 9.sp,
                            letterSpacing = 0.8.sp,
                            color = TextMuted,
                        )
                    }
                }
            }
        }

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
internal fun SelectionMark(
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
internal fun LibrarySelectionBar(
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
internal fun SelectionAction(
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
internal fun BulkGroupDialog(
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
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(TextPrimary),
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
internal fun GroupAssignmentRow(
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
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(TextPrimary),
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
internal fun LibraryGroupChip(
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

@Composable
internal fun SheetSectionLabel(label: String) {
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
internal fun SheetNameRow(
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
            cursorBrush = androidx.compose.ui.graphics.SolidColor(TextPrimary),
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
internal fun SheetIdentityRow(
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
internal fun SheetMiniAction(
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
internal fun SheetColorChoice(
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
internal fun SheetActionRow(
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
