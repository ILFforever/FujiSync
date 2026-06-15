package com.ilfforever.fujisync.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujisync.ui.components.DeleteConfirmDialog
import com.ilfforever.fujisync.ui.components.IconCheck
import com.ilfforever.fujisync.ui.components.PrimaryCTA
import com.ilfforever.fujisync.ui.model.RecipeUiModel
import com.ilfforever.fujisync.ui.theme.Border
import com.ilfforever.fujisync.ui.theme.Gold
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.SansFamily
import com.ilfforever.fujisync.ui.theme.TextDim
import com.ilfforever.fujisync.ui.theme.TextMuted
import com.ilfforever.fujisync.ui.theme.TextPrimary

@Composable
internal fun SyncToCameraSheet(
    connected: Boolean,
    cameraModel: String,
    cameraSlots: List<RecipeUiModel>,
    recipeName: String,
    writeBusy: Boolean,
    onDismiss: () -> Unit,
    onWriteToSlot: (String) -> Unit,
) {
    val slotNames = listOf("C1", "C2", "C3", "C4", "C5", "C6", "C7")
    val slotMap = cameraSlots.associateBy { it.slot }
    var selectedSlot by remember { mutableStateOf("C1") }
    var sheetVisible by remember { mutableStateOf(false) }
    var confirmingWrite by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { sheetVisible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.76f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = sheetVisible,
            enter = slideInVertically(
                animationSpec = tween(300, easing = FastOutSlowInEasing),
                initialOffsetY = { it / 3 },
            ) + fadeIn(tween(200)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color(0xFF111009))
                    .border(1.dp, Border, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
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
                        .background(TextDim.copy(alpha = 0.4f)),
                )
                Spacer(Modifier.height(18.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "SYNC TO CAMERA",
                            fontFamily = MonoFamily,
                            fontSize = 10.sp,
                            letterSpacing = 1.6.sp,
                            color = TextMuted,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (connected && cameraModel.isNotBlank()) cameraModel.uppercase() else "NO CAMERA",
                            fontFamily = SansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            letterSpacing = 0.4.sp,
                            color = TextPrimary,
                        )
                    }
                    if (connected) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Gold.copy(alpha = 0.12f))
                                .border(1.dp, Gold.copy(alpha = 0.4f), RoundedCornerShape(999.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Gold),
                            )
                            Text(
                                text = "CONNECTED",
                                fontFamily = MonoFamily,
                                fontSize = 9.sp,
                                letterSpacing = 1.2.sp,
                                color = Gold,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                if (!connected) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "Connect your Fujifilm camera via USB-C to sync this recipe.",
                            fontFamily = SansFamily,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = TextDim,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = "SET CAMERA TO USB RAW CONV. / BACKUP MODE",
                            fontFamily = MonoFamily,
                            fontSize = 9.5.sp,
                            letterSpacing = 1.2.sp,
                            color = TextMuted,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Border),
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        slotNames.forEach { slot ->
                            val currentRecipe = slotMap[slot]
                            val isSelected = slot == selectedSlot
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isSelected) Gold.copy(alpha = 0.08f) else Color.Transparent)
                                    .clickable { selectedSlot = slot }
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                ) {
                                    Text(
                                        text = slot,
                                        fontFamily = MonoFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        letterSpacing = 1.2.sp,
                                        color = if (isSelected) Gold else TextMuted,
                                        modifier = Modifier.width(26.dp),
                                    )
                                    Column {
                                        Text(
                                            text = currentRecipe?.name?.ifBlank { "—" } ?: "—",
                                            fontFamily = SansFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = if (isSelected) TextPrimary else TextPrimary.copy(alpha = 0.6f),
                                        )
                                        if (currentRecipe?.sim?.isNotBlank() == true) {
                                            Text(
                                                text = currentRecipe.sim.uppercase(),
                                                fontFamily = MonoFamily,
                                                fontSize = 9.5.sp,
                                                letterSpacing = 1.2.sp,
                                                color = TextDim,
                                            )
                                        }
                                    }
                                }
                                if (isSelected) {
                                    Icon(
                                        IconCheck,
                                        contentDescription = null,
                                        tint = Gold,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Border.copy(alpha = 0.5f)),
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    ) {
                        PrimaryCTA(
                            label = if (writeBusy) "Writing…" else "Write to $selectedSlot",
                            onClick = { confirmingWrite = true },
                            busy = writeBusy,
                            enabled = !writeBusy,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }

    if (confirmingWrite) {
        DeleteConfirmDialog(
            eyebrow = "WRITE TO CAMERA",
            title = selectedSlot,
            body = "\"$recipeName\" will overwrite the current recipe in $selectedSlot.",
            confirmLabel = "Write to $selectedSlot",
            confirmColor = Gold,
            onConfirm = { onWriteToSlot(selectedSlot) },
            onDismiss = { confirmingWrite = false },
        )
    }
}
