package com.paeki.fujirecipes.ui.camera

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paeki.fujirecipes.R
import com.paeki.fujirecipes.ui.components.IconClose
import com.paeki.fujirecipes.ui.theme.Bg
import com.paeki.fujirecipes.ui.theme.Border
import com.paeki.fujirecipes.ui.theme.Gold
import com.paeki.fujirecipes.ui.theme.MonoFamily
import com.paeki.fujirecipes.ui.theme.PanelLow
import com.paeki.fujirecipes.ui.theme.SansFamily
import com.paeki.fujirecipes.ui.theme.TextDim
import com.paeki.fujirecipes.ui.theme.TextMuted
import com.paeki.fujirecipes.ui.theme.TextPrimary

private data class TunerEntry(
    val model: String,
    val firmware: String,
    val drawableRes: Int,
    val default: CameraImageTuning,
)

private val TUNER_ENTRIES = listOf(
    TunerEntry("X-H2",   "7.10", R.drawable.camera_xh2,    CameraImageTuning(317.dp, 229.dp, 47.dp,  3.dp)),
    TunerEntry("X-T5",   "2.20", R.drawable.camera_xt5,    CameraImageTuning(414.dp, 394.dp, 91.dp,  (-77).dp)),
    TunerEntry("X-S20",  "3.10", R.drawable.camera_xs20,   CameraImageTuning(423.dp, 410.dp, 159.dp, (-87).dp)),
    TunerEntry("X100VI", "2.01", R.drawable.camera_x100vi, CameraImageTuning(420.dp, 325.dp, 115.dp, (-61).dp)),
    TunerEntry("X-T50",  "1.20", R.drawable.camera_xt50,   CameraImageTuning(460.dp, 387.dp, 145.dp, (-120).dp)),
    TunerEntry("X-M5",   "1.10", R.drawable.camera_xm5,    CameraImageTuning(360.dp, 360.dp, 111.dp, (-56).dp)),
    TunerEntry("X-E5",   "1.00", R.drawable.camera_xe5,    CameraImageTuning(430.dp, 273.dp, 157.dp, (-1).dp)),
    TunerEntry("X-T30 III", "1.00", R.drawable.camera_xt30iii, CameraImageTuning(460.dp, 430.dp, 160.dp, (-150).dp)),
    TunerEntry("X-Pro3", "4.10", R.drawable.camera_xpro3,  CameraImageTuning(430.dp, 229.dp, 140.dp, 14.dp)),
)

@Composable
fun CameraImageTunerScreen(onClose: () -> Unit) {
    val tunings = remember {
        mutableStateMapOf<String, CameraImageTuning>().also { map ->
            TUNER_ENTRIES.forEach { map[it.model] = it.default }
        }
    }
    val pagerState = rememberPagerState(pageCount = { TUNER_ENTRIES.size })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding(),
    ) {
        // ── Header ────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "IMAGE TUNER",
                    fontFamily = MonoFamily,
                    fontSize = 10.sp,
                    letterSpacing = 2.4.sp,
                    color = Gold,
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = TUNER_ENTRIES[pagerState.currentPage].model,
                        fontFamily = SansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        color = TextPrimary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        TUNER_ENTRIES.forEachIndexed { idx, _ ->
                            Box(
                                modifier = Modifier
                                    .size(if (idx == pagerState.currentPage) 6.dp else 4.dp)
                                    .clip(CircleShape)
                                    .background(if (idx == pagerState.currentPage) Gold else TextDim),
                            )
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PanelLow)
                    .border(1.dp, Border, RoundedCornerShape(12.dp))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(IconClose, contentDescription = "Close", tint = TextMuted, modifier = Modifier.size(18.dp))
            }
        }

        // ── Pager (card preview + sliders + code) ────────────────────
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val entry = TUNER_ENTRIES[page]
            val tuning = tunings[entry.model] ?: entry.default
            TunerPage(
                entry = entry,
                tuning = tuning,
                onChange = { tunings[entry.model] = it },
            )
        }
    }
}

@Composable
private fun TunerPage(
    entry: TunerEntry,
    tuning: CameraImageTuning,
    onChange: (CameraImageTuning) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // ── Card preview ──────────────────────────────────────────────
        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .height(166.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(PanelLow)
                .border(1.dp, Border, RoundedCornerShape(18.dp)),
        ) {
            Image(
                painter = painterResource(entry.drawableRes),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .wrapContentSize(unbounded = true, align = Alignment.TopEnd)
                    .requiredWidth(tuning.width)
                    .requiredHeight(tuning.height)
                    .offset(x = tuning.offsetX, y = tuning.offsetY)
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.horizontalGradient(
                                0f to Color.Transparent,
                                0.35f to Color.Black,
                                1f to Color.Black,
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                    },
            )
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 15.dp)) {
                Text(
                    text = "MY CAMERA",
                    fontFamily = MonoFamily,
                    fontSize = 9.5.sp,
                    letterSpacing = 1.8.sp,
                    color = TextDim,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = entry.model,
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 44.sp,
                    letterSpacing = (-0.5).sp,
                    color = TextPrimary,
                    lineHeight = 44.sp,
                )
                Spacer(Modifier.height(15.dp))
                TunerMetaRow("Firmware", entry.firmware)
                Spacer(Modifier.height(8.dp))
                TunerMetaRow("Battery", "87%")
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Sliders ───────────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            TunerSlider("Width",    tuning.width.value,   180f..460f)  { onChange(tuning.copy(width   = it.dp)) }
            TunerSlider("Height",   tuning.height.value,  180f..430f)  { onChange(tuning.copy(height  = it.dp)) }
            TunerSlider("Offset X", tuning.offsetX.value, -80f..180f)  { onChange(tuning.copy(offsetX = it.dp)) }
            TunerSlider("Offset Y", tuning.offsetY.value, -120f..120f) { onChange(tuning.copy(offsetY = it.dp)) }
        }

        Spacer(Modifier.height(24.dp))

        // ── Code output ───────────────────────────────────────────────
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0A0A0A))
                .border(1.dp, Border, RoundedCornerShape(14.dp))
                .padding(16.dp),
        ) {
            Text(
                text = "COPY INTO cameraImageTuning()",
                fontFamily = MonoFamily,
                fontSize = 9.sp,
                letterSpacing = 1.6.sp,
                color = TextDim,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = buildCodeSnippet(entry.model, tuning),
                fontFamily = MonoFamily,
                fontSize = 12.sp,
                lineHeight = 20.sp,
                color = Gold,
            )
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun TunerSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, fontFamily = SansFamily, fontSize = 13.sp, color = TextMuted)
            Text("${value.toInt()} dp", fontFamily = MonoFamily, fontSize = 12.sp, color = TextPrimary)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Gold,
                activeTrackColor = Gold,
                inactiveTrackColor = Border,
            ),
        )
    }
}

@Composable
private fun TunerMetaRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontFamily = MonoFamily, fontSize = 10.sp, color = TextDim)
        Text(value, fontFamily = MonoFamily, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, color = TextMuted)
    }
}

private fun buildCodeSnippet(model: String, t: CameraImageTuning): String {
    val oy = t.offsetY.value.toInt()
    val oyStr = if (oy < 0) "(${oy}).dp" else "${oy}.dp"
    return "model.contains(\"$model\", ignoreCase = true) ->\n" +
           "    CameraImageTuning(\n" +
           "        width   = ${t.width.value.toInt()}.dp,\n" +
           "        height  = ${t.height.value.toInt()}.dp,\n" +
           "        offsetX = ${t.offsetX.value.toInt()}.dp,\n" +
           "        offsetY = $oyStr,\n" +
           "    )"
}
