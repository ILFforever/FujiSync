package com.paeki.fujirecipes.ui.dev

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paeki.fujirecipes.data.usb.BENCH_DELAY_CANDIDATES
import com.paeki.fujirecipes.data.usb.CameraUsbMode
import com.paeki.fujirecipes.data.usb.DelayBenchResult
import com.paeki.fujirecipes.data.usb.CameraHeartbeat
import com.paeki.fujirecipes.data.usb.UsbPtpConnection
import com.paeki.fujirecipes.domain.repository.CameraRepository
import com.paeki.fujirecipes.data.usb.benchWriteDelay
import com.paeki.fujirecipes.data.usb.randomBenchPreset
import com.paeki.fujirecipes.domain.model.CameraSlot
import com.paeki.fujirecipes.ui.components.SectionLabel
import com.paeki.fujirecipes.ui.theme.Bg
import com.paeki.fujirecipes.ui.theme.Border
import com.paeki.fujirecipes.ui.theme.Gold
import com.paeki.fujirecipes.ui.theme.MonoFamily
import com.paeki.fujirecipes.ui.theme.PanelLow
import com.paeki.fujirecipes.ui.theme.SansFamily
import com.paeki.fujirecipes.ui.theme.TextDim
import com.paeki.fujirecipes.ui.theme.TextMuted
import com.paeki.fujirecipes.ui.theme.TextPrimary
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
class WriteDelayBenchViewModel @Inject constructor(
    private val repository: CameraRepository,
    private val connectionFactory: UsbPtpConnection,
    private val heartbeat: CameraHeartbeat,
) : ViewModel() {

    sealed class State {
        object Idle : State()
        data class Running(val phase: String) : State()
        data class Done(val results: List<DelayBenchResult>) : State()
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

                val results = heartbeat.usbMutex.withLock {
                    withContext(Dispatchers.IO) {
                        val conn = connectionFactory.open(device)
                            ?: throw IllegalStateException("Could not open camera USB interface.")
                        conn.use {
                            if (!conn.openSession()) throw IllegalStateException("Camera rejected OpenSession.")
                            val presets = CameraSlot.entries.map { slot -> randomBenchPreset(slot) }
                            benchWriteDelay(conn, presets, BENCH_DELAY_CANDIDATES) { phase ->
                                _state.value = State.Running(phase)
                            }
                        }
                    }
                }

                _state.value = State.Done(results)
            } catch (e: Exception) {
                _state.value = State.Error(e.message ?: "Bench failed.")
            }
        }
    }

    fun reset() {
        _state.value = State.Idle
    }
}

@Composable
fun WriteDelayBenchScreen(
    viewModel: WriteDelayBenchViewModel,
    onClose: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "WRITE DELAY BENCH",
                fontFamily = SansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                letterSpacing = 0.4.sp,
                color = TextPrimary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (state is WriteDelayBenchViewModel.State.Done || state is WriteDelayBenchViewModel.State.Error) {
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

            val running = state is WriteDelayBenchViewModel.State.Running
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PanelLow)
                    .border(
                        1.dp,
                        if (running) Border else Gold.copy(alpha = 0.5f),
                        RoundedCornerShape(12.dp),
                    )
                    .clickable(enabled = !running) { viewModel.run() }
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (running) {
                    CircularProgressIndicator(
                        color = Gold,
                        strokeWidth = 2.dp,
                        modifier = Modifier.height(16.dp).width(16.dp),
                    )
                }
                Text(
                    text = if (running) (state as WriteDelayBenchViewModel.State.Running).phase
                    else "RUN BENCH  (writes bench recipe to all 7 slots × 5 delays)",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.5.sp,
                    letterSpacing = 0.sp,
                    color = if (running) TextMuted else TextPrimary,
                )
            }

            Text(
                text = "Camera must be connected in PTP mode. The bench writes a known test recipe (Velvia, BENCH-Cx name) to all 7 slots at each candidate delay. Restore your backup afterwards.",
                fontFamily = SansFamily,
                fontSize = 11.sp,
                color = TextDim,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 10.dp),
            )

            when (val s = state) {
                is WriteDelayBenchViewModel.State.Error -> {
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

                is WriteDelayBenchViewModel.State.Done -> {
                    Spacer(Modifier.height(24.dp))
                    SectionLabel(text = "Results")
                    Spacer(Modifier.height(8.dp))
                    ResultsTable(results = s.results)
                }

                else -> Unit
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun ResultsTable(results: List<DelayBenchResult>) {
    val firstClean = results.firstOrNull { it.failed == 0 }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PanelLow)
            .border(1.dp, Border, RoundedCornerShape(14.dp)),
    ) {
        // Column header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Text("DELAY", fontFamily = MonoFamily, fontSize = 9.sp, letterSpacing = 1.4.sp, color = TextDim, modifier = Modifier.weight(0.20f))
            Text("OK", fontFamily = MonoFamily, fontSize = 9.sp, letterSpacing = 1.4.sp, color = TextDim, modifier = Modifier.weight(0.22f))
            Text("ERR", fontFamily = MonoFamily, fontSize = 9.sp, letterSpacing = 1.4.sp, color = TextDim, modifier = Modifier.weight(0.22f))
            Text("TIME", fontFamily = MonoFamily, fontSize = 9.sp, letterSpacing = 1.4.sp, color = TextDim, modifier = Modifier.weight(0.36f))
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Border))

        results.forEachIndexed { idx, result ->
            val isFirstClean = result == firstClean
            val hasFailed = result.failed > 0
            val accentColor = when {
                isFirstClean -> Gold
                hasFailed -> TextMuted
                else -> TextPrimary
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${result.delayMs}ms",
                    fontFamily = MonoFamily,
                    fontWeight = if (isFirstClean) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 12.sp,
                    color = accentColor,
                    modifier = Modifier.weight(0.20f),
                )
                Text(
                    text = "${result.success}/${result.total}",
                    fontFamily = MonoFamily,
                    fontSize = 12.sp,
                    color = if (hasFailed) TextMuted else TextPrimary,
                    modifier = Modifier.weight(0.22f),
                )
                Text(
                    text = if (result.failed == 0) "—" else "${result.failed}",
                    fontFamily = MonoFamily,
                    fontWeight = if (hasFailed) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 12.sp,
                    color = if (hasFailed) Gold else TextDim,
                    modifier = Modifier.weight(0.22f),
                )
                Text(
                    text = "%.1fs".format(result.durationMs / 1000.0),
                    fontFamily = MonoFamily,
                    fontSize = 12.sp,
                    color = TextDim,
                    modifier = Modifier.weight(0.36f),
                )
            }

            if (idx < results.lastIndex) {
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Border))
            }
        }
    }

    firstClean?.let {
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Minimum clean delay: ${it.delayMs}ms",
            fontFamily = MonoFamily,
            fontSize = 10.sp,
            letterSpacing = 0.6.sp,
            color = Gold,
        )
    }

    // Show failing props from the first run (consistent across all delays)
    val failedProps = results.firstOrNull()?.failedProps?.takeIf { it.isNotEmpty() }
    if (failedProps != null) {
        Spacer(Modifier.height(16.dp))
        Text(
            text = "FAILING PROPERTIES",
            fontFamily = MonoFamily,
            fontSize = 9.sp,
            letterSpacing = 1.4.sp,
            color = TextDim,
        )
        Spacer(Modifier.height(6.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(PanelLow)
                .border(1.dp, Gold.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            failedProps.entries.sortedByDescending { it.value }.forEach { (prop, count) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = prop?.displayName ?: "slot-select / name",
                        fontFamily = MonoFamily,
                        fontSize = 11.sp,
                        color = TextMuted,
                    )
                    Text(
                        text = "×$count",
                        fontFamily = MonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Gold,
                    )
                }
            }
        }
    }
}
