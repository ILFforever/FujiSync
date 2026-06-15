package com.ilfforever.fujisync.ui.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujisync.ui.components.IconClose
import com.ilfforever.fujisync.ui.theme.Bg
import com.ilfforever.fujisync.ui.theme.Border
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.PanelHigh
import com.ilfforever.fujisync.ui.theme.PanelLow
import com.ilfforever.fujisync.ui.theme.SansFamily
import com.ilfforever.fujisync.ui.theme.TextDim
import com.ilfforever.fujisync.ui.theme.TextMuted
import com.ilfforever.fujisync.ui.theme.TextPrimary

private const val MAX_PROPERTY_WRITE_DELAY_MS = 300L
private val DevWarningRed = Color(0xFFFF6B5F)
private val DevWarningBg = Color(0xFF241312)

@Composable
fun DevToolsScreen(
    onBack: () -> Unit,
    onOpenCameraImageTuner: () -> Unit,
    onLoadSampleLibrary: () -> Unit,
    onExploreDemo: () -> Unit,
    onOpenExifBench: () -> Unit,
    onOpenFxwSearchBench: () -> Unit,
    onOpenWriteDelayBench: () -> Unit,
    onOpenNameBench: () -> Unit,
    onOpenReadSlotsBench: () -> Unit,
    onOpenDrPriorityBench: () -> Unit,
    onOpenUsbReadWriteBench: () -> Unit,
    onOpenHapticBench: () -> Unit,
    onOpenPtpLog: () -> Unit,
    onAddMockCamera: () -> Unit,
    onShowScanLog: () -> Unit,
    propertyWriteDelayMs: Long = 0L,
    onSetPropertyWriteDelay: (Long) -> Unit = {},
) {
    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onBack)
                    .padding(horizontal = 4.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(IconClose, contentDescription = "Back", tint = TextPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "DEVELOPER TOOLS",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    letterSpacing = 1.6.sp,
                    color = TextPrimary,
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            DeveloperWarningCard()
            Spacer(Modifier.height(18.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(PanelLow)
                    .border(1.dp, Border, RoundedCornerShape(14.dp)),
            ) {
                ProfileNavRow(label = "Camera image tuner", onClick = onOpenCameraImageTuner, inCard = true)
                ProfileDivider()
                ProfileNavRow(label = "Load sample library", onClick = onLoadSampleLibrary, inCard = true)
                ProfileDivider()
                ProfileNavRow(label = "Explore without camera", onClick = onExploreDemo, inCard = true)
                ProfileDivider()
                ProfileNavRow(label = "Add mock camera", onClick = onAddMockCamera, inCard = true)
                ProfileDivider()
                ProfileNavRow(label = "Scan diagnostic log", onClick = onShowScanLog, inCard = true)
            }

            Spacer(Modifier.height(24.dp))
            Text(
                text = "BENCH",
                fontFamily = SansFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                letterSpacing = 1.6.sp,
                color = TextDim,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(PanelLow)
                    .border(1.dp, Border, RoundedCornerShape(14.dp)),
            ) {
                ProfileNavRow(label = "USB read/write bench", onClick = onOpenUsbReadWriteBench, inCard = true)
                ProfileDivider()
                ProfileNavRow(label = "Name bench", onClick = onOpenNameBench, inCard = true)
                ProfileDivider()
                ProfileNavRow(label = "Read slots bench", onClick = onOpenReadSlotsBench, inCard = true)
                ProfileDivider()
                ProfileNavRow(label = "DR Priority bench", onClick = onOpenDrPriorityBench, inCard = true)
                ProfileDivider()
                ProfileNavRow(label = "Write delay bench", onClick = onOpenWriteDelayBench, inCard = true)
                ProfileDivider()
                ProfileNavRow(label = "Haptic bench", onClick = onOpenHapticBench, inCard = true)
                ProfileDivider()
                ProfileNavRow(label = "EXIF bench", onClick = onOpenExifBench, inCard = true)
                ProfileDivider()
                ProfileNavRow(label = "FXW search bench", onClick = onOpenFxwSearchBench, inCard = true)
                ProfileDivider()
                ProfileNavRow(label = "PTP log", onClick = onOpenPtpLog, inCard = true)
            }

            Spacer(Modifier.height(24.dp))
            Text(
                text = "PROPERTY WRITE DELAY",
                fontFamily = SansFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                letterSpacing = 1.6.sp,
                color = TextDim,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(PanelLow)
                    .border(1.dp, Border, RoundedCornerShape(14.dp))
                    .padding(14.dp),
            ) {
                Text(
                    text = "Inter-property delay injected between each SET_DEVICE_PROP_VALUE call during a write. Default is 0ms — confirmed safe at 0ms on X-H2 fw 5.20. Increase only if a camera consistently rejects writes.",
                    fontFamily = SansFamily,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = TextMuted,
                )
                Spacer(Modifier.height(12.dp))
                PropertyWriteDelayInput(
                    propertyWriteDelayMs = propertyWriteDelayMs,
                    onSetPropertyWriteDelay = onSetPropertyWriteDelay,
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DeveloperWarningCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DevWarningBg)
            .border(1.dp, DevWarningRed.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        Text(
            text = "DEVELOPER ONLY",
            fontFamily = SansFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            letterSpacing = 1.4.sp,
            color = DevWarningRed,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "These tools can write directly to camera slots, change preset data, send raw PTP commands, and overwrite values without the normal app safeguards. Use only for active development with backed-up recipes and a camera you are prepared to reset.",
            fontFamily = SansFamily,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = TextPrimary,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Incorrect use may cause recipe loss, invalid camera settings, failed writes, or confusing readback results.",
            fontFamily = MonoFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.5.sp,
            lineHeight = 16.sp,
            color = DevWarningRed,
        )
    }
}

@Composable
private fun PropertyWriteDelayInput(
    propertyWriteDelayMs: Long,
    onSetPropertyWriteDelay: (Long) -> Unit,
) {
    var text by remember(propertyWriteDelayMs) { mutableStateOf(propertyWriteDelayMs.toString()) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(PanelHigh)
                .border(1.dp, Border, RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = text,
                onValueChange = { raw ->
                    val digits = raw.filter { it.isDigit() }.take(3)
                    text = digits
                    val value = digits.toLongOrNull() ?: return@BasicTextField
                    onSetPropertyWriteDelay(value.coerceIn(0L, MAX_PROPERTY_WRITE_DELAY_MS))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(
                    fontFamily = MonoFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp,
                    color = TextPrimary,
                ),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    Box {
                        if (text.isEmpty()) {
                            Text(
                                text = "0",
                                fontFamily = MonoFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 22.sp,
                                color = TextDim,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "ms",
                fontFamily = SansFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                letterSpacing = 1.2.sp,
                color = TextMuted,
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "Allowed range: 0-${MAX_PROPERTY_WRITE_DELAY_MS}ms",
            fontFamily = SansFamily,
            fontSize = 10.sp,
            color = TextDim,
        )
    }
}
