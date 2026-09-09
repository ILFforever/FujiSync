package com.ilfforever.fujisync.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.ilfforever.fujisync.BuildConfig
import com.ilfforever.fujisync.ui.dev.DrPriorityBenchScreen
import com.ilfforever.fujisync.ui.dev.DrPriorityBenchViewModel
import com.ilfforever.fujisync.ui.dev.ExifBenchScreen
import com.ilfforever.fujisync.ui.dev.FxwSearchBenchScreen
import com.ilfforever.fujisync.ui.dev.HapticBenchScreen
import com.ilfforever.fujisync.ui.dev.NameBenchScreen
import com.ilfforever.fujisync.ui.dev.NameBenchViewModel
import com.ilfforever.fujisync.ui.dev.CapabilityBenchScreen
import com.ilfforever.fujisync.ui.dev.MonoToneBenchScreen
import com.ilfforever.fujisync.ui.dev.MonoToneBenchViewModel
import com.ilfforever.fujisync.ui.dev.CapabilityBenchViewModel
import com.ilfforever.fujisync.ui.dev.LiveSettingsBenchScreen
import com.ilfforever.fujisync.ui.dev.LiveSettingsBenchViewModel
import com.ilfforever.fujisync.ui.dev.NewPathsProbeScreen
import com.ilfforever.fujisync.ui.dev.NewPathsProbeViewModel
import com.ilfforever.fujisync.ui.dev.AppLogScreen
import com.ilfforever.fujisync.ui.dev.PtpLogScreen
import com.ilfforever.fujisync.ui.dev.ReadSlotsBenchScreen
import com.ilfforever.fujisync.ui.dev.ReadSlotsBenchViewModel
import com.ilfforever.fujisync.ui.dev.UsbReadWriteBenchScreen
import com.ilfforever.fujisync.ui.dev.UsbReadWriteBenchViewModel
import com.ilfforever.fujisync.ui.dev.WriteDelayBenchScreen
import com.ilfforever.fujisync.ui.dev.WriteDelayBenchViewModel
import com.ilfforever.fujisync.ui.theme.Bg

// Dev-only diagnostic bench overlays (USB read/write bench, timing benches, name/exif/search
// benches, the PTP log, and the X RAW STUDIO-derived new-paths probe). Extracted out of
// AppOverlays.kt to keep that file's core recipe-detail/toast/guide overlay logic readable —
// this cluster is purely "if show*Bench, render *BenchScreen" repetition and grows every time a
// new dev tool is added.
@Composable
internal fun AppDevBenchOverlays(
    showExifBench: Boolean,
    showFxwSearchBench: Boolean,
    showUsbReadWriteBench: Boolean,
    showWriteDelayBench: Boolean,
    showNameBench: Boolean,
    showReadSlotsBench: Boolean,
    showDrPriorityBench: Boolean,
    showHapticBench: Boolean,
    showPtpLog: Boolean,
    ptpLogText: String,
    showAppLog: Boolean,
    showNewPathsProbe: Boolean,
    showLiveSettingsBench: Boolean,
    showCapabilityBench: Boolean,
    showMonoToneBench: Boolean,
    onExifBenchClose: () -> Unit,
    onFxwSearchBenchClose: () -> Unit,
    onUsbReadWriteBenchClose: () -> Unit,
    onWriteDelayBenchClose: () -> Unit,
    onNameBenchClose: () -> Unit,
    onReadSlotsBenchClose: () -> Unit,
    onDrPriorityBenchClose: () -> Unit,
    onHapticBenchClose: () -> Unit,
    onPtpLogClose: () -> Unit,
    onAppLogClose: () -> Unit,
    onNewPathsProbeClose: () -> Unit,
    onLiveSettingsBenchClose: () -> Unit,
    onCapabilityBenchClose: () -> Unit,
    onMonoToneBenchClose: () -> Unit,
) {
    val usbReadWriteBenchVm: UsbReadWriteBenchViewModel = hiltViewModel()
    val writeDelayBenchVm: WriteDelayBenchViewModel = hiltViewModel()
    val nameBenchVm: NameBenchViewModel = hiltViewModel()
    val readSlotsBenchVm: ReadSlotsBenchViewModel = hiltViewModel()
    val drPriorityBenchVm: DrPriorityBenchViewModel = hiltViewModel()
    val newPathsProbeVm: NewPathsProbeViewModel = hiltViewModel()
    val liveSettingsVm: LiveSettingsBenchViewModel = hiltViewModel()
    val capabilityVm: CapabilityBenchViewModel = hiltViewModel()
    val monoToneVm: MonoToneBenchViewModel = hiltViewModel()

    if (showExifBench) {
        Box(modifier = Modifier.fillMaxSize().background(Bg)) {
            ExifBenchScreen(onClose = onExifBenchClose)
        }
    }

    if (BuildConfig.DISCOVER_ENABLED && showFxwSearchBench) {
        Box(modifier = Modifier.fillMaxSize().background(Bg)) {
            FxwSearchBenchScreen(onClose = onFxwSearchBenchClose)
        }
    }

    if (showUsbReadWriteBench) {
        Box(modifier = Modifier.fillMaxSize().background(Bg)) {
            UsbReadWriteBenchScreen(viewModel = usbReadWriteBenchVm, onClose = onUsbReadWriteBenchClose)
        }
    }

    if (showWriteDelayBench) {
        Box(modifier = Modifier.fillMaxSize().background(Bg)) {
            WriteDelayBenchScreen(viewModel = writeDelayBenchVm, onClose = onWriteDelayBenchClose)
        }
    }

    if (showNameBench) {
        Box(modifier = Modifier.fillMaxSize().background(Bg)) {
            NameBenchScreen(viewModel = nameBenchVm, onClose = onNameBenchClose)
        }
    }

    if (showReadSlotsBench) {
        Box(modifier = Modifier.fillMaxSize().background(Bg)) {
            ReadSlotsBenchScreen(viewModel = readSlotsBenchVm, onClose = onReadSlotsBenchClose)
        }
    }

    if (showDrPriorityBench) {
        Box(modifier = Modifier.fillMaxSize().background(Bg)) {
            DrPriorityBenchScreen(viewModel = drPriorityBenchVm, onClose = onDrPriorityBenchClose)
        }
    }

    if (showHapticBench) {
        Box(modifier = Modifier.fillMaxSize().background(Bg)) {
            HapticBenchScreen(onClose = onHapticBenchClose)
        }
    }

    if (showPtpLog) {
        Box(modifier = Modifier.fillMaxSize().background(Bg)) {
            PtpLogScreen(log = ptpLogText, onClose = onPtpLogClose)
        }
    }

    if (showAppLog) {
        Box(modifier = Modifier.fillMaxSize().background(Bg)) {
            AppLogScreen(onClose = onAppLogClose)
        }
    }

    if (showNewPathsProbe) {
        Box(modifier = Modifier.fillMaxSize().background(Bg)) {
            NewPathsProbeScreen(viewModel = newPathsProbeVm, onClose = onNewPathsProbeClose)
        }
    }

    if (showLiveSettingsBench) {
        Box(modifier = Modifier.fillMaxSize().background(Bg)) {
            LiveSettingsBenchScreen(viewModel = liveSettingsVm, onClose = onLiveSettingsBenchClose)
        }
    }

    if (showCapabilityBench) {
        Box(modifier = Modifier.fillMaxSize().background(Bg)) {
            CapabilityBenchScreen(viewModel = capabilityVm, onClose = onCapabilityBenchClose)
        }
    }

    if (showMonoToneBench) {
        Box(modifier = Modifier.fillMaxSize().background(Bg)) {
            MonoToneBenchScreen(viewModel = monoToneVm, onClose = onMonoToneBenchClose)
        }
    }
}
