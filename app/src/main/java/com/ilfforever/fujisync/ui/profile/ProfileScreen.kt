package com.ilfforever.fujisync.ui.profile

import com.ilfforever.fujisync.ui.overlay.OverlayLayer
import com.ilfforever.fujisync.ui.overlay.BackHandler as OverlayBackHandler
import com.ilfforever.fujisync.ui.overlay.overlayStackOf
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujisync.BuildConfig
import com.ilfforever.fujisync.ui.adaptive.WideContentWidth
import com.ilfforever.fujisync.ui.adaptive.tabletContentWidth
import com.ilfforever.fujisync.ui.DisclaimerScreen
import com.ilfforever.fujisync.ui.BackupUiState
import com.ilfforever.fujisync.ui.UpdateUiState
import com.ilfforever.fujisync.ui.components.SectionLabel
import com.ilfforever.fujisync.ui.components.Wordmark
import com.ilfforever.fujisync.ui.haptics.FujiHapticEffect
import com.ilfforever.fujisync.ui.haptics.FujiHaptics
import com.ilfforever.fujisync.ui.model.AppSettings
import com.ilfforever.fujisync.ui.theme.Bg
import com.ilfforever.fujisync.ui.theme.Border
import com.ilfforever.fujisync.ui.theme.PanelLow
import com.ilfforever.fujisync.ui.theme.SansFamily
import com.ilfforever.fujisync.ui.theme.TextPrimary

@Composable
fun ProfileScreen(
    cameraLabels: Map<String, String> = emptyMap(),
    cameraModels: Map<String, String> = emptyMap(),
    cameraFirmwares: Map<String, String> = emptyMap(),
    activeCameraSerial: String = "",
    activeCameraModel: String = "",
    onRenameCameraLabel: (serial: String, name: String) -> Unit = { _, _ -> },
    onDeleteCamera: (serial: String) -> Unit = {},
    onResetCameraLabel: (serial: String) -> Unit = {},
    settings: AppSettings = AppSettings(),
    onToggleLibraryShowImages: () -> Unit = {},
    onToggleCardImageCount: () -> Unit = {},
    onToggleReferenceImageBlur: () -> Unit = {},
    onToggleFavoritesOnTop: () -> Unit = {},
    onToggleHaptics: () -> Unit = {},
    onOpenCameraImageTuner: () -> Unit = {},
    onLoadSampleLibrary: () -> Unit = {},
    onExploreDemo: () -> Unit = {},
    onOpenExifBench: () -> Unit = {},
    onOpenFxwSearchBench: () -> Unit = {},
    onOpenUsbReadWriteBench: () -> Unit = {},
    onOpenWriteDelayBench: () -> Unit = {},
    onOpenNameBench: () -> Unit = {},
    onOpenReadSlotsBench: () -> Unit = {},
    onOpenDrPriorityBench: () -> Unit = {},
    onOpenHapticBench: () -> Unit = {},
    onOpenPtpLog: () -> Unit = {},
    onOpenAppLog: () -> Unit = {},
    onOpenNewPathsProbe: () -> Unit = {},
    onOpenLiveSettingsBench: () -> Unit = {},
    onOpenCapabilityBench: () -> Unit = {},
    onOpenMonoToneBench: () -> Unit = {},
    onAddMockCamera: () -> Unit = {},
    onShowScanLog: () -> Unit = {},
    onSetPropertyWriteDelay: (Long) -> Unit = {},
    update: UpdateUiState = UpdateUiState(),
    onCheckForUpdates: () -> Unit = {},
    onInstallUpdate: () -> Unit = {},
    backup: BackupUiState = BackupUiState(),
    onExportBackup: () -> Unit = {},
    onImportBackupMerge: () -> Unit = {},
    onImportBackupReplace: () -> Unit = {},
    onDismissBackupMessage: () -> Unit = {},
    onShutterCheck: () -> Unit = {},
    onSmartRef: () -> Unit = {},
    onSetSmartRefSimilarityPct: (Int) -> Unit = {},
    onSetMaxReferenceImages: (Int) -> Unit = {},
) {
    val context = LocalContext.current
    val view = LocalView.current
    fun haptic() = FujiHaptics.perform(context, view, FujiHapticEffect.SoftConfirm)

    var settingsOpen by remember { mutableStateOf(false) }
    var aboutOpen by remember { mutableStateOf(false) }
    var myCamerasOpen by remember { mutableStateOf(false) }
    var backupOpen by remember { mutableStateOf(false) }
    var devToolsOpen by remember { mutableStateOf(false) }
    var disclaimerOpen by remember { mutableStateOf(false) }
    var supportOpen by remember { mutableStateOf(false) }

    overlayStackOf(
        OverlayLayer(settingsOpen) { settingsOpen = false },
        OverlayLayer(aboutOpen) { aboutOpen = false },
        OverlayLayer(myCamerasOpen) { myCamerasOpen = false },
        OverlayLayer(backupOpen) { backupOpen = false },
        OverlayLayer(devToolsOpen) { devToolsOpen = false },
        OverlayLayer(supportOpen) { supportOpen = false },
    ).OverlayBackHandler()

    Box(modifier = Modifier.fillMaxSize().tabletContentWidth(WideContentWidth)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Bg)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Bg)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.height(28.dp), contentAlignment = Alignment.CenterStart) {
                    Wordmark()
                }
            }
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = "PROFILE",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    letterSpacing = 0.4.sp,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 24.dp, top = 8.dp),
                )

                // Account + nav rows
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(PanelLow)
                        .border(1.dp, Border, RoundedCornerShape(14.dp)),
                ) {
                    ProfileNavRow(label = "My Cameras", badge = cameraLabels.size.takeIf { it > 0 }?.toString(), onClick = { haptic(); myCamerasOpen = true }, inCard = true)
                    ProfileDivider()
                    ProfileNavRow(label = "Settings", onClick = { haptic(); settingsOpen = true }, inCard = true)
                    ProfileDivider()
                    ProfileNavRow(label = "Backup & restore", onClick = { haptic(); backupOpen = true }, inCard = true)
                    ProfileDivider()
                    ProfileNavRow(label = "About", onClick = { haptic(); aboutOpen = true }, inCard = true)
                }

                Spacer(Modifier.height(24.dp))

                // ── Tools ──────────────────────────────────────────────────
                SectionLabel(text = "Tools")
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(PanelLow)
                        .border(1.dp, Border, RoundedCornerShape(14.dp)),
                ) {
                    ProfileNavRow(
                        label = "Shutter count",
                        subtitle = "Check actuations from a JPEG",
                        onClick = { haptic(); onShutterCheck() },
                        inCard = true,
                    )
                    ProfileDivider()
                    ProfileNavRow(
                        label = "Smart reference",
                        subtitle = "Tag a JPEG to a library recipe",
                        onClick = { haptic(); onSmartRef() },
                        inCard = true,
                    )
                }

                Spacer(Modifier.height(24.dp))

                // ── Dev ────────────────────────────────────────────────────
                // Play builds hide developer tooling and drop the Buy Me a Coffee link, which
                // empties this section entirely — so the whole block is omitted rather than
                // leaving a labelled empty card.
                if (BuildConfig.DEV_TOOLS_ENABLED || BuildConfig.SUPPORT_LINK_ENABLED) {
                    SectionLabel(text = if (BuildConfig.DEV_TOOLS_ENABLED) "Dev" else "Support")
                    Spacer(Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(PanelLow)
                            .border(1.dp, Border, RoundedCornerShape(14.dp)),
                    ) {
                        if (BuildConfig.DEV_TOOLS_ENABLED) {
                            ProfileNavRow(
                                label = "Developer tools",
                                onClick = { haptic(); devToolsOpen = true },
                                inCard = true,
                            )
                            if (BuildConfig.SUPPORT_LINK_ENABLED) ProfileDivider()
                        }
                        if (BuildConfig.SUPPORT_LINK_ENABLED) {
                            ProfileNavRow(
                                label = "Support the developer",
                                onClick = { haptic(); supportOpen = true },
                                inCard = true,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(40.dp))
            }
        }

        AnimatedVisibility(
            visible = myCamerasOpen,
            enter = fadeIn(tween(180, easing = FastOutSlowInEasing)) +
                    slideInVertically(tween(340, easing = FastOutSlowInEasing)) { it },
            exit = fadeOut(tween(200, easing = FastOutSlowInEasing)) +
                   slideOutVertically(tween(260, easing = FastOutSlowInEasing)) { it },
        ) {
            MyCamerasScreen(
                cameraLabels = cameraLabels,
                cameraModels = cameraModels,
                cameraFirmwares = cameraFirmwares,
                activeCameraSerial = activeCameraSerial,
                activeCameraModel = activeCameraModel,
                onRenameCameraLabel = onRenameCameraLabel,
                onDeleteCamera = onDeleteCamera,
                onResetCameraLabel = onResetCameraLabel,
                onBack = { myCamerasOpen = false },
            )
        }

        AnimatedVisibility(
            visible = settingsOpen,
            enter = fadeIn(tween(180, easing = FastOutSlowInEasing)) +
                    slideInVertically(tween(340, easing = FastOutSlowInEasing)) { it },
            exit = fadeOut(tween(200, easing = FastOutSlowInEasing)) +
                   slideOutVertically(tween(260, easing = FastOutSlowInEasing)) { it },
        ) {
            SettingsScreen(
                settings = settings,
                onToggleLibraryShowImages = onToggleLibraryShowImages,
                onToggleCardImageCount = onToggleCardImageCount,
                onToggleReferenceImageBlur = onToggleReferenceImageBlur,
                onToggleFavoritesOnTop = onToggleFavoritesOnTop,
                onToggleHaptics = onToggleHaptics,
                onSetSmartRefSimilarityPct = onSetSmartRefSimilarityPct,
                onSetMaxReferenceImages = onSetMaxReferenceImages,
                onBack = { settingsOpen = false },
            )
        }

        AnimatedVisibility(
            visible = backupOpen,
            enter = fadeIn(tween(180, easing = FastOutSlowInEasing)) +
                    slideInVertically(tween(340, easing = FastOutSlowInEasing)) { it },
            exit = fadeOut(tween(200, easing = FastOutSlowInEasing)) +
                   slideOutVertically(tween(260, easing = FastOutSlowInEasing)) { it },
        ) {
            BackupRestoreScreen(
                backup = backup,
                onExportBackup = onExportBackup,
                onImportBackupMerge = onImportBackupMerge,
                onImportBackupReplace = onImportBackupReplace,
                onDismissBackupMessage = onDismissBackupMessage,
                onBack = { backupOpen = false },
            )
        }

        AnimatedVisibility(
            visible = aboutOpen,
            enter = fadeIn(tween(180, easing = FastOutSlowInEasing)) +
                    slideInVertically(tween(340, easing = FastOutSlowInEasing)) { it },
            exit = fadeOut(tween(200, easing = FastOutSlowInEasing)) +
                   slideOutVertically(tween(260, easing = FastOutSlowInEasing)) { it },
        ) {
            AboutScreen(
                update = update,
                onCheckForUpdates = onCheckForUpdates,
                onInstallUpdate = onInstallUpdate,
                onBack = { aboutOpen = false },
            )
        }

        // Gated so R8 can prove SupportScreen unreachable in play builds and drop the
        // Buy Me a Coffee URL from the binary entirely — not just from the UI.
        if (BuildConfig.SUPPORT_LINK_ENABLED) {
            AnimatedVisibility(
                visible = supportOpen,
                enter = fadeIn(tween(180, easing = FastOutSlowInEasing)) +
                        slideInVertically(tween(340, easing = FastOutSlowInEasing)) { it },
                exit = fadeOut(tween(200, easing = FastOutSlowInEasing)) +
                       slideOutVertically(tween(260, easing = FastOutSlowInEasing)) { it },
            ) {
                SupportScreen(onBack = { supportOpen = false })
            }
        }

        AnimatedVisibility(
            visible = devToolsOpen,
            enter = fadeIn(tween(180, easing = FastOutSlowInEasing)) +
                    slideInVertically(tween(340, easing = FastOutSlowInEasing)) { it },
            exit = fadeOut(tween(200, easing = FastOutSlowInEasing)) +
                   slideOutVertically(tween(260, easing = FastOutSlowInEasing)) { it },
        ) {
            DevToolsScreen(
                onBack = { devToolsOpen = false },
                onOpenCameraImageTuner = onOpenCameraImageTuner,
                onLoadSampleLibrary = onLoadSampleLibrary,
                onExploreDemo = onExploreDemo,
                onOpenExifBench = onOpenExifBench,
                onOpenFxwSearchBench = onOpenFxwSearchBench,
                onOpenUsbReadWriteBench = onOpenUsbReadWriteBench,
                onOpenWriteDelayBench = onOpenWriteDelayBench,
                onOpenNameBench = onOpenNameBench,
                onOpenReadSlotsBench = onOpenReadSlotsBench,
                onOpenDrPriorityBench = onOpenDrPriorityBench,
                onOpenHapticBench = onOpenHapticBench,
                onOpenPtpLog = onOpenPtpLog,
                onOpenAppLog = onOpenAppLog,
                onOpenNewPathsProbe = onOpenNewPathsProbe,
                onOpenLiveSettingsBench = onOpenLiveSettingsBench,
                onOpenCapabilityBench = onOpenCapabilityBench,
                onOpenMonoToneBench = onOpenMonoToneBench,
                onAddMockCamera = onAddMockCamera,
                onShowScanLog = onShowScanLog,
                onShowDisclaimer = { disclaimerOpen = true },
                propertyWriteDelayMs = settings.propertyWriteDelayMs,
                onSetPropertyWriteDelay = onSetPropertyWriteDelay,
            )
        }

        AnimatedVisibility(
            visible = disclaimerOpen,
            enter = fadeIn(tween(180, easing = FastOutSlowInEasing)) +
                    slideInVertically(tween(340, easing = FastOutSlowInEasing)) { it },
            exit = fadeOut(tween(200, easing = FastOutSlowInEasing)) +
                   slideOutVertically(tween(260, easing = FastOutSlowInEasing)) { it },
        ) {
            DisclaimerScreen(
                onAccept = { disclaimerOpen = false },
                actionLabel = "CLOSE",
            )
        }
    }
}
