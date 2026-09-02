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
import com.ilfforever.fujisync.ui.model.formatExposureComp
import com.ilfforever.fujisync.ui.theme.Border
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.TextPrimary
import kotlin.math.roundToInt

@Composable
internal fun ThirdStepControl(
    value: Float,
    min: Float,
    max: Float,
    modifier: Modifier = Modifier,
    onValueChange: (Float) -> Unit,
) {
    val valueSteps = (value * 3f).roundToInt()
    val minSteps = (min * 3f).roundToInt()
    val maxSteps = (max * 3f).roundToInt()

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .border(1.dp, Border, RoundedCornerShape(9.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton("−", enabled = valueSteps > minSteps) {
            onValueChange((valueSteps - 1).coerceAtLeast(minSteps) / 3f)
        }
        Box(
            modifier = Modifier
                .width(68.dp)
                .height(42.dp)
                .background(androidx.compose.ui.graphics.Color(0xFF0A0908)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = formatExposureComp(value),
                fontFamily = MonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextPrimary,
            )
        }
        StepButton("+", enabled = valueSteps < maxSteps) {
            onValueChange((valueSteps + 1).coerceAtMost(maxSteps) / 3f)
        }
    }
}
