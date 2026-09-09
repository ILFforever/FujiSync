package com.ilfforever.fujisync.ui.dev

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.ilfforever.fujisync.data.debug.AppLogReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * This app's own recent logcat, captured without adb — reads the log this app is already
 * restricted to seeing (its own UID's lines), so nothing beyond default app permissions is
 * needed. Useful for capturing a connect-time failure (e.g. a DeviceInfo parse error) to share
 * later, when re-running with adb attached isn't practical.
 */
@Composable
fun AppLogScreen(onClose: () -> Unit) {
    var log by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        log = withContext(Dispatchers.IO) { AppLogReader.readRecent() }
        loading = false
    }

    PtpLogScreen(
        log = if (loading) "" else log,
        onClose = onClose,
        title = "DEBUG LOG",
        emptyHint = if (loading) "(reading device log…)" else "(no log entries captured)",
    )
}
