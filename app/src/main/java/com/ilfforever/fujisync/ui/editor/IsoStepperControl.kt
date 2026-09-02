package com.ilfforever.fujisync.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujisync.ui.theme.Border
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.TextPrimary

internal val cameraIsoValues = listOf(
    50, 64, 80, 100, 125, 160, 200, 250, 320, 400, 500, 640,
    800, 1000, 1250, 1600, 2000, 2500, 3200, 4000, 5000, 6400,
    8000, 10000, 12800, 16000, 20000, 25600, 32000, 40000, 51200,
)

internal fun previousIso(value: Int?): Int? = when (value) {
    null -> null
    else -> cameraIsoValues.lastOrNull { it < value }
}

internal fun nextIso(value: Int?): Int? = when (value) {
    null -> cameraIsoValues.first()
    else -> cameraIsoValues.firstOrNull { it > value }
}

@Composable
internal fun IsoStepperControl(
    value: Int?,
    modifier: Modifier = Modifier,
    onValueChange: (Int?) -> Unit,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .border(1.dp, Border, RoundedCornerShape(9.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val previous = previousIso(value)
        val next = nextIso(value)
        StepButton("−", enabled = value != null) { onValueChange(previous) }
        Box(
            modifier = Modifier
                .width(68.dp)
                .height(42.dp)
                .background(androidx.compose.ui.graphics.Color(0xFF0A0908)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = value?.toString() ?: "—",
                fontFamily = MonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextPrimary,
            )
        }
        StepButton("+", enabled = next != null) { onValueChange(next) }
    }
}
