package com.ilfforever.fujirecipes.ui.dev

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
import com.ilfforever.fujirecipes.data.ptp.PtpConstants
import com.ilfforever.fujirecipes.data.ptp.decodeInt16Le
import com.ilfforever.fujirecipes.data.ptp.decodeUInt16Le
import com.ilfforever.fujirecipes.data.ptp.hexDump
import com.ilfforever.fujirecipes.data.ptp.parsePtpString
import com.ilfforever.fujirecipes.data.ptp.uint16Le
import com.ilfforever.fujirecipes.data.usb.CameraHeartbeat
import com.ilfforever.fujirecipes.data.usb.CameraUsbMode
import com.ilfforever.fujirecipes.data.usb.OpenPtpConnection
import com.ilfforever.fujirecipes.data.usb.UsbPtpConnection
import com.ilfforever.fujirecipes.domain.model.CameraSlot
import com.ilfforever.fujirecipes.domain.model.FujiPropertyCode
import com.ilfforever.fujirecipes.domain.repository.CameraRepository
import com.ilfforever.fujirecipes.ui.theme.Bg
import com.ilfforever.fujirecipes.ui.theme.Border
import com.ilfforever.fujirecipes.ui.theme.Gold
import com.ilfforever.fujirecipes.ui.theme.MonoFamily
import com.ilfforever.fujirecipes.ui.theme.PanelHigh
import com.ilfforever.fujirecipes.ui.theme.PanelLow
import com.ilfforever.fujirecipes.ui.theme.SansFamily
import com.ilfforever.fujirecipes.ui.theme.TextDim
import com.ilfforever.fujirecipes.ui.theme.TextMuted
import com.ilfforever.fujirecipes.ui.theme.TextPrimary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

private val DrPriorityLabels = listOf("Off", "Weak", "Strong", "Auto")
private val DynamicRangeLabels = listOf("DR Auto", "DR100%", "DR200%", "DR400%")

data class RawPropertySample(
    val code: Int,
    val name: String,
    val response: Int,
    val bytes: Int,
    val rawHex: String,
    val decoded: String,
    val ok: Boolean,
)

data class DrPriorityWriteResult(
    val priorityTargetLabel: String,
    val priorityTargetRaw: Int,
    val dynamicRangeTargetLabel: String,
    val dynamicRangeTargetRaw: Int,
    val slot: CameraSlot,
    val presetName: String,
    val priorityBefore: RawPropertySample,
    val dynamicRangeBefore: RawPropertySample,
    val priorityWriteResponse: Int,
    val priorityWriteOk: Boolean,
    val priorityAfterWrite: RawPropertySample,
    val dynamicRangeWriteResponse: Int,
    val dynamicRangeWriteOk: Boolean,
    val dynamicRangeAfterWrite: RawPropertySample,
    val priorityAfterDynamicRangeWrite: RawPropertySample,
)

@HiltViewModel
class DrPriorityBenchViewModel @Inject constructor(
    private val repository: CameraRepository,
    private val connectionFactory: UsbPtpConnection,
    private val heartbeat: CameraHeartbeat,
) : ViewModel() {

    data class UiState(
        val selectedSlot: CameraSlot = CameraSlot.C1,
        val selectedLabel: String = "Off",
        val selectedDynamicRange: String = "DR400%",
        val running: Boolean = false,
        val phase: String = "",
        val error: String? = null,
        val results: List<DrPriorityWriteResult> = emptyList(),
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun selectSlot(slot: CameraSlot) {
        if (_state.value.running) return
        _state.update { it.copy(selectedSlot = slot) }
    }

    fun selectLabel(label: String) {
        if (_state.value.running) return
        _state.update { it.copy(selectedLabel = label) }
    }

    fun selectDynamicRange(label: String) {
        if (_state.value.running) return
        _state.update { it.copy(selectedDynamicRange = label) }
    }

    fun reset() {
        if (_state.value.running) return
        _state.value = UiState()
    }

    fun capture() {
        val current = _state.value
        if (current.running) return
        _state.update { it.copy(running = true, phase = "Finding camera...", error = null) }

        viewModelScope.launch {
            try {
                val device = withContext(Dispatchers.IO) {
                    repository.scanUsb().firstOrNull { it.mode == CameraUsbMode.Ptp }?.device
                } ?: run {
                    _state.update { it.copy(running = false, phase = "", error = "No camera in PTP mode.") }
                    return@launch
                }

                val result = heartbeat.usbMutex.withLock {
                    withContext(Dispatchers.IO) {
                        val conn = connectionFactory.open(device)
                            ?: throw IllegalStateException("Could not open camera USB interface.")
                        conn.use {
                            if (!conn.openSession()) throw IllegalStateException("Camera rejected OpenSession.")
                            writeAndRead(
                                conn = conn,
                                slot = current.selectedSlot,
                                priorityLabel = current.selectedLabel,
                                dynamicRangeLabel = current.selectedDynamicRange,
                            )
                        }
                    }
                }

                _state.update {
                    it.copy(
                        running = false,
                        phase = "",
                        results = it.results + result,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(running = false, phase = "", error = e.message ?: "DR Priority bench failed.")
                }
            }
        }
    }

    private suspend fun writeAndRead(
        conn: OpenPtpConnection,
        slot: CameraSlot,
        priorityLabel: String,
        dynamicRangeLabel: String,
    ): DrPriorityWriteResult {
        _state.update { it.copy(phase = "Selecting ${slot.label}...") }
        val select = conn.executeCommandWithData(
            code = PtpConstants.SET_DEVICE_PROP_VALUE,
            params = listOf(PtpConstants.FUJI_SLOT_SELECTOR),
            payload = uint16Le(slot.protocolValue),
        )
        check(select.isOk) { "Camera rejected slot select for ${slot.label}." }
        delay(50)

        _state.update { it.copy(phase = "Reading current values...") }
        val priorityBefore = readSample(conn, FujiPropertyCode.DRangePriority.code)
        val dynamicRangeBefore = readSample(conn, FujiPropertyCode.DynamicRange.code)
        val presetName = readPresetName(conn).ifBlank { slot.label }

        _state.update { it.copy(phase = "Writing priority $priorityLabel...") }
        val priorityTargetRaw = priorityLabel.toDrPriorityRaw()
        val priorityWriteTx = conn.executeCommandWithData(
            code = PtpConstants.SET_DEVICE_PROP_VALUE,
            params = listOf(FujiPropertyCode.DRangePriority.code),
            payload = uint16Le(priorityTargetRaw),
        )
        delay(80)

        _state.update { it.copy(phase = "Reading priority back...") }
        val priorityAfterWrite = readSample(conn, FujiPropertyCode.DRangePriority.code)

        _state.update { it.copy(phase = "Writing $dynamicRangeLabel while priority is set...") }
        val dynamicRangeTargetRaw = dynamicRangeLabel.toDynamicRangeRaw()
        val dynamicRangeWriteTx = conn.executeCommandWithData(
            code = PtpConstants.SET_DEVICE_PROP_VALUE,
            params = listOf(FujiPropertyCode.DynamicRange.code),
            payload = uint16Le(dynamicRangeTargetRaw),
        )
        delay(80)

        _state.update { it.copy(phase = "Reading DR and priority back...") }
        val dynamicRangeAfterWrite = readSample(conn, FujiPropertyCode.DynamicRange.code)
        val priorityAfterDynamicRangeWrite = readSample(conn, FujiPropertyCode.DRangePriority.code)

        return DrPriorityWriteResult(
            priorityTargetLabel = priorityLabel,
            priorityTargetRaw = priorityTargetRaw,
            dynamicRangeTargetLabel = dynamicRangeLabel,
            dynamicRangeTargetRaw = dynamicRangeTargetRaw,
            slot = slot,
            presetName = presetName.ifBlank { slot.label },
            priorityBefore = priorityBefore,
            dynamicRangeBefore = dynamicRangeBefore,
            priorityWriteResponse = priorityWriteTx.response.code,
            priorityWriteOk = priorityWriteTx.isOk,
            priorityAfterWrite = priorityAfterWrite,
            dynamicRangeWriteResponse = dynamicRangeWriteTx.response.code,
            dynamicRangeWriteOk = dynamicRangeWriteTx.isOk,
            dynamicRangeAfterWrite = dynamicRangeAfterWrite,
            priorityAfterDynamicRangeWrite = priorityAfterDynamicRangeWrite,
        )
    }

    private fun readPresetName(conn: OpenPtpConnection): String =
        runCatching {
            val tx = conn.executeCommand(
                code = PtpConstants.GET_DEVICE_PROP_VALUE,
                params = listOf(PtpConstants.FUJI_PRESET_NAME),
            )
            if (tx.isOk) parsePtpString(tx.data?.payload ?: ByteArray(0)) else ""
        }.getOrDefault("")

    private fun readSample(conn: OpenPtpConnection, code: Int): RawPropertySample {
        val property = FujiPropertyCode.fromCode(code)
        return runCatching {
            val tx = conn.executeCommand(
                code = PtpConstants.GET_DEVICE_PROP_VALUE,
                params = listOf(code),
                timeoutMs = PtpConstants.STANDARD_TIMEOUT_MS,
            )
            val payload = tx.data?.payload ?: ByteArray(0)
            RawPropertySample(
                code = code,
                name = property?.displayName ?: rawPropertyName(code),
                response = tx.response.code,
                bytes = payload.size,
                rawHex = hexDump(payload, maxBytes = 64),
                decoded = decodeRawValue(payload),
                ok = tx.isOk,
            )
        }.getOrElse { error ->
            RawPropertySample(
                code = code,
                name = property?.displayName ?: rawPropertyName(code),
                response = 0,
                bytes = 0,
                rawHex = "-",
                decoded = error.message ?: error::class.java.simpleName,
                ok = false,
            )
        }
    }

    private fun rawPropertyName(code: Int): String = when (code) {
        PtpConstants.FUJI_SLOT_SELECTOR -> "Slot Selector"
        PtpConstants.FUJI_PRESET_NAME -> "Preset Name"
        FujiPropertyCode.DynamicRange.code -> "Dynamic Range"
        FujiPropertyCode.DRangePriority.code -> "D Range Priority"
        in PtpConstants.PRESET_BLOCK_START..PtpConstants.PRESET_BLOCK_END -> "Unknown preset block"
        else -> "Vendor property"
    }

    private fun decodeRawValue(payload: ByteArray): String {
        if (payload.isEmpty()) return "<empty>"
        val ptpString = runCatching { parsePtpString(payload) }.getOrNull()
            ?.takeIf { it.isNotBlank() && payload.size > 2 }
        if (ptpString != null) return "ptpString=\"$ptpString\""
        val u16 = decodeUInt16Le(payload)
        val i16 = decodeInt16Le(payload)
        return when {
            payload.size == 2 && u16 != null && i16 != null -> "u16=$u16, i16=$i16"
            else -> "${payload.size} bytes"
        }
    }

}

@Composable
fun DrPriorityBenchScreen(
    viewModel: DrPriorityBenchViewModel,
    onClose: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val running = state.running

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
                text = "DR PRIORITY BENCH",
                fontFamily = SansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = TextPrimary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (state.results.isNotEmpty() || state.error != null) {
                    BenchTextButton("RESET", enabled = !running, onClick = viewModel::reset)
                }
                BenchTextButton("CLOSE", enabled = !running, onClick = onClose)
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Border))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(18.dp))
            Text(
                text = "Select a slot, write D Range Priority (0xD191), then send Dynamic Range (0xD190) while priority is enabled. The readback shows whether Fuji accepts or overrides the DR command.",
                fontFamily = SansFamily,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = TextDim,
            )
            Spacer(Modifier.height(16.dp))

            SelectorBlock("SLOT") {
                CameraSlot.entries.forEach { slot ->
                    SelectorChip(slot.label, selected = state.selectedSlot == slot, enabled = !running) {
                        viewModel.selectSlot(slot)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            SelectorBlock("TARGET D RANGE PRIORITY") {
                DrPriorityLabels.forEach { label ->
                    SelectorChip(label.uppercase(), selected = state.selectedLabel == label, enabled = !running) {
                        viewModel.selectLabel(label)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            SelectorBlock("DYNAMIC RANGE WRITE TEST") {
                DynamicRangeLabels.forEach { label ->
                    SelectorChip(label.uppercase(), selected = state.selectedDynamicRange == label, enabled = !running) {
                        viewModel.selectDynamicRange(label)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (running) PanelHigh else Gold)
                    .clickable(enabled = !running) { viewModel.capture() }
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (running) {
                    CircularProgressIndicator(color = Gold, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(10.dp))
                }
                Text(
                    text = if (running) state.phase.ifBlank { "Writing..." } else "WRITE PRIORITY + DR, READ BACK",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.1.sp,
                    color = if (running) TextMuted else Bg,
                )
            }

            state.error?.let { message ->
                Spacer(Modifier.height(14.dp))
                Text(
                    text = message,
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

            state.results.asReversed().forEach { result ->
                Spacer(Modifier.height(16.dp))
                WriteResultCard(result)
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SelectorBlock(label: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PanelLow)
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Text(
            text = label,
            fontFamily = MonoFamily,
            fontSize = 9.sp,
            letterSpacing = 1.4.sp,
            color = TextDim,
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}

@Composable
private fun SelectorChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        fontFamily = MonoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        letterSpacing = 0.7.sp,
        color = when {
            selected -> Bg
            enabled -> TextPrimary
            else -> TextDim
        },
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) Gold else PanelHigh)
            .border(1.dp, if (selected) Gold else Border, RoundedCornerShape(999.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    )
}

@Composable
private fun BenchTextButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Text(
        text = label,
        fontFamily = SansFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 10.5.sp,
        letterSpacing = 1.3.sp,
        color = if (enabled) TextMuted else TextDim.copy(alpha = 0.5f),
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 8.dp),
    )
}

@Composable
private fun WriteResultCard(result: DrPriorityWriteResult) {
    val priorityAfterRaw = result.priorityAfterWrite.u16Value()
    val priorityFinalRaw = result.priorityAfterDynamicRangeWrite.u16Value()
    val dynamicRangeAfterRaw = result.dynamicRangeAfterWrite.u16Value()
    val priorityConfirmed = result.priorityWriteOk && priorityAfterRaw == result.priorityTargetRaw
    val priorityHeld = priorityFinalRaw == result.priorityTargetRaw
    val dynamicRangeAccepted = result.dynamicRangeWriteOk && dynamicRangeAfterRaw == result.dynamicRangeTargetRaw
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PanelLow)
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "${result.slot.label} · ${result.priorityTargetLabel.uppercase()} + ${result.dynamicRangeTargetLabel.uppercase()}",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary,
                )
                Text(
                    text = "${result.presetName} · priority ${result.priorityTargetRaw} (${result.priorityTargetRaw.hex4()}) · DR ${result.dynamicRangeTargetRaw}",
                    fontFamily = SansFamily,
                    fontSize = 10.5.sp,
                    color = TextDim,
                )
            }
            Text(
                text = when {
                    priorityConfirmed && dynamicRangeAccepted -> "DR READS BACK"
                    priorityConfirmed && !dynamicRangeAccepted -> "DR OVERRIDDEN"
                    else -> "CHECK"
                },
                fontFamily = MonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                color = if (priorityConfirmed) Gold else TextMuted,
            )
        }

        ResultRow("PRIORITY BEFORE", result.priorityBefore, RawValueKind.Priority)
        ResultLine(
            label = "PRIORITY WRITE",
            value = "resp=${result.priorityWriteResponse.hex4()} · raw=${result.priorityTargetRaw} · ${result.priorityTargetRaw.toDrPriorityLabel()}",
            highlighted = result.priorityWriteOk,
        )
        ResultRow("PRIORITY AFTER", result.priorityAfterWrite, RawValueKind.Priority)
        ResultRow("DR BEFORE", result.dynamicRangeBefore, RawValueKind.DynamicRange)
        ResultLine(
            label = "DR WRITE",
            value = "resp=${result.dynamicRangeWriteResponse.hex4()} · raw=${result.dynamicRangeTargetRaw} · ${result.dynamicRangeTargetRaw.toDynamicRangeLabel()}",
            highlighted = result.dynamicRangeWriteOk,
        )
        ResultRow("DR AFTER", result.dynamicRangeAfterWrite, RawValueKind.DynamicRange)
        ResultRow("PRIORITY FINAL", result.priorityAfterDynamicRangeWrite, RawValueKind.Priority)
        ResultLine(
            label = "INTERPRET",
            value = when {
                !priorityConfirmed -> "Priority did not read back as target; DR override test is inconclusive."
                !priorityHeld -> "DR write changed priority state; check camera UI against final priority readback."
                dynamicRangeAccepted -> "Camera accepted and read back the DR write while DR Priority was active."
                result.dynamicRangeWriteOk -> "Camera accepted the DR command response, but DR readback did not match target."
                else -> "Camera rejected the DR command while DR Priority was active."
            },
            highlighted = priorityConfirmed,
        )
    }
}

private enum class RawValueKind {
    Priority,
    DynamicRange,
}

@Composable
private fun ResultRow(
    label: String,
    sample: RawPropertySample,
    kind: RawValueKind,
) {
    val raw = sample.u16Value()
    val decoded = when (kind) {
        RawValueKind.Priority -> raw?.toDrPriorityLabel()
        RawValueKind.DynamicRange -> raw?.toDynamicRangeLabel()
    }
    ResultLine(
        label = label,
        value = "resp=${sample.response.hex4()} · ${sample.decoded} · ${decoded ?: "Unknown"} · raw=${sample.rawHex}",
        highlighted = sample.ok,
    )
}

@Composable
private fun ResultLine(
    label: String,
    value: String,
    highlighted: Boolean,
) {
    Text(
        text = "$label  $value",
        fontFamily = MonoFamily,
        fontSize = 10.sp,
        lineHeight = 15.sp,
        color = if (highlighted) TextPrimary else Gold,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PanelHigh)
            .padding(9.dp),
    )
}

private fun Int.hex4(): String =
    "0x${toString(16).uppercase().padStart(4, '0')}"

private fun String.toDrPriorityRaw(): Int = when (this) {
    "Weak" -> 1
    "Strong" -> 2
    "Auto" -> 32768
    else -> 0
}

private fun Int.toDrPriorityLabel(): String = when (this) {
    0 -> "Off"
    1 -> "Weak"
    2 -> "Strong"
    32768 -> "Auto"
    else -> "Unknown"
}

private fun String.toDynamicRangeRaw(): Int = when (this) {
    "DR100%" -> 100
    "DR200%" -> 200
    "DR400%" -> 400
    else -> 0
}

private fun Int.toDynamicRangeLabel(): String = when (this) {
    0 -> "DR Auto"
    100 -> "DR100%"
    200 -> "DR200%"
    400 -> "DR400%"
    else -> "Unknown"
}

private fun RawPropertySample.u16Value(): Int? =
    rawHex.takeUnless { it == "<empty>" || it == "-" }
        ?.substringBefore(" …")
        ?.split(" ")
        ?.filter { it.length == 2 }
        ?.takeIf { it.size >= 2 }
        ?.let { bytes ->
            (bytes[0].toInt(16) and 0xFF) or ((bytes[1].toInt(16) and 0xFF) shl 8)
        }
