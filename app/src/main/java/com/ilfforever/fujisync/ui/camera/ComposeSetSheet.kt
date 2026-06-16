package com.ilfforever.fujisync.ui.camera

import android.animation.ValueAnimator
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujisync.ui.components.IconCheck
import com.ilfforever.fujisync.ui.components.IconChevronRight
import com.ilfforever.fujisync.ui.components.PrimaryCTA
import com.ilfforever.fujisync.ui.haptics.FujiHapticEffect
import com.ilfforever.fujisync.ui.haptics.FujiHaptics
import com.ilfforever.fujisync.ui.model.LibraryRecipeUiModel
import com.ilfforever.fujisync.ui.model.RecipeUiModel
import com.ilfforever.fujisync.ui.theme.Bg
import com.ilfforever.fujisync.ui.theme.Border
import com.ilfforever.fujisync.ui.theme.Gold
import com.ilfforever.fujisync.ui.theme.GoldDim
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.PanelHigh
import com.ilfforever.fujisync.ui.theme.SansFamily
import com.ilfforever.fujisync.ui.theme.SheetBg
import com.ilfforever.fujisync.ui.theme.SheetBorder
import com.ilfforever.fujisync.ui.theme.TextDim
import com.ilfforever.fujisync.ui.theme.TextMuted
import com.ilfforever.fujisync.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ComposeControlBg = Color(0xFF141210)

private val slotLabels = listOf("C1", "C2", "C3", "C4", "C5", "C6", "C7")

private sealed class ComposePhase {
    data object Assigning : ComposePhase()
    data class Picking(val slotIdx: Int) : ComposePhase()
    data class Saved(val setLabel: String) : ComposePhase()
}

@Composable
fun ComposeSetSheet(
    libraryRecipes: List<LibraryRecipeUiModel>,
    onDismiss: () -> Unit,
    onSave: (label: String, slots: List<RecipeUiModel>) -> Unit,
) {
    val motionEnabled = ValueAnimator.areAnimatorsEnabled()
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(!motionEnabled) }
    val defaultLabel = remember {
        "Custom · " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d", Locale.US))
    }
    var label by remember { mutableStateOf(defaultLabel) }
    val assignments = remember { mutableStateMapOf<Int, LibraryRecipeUiModel>() }
    var phase by remember { mutableStateOf<ComposePhase>(ComposePhase.Assigning) }
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current
    val view = LocalView.current

    val allAssigned = slotLabels.indices.all { assignments.containsKey(it) }

    fun dismissWithMotion() {
        FujiHaptics.perform(context, view, FujiHapticEffect.SheetDismiss)
        if (!motionEnabled) { onDismiss(); return }
        scope.launch { visible = false; delay(180); onDismiss() }
    }

    BackHandler {
        when (val p = phase) {
            is ComposePhase.Picking -> { phase = ComposePhase.Assigning; searchQuery = "" }
            is ComposePhase.Saved -> dismissWithMotion()
            is ComposePhase.Assigning -> dismissWithMotion()
        }
    }

    LaunchedEffect(motionEnabled) {
        visible = true
        FujiHaptics.perform(context, view, FujiHapticEffect.SheetOpen)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .imePadding()
            .clickable(onClick = ::dismissWithMotion),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(SheetBg)
                .border(1.dp, SheetBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .clickable(onClick = {})
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

            AnimatedContent(
                targetState = phase,
                transitionSpec = {
                    when {
                        targetState is ComposePhase.Saved ->
                            (fadeIn(tween(220)) + slideInVertically(tween(280, easing = FastOutSlowInEasing)) { it / 5 })
                                .togetherWith(fadeOut(tween(150)))
                        targetState is ComposePhase.Picking ->
                            (fadeIn(tween(150)) + slideInHorizontally(tween(250, easing = FastOutSlowInEasing)) { it / 4 })
                                .togetherWith(fadeOut(tween(100)) + slideOutHorizontally(tween(200, easing = FastOutSlowInEasing)) { -it / 4 })
                        else ->
                            (fadeIn(tween(150)) + slideInHorizontally(tween(250, easing = FastOutSlowInEasing)) { -it / 4 })
                                .togetherWith(fadeOut(tween(100)) + slideOutHorizontally(tween(200, easing = FastOutSlowInEasing)) { it / 4 })
                    }
                },
                label = "compose-phase",
            ) { p ->
                when (p) {
                    is ComposePhase.Picking -> RecipePickerContent(
                        slotLabel = slotLabels[p.slotIdx],
                        recipes = libraryRecipes,
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        onPick = { recipe ->
                            assignments[p.slotIdx] = recipe
                            FujiHaptics.perform(context, view, FujiHapticEffect.Confirm)
                            phase = ComposePhase.Assigning
                            searchQuery = ""
                        },
                        onBack = { phase = ComposePhase.Assigning; searchQuery = "" },
                    )
                    is ComposePhase.Saved -> SavedContent(setLabel = p.setLabel, onDone = ::dismissWithMotion)
                    is ComposePhase.Assigning -> SlotAssignmentContent(
                        label = label,
                        onLabelChange = { label = it },
                        assignments = assignments,
                        allAssigned = allAssigned,
                        onSlotTap = { idx ->
                            FujiHaptics.perform(context, view, FujiHapticEffect.SheetOpen)
                            phase = ComposePhase.Picking(idx)
                        },
                        onCancel = ::dismissWithMotion,
                        onSave = {
                            val finalLabel = label.trim().ifBlank { defaultLabel }
                            val slots = slotLabels.mapIndexed { idx, slotLabel ->
                                assignments[idx]!!.toSlotRecipe(slotLabel)
                            }
                            onSave(finalLabel, slots)
                            FujiHaptics.perform(context, view, FujiHapticEffect.Confirm)
                            phase = ComposePhase.Saved(finalLabel)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedContent(setLabel: String, onDone: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    var scaleTarget by remember { mutableStateOf(0f) }
    val checkScale by animateFloatAsState(
        targetValue = scaleTarget,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "check-scale",
    )
    var contentVisible by remember { mutableStateOf(false) }
    val contentAlpha by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "saved-content-alpha",
    )

    LaunchedEffect(Unit) {
        scaleTarget = 1f
        delay(180)
        contentVisible = true
        delay(40)
        FujiHaptics.perform(context, view, FujiHapticEffect.SoftSuccess)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp)
            .padding(top = 36.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .scale(checkScale)
                .clip(CircleShape)
                .background(Gold),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = IconCheck,
                contentDescription = null,
                tint = Bg,
                modifier = Modifier.size(32.dp),
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "SET SAVED",
            fontFamily = SansFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            color = TextDim.copy(alpha = contentAlpha),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = setLabel,
            fontFamily = SansFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            letterSpacing = 0.2.sp,
            color = TextPrimary.copy(alpha = contentAlpha),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(20.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(horizontal = 4.dp),
        ) {
            Text(
                text = "Find it in",
                fontFamily = SansFamily,
                fontSize = 13.sp,
                color = TextMuted.copy(alpha = contentAlpha),
            )
            Text(
                text = "CAMERA",
                fontFamily = MonoFamily,
                fontSize = 11.sp,
                letterSpacing = 1.2.sp,
                color = Gold.copy(alpha = contentAlpha),
            )
            Icon(
                imageVector = IconChevronRight,
                contentDescription = null,
                tint = TextDim.copy(alpha = contentAlpha),
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = "RESTORE SETS",
                fontFamily = MonoFamily,
                fontSize = 11.sp,
                letterSpacing = 1.2.sp,
                color = Gold.copy(alpha = contentAlpha),
            )
        }

        Spacer(Modifier.height(32.dp))

        PrimaryCTA(
            label = "Done",
            onClick = onDone,
            enabled = contentVisible,
        )
    }
}

@Composable
private fun SlotAssignmentContent(
    label: String,
    onLabelChange: (String) -> Unit,
    assignments: Map<Int, LibraryRecipeUiModel>,
    allAssigned: Boolean,
    onSlotTap: (Int) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .padding(top = 18.dp, bottom = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "COMPOSE SET",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    letterSpacing = 0.4.sp,
                    color = TextPrimary,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "ASSIGN C1–C7 FROM LIBRARY",
                    fontFamily = MonoFamily,
                    fontSize = 10.sp,
                    letterSpacing = 1.3.sp,
                    color = TextDim,
                )
            }
            Text(
                text = "CANCEL",
                fontFamily = SansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.3.sp,
                color = Gold,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = onCancel)
                    .padding(horizontal = 4.dp, vertical = 6.dp),
            )
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = "NAME",
            fontFamily = SansFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 9.5.sp,
            letterSpacing = 2.sp,
            color = TextDim,
        )
        Spacer(Modifier.height(8.dp))
        BasicTextField(
            value = label,
            onValueChange = onLabelChange,
            singleLine = true,
            cursorBrush = androidx.compose.ui.graphics.SolidColor(TextPrimary),
            textStyle = TextStyle(
                fontFamily = SansFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = TextPrimary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(ComposeControlBg)
                .border(1.dp, Gold, RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        )

        Spacer(Modifier.height(20.dp))
        Text(
            text = "SLOTS",
            fontFamily = SansFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 9.5.sp,
            letterSpacing = 2.sp,
            color = TextDim,
        )
        Spacer(Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Border, RoundedCornerShape(12.dp)),
        ) {
            slotLabels.forEachIndexed { idx, slotLabel ->
                val assigned = assignments[idx]
                if (idx > 0) {
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Border))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSlotTap(idx) }
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = slotLabel,
                        fontFamily = MonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        color = if (assigned != null) Gold else TextMuted,
                        modifier = Modifier.width(28.dp),
                    )
                    Text(
                        text = assigned?.name ?: "—",
                        fontFamily = SansFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = if (assigned != null) TextPrimary else TextMuted,
                        modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (assigned != null) {
                        Text(
                            text = assigned.sim,
                            fontFamily = MonoFamily,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp,
                            color = TextDim,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(end = 6.dp),
                        )
                    }
                    Icon(
                        imageVector = IconChevronRight,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        PrimaryCTA(
            label = if (allAssigned) "Save Set" else "Assign All 7 Slots to Save",
            enabled = allAssigned,
            onClick = onSave,
        )
    }
}

@Composable
private fun RecipePickerContent(
    slotLabel: String,
    recipes: List<LibraryRecipeUiModel>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onPick: (LibraryRecipeUiModel) -> Unit,
    onBack: () -> Unit,
) {
    val filtered = remember(searchQuery, recipes) {
        if (searchQuery.isBlank()) recipes
        else recipes.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                it.sim.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .padding(top = 18.dp, bottom = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "←",
                fontFamily = SansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Gold,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = onBack)
                    .padding(end = 10.dp, top = 4.dp, bottom = 4.dp),
            )
            Column {
                Text(
                    text = "ASSIGN TO $slotLabel",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    letterSpacing = 0.3.sp,
                    color = TextPrimary,
                )
                Text(
                    text = "PICK FROM LIBRARY",
                    fontFamily = MonoFamily,
                    fontSize = 10.sp,
                    letterSpacing = 1.3.sp,
                    color = TextDim,
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        BasicTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            singleLine = true,
            cursorBrush = androidx.compose.ui.graphics.SolidColor(TextPrimary),
            textStyle = TextStyle(
                fontFamily = SansFamily,
                fontSize = 14.sp,
                color = TextPrimary,
            ),
            decorationBox = { inner ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search recipes…",
                            fontFamily = SansFamily,
                            fontSize = 14.sp,
                            color = TextMuted,
                        )
                    }
                    inner()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(ComposeControlBg)
                .border(1.dp, Border, RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 11.dp),
        )

        Spacer(Modifier.height(10.dp))

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (recipes.isEmpty()) "No recipes in library.\nAdd recipes first." else "No recipes match \"$searchQuery\".",
                    fontFamily = SansFamily,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Border, RoundedCornerShape(12.dp)),
                ) {
                    filtered.forEachIndexed { idx, recipe ->
                        if (idx > 0) {
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Border))
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(recipe) }
                                .background(PanelHigh)
                                .padding(horizontal = 16.dp, vertical = 13.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    text = recipe.name,
                                    fontFamily = SansFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = recipe.sim.uppercase(),
                                    fontFamily = MonoFamily,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.8.sp,
                                    color = TextDim,
                                )
                            }
                            Icon(
                                imageVector = IconChevronRight,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}

private fun LibraryRecipeUiModel.toSlotRecipe(slotLabel: String) = RecipeUiModel(
    libraryId = id,
    slot = slotLabel,
    name = name,
    sim = sim,
    pills = pills,
    description = description,
    effects = effects,
    tone = tone,
    wb = wb,
    saved = saved,
    sourceCameraName = sourceCameraName,
    sourceCameraModel = sourceCameraModel,
    sourceUsbId = sourceUsbId,
    sourceUrl = sourceUrl,
    sourceLabel = sourceLabel,
    referenceImageUris = referenceImageUris,
    groupIds = groupIds,
    favorite = favorite,
    isoMin = isoMin,
    isoMax = isoMax,
    exposureCompMin = exposureCompMin,
    exposureCompMax = exposureCompMax,
    sensorGens = sensorGens,
)
