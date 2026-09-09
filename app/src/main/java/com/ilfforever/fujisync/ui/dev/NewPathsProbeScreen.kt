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
import com.ilfforever.fujisync.data.usb.CameraSessionManager
import com.ilfforever.fujisync.data.usb.CameraUsbMode
import com.ilfforever.fujisync.data.usb.NewPathsProbeResult
import com.ilfforever.fujisync.data.usb.UsbPtpConnection
import com.ilfforever.fujisync.data.usb.runNewPathsProbe
import com.ilfforever.fujisync.domain.repository.CameraRepository
import com.ilfforever.fujisync.ui.dev.components.PropListResultsTable
import com.ilfforever.fujisync.ui.dev.components.ScalarProbeResultsTable
import com.ilfforever.fujisync.ui.dev.components.formatNewPathsProbeShareText
import com.ilfforever.fujisync.ui.theme.Bg
import com.ilfforever.fujisync.ui.theme.Border
import com.ilfforever.fujisync.ui.theme.Gold
import com.ilfforever.fujisync.ui.theme.MonoFamily
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

// Read-only probe for the properties found by reverse-engineering FUJIFILM X RAW STUDIO but never
// checked against a real camera: 0xD186/0xD187 (tether check codes), 0xD184 (IOP code), 0xD36A/
// 0xD36B (battery info), and 0xD235 (the live per-camera supported-property list). Unlike the
// USB read/write bench this never writes anything — several of these are unconfirmed as writable,
// and 0xD235's response isn't a simple scalar. See docs in the fujifilm-ptp-recipes repo
// (docs/research/xraw-studio-sdk.md) for how each was found.
@HiltViewModel
class NewPathsProbeViewModel @Inject constructor(
    private val repository: CameraRepository,
    private val connectionFactory: UsbPtpConnection,
    private val sessionManager: CameraSessionManager,
) : ViewModel() {

    sealed class State {
        object Idle : State()
        data class Running(val phase: String) : State()
        data class Done(val result: NewPathsProbeResult) : State()
        data class Error(val message: String) : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    fun run() {
        if (_state.value is State.Running) return
        _state.value = State.Running("Finding camera…")

        viewModelScope.launch {
            try {
                val found = withContext(Dispatchers.IO) {
                    repository.scanUsb().firstOrNull { it.mode == CameraUsbMode.Ptp }
                } ?: run {
                    _state.value = State.Error("No camera in PTP mode. Connect the camera and set USB to USB RAW CONV.")
                    return@launch
                }

                _state.value = State.Running("Reading new property paths…")

                val result = sessionManager.withExclusiveUsb {
                    withContext(Dispatchers.IO) {
                        val conn = connectionFactory.open(found.device)
                            ?: throw IllegalStateException("Could not open camera USB interface.")
                        conn.use {
                            if (!conn.openSession()) throw IllegalStateException("Camera rejected OpenSession.")
                            runNewPathsProbe(conn, found.productName)
                        }
                    }
                }

                _state.value = State.Done(result)
            } catch (e: Exception) {
                _state.value = State.Error(e.message ?: "Probe failed.")
            }
        }
    }

    fun reset() { _state.value = State.Idle }
}

@Composable
fun NewPathsProbeScreen(
    viewModel: NewPathsProbeViewModel,
    onClose: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val running = state is NewPathsProbeViewModel.State.Running
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
                text = "NEW PATHS PROBE",
                fontFamily = SansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                letterSpacing = 0.4.sp,
                color = TextPrimary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (state is NewPathsProbeViewModel.State.Done || state is NewPathsProbeViewModel.State.Error) {
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
                    text = if (running) (state as NewPathsProbeViewModel.State.Running).phase
                    else "RUN PROBE  (read-only — 0xD186 · 0xD187 · 0xD184 · 0xD235 · 0xD36A · 0xD36B)",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.5.sp,
                    color = if (running) TextMuted else TextPrimary,
                )
            }

            Text(
                text = "Reads properties found by reverse-engineering X RAW STUDIO, unconfirmed against a real camera until now. Read-only — never writes anything.",
                fontFamily = SansFamily,
                fontSize = 11.sp,
                color = TextDim,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 10.dp),
            )

            when (val s = state) {
                is NewPathsProbeViewModel.State.Error -> {
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
                is NewPathsProbeViewModel.State.Done -> {
                    Spacer(Modifier.height(24.dp))
                    s.result.cameraModel?.let {
                        Text("Camera: $it", fontFamily = MonoFamily, fontSize = 11.sp, color = TextDim)
                        Spacer(Modifier.height(12.dp))
                    }
                    ScalarProbeResultsTable(results = s.result.scalarResults)
                    Spacer(Modifier.height(20.dp))
                    PropListResultsTable(raw = s.result.propListRaw, entries = s.result.propList)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Total: %.2fs".format(s.result.durationMs / 1000.0),
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
                                    putExtra(Intent.EXTRA_TEXT, formatNewPathsProbeShareText(s.result))
                                }
                                context.startActivity(Intent.createChooser(intent, "Share probe results"))
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
