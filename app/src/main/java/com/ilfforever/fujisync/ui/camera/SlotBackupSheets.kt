package com.ilfforever.fujisync.ui.camera

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujisync.ui.haptics.FujiHapticEffect
import com.ilfforever.fujisync.ui.haptics.FujiHaptics
import com.ilfforever.fujisync.ui.components.PrimaryCTA
import com.ilfforever.fujisync.ui.model.RecipeUiModel
import com.ilfforever.fujisync.ui.theme.Gold
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.SansFamily
import com.ilfforever.fujisync.ui.theme.SheetBorder
import com.ilfforever.fujisync.ui.theme.TextDim
import com.ilfforever.fujisync.ui.theme.TextMuted
import com.ilfforever.fujisync.ui.theme.TextPrimary

private val BackupControlBg = Color(0xFF141210)

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
