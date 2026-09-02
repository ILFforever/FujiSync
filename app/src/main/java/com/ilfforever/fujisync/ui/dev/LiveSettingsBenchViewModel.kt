package com.ilfforever.fujisync.ui.dev

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilfforever.fujisync.data.usb.CameraHeartbeat
import com.ilfforever.fujisync.data.usb.CameraUsbMode
import com.ilfforever.fujisync.data.usb.LivePropReading
import com.ilfforever.fujisync.data.usb.LiveReadResult
import com.ilfforever.fujisync.data.usb.LiveWriteTest
import com.ilfforever.fujisync.data.usb.OpenPtpConnection
import com.ilfforever.fujisync.data.usb.UsbPtpConnection
import com.ilfforever.fujisync.data.usb.WbModeSweep
import com.ilfforever.fujisync.data.usb.runLiveSettingsRead
import com.ilfforever.fujisync.data.usb.runLiveWriteTest
import com.ilfforever.fujisync.data.usb.runWbModeSweep
import com.ilfforever.fujisync.domain.repository.CameraRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

// Bench for the live / C0 (P-A-S-M) push path recovered from X RAW STUDIO.
//
// The read pass is harmless: it reads each of the 23 paired live property codes and cross-checks
// them against the camera's own DeviceInfo supported-property list. It also captures a raw
// operation diagnostic, so a camera refusing an operation can be told apart from a malformed
// request.
//
// The write test is the only part that changes the camera, and it is deliberately
// change -> verify -> restore. Writing a value back unchanged would prove nothing, since a no-op
// write can succeed on a property that is not really settable. Target values are taken from the
// same setting stored in slots C1-C7, so they are values this body already holds and therefore
// legal; the original is always written back afterwards.
@HiltViewModel
class LiveSettingsBenchViewModel @Inject constructor(
    private val repository: CameraRepository,
    private val connectionFactory: UsbPtpConnection,
    private val heartbeat: CameraHeartbeat,
) : ViewModel() {

    sealed class State {
        object Idle : State()
        data class Running(val phase: String) : State()
        data class Done(val result: LiveReadResult) : State()
        data class Error(val message: String) : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _tests = MutableStateFlow<Map<Int, LiveWriteTest>>(emptyMap())
    val tests: StateFlow<Map<Int, LiveWriteTest>> = _tests.asStateFlow()

    private val _busyCode = MutableStateFlow<Int?>(null)
    val busyCode: StateFlow<Int?> = _busyCode.asStateFlow()

    private val _wbSweep = MutableStateFlow<WbModeSweep?>(null)
    val wbSweep: StateFlow<WbModeSweep?> = _wbSweep.asStateFlow()

    private val _wbRunning = MutableStateFlow(false)
    val wbRunning: StateFlow<Boolean> = _wbRunning.asStateFlow()

    private suspend fun <T> withCamera(block: suspend (OpenPtpConnection, String?) -> T): T {
        val found = withContext(Dispatchers.IO) {
            repository.scanUsb().firstOrNull { it.mode == CameraUsbMode.Ptp }
        } ?: throw IllegalStateException(
            "No camera in PTP mode. Connect the camera and set USB to USB RAW CONV.",
        )
        return heartbeat.usbMutex.withLock {
            withContext(Dispatchers.IO) {
                val conn = connectionFactory.open(found.device)
                    ?: throw IllegalStateException("Could not open camera USB interface.")
                conn.use {
                    if (!conn.openSession()) throw IllegalStateException("Camera rejected OpenSession.")
                    block(conn, found.productName)
                }
            }
        }
    }

    fun run() {
        if (_state.value is State.Running) return
        _state.value = State.Running("Reading live property descriptors…")
        _tests.value = emptyMap()

        viewModelScope.launch {
            try {
                _state.value = State.Done(withCamera { conn, model -> runLiveSettingsRead(conn, model) })
            } catch (e: Exception) {
                _state.value = State.Error(e.message ?: "Read failed.")
            }
        }
    }

    /**
     * Read, then change -> verify -> restore every testable property, all inside one session.
     * Each result is published as it lands so progress is visible rather than arriving in a lump.
     */
    fun runAll() {
        if (_state.value is State.Running || _busyCode.value != null) return
        _state.value = State.Running("Reading…")
        _tests.value = emptyMap()

        viewModelScope.launch {
            try {
                withCamera { conn, model ->
                    val read = runLiveSettingsRead(conn, model)
                    _state.value = State.Done(read)

                    val targets = read.readings.filter { it.testable }
                    targets.forEachIndexed { index, reading ->
                        _busyCode.value = reading.prop.liveCode
                        _state.value = State.Running(
                            "Testing ${index + 1}/${targets.size}: ${reading.prop.label}…",
                        )
                        runCatching { runLiveWriteTest(conn, reading) }
                            .onSuccess { _tests.value = _tests.value + (reading.prop.liveCode to it) }
                        _busyCode.value = null
                    }

                    // Re-read at the end so the table shows where the camera actually ended up,
                    // which is the real check that every restore took.
                    _state.value = State.Done(runLiveSettingsRead(conn, model))
                }
            } catch (e: Exception) {
                _state.value = State.Error(e.message ?: "Run failed.")
            } finally {
                _busyCode.value = null
            }
        }
    }

    fun test(reading: LivePropReading) {
        if (_busyCode.value != null) return
        _busyCode.value = reading.prop.liveCode

        viewModelScope.launch {
            try {
                val test = withCamera { conn, _ -> runLiveWriteTest(conn, reading) }
                _tests.value = _tests.value + (reading.prop.liveCode to test)
            } catch (e: Exception) {
                _state.value = State.Error(
                    "Write test on ${reading.prop.label} failed: ${e.message}. " +
                        "Re-run the read pass to confirm the camera's current values.",
                )
            } finally {
                _busyCode.value = null
            }
        }
    }

    /**
     * Sweep every White Balance mode to find out which of the five recovered from X RAW STUDIO the
     * camera actually accepts. Modes already confirmed on hardware ride along as controls, so a run
     * that breaks them reports itself as untrustworthy instead of producing false negatives.
     */
    fun runWbModeSweep() {
        if (_wbRunning.value || _busyCode.value != null || _state.value is State.Running) return
        _wbRunning.value = true
        _wbSweep.value = null

        viewModelScope.launch {
            try {
                _wbSweep.value = withCamera { conn, _ -> runWbModeSweep(conn) }
            } catch (e: Exception) {
                _state.value = State.Error(
                    "White Balance sweep failed: ${e.message}. Check the camera's WB setting by " +
                        "hand — the sweep restores it, but not if the session dropped mid-run.",
                )
            } finally {
                _wbRunning.value = false
            }
        }
    }

    fun reset() {
        _state.value = State.Idle
        _tests.value = emptyMap()
        _wbSweep.value = null
    }
}
