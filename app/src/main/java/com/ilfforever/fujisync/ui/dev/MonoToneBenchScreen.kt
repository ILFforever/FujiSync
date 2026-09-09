package com.ilfforever.fujisync.ui.dev

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujisync.data.usb.MonoToneHold
import com.ilfforever.fujisync.data.usb.MonoToneProbe
import com.ilfforever.fujisync.data.usb.MonoToneReading
import com.ilfforever.fujisync.data.usb.MonoToneSweep
import com.ilfforever.fujisync.domain.model.CameraSlot
import com.ilfforever.fujisync.ui.components.PrimaryCTA
import com.ilfforever.fujisync.ui.components.SectionLabel
import com.ilfforever.fujisync.ui.theme.Bg
import com.ilfforever.fujisync.ui.theme.Border
import com.ilfforever.fujisync.ui.theme.Gold
import com.ilfforever.fujisync.ui.theme.GoldDim
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.PanelHigh
import com.ilfforever.fujisync.ui.theme.PanelLow
import com.ilfforever.fujisync.ui.theme.SansFamily
import com.ilfforever.fujisync.ui.theme.TextDim
import com.ilfforever.fujisync.ui.theme.TextMuted
import com.ilfforever.fujisync.ui.theme.TextPrimary

/**
 * Writes `0xD193` and `0xD194` on a chosen slot to find out what they actually are.
 *
 * The read-back only proves the camera stored a number. Which axis moved is read off the camera's
 * own Monochromatic Color screen, which is why the instructions on this page matter as much as the
 * table it prints.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MonoToneBenchScreen(
    viewModel: MonoToneBenchViewModel,
    onClose: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val live by viewModel.live.collectAsState()
    val hold by viewModel.hold.collectAsState()
    val reading by viewModel.reading.collectAsState()
    var slot by remember { mutableStateOf(CameraSlot.entries.last()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 40.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "MONO TONE BENCH",
                    fontFamily = MonoFamily,
                    fontSize = 10.sp,
                    letterSpacing = 1.6.sp,
                    color = TextMuted,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "0xD193 · 0xD194",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    color = TextPrimary,
                )
            }
            Text(
                text = "CLOSE",
                fontFamily = MonoFamily,
                fontSize = 10.sp,
                letterSpacing = 1.6.sp,
                color = Gold,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onClose)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }

        Text(
            text = "Writes a spread of values to both monochrome toning properties and reads each " +
                "one back. The slot is moved to Acros first, because both are refused under a " +
                "colour film simulation. The original simulation and both tone values are put back " +
                "afterwards.",
            fontFamily = SansFamily,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            color = TextMuted,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "This writes to the camera. Pick a slot you do not mind disturbing.",
            fontFamily = SansFamily,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            color = Gold,
        )

        Spacer(Modifier.height(18.dp))
        SectionLabel(text = "Slot to use")
        Spacer(Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CameraSlot.entries.forEach { candidate ->
                val active = candidate == slot
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) GoldDim else PanelHigh)
                        .border(1.dp, if (active) Gold else Border, RoundedCornerShape(8.dp))
                        .clickable { slot = candidate }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = candidate.label,
                        fontFamily = MonoFamily,
                        fontSize = 12.sp,
                        color = if (active) Gold else TextMuted,
                    )
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        ReadSection(
            slot = slot,
            reading = reading,
            busy = state is MonoToneBenchViewModel.State.Running,
            onRead = { viewModel.readCurrent(slot) },
        )

        Spacer(Modifier.height(26.dp))
        SectionLabel(text = "Or write and measure")
        Spacer(Modifier.height(10.dp))
        PrimaryCTA(
            label = when (state) {
                is MonoToneBenchViewModel.State.Running -> "Running…"
                else -> "Sweep ${slot.label}"
            },
            onClick = { viewModel.run(slot) },
            busy = state is MonoToneBenchViewModel.State.Running,
            enabled = state !is MonoToneBenchViewModel.State.Running,
        )

        Spacer(Modifier.height(26.dp))
        HoldSection(
            slot = slot,
            hold = hold,
            busy = state is MonoToneBenchViewModel.State.Running,
            onHold = { code, value -> viewModel.holdValue(slot, code, value) },
            onRestore = viewModel::restoreHold,
        )

        when (val current = state) {
            is MonoToneBenchViewModel.State.Idle -> Unit

            is MonoToneBenchViewModel.State.Running -> {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = current.phase,
                    fontFamily = MonoFamily,
                    fontSize = 11.5.sp,
                    color = TextDim,
                )
                ProbeTable(live)
            }

            is MonoToneBenchViewModel.State.Error -> {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = current.message,
                    fontFamily = SansFamily,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = Gold,
                )
            }

            is MonoToneBenchViewModel.State.Done -> {
                Spacer(Modifier.height(18.dp))
                Verdict(current.sweep)
                ProbeTable(current.sweep.probes)
                Spacer(Modifier.height(18.dp))
                NextStep(current.sweep)
            }
        }
    }
}

@Composable
private fun Verdict(sweep: MonoToneSweep) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PanelLow)
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = sweep.summary,
            fontFamily = SansFamily,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            color = TextPrimary,
        )
        Text(
            text = if (sweep.restored) {
                "Slot ${sweep.slot.label} restored · ${sweep.durationMs} ms"
            } else {
                "RESTORE DID NOT CONFIRM — check slot ${sweep.slot.label} on the camera by hand."
            },
            fontFamily = MonoFamily,
            fontSize = 10.5.sp,
            letterSpacing = 0.4.sp,
            color = if (sweep.restored) TextDim else Gold,
        )
    }
}

@Composable
private fun NextStep(sweep: MonoToneSweep) {
    if (sweep.probes.none { it.accepted }) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PanelLow)
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "Read the answer off the camera",
            fontFamily = SansFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.5.sp,
            color = TextPrimary,
        )
        Text(
            text = "Re-run with the restore in mind: the sweep puts both values back, so the camera " +
                "shows nothing by the end. To see which axis a code drives, write one value, stop, " +
                "and open ${sweep.slot.label} → Monochromatic Color on the camera. Whichever of " +
                "warm/cool or magenta/green has moved is what that code is.",
            fontFamily = SansFamily,
            fontSize = 12.5.sp,
            lineHeight = 18.sp,
            color = TextMuted,
        )
    }
}

@Composable
private fun ProbeTable(probes: List<MonoToneProbe>) {
    if (probes.isEmpty()) return

    Spacer(Modifier.height(14.dp))
    probes.groupBy { it.propertyCode }.forEach { (code, rows) ->
        SectionLabel(text = "${rows.first().propertyLabel} · 0x%04X".format(code))
        Spacer(Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(PanelLow)
                .border(1.dp, Border, RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            rows.forEach { probe ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = "wrote ${probe.written}",
                        fontFamily = MonoFamily,
                        fontSize = 11.5.sp,
                        color = TextMuted,
                    )
                    Text(
                        text = probe.verdict,
                        fontFamily = SansFamily,
                        fontSize = 12.sp,
                        color = if (probe.accepted) TextPrimary else Gold,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
    }
}

/**
 * Writes one value and leaves it on the camera.
 *
 * The sweep restores everything, which is correct for measuring but hides the answer to the only
 * question the camera itself can settle: which axis moved. This holds a value so the body's own
 * Monochromatic Color screen can be read while it is still set.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HoldSection(
    slot: CameraSlot,
    hold: MonoToneHold?,
    busy: Boolean,
    onHold: (Int, Int) -> Unit,
    onRestore: () -> Unit,
) {
    var code by remember { mutableStateOf(0xD193) }
    var value by remember { mutableStateOf(90) }

    SectionLabel(text = "Set one value and leave it")
    Spacer(Modifier.height(6.dp))
    Text(
        text = "Writes the value, applies Acros, and does not restore. Open ${slot.label} → " +
            "Monochromatic Color on the camera while it is set: whichever axis has moved is what " +
            "that code drives. Restore when you are done.",
        fontFamily = SansFamily,
        fontSize = 12.5.sp,
        lineHeight = 18.sp,
        color = TextMuted,
    )

    Spacer(Modifier.height(12.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(0xD193 to "0xD193", 0xD194 to "0xD194").forEach { (candidate, label) ->
            BenchToggle(label = label, active = candidate == code) { code = candidate }
        }
    }

    Spacer(Modifier.height(8.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(90, 50, 20, 9, 5, -9, -50, -90).forEach { candidate ->
            BenchToggle(
                label = if (candidate > 0) "+$candidate" else "$candidate",
                active = candidate == value,
            ) { value = candidate }
        }
    }

    Spacer(Modifier.height(12.dp))
    PrimaryCTA(
        label = "Hold 0x%04X = %d".format(code, value),
        onClick = { onHold(code, value) },
        enabled = !busy,
        secondary = true,
    )

    hold?.let { held ->
        Spacer(Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(PanelLow)
                .border(1.dp, Border, RoundedCornerShape(12.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = held.summary,
                fontFamily = SansFamily,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = TextPrimary,
            )
            PrimaryCTA(
                label = "Restore ${held.slot.label}",
                onClick = onRestore,
                enabled = !busy,
                secondary = true,
            )
        }
    }
}

@Composable
private fun BenchToggle(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) GoldDim else PanelHigh)
            .border(1.dp, if (active) Gold else Border, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 9.dp),
    ) {
        Text(
            text = label,
            fontFamily = MonoFamily,
            fontSize = 11.5.sp,
            color = if (active) Gold else TextMuted,
        )
    }
}

/**
 * Reads what the camera holds, writing nothing.
 *
 * The best experiment available here. Set both axes on the camera to different values with opposite
 * signs, read, and the mapping falls out directly — no inferring which axis moved, and the numbers
 * on screen versus the numbers on the wire settle the scale at the same time.
 */
@Composable
private fun ReadSection(
    slot: CameraSlot,
    reading: MonoToneReading?,
    busy: Boolean,
    onRead: () -> Unit,
) {
    SectionLabel(text = "Read what the camera holds")
    Spacer(Modifier.height(6.dp))
    Text(
        text = "Set Monochromatic Color on the camera for ${slot.label} first — pick two different " +
            "values with opposite signs, say WC +5 and MG −3. Then read. Whichever code comes back " +
            "positive is warm/cool, and the size of the number tells you whether the wire value is " +
            "the dial or the dial × 10. Nothing is written.",
        fontFamily = SansFamily,
        fontSize = 12.5.sp,
        lineHeight = 18.sp,
        color = TextMuted,
    )
    Spacer(Modifier.height(12.dp))
    PrimaryCTA(
        label = "Read ${slot.label}",
        onClick = onRead,
        enabled = !busy,
    )

    reading?.let { current ->
        Spacer(Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(PanelLow)
                .border(1.dp, Border, RoundedCornerShape(12.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ReadRow("0xD193  BlackImageTone", current.asDial(current.d193))
            ReadRow("0xD194  MonochromaticColor", current.asDial(current.d194))
            ReadRow("0xD192  Film simulation", current.filmSim?.toString() ?: "—")
            Text(
                text = current.scaleHint,
                fontFamily = SansFamily,
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                color = Gold,
            )
        }
    }
}

@Composable
private fun ReadRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontFamily = MonoFamily, fontSize = 11.5.sp, color = TextMuted)
        Text(
            value,
            fontFamily = MonoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = TextPrimary,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}
