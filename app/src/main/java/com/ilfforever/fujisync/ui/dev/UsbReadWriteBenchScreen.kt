package com.ilfforever.fujisync.ui.dev

import android.content.Intent
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilfforever.fujisync.data.usb.CameraHeartbeat
import com.ilfforever.fujisync.data.usb.CameraUsbMode
import com.ilfforever.fujisync.data.usb.FujiRecipeCamera
import com.ilfforever.fujisync.data.usb.SlotRoundTripResult
import com.ilfforever.fujisync.data.usb.UsbPtpConnection
import com.ilfforever.fujisync.data.usb.benchRoundTripSlot
import com.ilfforever.fujisync.domain.model.CameraSlot
import com.ilfforever.fujisync.domain.repository.CameraRepository
import com.ilfforever.fujisync.ui.components.SectionLabel
import com.ilfforever.fujisync.ui.theme.Bg
import com.ilfforever.fujisync.ui.theme.Border
import com.ilfforever.fujisync.ui.theme.Gold
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.PanelHigh
import com.ilfforever.fujisync.ui.theme.PanelLow
import com.ilfforever.fujisync.ui.theme.SansFamily
import com.ilfforever.fujisync.ui.theme.TextDim
import com.ilfforever.fujisync.ui.theme.TextMuted
import com.ilfforever.fujisync.ui.theme.TextPrimary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class UsbReadWriteBenchViewModel @Inject constructor(
    private val repository: CameraRepository,
    private val connectionFactory: UsbPtpConnection,
    private val heartbeat: CameraHeartbeat,
) : ViewModel() {

    sealed class State {
        object Idle : State()
        data class Running(val phase: String) : State()
        data class Done(val results: List<SlotRoundTripResult>, val totalMs: Long) : State()
        data class Error(val message: String) : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    fun run() {
        if (_state.value is State.Running) return
        _state.value = State.Running("Finding camera…")

        viewModelScope.launch {
            try {
                val device = withContext(Dispatchers.IO) {
                    repository.scanUsb().firstOrNull { it.mode == CameraUsbMode.Ptp }?.device
                } ?: run {
                    _state.value = State.Error("No camera in PTP mode. Connect the camera and set USB to USB RAW CONV.")
                    return@launch
                }

                val totalStart = System.currentTimeMillis()

                val results = heartbeat.usbMutex.withLock {
                    withContext(Dispatchers.IO) {
                        val conn = connectionFactory.open(device)
                            ?: throw IllegalStateException("Could not open camera USB interface.")
                        conn.use {
                            if (!conn.openSession()) throw IllegalStateException("Camera rejected OpenSession.")
                            val cam = FujiRecipeCamera(conn)
                            CameraSlot.entries.map { slot ->
                                _state.value = State.Running("${slot.label}…")
                                benchRoundTripSlot(conn, cam, slot)
                            }
                        }
                    }
                }

                _state.value = State.Done(results, System.currentTimeMillis() - totalStart)
            } catch (e: Exception) {
                _state.value = State.Error(e.message ?: "Bench failed.")
            }
        }
    }

    fun reset() { _state.value = State.Idle }
}

private fun formatShareText(results: List<SlotRoundTripResult>, totalMs: Long): String = buildString {
    appendLine("USB READ/WRITE BENCH — FujiSync")
    appendLine("Total: %.2fs".format(totalMs / 1000.0))
    appendLine()
    for (r in results) {
        val readStatus = if (r.readOk) "OK" else "ERR"
        val writeStatus = if (!r.readOk) "—" else if (r.writeFailed == 0) "OK (${r.writeSuccess})" else "ERR (${r.writeFailed} failed, ${r.writeSuccess} ok, ${r.writeSkipped} skipped)"
        val verifyStatus = if (!r.readOk || r.verifyTotal == 0) "—" else if (r.verifyMatched == r.verifyTotal) "OK (${r.verifyTotal}/${r.verifyTotal})" else "ERR (${r.verifyMatched}/${r.verifyTotal} matched)"
        appendLine("${r.slot.label}  read=$readStatus  write=$writeStatus  verify=$verifyStatus  %.1fs".format(r.durationMs / 1000.0))
        if (r.failedWriteProps.isNotEmpty()) {
            appendLine("  Write failures: ${r.failedWriteProps.joinToString(", ")}")
        }
        if (r.mismatchedProps.isNotEmpty()) {
            for (m in r.mismatchedProps) {
                appendLine("  Verify mismatch: ${m.name}  expected=${m.expected}  actual=${m.actual ?: "missing"}")
            }
        }
    }
    appendLine()
    val ok = results.filter { it.readOk }
    val allClean = results.all { it.allOk }
    appendLine(if (allClean) "PASS — all 7 slots clean" else "FAIL — see per-slot detail above")
    appendLine("${ok.size}/${results.size} read · ${ok.sumOf { it.writeSuccess }}/${ok.sumOf { it.writeAttempted + it.writeSkipped }} write · ${ok.sumOf { it.verifyMatched }}/${ok.sumOf { it.verifyTotal }} verified")
}

@Composable
fun UsbReadWriteBenchScreen(
    viewModel: UsbReadWriteBenchViewModel,
    onClose: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val running = state is UsbReadWriteBenchViewModel.State.Running
    val context = LocalContext.current

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
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "USB READ/WRITE BENCH",
                fontFamily = SansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                letterSpacing = 0.4.sp,
                color = TextPrimary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (state is UsbReadWriteBenchViewModel.State.Done || state is UsbReadWriteBenchViewModel.State.Error) {
                    Text(
                        text = "RESET",
                        fontFamily = SansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp,
                        letterSpacing = 1.3.sp,
                        color = TextMuted,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { viewModel.reset() }
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                    )
                }
                Text(
                    text = "CLOSE",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    letterSpacing = 1.3.sp,
                    color = TextMuted,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable(onClick = onClose)
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                )
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Border))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PanelLow)
                    .border(1.dp, if (running) Border else Gold.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .clickable(enabled = !running) { viewModel.run() }
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (running) {
                    CircularProgressIndicator(color = Gold, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                }
                Text(
                    text = if (running) (state as UsbReadWriteBenchViewModel.State.Running).phase
                    else "RUN BENCH  (read → write → verify all 7 slots)",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.5.sp,
                    color = if (running) TextMuted else TextPrimary,
                )
            }

            Text(
                text = "For each slot: reads the original, writes a known bench preset (Velvia) property-by-property, re-reads and verifies each value landed, then restores the original. Backup your recipes first.",
                fontFamily = SansFamily,
                fontSize = 11.sp,
                color = TextDim,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 10.dp),
            )

            when (val s = state) {
                is UsbReadWriteBenchViewModel.State.Error -> {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = s.message,
                        fontFamily = MonoFamily,
                        fontSize = 11.sp,
                        color = Gold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(PanelLow)
                            .border(1.dp, Gold.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                    )
                }
                is UsbReadWriteBenchViewModel.State.Done -> {
                    Spacer(Modifier.height(24.dp))
                    SectionLabel(text = "Results")
                    Spacer(Modifier.height(8.dp))
                    RoundTripResultsTable(results = s.results)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Total: %.2fs".format(s.totalMs / 1000.0),
                        fontFamily = MonoFamily,
                        fontSize = 10.sp,
                        color = TextDim,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "SHARE RESULTS",
                        fontFamily = SansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.3.sp,
                        color = Gold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(PanelLow)
                            .border(1.dp, Gold.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, formatShareText(s.results, s.totalMs))
                                }
                                context.startActivity(Intent.createChooser(intent, "Share bench results"))
                            }
                            .padding(vertical = 14.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
                else -> Unit
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun RoundTripResultsTable(results: List<SlotRoundTripResult>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PanelLow)
            .border(1.dp, Border, RoundedCornerShape(14.dp)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Text("SLOT",   fontFamily = MonoFamily, fontSize = 9.sp, letterSpacing = 1.2.sp, color = TextDim, modifier = Modifier.weight(0.14f))
            Text("READ",   fontFamily = MonoFamily, fontSize = 9.sp, letterSpacing = 1.2.sp, color = TextDim, modifier = Modifier.weight(0.22f))
            Text("WRITE",  fontFamily = MonoFamily, fontSize = 9.sp, letterSpacing = 1.2.sp, color = TextDim, modifier = Modifier.weight(0.22f))
            Text("VERIFY", fontFamily = MonoFamily, fontSize = 9.sp, letterSpacing = 1.2.sp, color = TextDim, modifier = Modifier.weight(0.22f))
            Text("TIME",   fontFamily = MonoFamily, fontSize = 9.sp, letterSpacing = 1.2.sp, color = TextDim, modifier = Modifier.weight(0.20f))
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Border))

        results.forEachIndexed { idx, r ->
            val hasFailure = !r.allOk
            val slotColor = if (r.allOk) Gold else if (!r.readOk) TextMuted else TextPrimary

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (r.allOk) Gold.copy(alpha = 0.04f) else PanelLow),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(r.slot.label, fontFamily = MonoFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = slotColor, modifier = Modifier.weight(0.14f))
                    Text(
                        text = if (r.readOk) "OK" else "ERR",
                        fontFamily = MonoFamily, fontSize = 12.sp,
                        color = if (r.readOk) TextPrimary else Gold,
                        modifier = Modifier.weight(0.22f),
                    )
                    Text(
                        text = when {
                            !r.readOk -> "—"
                            r.writeFailed == 0 -> "OK"
                            else -> "ERR"
                        },
                        fontFamily = MonoFamily, fontSize = 12.sp,
                        color = when {
                            !r.readOk -> TextDim
                            r.writeFailed > 0 -> Gold
                            else -> TextPrimary
                        },
                        modifier = Modifier.weight(0.22f),
                    )
                    Text(
                        text = when {
                            !r.readOk || r.verifyTotal == 0 -> "—"
                            r.verifyMatched == r.verifyTotal -> "OK"
                            else -> "ERR"
                        },
                        fontFamily = MonoFamily, fontSize = 12.sp,
                        color = when {
                            !r.readOk || r.verifyTotal == 0 -> TextDim
                            r.verifyMatched < r.verifyTotal -> Gold
                            else -> TextPrimary
                        },
                        modifier = Modifier.weight(0.22f),
                    )
                    Text("%.1fs".format(r.durationMs / 1000.0), fontFamily = MonoFamily, fontSize = 12.sp, color = TextDim, modifier = Modifier.weight(0.20f))
                }

                if (hasFailure && r.readOk) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        for (name in r.failedWriteProps) {
                            Text(
                                text = "write failed: $name",
                                fontFamily = MonoFamily,
                                fontSize = 10.sp,
                                color = Gold,
                            )
                        }
                        for (m in r.mismatchedProps) {
                            Text(
                                text = "verify mismatch: ${m.name}  expected=${m.expected}  got=${m.actual ?: "missing"}",
                                fontFamily = MonoFamily,
                                fontSize = 10.sp,
                                color = TextMuted,
                            )
                        }
                    }
                }
            }

            if (idx < results.lastIndex) {
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Border))
            }
        }
    }

    val ok = results.filter { it.readOk }
    if (ok.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
        val allClean = results.all { it.allOk }
        Text(
            text = "${ok.size}/${results.size} read · ${ok.sumOf { it.writeSuccess }}/${ok.sumOf { it.writeAttempted + it.writeSkipped }} write · ${ok.sumOf { it.verifyMatched }}/${ok.sumOf { it.verifyTotal }} verified",
            fontFamily = MonoFamily,
            fontSize = 10.sp,
            letterSpacing = 0.5.sp,
            color = if (allClean) Gold else TextMuted,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(PanelHigh)
                .border(1.dp, if (allClean) Gold.copy(alpha = 0.3f) else Border, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 9.dp),
        )
    }
}
