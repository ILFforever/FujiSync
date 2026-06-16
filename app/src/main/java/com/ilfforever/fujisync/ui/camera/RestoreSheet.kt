package com.ilfforever.fujisync.ui.camera

import android.animation.ValueAnimator
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujisync.ui.haptics.FujiHapticEffect
import com.ilfforever.fujisync.ui.haptics.FujiHaptics
import com.ilfforever.fujisync.ui.components.IconChevronRight
import com.ilfforever.fujisync.ui.components.IconEdit
import com.ilfforever.fujisync.ui.components.IconTrash
import com.ilfforever.fujisync.ui.components.PrimaryCTA
import com.ilfforever.fujisync.ui.model.RecipeUiModel
import com.ilfforever.fujisync.ui.model.SlotBackupMeta
import com.ilfforever.fujisync.ui.model.SlotBackupSet
import com.ilfforever.fujisync.ui.theme.Gold
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.SansFamily
import com.ilfforever.fujisync.ui.theme.SheetBg
import com.ilfforever.fujisync.ui.theme.SheetBorder
import com.ilfforever.fujisync.ui.theme.TextDim
import com.ilfforever.fujisync.ui.theme.TextMuted
import com.ilfforever.fujisync.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val BackupControlBg = Color(0xFF141210)

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
        FujiHaptics.perform(context, view, FujiHapticEffect.Confirm)
        phase = RestorePhase.Working
        onConfirm()
        // sheet stays open — loading overlay takes over, then reading animation
    }
    fun confirmDelete() {
        FujiHaptics.perform(context, view, FujiHapticEffect.Confirm)
        onDelete()
        dismissWithMotion()
    }

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
                                                if (selected) {
                                                    slotsExpanded = !slotsExpanded
                                                    FujiHaptics.perform(context, view, FujiHapticEffect.SoftSelection)
                                                } else {
                                                    onSelectBackup(set.meta.id)
                                                    confirmingDelete = false
                                                    renaming = false
                                                    slotsExpanded = false
                                                    FujiHaptics.perform(context, view, FujiHapticEffect.Selection)
                                                }
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
                                                IconButton(IconEdit, tint = TextMuted, onClick = {
                                                    FujiHaptics.perform(context, view, FujiHapticEffect.SoftSelection)
                                                    renaming = true
                                                    confirmingDelete = false
                                                })
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .clickable {
                                                            slotsExpanded = !slotsExpanded
                                                            FujiHaptics.perform(context, view, FujiHapticEffect.SoftSelection)
                                                        },
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
                                                IconButton(IconTrash, tint = Color(0xFFD94040), onClick = {
                                                    FujiHaptics.perform(context, view, FujiHapticEffect.Selection)
                                                    confirmingDelete = true
                                                })
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
                                                        .clickable {
                                                            FujiHaptics.perform(context, view, FujiHapticEffect.SoftSelection)
                                                            confirmingDelete = false
                                                        }
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
