package com.paeki.fujirecipes.ui.detail

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.paeki.fujirecipes.ui.components.IconClose
import com.paeki.fujirecipes.ui.components.IconEdit
import com.paeki.fujirecipes.ui.components.IconMore
import com.paeki.fujirecipes.ui.components.IconStar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paeki.fujirecipes.ui.components.Pill
import com.paeki.fujirecipes.ui.components.PrimaryCTA
import com.paeki.fujirecipes.ui.components.PropRow
import com.paeki.fujirecipes.ui.components.SectionLabel
import com.paeki.fujirecipes.ui.model.RecipeUiModel
import com.paeki.fujirecipes.ui.theme.Bg
import com.paeki.fujirecipes.ui.theme.Border
import com.paeki.fujirecipes.ui.theme.Gold
import com.paeki.fujirecipes.ui.theme.MonoFamily
import com.paeki.fujirecipes.ui.theme.PanelHigh
import com.paeki.fujirecipes.ui.theme.PanelLow
import com.paeki.fujirecipes.ui.theme.SansFamily
import com.paeki.fujirecipes.ui.theme.TextMuted
import com.paeki.fujirecipes.ui.theme.TextPrimary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipeDetailScreen(
    recipe: RecipeUiModel?,
    connected: Boolean,
    onClose: () -> Unit,
    onWrite: () -> Unit,
    writeBusy: Boolean,
) {
    AnimatedVisibility(
        visible = recipe != null,
        enter = slideInVertically(initialOffsetY = { it / 3 }, animationSpec = tween(300)) + fadeIn(tween(200)),
        exit = slideOutVertically(targetOffsetY = { it / 3 }, animationSpec = tween(250)) + fadeOut(tween(150)),
    ) {
        recipe ?: return@AnimatedVisibility

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Bg),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top nav bar
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            modifier = Modifier
                                .clickable(onClick = {})
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(IconEdit, contentDescription = "Edit", tint = Gold, modifier = Modifier.size(16.dp))
                            Text(
                                text = "EDIT",
                                fontFamily = SansFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                letterSpacing = 1.8.sp,
                                color = Gold,
                            )
                        }
                        IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                            Icon(IconStar, contentDescription = "Save", tint = TextMuted, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                            Icon(IconMore, contentDescription = "More", tint = TextMuted, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    // Hero card
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 14.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(PanelLow)
                            .border(1.dp, Border, RoundedCornerShape(16.dp))
                            .padding(horizontal = 22.dp, vertical = 20.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .border(1.dp, Gold, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = recipe.sim.uppercase(),
                                fontFamily = MonoFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 9.5.sp,
                                letterSpacing = 1.4.sp,
                                color = Gold,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = recipe.name,
                            fontFamily = SansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 38.sp,
                            letterSpacing = 0.1.sp,
                            color = TextPrimary,
                            lineHeight = 38.sp,
                        )
                        Spacer(Modifier.height(14.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            recipe.pills.take(3).forEach { Pill(text = it, large = true) }
                        }
                        if (recipe.description.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = recipe.description,
                                fontFamily = SansFamily,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                color = TextMuted,
                            )
                        }
                    }

                    // Property sections
                    Spacer(Modifier.height(4.dp))
                    Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                        PropSectionDetail("Effects", recipe.effects)
                        PropSectionDetail("Tone", recipe.tone)
                        PropSectionDetail("White Balance", recipe.wb)
                    }

                    // Slot selector (only for camera slots)
                    if (recipe.slot.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                            SectionLabel(
                                text = "Install to slot",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                listOf("C1", "C2", "C3", "C4", "C5", "C6", "C7").forEach { slot ->
                                    val isCurrentSlot = slot == recipe.slot
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isCurrentSlot) Gold else PanelHigh)
                                            .border(1.dp, if (isCurrentSlot) Gold else Border, RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = slot,
                                            fontFamily = MonoFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            letterSpacing = 0.4.sp,
                                            color = if (isCurrentSlot) Bg else TextMuted,
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }

                    Spacer(Modifier.height(100.dp))
                }

                // Sticky CTA
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Bg)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Border),
                    )
                    Spacer(Modifier.height(12.dp))
                    val ctaLabel = when {
                        writeBusy -> "Writing…"
                        connected -> "Write to ${recipe.slot.ifEmpty { "C1" }}"
                        else -> "Save to Library"
                    }
                    PrimaryCTA(
                        label = ctaLabel,
                        onClick = onWrite,
                        busy = writeBusy,
                        enabled = connected || recipe.slot.isEmpty(),
                    )
                }
            }
        }
    }
}

@Composable
private fun PropSectionDetail(label: String, data: Map<String, String>) {
    if (data.isEmpty()) return
    val entries = data.entries.toList()
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Border),
                )
            }
        }
    }
    Spacer(Modifier.height(16.dp))
}
