package com.paeki.fujirecipes.ui.camera

import android.animation.ValueAnimator
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paeki.fujirecipes.ui.components.IconChevronRight
import com.paeki.fujirecipes.ui.components.IconEdit
import com.paeki.fujirecipes.ui.components.IconTrash
import com.paeki.fujirecipes.ui.components.PrimaryCTA
import com.paeki.fujirecipes.ui.model.DuplicateMatchKind
import com.paeki.fujirecipes.ui.model.RecipeUiModel
import com.paeki.fujirecipes.ui.model.SaveAllReport
import com.paeki.fujirecipes.ui.model.SlotBackupMeta
import com.paeki.fujirecipes.ui.theme.Border
import com.paeki.fujirecipes.ui.theme.Gold
import com.paeki.fujirecipes.ui.theme.GoldDim
import com.paeki.fujirecipes.ui.theme.MonoFamily
import com.paeki.fujirecipes.ui.theme.PanelHigh
import com.paeki.fujirecipes.ui.theme.SansFamily
import com.paeki.fujirecipes.ui.theme.TextDim
import com.paeki.fujirecipes.ui.theme.SheetBg
import com.paeki.fujirecipes.ui.theme.SheetBorder
import com.paeki.fujirecipes.ui.theme.TextMuted
import com.paeki.fujirecipes.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val BackupControlBg = Color(0xFF141210)

@Composable
fun BackupSheet(
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

    fun dismissWithMotion() {
        if (!motionEnabled) { onDismiss(); return }
        scope.launch { visible = false; delay(180); onDismiss() }
    }

    BackHandler(onBack = ::dismissWithMotion)

    fun save() {
        val trimmed = label.trim().ifBlank { defaultLabel }
        onConfirm(trimmed)
        dismissWithMotion()
    }

    LaunchedEffect(motionEnabled) { visible = true }

    val overlayTransition = updateTransition(targetState = visible, label = "backup-overlay")
    val overlayAlpha by overlayTransition.animateFloat(
        transitionSpec = { tween(if (targetState) 170 else 120, easing = FastOutSlowInEasing) },
        label = "backup-overlay-alpha",
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
                    .heightIn(max = 500.dp)
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
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Saves all 7 custom slots to this device.",
                        fontFamily = SansFamily,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = TextDim,
                    )
                    Spacer(Modifier.height(22.dp))
                    PrimaryCTA(label = "Save Set", onClick = ::save)
                }
            }
        }
    }
}

@Composable
fun RestoreSheet(
    meta: SlotBackupMeta?,
    backupSlots: List<RecipeUiModel>? = null,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onDelete: () -> Unit = {},
    onRename: (String) -> Unit = {},
) {
    val motionEnabled = ValueAnimator.areAnimatorsEnabled()
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(!motionEnabled) }
    var renaming by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }
    var slotsExpanded by remember { mutableStateOf(false) }
    val chevronAngle by animateFloatAsState(if (slotsExpanded) 90f else 0f, label = "chevron")

    fun dismissWithMotion() {
        if (!motionEnabled) { onDismiss(); return }
        scope.launch { visible = false; delay(180); onDismiss() }
    }

    fun restore() { onConfirm(); dismissWithMotion() }
    fun confirmDelete() { onDelete(); dismissWithMotion() }

    BackHandler(onBack = ::dismissWithMotion)
    LaunchedEffect(motionEnabled) { visible = true }

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

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(BackupControlBg)
                            .border(
                                1.dp,
                                if (meta != null && !confirmingDelete) Gold else SheetBorder,
                                RoundedCornerShape(12.dp),
                            ),
                    ) {
                        if (meta != null) {
                            // ── Main row ──────────────────────────────────────
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 10.dp, top = 14.dp, bottom = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = meta.label,
                                        fontFamily = SansFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = TextPrimary,
                                    )
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        text = meta.savedAt.uppercase(),
                                        fontFamily = MonoFamily,
                                        fontSize = 10.sp,
                                        letterSpacing = 1.4.sp,
                                        color = TextDim,
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    IconButton(IconEdit, tint = TextMuted, onClick = { renaming = true; confirmingDelete = false })
                                    if (backupSlots != null) {
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
                                    }
                                    IconButton(IconTrash, tint = Color(0xFFD94040), onClick = { confirmingDelete = true })
                                }
                            }

                            // ── Slot preview (expandable) ─────────────────────
                            if (slotsExpanded && backupSlots != null) {
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SheetBorder))
                                backupSlots.forEach { slot ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
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

                            // ── Delete confirmation ───────────────────────────
                            if (confirmingDelete) {
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
                        } else {
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

    fun dismissWithMotion() {
        if (!motionEnabled) { onDismiss(); return }
        scope.launch { visible = false; delay(180); onDismiss() }
    }

    fun save() { onConfirm(label.trim()); dismissWithMotion() }

    BackHandler(onBack = ::dismissWithMotion)
    LaunchedEffect(motionEnabled) { visible = true }

    val overlayTransition = updateTransition(targetState = visible, label = "rename-overlay")
    val overlayAlpha by overlayTransition.animateFloat(
        transitionSpec = { tween(if (targetState) 170 else 120, easing = FastOutSlowInEasing) },
        label = "rename-overlay-alpha",
    ) { if (it) 1f else 0f }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f * overlayAlpha))
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

    fun dismissWithMotion() {
        if (!motionEnabled) { onDismiss(); return }
        scope.launch { visible = false; delay(180); onDismiss() }
    }

    BackHandler(onBack = ::dismissWithMotion)
    LaunchedEffect(motionEnabled) { visible = true }

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

    fun dismissWithMotion() {
        if (!motionEnabled) { onDismiss(); return }
        scope.launch { visible = false; delay(180); onDismiss() }
    }

    fun save() { onConfirm(); dismissWithMotion() }

    BackHandler(onBack = ::dismissWithMotion)
    LaunchedEffect(motionEnabled) { visible = true }

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
