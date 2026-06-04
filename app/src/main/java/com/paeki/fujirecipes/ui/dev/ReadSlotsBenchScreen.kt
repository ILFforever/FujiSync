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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paeki.fujirecipes.data.usb.CameraHeartbeat
import com.paeki.fujirecipes.data.usb.CameraUsbMode
import com.paeki.fujirecipes.data.usb.FujiRecipeCamera
import com.paeki.fujirecipes.data.usb.UsbPtpConnection
import com.paeki.fujirecipes.domain.repository.CameraRepository
import com.paeki.fujirecipes.domain.model.CameraSlot
import com.paeki.fujirecipes.ui.theme.Bg
import com.paeki.fujirecipes.ui.theme.Border
import com.paeki.fujirecipes.ui.theme.Gold
import com.paeki.fujirecipes.ui.theme.MonoFamily
import com.paeki.fujirecipes.ui.theme.PanelHigh
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
class ReadSlotsBenchViewModel @Inject constructor(
    private val repository: CameraRepository,
    private val connectionFactory: UsbPtpConnection,
    private val heartbeat: CameraHeartbeat,
) : ViewModel() {

    sealed class State {
        object Idle : State()
        data class Running(val slot: String) : State()
        data class Done(val durationMs: Long, val failed: Int) : State()
        data class Error(val message: String) : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    fun run() {
        if (_state.value is State.Running) return
        _state.value = State.Running("C1")

        viewModelScope.launch {
            try {
                val device = withContext(Dispatchers.IO) {
                    repository.scanUsb().firstOrNull { it.mode == CameraUsbMode.Ptp }?.device
                } ?: run {
                    _state.value = State.Error("No camera in PTP mode.")
                    return@launch
                }

                val started = System.currentTimeMillis()
                var failed = 0

                heartbeat.usbMutex.withLock {
                    withContext(Dispatchers.IO) {
                        val conn = connectionFactory.open(device)
                            ?: throw IllegalStateException("Could not open camera USB interface.")
                        conn.use {
                            if (!conn.openSession()) throw IllegalStateException("Camera rejected OpenSession.")
                            val cam = FujiRecipeCamera(conn)
                            for (slot in CameraSlot.entries) {
                                _state.value = State.Running(slot.label)
                                runCatching { cam.readPreset(slot) }.onFailure { failed++ }
                            }
                        }
                    }
                }

                _state.value = State.Done(System.currentTimeMillis() - started, failed)
            } catch (e: Exception) {
                _state.value = State.Error(e.message ?: "Bench failed.")
            }
        }
    }

    fun reset() { _state.value = State.Idle }
}

@Composable
fun ReadSlotsBenchScreen(
    viewModel: ReadSlotsBenchViewModel,
    onClose: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val running = state is ReadSlotsBenchViewModel.State.Running

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
                text = "READ BENCH",
                fontFamily = SansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = TextPrimary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (state is ReadSlotsBenchViewModel.State.Done || state is ReadSlotsBenchViewModel.State.Error) {
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
                    text = if (running) "Reading ${(state as ReadSlotsBenchViewModel.State.Running).slot}..."
                    else "RUN BENCH  (reads all 7 slots)",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.5.sp,
                    color = if (running) TextMuted else TextPrimary,
                )
            }

            Text(
                text = "Camera must be connected in PTP mode. This bench only reads C1-C7 and reports total time plus read failures.",
                fontFamily = SansFamily,
                fontSize = 11.sp,
                color = TextDim,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 10.dp),
            )

            when (val s = state) {
                is ReadSlotsBenchViewModel.State.Done -> {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "7 slots  ·  ${if (s.failed == 0) "CLEAN" else "${s.failed} FAILED"}  ·  %.2fs".format(s.durationMs / 1000.0),
                        fontFamily = MonoFamily,
                        fontSize = 12.sp,
                        color = if (s.failed == 0) Gold else TextMuted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(PanelHigh)
                            .border(1.dp, if (s.failed == 0) Gold.copy(alpha = 0.35f) else Border, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                    )
                }
                is ReadSlotsBenchViewModel.State.Error -> {
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
