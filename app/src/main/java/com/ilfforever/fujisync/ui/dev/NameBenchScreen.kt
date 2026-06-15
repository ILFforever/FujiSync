package com.ilfforever.fujisync.ui.dev

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilfforever.fujisync.data.usb.CameraHeartbeat
import com.ilfforever.fujisync.data.usb.CameraUsbMode
import com.ilfforever.fujisync.data.usb.FujiRecipeCamera
import com.ilfforever.fujisync.data.usb.NameBenchResult
import com.ilfforever.fujisync.data.usb.UsbPtpConnection
import com.ilfforever.fujisync.domain.repository.CameraRepository
import com.ilfforever.fujisync.data.usb.benchPresetNames
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
class NameBenchViewModel @Inject constructor(
    private val repository: CameraRepository,
    private val connectionFactory: UsbPtpConnection,
    private val heartbeat: CameraHeartbeat,
) : ViewModel() {

    sealed class State {
        object Idle : State()
        data class Running(val phase: String) : State()
        data class Done(val results: List<NameBenchResult>) : State()
        data class Error(val message: String) : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    fun run() {
        if (_state.value is State.Running) return
        _state.value = State.Running("Finding camera...")

        viewModelScope.launch {
            try {
                val device = withContext(Dispatchers.IO) {
                    repository.scanUsb().firstOrNull { it.mode == CameraUsbMode.Ptp }?.device
                } ?: run {
                    _state.value = State.Error("No camera in PTP mode.")
                    return@launch
                }

                val results = heartbeat.usbMutex.withLock {
                    withContext(Dispatchers.IO) {
                        val conn = connectionFactory.open(device)
                            ?: throw IllegalStateException("Could not open camera USB interface.")
                        conn.use {
                            if (!conn.openSession()) throw IllegalStateException("Camera rejected OpenSession.")
                            benchPresetNames(FujiRecipeCamera(conn)) { phase ->
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

    fun reset() { _state.value = State.Idle }
}

@Composable
fun NameBenchScreen(
    viewModel: NameBenchViewModel,
    onClose: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val running = state is NameBenchViewModel.State.Running

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
                text = "NAME BENCH",
                fontFamily = SansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = TextPrimary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (state is NameBenchViewModel.State.Done || state is NameBenchViewModel.State.Error) {
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
                    CircularProgressIndicator(
                        color = Gold,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    text = if (running) (state as NameBenchViewModel.State.Running).phase
                    else "RUN BENCH  (writes preset names to C1-C7)",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.5.sp,
                    color = if (running) TextMuted else TextPrimary,
                )
            }

            Text(
                text = "Camera must be connected in PTP mode. This writes targeted preset names to all 7 slots, then reads them back. Restore your backup afterwards.",
                fontFamily = SansFamily,
                fontSize = 11.sp,
                color = TextDim,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 10.dp),
            )

            when (val s = state) {
                is NameBenchViewModel.State.Done -> {
                    val clean = s.results.all { it.matchesExpected }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "${s.results.count { it.matchesExpected }}/7 MATCH  ·  ${if (clean) "CLEAN" else "CHECK READBACK"}",
                        fontFamily = MonoFamily,
                        fontSize = 12.sp,
                        color = if (clean) Gold else TextMuted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(PanelHigh)
                            .border(1.dp, if (clean) Gold.copy(alpha = 0.35f) else Border, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                    s.results.forEach { result ->
                        NameBenchResultRow(result)
                        Spacer(Modifier.height(6.dp))
                    }
                }
                is NameBenchViewModel.State.Error -> {
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
                else -> Unit
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun NameBenchResultRow(result: NameBenchResult) {
    val ok = result.matchesExpected
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PanelHigh)
            .border(1.dp, if (ok) Gold.copy(alpha = 0.28f) else Border, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${result.case.slot.label} · ${result.case.label}",
                fontFamily = SansFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = TextPrimary,
            )
            Text(
                text = if (ok) "OK" else if (!result.writeOk) "WRITE FAIL" else "MISMATCH",
                fontFamily = MonoFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                color = if (ok) Gold else Color(0xFFD45B4A),
            )
        }
        Spacer(Modifier.height(5.dp))
        NameBenchLine(label = "Expected", value = result.case.expectedName)
        NameBenchLine(label = "Camera", value = result.readName ?: "Read failed")
    }
}

@Composable
private fun NameBenchLine(label: String, value: String) {
    Text(
        text = "$label: $value",
        fontFamily = MonoFamily,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        color = TextMuted,
    )
}
