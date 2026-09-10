package com.ilfforever.fujisync.ui.dev

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilfforever.fujisync.data.usb.CameraSessionManager
import com.ilfforever.fujisync.data.usb.CameraUsbMode
import com.ilfforever.fujisync.data.usb.MonoToneHold
import com.ilfforever.fujisync.data.usb.MonoToneProbe
import com.ilfforever.fujisync.data.usb.MonoToneReading
import com.ilfforever.fujisync.data.usb.MonoToneSweep
import com.ilfforever.fujisync.data.usb.OpenPtpConnection
import com.ilfforever.fujisync.data.usb.holdMonoToneValue
import com.ilfforever.fujisync.data.usb.readMonoToneState
import com.ilfforever.fujisync.data.usb.restoreMonoTone
import com.ilfforever.fujisync.data.usb.runMonoToneSweep
import com.ilfforever.fujisync.domain.model.CameraSlot
import com.ilfforever.fujisync.domain.repository.CameraRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Drives the monochrome toning sweep against one slot.
 *
 * This bench writes to the camera, unlike the capability bench. It picks a slot deliberately rather
 * than defaulting to C1, restores the film simulation and both tone values afterwards, and reports
 * whether the restore actually took.
 */
@HiltViewModel
class MonoToneBenchViewModel @Inject constructor(
    private val repository: CameraRepository,
    private val sessionManager: CameraSessionManager,
) : ViewModel() {

    sealed class State {
        object Idle : State()
        data class Running(val phase: String) : State()
        data class Done(val sweep: MonoToneSweep) : State()
        data class Error(val message: String) : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _live = MutableStateFlow<List<MonoToneProbe>>(emptyList())
    val live: StateFlow<List<MonoToneProbe>> = _live.asStateFlow()

    /** What the camera currently holds — the read-only experiment. */
    private val _reading = MutableStateFlow<MonoToneReading?>(null)
    val reading: StateFlow<MonoToneReading?> = _reading.asStateFlow()

    /** A value deliberately left on the camera so the axis can be read off its own screen. */
    private val _hold = MutableStateFlow<MonoToneHold?>(null)
    val hold: StateFlow<MonoToneHold?> = _hold.asStateFlow()

    private suspend fun <T> withCamera(block: suspend (OpenPtpConnection) -> T): T {
        val found = withContext(Dispatchers.IO) {
            repository.scanUsb().firstOrNull { it.mode == CameraUsbMode.Ptp }
        } ?: throw IllegalStateException(
            "No camera in PTP mode. Connect it and set USB to USB RAW CONV.",
        )
        return sessionManager.withRawSession(found.device) { conn -> block(conn) }.getOrThrow()
    }

    /**
     * Reads what the camera is holding, writing nothing.
     *
     * Set both axes on the camera first, to different values with opposite signs — then whichever
     * code carries which is unambiguous, and comparing the numbers settles the scale too.
     */
    fun readCurrent(slot: CameraSlot) {
        if (_state.value is State.Running) return
        _state.value = State.Running("Reading ${slot.label}…")

        viewModelScope.launch {
            try {
                _reading.value = withCamera { conn -> readMonoToneState(conn, slot) }
                _state.value = State.Idle
            } catch (e: Exception) {
                _state.value = State.Error(e.message ?: "Could not read the slot.")
            }
        }
    }

    fun run(slot: CameraSlot) {
        if (_state.value is State.Running) return
        _state.value = State.Running("Selecting ${slot.label}…")
        _live.value = emptyList()

        viewModelScope.launch {
            try {
                val sweep = withCamera { conn ->
                    runMonoToneSweep(conn, slot) { probe ->
                        _live.value = _live.value + probe
                        _state.value = State.Running(
                            "${probe.propertyLabel} ${probe.hex} = ${probe.written}…",
                        )
                    }
                }
                _state.value = State.Done(sweep)
            } catch (e: Exception) {
                _state.value = State.Error(
                    (e.message ?: "Sweep failed.") +
                        " Check the slot on the camera — the restore may not have run.",
                )
            }
        }
    }

    /**
     * Writes one value and leaves it on the camera, so the Monochromatic Color screen can be read
     * while it is still set. The sweep cannot answer that question because it restores.
     */
    fun holdValue(slot: CameraSlot, propertyCode: Int, value: Int) {
        if (_state.value is State.Running) return
        _state.value = State.Running("Holding 0x%04X = $value…".format(propertyCode))

        viewModelScope.launch {
            try {
                val held = withCamera { conn -> holdMonoToneValue(conn, slot, propertyCode, value) }
                _hold.value = held
                _state.value = State.Idle
            } catch (e: Exception) {
                _state.value = State.Error(e.message ?: "Could not write the value.")
            }
        }
    }

    /** Puts back whatever the last hold displaced. */
    fun restoreHold() {
        val held = _hold.value ?: return
        if (_state.value is State.Running) return
        _state.value = State.Running("Restoring ${held.slot.label}…")

        viewModelScope.launch {
            try {
                val ok = withCamera { conn -> restoreMonoTone(conn, held) }
                _hold.value = null
                _state.value = if (ok) {
                    State.Idle
                } else {
                    State.Error(
                        "Restore did not confirm. Check ${held.slot.label} on the camera by hand.",
                    )
                }
            } catch (e: Exception) {
                _state.value = State.Error(
                    (e.message ?: "Restore failed.") +
                        " ${held.slot.label} may still be holding the test value.",
                )
            }
        }
    }

    fun reset() {
        _state.value = State.Idle
        _live.value = emptyList()
    }
}
