package com.paeki.fujirecipes.ui.camera

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import com.paeki.fujirecipes.ui.components.FilmSimBadgeImage
import com.paeki.fujirecipes.ui.components.FilmSimLabel
import com.paeki.fujirecipes.ui.components.IconChevronRight
import com.paeki.fujirecipes.ui.components.Pill
import com.paeki.fujirecipes.ui.components.PrimaryCTA
import com.paeki.fujirecipes.ui.components.PropRow
import com.paeki.fujirecipes.ui.components.SectionLabel
import com.paeki.fujirecipes.ui.model.RecipeUiModel
import com.paeki.fujirecipes.ui.theme.Border
import com.paeki.fujirecipes.ui.theme.Gold
import com.paeki.fujirecipes.ui.theme.MonoFamily
import com.paeki.fujirecipes.ui.theme.PanelHigh
import com.paeki.fujirecipes.ui.theme.PanelLow
import com.paeki.fujirecipes.ui.theme.SansFamily
import com.paeki.fujirecipes.ui.theme.TextMuted
import com.paeki.fujirecipes.ui.theme.TextPrimary

// ── Bottom sheet content ──────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SheetContent(
    recipe: RecipeUiModel,
    sheetRevealProgress: Float,
    onOpenDetail: () -> Unit,
    onSaveToLibrary: () -> Unit,
    writeBusy: Boolean,
    librarySaveConfirmed: Boolean,
) {
    val recipeScrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
    Column(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
        Column(modifier = Modifier.padding(top = 6.dp, start = 20.dp, end = 20.dp, bottom = 0.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilmSimLabel(sim = recipe.sim, imageSize = 22.dp)
                val addToLibAlpha = (1f - sheetRevealProgress * 2f).coerceIn(0f, 1f)
                if (addToLibAlpha > 0f) {
                    Row(
                        modifier = Modifier
                            .graphicsLayer { alpha = addToLibAlpha }
                            .clickable(onClick = onSaveToLibrary)
                            .clip(RoundedCornerShape(999.dp))
                            .padding(horizontal = 6.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (librarySaveConfirmed) "SAVED" else "SAVE",
                            fontFamily = SansFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            letterSpacing = 1.8.sp,
                            color = if (librarySaveConfirmed) TextMuted else Gold,
                        )
                        if (!librarySaveConfirmed) {
                            Spacer(Modifier.width(4.dp))
                            Icon(IconChevronRight, contentDescription = null, tint = Gold, modifier = Modifier.width(11.dp).height(11.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(text = recipe.name, fontFamily = SansFamily, fontWeight = FontWeight.Bold, fontSize = 26.sp, letterSpacing = 0.1.sp, color = TextPrimary)
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                recipe.pills.forEach { Pill(text = it, large = true) }
            }
        }

        Spacer(Modifier.height(16.dp))
        Box(modifier = Modifier.weight(1f, fill = false)) {
            Column(
                modifier = Modifier
                    .verticalScroll(recipeScrollState)
                    .padding(bottom = 16.dp),
            ) {
                PropSectionBlock(label = "Effects", data = recipe.effects)
                PropSectionBlock(label = "Tone", data = recipe.tone)
                PropSectionBlock(label = "White Balance", data = recipe.wb)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, PanelLow))),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PanelLow)
                .border(1.dp, Border, RoundedCornerShape(0.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            PrimaryCTA(
                label = when {
                    writeBusy -> "Saving…"
                    librarySaveConfirmed -> "Saved to Library"
                    else -> "Save to Library"
                },
                onClick = onSaveToLibrary,
                busy = writeBusy,
            )
        }
    }

    // Gradient pinned to peek cutoff — fades out as sheet opens
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .offset(y = PEEK_HEIGHT - DRAG_HANDLE_HEIGHT - 52.dp)
            .graphicsLayer { alpha = (1f - sheetRevealProgress).coerceIn(0f, 1f) }
            .background(Brush.verticalGradient(listOf(Color.Transparent, PanelLow))),
    )
    }
}

// ── Slot chips (sheet) ────────────────────────────────────────────

@Composable
internal fun SlotRow(
    slots: List<RecipeUiModel>,
    selectedSlotIdx: Int,
    onSelectSlot: (Int) -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            slots.forEachIndexed { idx, slot ->
                SheetSlotChip(recipe = slot, active = idx == selectedSlotIdx, onClick = { onSelectSlot(idx) })
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(48.dp)
                .height(44.dp)
                .background(Brush.horizontalGradient(0f to Color.Transparent, 1f to PanelLow)),
        )
    }
}

@Composable
internal fun SlotGrid(
    slots: List<RecipeUiModel>,
    selectedSlotIdx: Int,
    onSelectSlot: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        slots.chunked(2).forEachIndexed { rowIdx, rowSlots ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowSlots.forEachIndexed { colIdx, slot ->
                    val idx = rowIdx * 2 + colIdx
                    SheetSlotChip(recipe = slot, active = idx == selectedSlotIdx, onClick = { onSelectSlot(idx) }, modifier = Modifier.weight(1f))
                }
                if (rowSlots.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SheetSlotChip(
    recipe: RecipeUiModel,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) PanelHigh else PanelLow)
            .border(1.dp, if (active) Gold else Border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(start = 11.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilmSimBadgeImage(sim = recipe.sim, size = 24.dp)
        Spacer(Modifier.width(8.dp))
        Text(text = recipe.slot, fontFamily = MonoFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.4.sp, color = Gold)
        Spacer(Modifier.width(9.dp))
        Text(text = recipe.name, fontFamily = SansFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, letterSpacing = 0.5.sp, color = if (active) TextPrimary else TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ── Prop section block ────────────────────────────────────────────
@Composable
internal fun PropSectionBlock(label: String, data: Map<String, String>) {
    if (data.isEmpty()) return
    val entries = data.entries.toList()
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
            entries.forEachIndexed { i, (k, v) ->
                PropRow(label = k, value = v, isLast = i == entries.lastIndex)
                if (i < entries.lastIndex) {
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Border))
                }
            }
        }
        Spacer(Modifier.height(14.dp))
    }
}
