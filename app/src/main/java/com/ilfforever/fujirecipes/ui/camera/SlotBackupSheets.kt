package com.ilfforever.fujirecipes.ui.camera

import android.animation.ValueAnimator
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujirecipes.ui.haptics.FujiHapticEffect
import com.ilfforever.fujirecipes.ui.haptics.FujiHaptics
import com.ilfforever.fujirecipes.ui.components.IconChevronRight
import com.ilfforever.fujirecipes.ui.components.IconEdit
import com.ilfforever.fujirecipes.ui.components.IconTrash
import com.ilfforever.fujirecipes.ui.components.PrimaryCTA
import com.ilfforever.fujirecipes.ui.model.DuplicateMatchKind
import com.ilfforever.fujirecipes.ui.model.RecipeUiModel
import com.ilfforever.fujirecipes.ui.model.SaveAllReport
import com.ilfforever.fujirecipes.ui.model.SlotBackupMeta
import com.ilfforever.fujirecipes.ui.model.SlotBackupSet
import com.ilfforever.fujirecipes.ui.theme.Border
import com.ilfforever.fujirecipes.ui.theme.Gold
import com.ilfforever.fujirecipes.ui.theme.GoldDim
import com.ilfforever.fujirecipes.ui.theme.MonoFamily
import com.ilfforever.fujirecipes.ui.theme.PanelHigh
import com.ilfforever.fujirecipes.ui.theme.SansFamily
import com.ilfforever.fujirecipes.ui.theme.TextDim
import com.ilfforever.fujirecipes.ui.theme.SheetBg
import com.ilfforever.fujirecipes.ui.theme.SheetBorder
import com.ilfforever.fujirecipes.ui.theme.TextMuted
import com.ilfforever.fujirecipes.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val BackupControlBg = Color(0xFF141210)

@Composable
fun BackupSheet(
    saving: Boolean = false,
    savingSlotIndex: Int = -1,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val motionEnabled = ValueAnimator.areAnimatorsEnabled()
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(!motionEnabled) }
    val defaultLabel = remember {
        "C1–C7 · " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d", Locale.US))
    }
    var label by remember { mutableStateOf(defaultLabel) }
    var submitted by remember { mutableStateOf(false) }
    var observedSaving by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val view = LocalView.current

    fun dismissWithMotion(playHaptic: Boolean = true) {
        if (saving) return
        if (playHaptic) FujiHaptics.perform(context, view, FujiHapticEffect.SheetDismiss)
        if (!motionEnabled) { onDismiss(); return }
        scope.launch { visible = false; delay(180); onDismiss() }
    }

    BackHandler(onBack = ::dismissWithMotion)

    fun save() {
        val trimmed = label.trim().ifBlank { defaultLabel }
        submitted = true
        onConfirm(trimmed)
    }

    LaunchedEffect(saving, submitted) {
        if (saving) {
            observedSaving = true
            FujiHaptics.perform(context, view, FujiHapticEffect.Confirm)
        }
        if (submitted && observedSaving && !saving) dismissWithMotion(playHaptic = false)
    }

    LaunchedEffect(motionEnabled) {
        visible = true
        FujiHaptics.perform(context, view, FujiHapticEffect.SheetOpen)
    }

    val overlayTransition = updateTransition(targetState = visible, label = "backup-overlay")
    val overlayAlpha by overlayTransition.animateFloat(
        transitionSpec = { tween(if (targetState) 170 else 120, easing = FastOutSlowInEasing) },
        label = "backup-overlay-alpha",
    ) { if (it) 1f else 0f }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f * overlayAlpha))
            .imePadding()
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
                    .heightIn(max = 500.dp)
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
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
                                text = "BACKUP SETS",
                                fontFamily = SansFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                letterSpacing = 0.4.sp,
                                color = TextPrimary,
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "SAVE C1–C7 TO PHONE",
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
                                .clickable(enabled = !saving, onClick = ::dismissWithMotion)
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                        )
                    }
                    Spacer(Modifier.height(22.dp))
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
                        onValueChange = { label = it },
                        enabled = !saving,
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = SansFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = TextPrimary,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(BackupControlBg)
                            .border(1.dp, Gold, RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Reads C1–C7 from the camera, then saves a global backup set to this device.",
                        fontFamily = SansFamily,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = TextDim,
                    )
                    AnimatedVisibility(visible = saving) {
                        Column {
                            Spacer(Modifier.height(16.dp))
                            BackupReadProgress(currentSlotIndex = savingSlotIndex)
                        }
                    }
                    Spacer(Modifier.height(22.dp))
                    PrimaryCTA(
                        label = if (saving) "Reading Camera" else "Save Set",
                        busy = saving,
                        enabled = !saving,
                        onClick = ::save,
                    )
                }
            }
        }
    }
}

@Composable
private fun BackupReadProgress(currentSlotIndex: Int) {
    val context = LocalContext.current
    val view = LocalView.current
    LaunchedEffect(currentSlotIndex) {
        if (currentSlotIndex >= 0) FujiHaptics.performStepClick(context, view, step = currentSlotIndex, total = 7)
    }
    val transition = rememberInfiniteTransition(label = "backup-read-pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(720, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "backup-read-pulse-alpha",
    )
    val safeIndex = currentSlotIndex.coerceIn(-1, 6)
    val statusText = if (safeIndex >= 0) {
        "Reading C${safeIndex + 1} from camera"
    } else {
        "Opening camera session"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PanelHigh.copy(alpha = 0.72f))
            .border(1.dp, GoldDim.copy(alpha = 0.35f + (0.18f * pulse)), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "VALIDATING CAMERA",
                fontFamily = MonoFamily,
                fontSize = 10.sp,
                letterSpacing = 1.4.sp,
                color = Gold,
            )
            Text(
                text = statusText.uppercase(Locale.US),
                fontFamily = SansFamily,
                fontSize = 11.sp,
                color = TextMuted,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(7) { index ->
                val active = safeIndex == index || (safeIndex < 0 && index == 0)
                val complete = safeIndex > index
                val slotAlpha = when {
                    active -> 0.18f + (0.20f * pulse)
                    complete -> 0.28f
                    else -> 0.08f
                }
                val scale by animateFloatAsState(
                    targetValue = if (active) 1.06f else 1f,
                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                    label = "backup-slot-scale-$index",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(RoundedCornerShape(8.dp))
                        .background(Gold.copy(alpha = slotAlpha))
                        .border(
                            1.dp,
                            when {
                                active -> Gold.copy(alpha = 0.82f)
                                complete -> GoldDim.copy(alpha = 0.62f)
                                else -> Border.copy(alpha = 0.65f)
                            },
                            RoundedCornerShape(8.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "C${index + 1}",
                        fontFamily = MonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (complete || active) TextPrimary else TextMuted,
                    )
                }
            }
        }
    }
}

private enum class RestorePhase { Idle, Working, Validating }

@Composable
fun RestoreSheet(
    meta: SlotBackupMeta?,
    backupSlots: List<RecipeUiModel>? = null,
    backupSets: List<SlotBackupSet> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onDelete: () -> Unit = {},
    onRename: (String) -> Unit = {},
    onSelectBackup: (String) -> Unit = {},
    restoreInProgress: Boolean = false,
    isRestoringValidation: Boolean = false,
    readingSlots: Boolean = false,
    readingSlotIndex: Int = -1,
    loadedSlots: List<RecipeUiModel> = emptyList(),
) {
    val motionEnabled = ValueAnimator.areAnimatorsEnabled()
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(!motionEnabled) }
    var renaming by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }
    var slotsExpanded by remember { mutableStateOf(false) }
    var phase by remember { mutableStateOf(RestorePhase.Idle) }
    val chevronAngle by animateFloatAsState(
        targetValue = if (slotsExpanded) 90f else 0f,
        animationSpec = tween(if (motionEnabled) 220 else 0, easing = FastOutSlowInEasing),
        label = "chevron",
    )
    val showValidation = isRestoringValidation || phase == RestorePhase.Validating
    val selectedId = meta?.id
    val visibleBackupSets = backupSets.ifEmpty {
        if (meta != null && backupSlots != null) listOf(SlotBackupSet(meta, backupSlots)) else emptyList()
    }
    val context = LocalContext.current
    val view = LocalView.current

    fun dismissWithMotion() {
        if (showValidation && readingSlots) return
        if (restoreInProgress) return  // block only while writes are actively in flight
        FujiHaptics.perform(context, view, FujiHapticEffect.SheetDismiss)
        if (!motionEnabled) { onDismiss(); return }
        scope.launch { visible = false; delay(180); onDismiss() }
    }

    fun restore() {
        phase = RestorePhase.Working
        onConfirm()
        // sheet stays open — loading overlay takes over, then reading animation
    }
    fun confirmDelete() { onDelete(); dismissWithMotion() }

    LaunchedEffect(readingSlotIndex) {
        if (readingSlotIndex >= 0) FujiHaptics.performStepClick(context, view, step = readingSlotIndex, total = 7)
    }

    // Transition Working → Validating when the VM starts its post-restore read.
    // The validation result stays open until the user closes it.
    LaunchedEffect(readingSlots, restoreInProgress, isRestoringValidation) {
        if (isRestoringValidation) {
            phase = RestorePhase.Validating
            return@LaunchedEffect
        }
        when (phase) {
            RestorePhase.Working -> if (readingSlots) phase = RestorePhase.Validating
            else -> {}
        }
    }

    BackHandler(onBack = ::dismissWithMotion)
    LaunchedEffect(motionEnabled) {
        visible = true
        FujiHaptics.perform(context, view, FujiHapticEffect.SheetOpen)
    }

    val overlayTransition = updateTransition(targetState = visible, label = "restore-overlay")
    val overlayAlpha by overlayTransition.animateFloat(
        transitionSpec = { tween(if (targetState) 170 else 120, easing = FastOutSlowInEasing) },
        label = "restore-overlay-alpha",
    ) { if (it) 1f else 0f }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f * overlayAlpha))
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
                    .heightIn(max = 560.dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(SheetBg)
                    .border(1.dp, SheetBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .clickable(onClick = {})
                    .navigationBarsPadding()
                    .imePadding(),
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
                if (showValidation) {
                    RestoreReadingContent(
                        readingSlotIndex = readingSlotIndex,
                        loadedSlots = loadedSlots,
                        readingSlots = readingSlots,
                        onClose = ::dismissWithMotion,
                    )
                } else Column(
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
                                text = "RESTORE SETS",
                                fontFamily = SansFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                letterSpacing = 0.4.sp,
                                color = TextPrimary,
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "LOAD BACKUP TO CAMERA VIEW",
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
                                .clickable(onClick = ::dismissWithMotion)
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                        )
                    }

                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = "SAVED SETS",
                        fontFamily = MonoFamily,
                        fontSize = 10.sp,
                        letterSpacing = 2.sp,
                        color = TextDim,
                    )
                    Spacer(Modifier.height(8.dp))

                    if (visibleBackupSets.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            visibleBackupSets.forEach { set ->
                                val selected = set.meta.id == selectedId
                                val cardBorder by animateColorAsState(
                                    targetValue = if (selected && !confirmingDelete) Gold else SheetBorder,
                                    animationSpec = tween(if (motionEnabled) 220 else 0, easing = FastOutSlowInEasing),
                                    label = "restore-set-border-${set.meta.id}",
                                )
                                val cardBg by animateColorAsState(
                                    targetValue = if (selected) BackupControlBg.copy(alpha = 0.96f) else BackupControlBg,
                                    animationSpec = tween(if (motionEnabled) 220 else 0, easing = FastOutSlowInEasing),
                                    label = "restore-set-bg-${set.meta.id}",
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateContentSize(
                                            animationSpec = tween(if (motionEnabled) 260 else 0, easing = FastOutSlowInEasing),
                                        )
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(cardBg)
                                        .border(
                                            1.dp,
                                            cardBorder,
                                            RoundedCornerShape(12.dp),
                                        ),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (selected) Gold.copy(alpha = 0.08f) else Color.Transparent)
                                            .clickable {
                                                onSelectBackup(set.meta.id)
                                                confirmingDelete = false
                                                renaming = false
                                                slotsExpanded = false
                                            }
                                            .padding(start = 16.dp, end = 10.dp, top = 14.dp, bottom = 14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = set.meta.label,
                                                fontFamily = SansFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = TextPrimary,
                                            )
                                            Spacer(Modifier.height(3.dp))
                                            Text(
                                                text = set.meta.savedAt.uppercase(),
                                                fontFamily = MonoFamily,
                                                fontSize = 10.sp,
                                                letterSpacing = 1.4.sp,
                                                color = if (selected) Gold else TextDim,
                                            )
                                        }
                                        if (selected) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            ) {
                                                IconButton(IconEdit, tint = TextMuted, onClick = { renaming = true; confirmingDelete = false })
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .clickable { slotsExpanded = !slotsExpanded },
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Icon(
                                                        IconChevronRight,
                                                        contentDescription = "View slots",
                                                        tint = TextMuted,
                                                        modifier = Modifier
                                                            .size(16.dp)
                                                            .graphicsLayer { rotationZ = chevronAngle },
                                                    )
                                                }
                                                IconButton(IconTrash, tint = Color(0xFFD94040), onClick = { confirmingDelete = true })
                                            }
                                        }
                                    }

                                    AnimatedVisibility(
                                        visible = selected && slotsExpanded,
                                        enter = fadeIn(tween(if (motionEnabled) 140 else 0, easing = FastOutSlowInEasing)) +
                                            expandVertically(
                                                animationSpec = tween(if (motionEnabled) 260 else 0, easing = FastOutSlowInEasing),
                                                expandFrom = Alignment.Top,
                                            ) +
                                            slideInVertically(
                                                animationSpec = tween(if (motionEnabled) 220 else 0, easing = FastOutSlowInEasing),
                                                initialOffsetY = { -it / 10 },
                                            ),
                                        exit = fadeOut(tween(if (motionEnabled) 100 else 0, easing = FastOutSlowInEasing)) +
                                            shrinkVertically(
                                                animationSpec = tween(if (motionEnabled) 180 else 0, easing = FastOutSlowInEasing),
                                                shrinkTowards = Alignment.Top,
                                            ) +
                                            slideOutVertically(
                                                animationSpec = tween(if (motionEnabled) 150 else 0, easing = FastOutSlowInEasing),
                                                targetOffsetY = { -it / 12 },
                                            ),
                                    ) {
                                        Column {
                                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SheetBorder))
                                            set.slots.forEachIndexed { index, slot ->
                                                val rowAlpha by animateFloatAsState(
                                                    targetValue = if (selected && slotsExpanded) 1f else 0.74f,
                                                    animationSpec = tween(
                                                        durationMillis = if (motionEnabled) 180 else 0,
                                                        delayMillis = if (motionEnabled) index * 18 else 0,
                                                        easing = FastOutSlowInEasing,
                                                    ),
                                                    label = "restore-slot-row-${set.meta.id}-$index",
                                                )
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .graphicsLayer { alpha = rowAlpha }
                                                        .padding(horizontal = 16.dp, vertical = 9.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    Text(
                                                        text = slot.slot,
                                                        fontFamily = MonoFamily,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.5.sp,
                                                        letterSpacing = 1.sp,
                                                        color = Gold,
                                                        modifier = Modifier.width(28.dp),
                                                    )
                                                    Text(
                                                        text = slot.name,
                                                        fontFamily = SansFamily,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 13.sp,
                                                        color = TextPrimary,
                                                        modifier = Modifier.weight(1f),
                                                    )
                                                    Text(
                                                        text = slot.sim.trimEnd('.').uppercase(),
                                                        fontFamily = MonoFamily,
                                                        fontSize = 10.sp,
                                                        letterSpacing = 1.sp,
                                                        color = TextMuted,
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (selected && confirmingDelete) {
                                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SheetBorder))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                text = "Delete this set?",
                                                fontFamily = SansFamily,
                                                fontSize = 13.sp,
                                                color = TextMuted,
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text(
                                                    text = "CANCEL",
                                                    fontFamily = SansFamily,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 11.sp,
                                                    letterSpacing = 1.2.sp,
                                                    color = TextMuted,
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(999.dp))
                                                        .clickable { confirmingDelete = false }
                                                        .padding(horizontal = 8.dp, vertical = 5.dp),
                                                )
                                                Text(
                                                    text = "DELETE",
                                                    fontFamily = SansFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    letterSpacing = 1.2.sp,
                                                    color = Color(0xFFD94040),
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(999.dp))
                                                        .background(Color(0xFFD94040).copy(alpha = 0.12f))
                                                        .clickable(onClick = ::confirmDelete)
                                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(BackupControlBg)
                                .border(1.dp, SheetBorder, RoundedCornerShape(12.dp)),
                        ) {
                            Text(
                                text = "No saved sets yet.",
                                fontFamily = SansFamily,
                                fontSize = 13.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(22.dp))
                    PrimaryCTA(
                        label = "Restore Set",
                        enabled = meta != null && !confirmingDelete,
                        onClick = ::restore,
                    )
                }
            }
        }

        if (renaming && meta != null) {
            RenameSheet(
                currentLabel = meta.label,
                onDismiss = { renaming = false },
                onConfirm = { newLabel ->
                    if (newLabel.isNotBlank() && newLabel != meta.label) onRename(newLabel)
                    renaming = false
                },
            )
        }
    }
}

@Composable
private fun RenameSheet(
    currentLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val motionEnabled = ValueAnimator.areAnimatorsEnabled()
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(!motionEnabled) }
    var label by remember { mutableStateOf(currentLabel) }

    val context = LocalContext.current
    val view = LocalView.current

    fun dismissWithMotion(playHaptic: Boolean = true) {
        if (playHaptic) FujiHaptics.perform(context, view, FujiHapticEffect.SheetDismiss)
        if (!motionEnabled) { onDismiss(); return }
        scope.launch { visible = false; delay(180); onDismiss() }
    }

    fun save() {
        FujiHaptics.perform(context, view, FujiHapticEffect.Confirm)
        onConfirm(label.trim())
        dismissWithMotion(playHaptic = false)
    }

    BackHandler(onBack = ::dismissWithMotion)
    LaunchedEffect(motionEnabled) {
        visible = true
        FujiHaptics.perform(context, view, FujiHapticEffect.SheetOpen)
    }

    val overlayTransition = updateTransition(targetState = visible, label = "rename-overlay")
    val overlayAlpha by overlayTransition.animateFloat(
        transitionSpec = { tween(if (targetState) 170 else 120, easing = FastOutSlowInEasing) },
        label = "rename-overlay-alpha",
    ) { if (it) 1f else 0f }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f * overlayAlpha))
            .imePadding()
            .clickable(onClick = ::dismissWithMotion),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(150, easing = FastOutSlowInEasing)) + slideInVertically(
                animationSpec = tween(260, easing = FastOutSlowInEasing),
                initialOffsetY = { it / 4 },
            ),
            exit = fadeOut(tween(100, easing = FastOutSlowInEasing)) + slideOutVertically(
                animationSpec = tween(160, easing = FastOutSlowInEasing),
                targetOffsetY = { it / 4 },
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
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
                                text = "RENAME SET",
                                fontFamily = SansFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                letterSpacing = 0.4.sp,
                                color = TextPrimary,
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "EDIT SET LABEL",
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
                                .clickable(onClick = ::dismissWithMotion)
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                        )
                    }
                    Spacer(Modifier.height(22.dp))
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
                        onValueChange = { label = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = SansFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = TextPrimary,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(BackupControlBg)
                            .border(1.dp, Gold, RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                    )
                    Spacer(Modifier.height(22.dp))
                    PrimaryCTA(
                        label = "Save",
                        enabled = label.trim().isNotBlank(),
                        onClick = ::save,
                    )
                }
            }
        }
    }
}

@Composable
fun SaveAllReportSheet(
    report: SaveAllReport,
    onDismiss: () -> Unit,
) {
    val motionEnabled = ValueAnimator.areAnimatorsEnabled()
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(!motionEnabled) }
    val context = LocalContext.current
    val view = LocalView.current

    fun dismissWithMotion() {
        FujiHaptics.perform(context, view, FujiHapticEffect.SheetDismiss)
        if (!motionEnabled) { onDismiss(); return }
        scope.launch { visible = false; delay(180); onDismiss() }
    }

    BackHandler(onBack = ::dismissWithMotion)
    LaunchedEffect(motionEnabled) {
        visible = true
        FujiHaptics.perform(context, view, FujiHapticEffect.SheetOpen)
    }

    val overlayTransition = updateTransition(targetState = visible, label = "savereport-overlay")
    val overlayAlpha by overlayTransition.animateFloat(
        transitionSpec = { tween(if (targetState) 170 else 120, easing = FastOutSlowInEasing) },
        label = "savereport-overlay-alpha",
    ) { if (it) 1f else 0f }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f * overlayAlpha))
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
                    .heightIn(max = 560.dp)
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
                                text = "SAVE REPORT",
                                fontFamily = SansFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                letterSpacing = 0.4.sp,
                                color = TextPrimary,
                            )
                            Spacer(Modifier.height(3.dp))
                            val summary = buildString {
                                if (report.saved > 0) append("${report.saved} SAVED")
                                if (report.saved > 0 && report.skipped.isNotEmpty()) append(" · ")
                                if (report.skipped.isNotEmpty()) append("${report.skipped.size} SKIPPED")
                            }
                            Text(
                                text = summary,
                                fontFamily = MonoFamily,
                                fontSize = 10.sp,
                                letterSpacing = 1.3.sp,
                                color = TextDim,
                            )
                        }
                    }

                    Spacer(Modifier.height(22.dp))
                    Text(
                        text = "SKIPPED",
                        fontFamily = MonoFamily,
                        fontSize = 10.sp,
                        letterSpacing = 2.sp,
                        color = TextDim,
                    )
                    Spacer(Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(BackupControlBg)
                            .border(1.dp, SheetBorder, RoundedCornerShape(12.dp)),
                    ) {
                        report.skipped.forEachIndexed { i, item ->
                            if (i > 0) Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SheetBorder))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = item.slot,
                                        fontFamily = MonoFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        letterSpacing = 1.sp,
                                        color = Gold,
                                    )
                                    Text(
                                        text = item.name,
                                        fontFamily = SansFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = TextPrimary,
                                    )
                                }
                                Spacer(Modifier.height(3.dp))
                                val reason = when (item.matchKind) {
                                    DuplicateMatchKind.ExactSettings -> "Same settings as"
                                    DuplicateMatchKind.SameName -> "Same name as"
                                    DuplicateMatchKind.Similar -> "Similar to"
                                }
                                Text(
                                    text = "$reason · ${item.matchedName}",
                                    fontFamily = SansFamily,
                                    fontSize = 12.sp,
                                    color = TextDim,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(22.dp))
                    PrimaryCTA(label = "Done", onClick = ::dismissWithMotion)
                }
            }
        }
    }
}

@Composable
fun SaveAllSheet(
    slots: List<RecipeUiModel>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val motionEnabled = ValueAnimator.areAnimatorsEnabled()
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(!motionEnabled) }
    val context = LocalContext.current
    val view = LocalView.current

    fun dismissWithMotion(playHaptic: Boolean = true) {
        if (playHaptic) FujiHaptics.perform(context, view, FujiHapticEffect.SheetDismiss)
        if (!motionEnabled) { onDismiss(); return }
        scope.launch { visible = false; delay(180); onDismiss() }
    }

    fun save() {
        FujiHaptics.perform(context, view, FujiHapticEffect.Confirm)
        onConfirm()
        dismissWithMotion(playHaptic = false)
    }

    BackHandler(onBack = ::dismissWithMotion)
    LaunchedEffect(motionEnabled) {
        visible = true
        FujiHaptics.perform(context, view, FujiHapticEffect.SheetOpen)
    }

    val overlayTransition = updateTransition(targetState = visible, label = "saveall-overlay")
    val overlayAlpha by overlayTransition.animateFloat(
        transitionSpec = { tween(if (targetState) 170 else 120, easing = FastOutSlowInEasing) },
        label = "saveall-overlay-alpha",
    ) { if (it) 1f else 0f }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f * overlayAlpha))
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
                    .heightIn(max = 560.dp)
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
                                text = "SAVE TO LIBRARY",
                                fontFamily = SansFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                letterSpacing = 0.4.sp,
                                color = TextPrimary,
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "ADD C1–C7 TO YOUR LIBRARY",
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
                                .clickable(onClick = ::dismissWithMotion)
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                        )
                    }

                    Spacer(Modifier.height(22.dp))
                    Text(
                        text = "SLOTS",
                        fontFamily = MonoFamily,
                        fontSize = 10.sp,
                        letterSpacing = 2.sp,
                        color = TextDim,
                    )
                    Spacer(Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(BackupControlBg)
                            .border(1.dp, SheetBorder, RoundedCornerShape(12.dp)),
                    ) {
                        slots.forEachIndexed { i, slot ->
                            if (i > 0) Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SheetBorder))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 11.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = slot.slot,
                                    fontFamily = MonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp,
                                    letterSpacing = 1.sp,
                                    color = Gold,
                                    modifier = Modifier.width(28.dp),
                                )
                                Text(
                                    text = slot.name,
                                    fontFamily = SansFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = slot.sim.trimEnd('.').uppercase(),
                                    fontFamily = MonoFamily,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.sp,
                                    color = TextMuted,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Duplicates and similar recipes already in your library will be skipped.",
                        fontFamily = SansFamily,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = TextDim,
                    )
                    Spacer(Modifier.height(22.dp))
                    PrimaryCTA(label = "Save All to Library", onClick = ::save)
                }
            }
        }
    }
}

@Composable
internal fun RestoreReadingContent(
    readingSlotIndex: Int,
    loadedSlots: List<RecipeUiModel>,
    expectedSlots: List<RecipeUiModel>? = null,
    readingSlots: Boolean,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    var wasReading by remember { mutableStateOf(readingSlots) }
    LaunchedEffect(readingSlots) {
        if (wasReading && !readingSlots) {
            FujiHaptics.perform(context, view, FujiHapticEffect.SuccessPause)
        }
        wasReading = readingSlots
    }
    val infiniteTransition = rememberInfiniteTransition(label = "restore-read")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "restore-read-pulse",
    )

    val mismatchColor = Color(0xFFE57373)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .padding(top = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = if (readingSlots) "VALIDATING" else "VALIDATED",
                fontFamily = MonoFamily,
                fontSize = 10.sp,
                letterSpacing = 2.sp,
                color = TextDim,
            )
            Text(
                text = "Reading slots from camera",
                fontFamily = SansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                letterSpacing = 0.4.sp,
                color = TextPrimary,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(BackupControlBg)
                .border(1.dp, SheetBorder, RoundedCornerShape(12.dp)),
        ) {
            repeat(7) { idx ->
                val slotLabel = "C${idx + 1}"
                val loaded = loadedSlots.getOrNull(idx)
                val expected = expectedSlots?.getOrNull(idx)
                val isActive = idx == readingSlotIndex
                val hasMismatch = loaded != null && expected != null &&
                    loaded.copy(slot = "", libraryId = null) != expected.copy(slot = "", libraryId = null)

                if (idx > 0) Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SheetBorder))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 11.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = slotLabel,
                        fontFamily = MonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp,
                        letterSpacing = 1.sp,
                        color = when {
                            hasMismatch -> mismatchColor
                            loaded != null -> Gold
                            isActive -> Gold.copy(alpha = pulse)
                            else -> TextDim.copy(alpha = 0.4f)
                        },
                        modifier = Modifier.width(28.dp),
                    )
                    when {
                        loaded != null -> {
                            Text(
                                text = loaded.name,
                                fontFamily = SansFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = if (hasMismatch) mismatchColor else TextPrimary,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = if (hasMismatch) "MISMATCH" else loaded.sim.trimEnd('.').uppercase(),
                                fontFamily = MonoFamily,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp,
                                color = if (hasMismatch) mismatchColor else TextMuted,
                            )
                        }
                        isActive -> Text(
                            text = "reading…",
                            fontFamily = MonoFamily,
                            fontSize = 10.sp,
                            letterSpacing = 0.8.sp,
                            color = Gold.copy(alpha = pulse),
                            modifier = Modifier.weight(1f),
                        )
                        else -> Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(SheetBorder.copy(alpha = 0.6f)),
                        )
                    }
                }
            }
        }

        PrimaryCTA(label = "Close", enabled = !readingSlots, onClick = onClose)
    }
}

@Composable
private fun IconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
    }
}
