package com.paeki.fujirecipes.ui.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paeki.fujirecipes.BuildConfig
import com.paeki.fujirecipes.ui.UpdateUiState
import com.paeki.fujirecipes.ui.components.Wordmark
import com.paeki.fujirecipes.ui.theme.Bg
import com.paeki.fujirecipes.ui.theme.Border
import com.paeki.fujirecipes.ui.theme.Gold
import com.paeki.fujirecipes.ui.theme.MonoFamily
import com.paeki.fujirecipes.ui.theme.PanelLow
import com.paeki.fujirecipes.ui.theme.SansFamily
import com.paeki.fujirecipes.ui.theme.TextDim
import com.paeki.fujirecipes.ui.theme.TextMuted
import com.paeki.fujirecipes.ui.theme.TextPrimary

private const val DEVELOPER = "ILFforever"
private const val REPO_URL = "https://github.com/ILFforever/FujiSync"
private const val DEVELOPER_URL = "https://github.com/ILFforever"

private val MIT_LICENSE = """
MIT License

Copyright (c) 2026 FujiSync Contributors

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
""".trimIndent()

@Composable
fun AboutScreen(
    update: UpdateUiState = UpdateUiState(),
    onCheckForUpdates: () -> Unit = {},
    onInstallUpdate: () -> Unit = {},
    onBack: () -> Unit,
) {
    var licenseOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "‹",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Gold,
                    modifier = Modifier
                        .clickable(onClick = onBack)
                        .padding(end = 12.dp, top = 2.dp, bottom = 2.dp),
                )
                Text(
                    text = "ABOUT",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    letterSpacing = 0.4.sp,
                    color = TextPrimary,
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(Modifier.height(8.dp))

                // App identity block
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(PanelLow)
                        .border(1.dp, Border, RoundedCornerShape(14.dp))
                        .padding(18.dp),
                ) {
                    Wordmark()
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Manage Fujifilm X-series film simulation recipes from your phone over USB-C OTG.",
                        fontFamily = SansFamily,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = TextMuted,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AboutChip(label = "VERSION", value = BuildConfig.VERSION_NAME)
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Updates section
                Text(
                    text = "UPDATES",
                    fontFamily = MonoFamily,
                    fontSize = 10.sp,
                    letterSpacing = 2.sp,
                    color = TextDim,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(PanelLow)
                        .border(1.dp, Border, RoundedCornerShape(14.dp))
                        .padding(16.dp),
                ) {
                    Text(
                        text = updateStatusText(update),
                        fontFamily = SansFamily,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = if (update.error != null) Gold else TextMuted,
                    )
                    update.assetName?.let { asset ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = asset,
                            fontFamily = MonoFamily,
                            fontSize = 10.sp,
                            lineHeight = 15.sp,
                            color = TextDim,
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UpdateAction(
                            label = updateActionLabel(update),
                            enabled = !update.checking && !update.downloading,
                            onClick = if (update.updateAvailable || update.downloaded) onInstallUpdate else onCheckForUpdates,
                        )
                        if (update.updateAvailable || update.error != null) {
                            Spacer(Modifier.width(10.dp))
                            UpdateAction(
                                label = "CHECK",
                                enabled = !update.checking && !update.downloading,
                                secondary = true,
                                onClick = onCheckForUpdates,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Links section
                Text(
                    text = "LINKS",
                    fontFamily = MonoFamily,
                    fontSize = 10.sp,
                    letterSpacing = 2.sp,
                    color = TextDim,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(PanelLow)
                        .border(1.dp, Border, RoundedCornerShape(14.dp)),
                ) {
                    AboutLinkRow(label = "Developer", value = DEVELOPER, onClick = { openUrl(DEVELOPER_URL) })
                    ProfileDivider()
                    AboutLinkRow(label = "Repository", value = "github.com/ILFforever/FujiSync", onClick = { openUrl(REPO_URL) })
                }

                Spacer(Modifier.height(24.dp))

                // Legal section
                Text(
                    text = "LEGAL",
                    fontFamily = MonoFamily,
                    fontSize = 10.sp,
                    letterSpacing = 2.sp,
                    color = TextDim,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(PanelLow)
                        .border(1.dp, Border, RoundedCornerShape(14.dp)),
                ) {
                    ProfileNavRow(label = "License", onClick = { licenseOpen = true }, inCard = true)
                }

                Spacer(Modifier.height(40.dp))
            }
        }

        AnimatedVisibility(
            visible = licenseOpen,
            enter = fadeIn(tween(180, easing = FastOutSlowInEasing)) +
                    slideInVertically(tween(340, easing = FastOutSlowInEasing)) { it },
            exit = fadeOut(tween(200, easing = FastOutSlowInEasing)) +
                   slideOutVertically(tween(260, easing = FastOutSlowInEasing)) { it },
        ) {
            LicenseScreen(onBack = { licenseOpen = false })
        }
    }
}

@Composable
private fun LicenseScreen(onBack: () -> Unit) {
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
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "‹",
                fontFamily = SansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Gold,
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .padding(end = 12.dp, top = 2.dp, bottom = 2.dp),
            )
            Text(
                text = "LICENSE",
                fontFamily = SansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                letterSpacing = 0.4.sp,
                color = TextPrimary,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(PanelLow)
                    .border(1.dp, Border, RoundedCornerShape(14.dp))
                    .padding(16.dp),
            ) {
                Text(
                    text = "MIT",
                    fontFamily = MonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.6.sp,
                    color = Gold,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                Text(
                    text = MIT_LICENSE,
                    fontFamily = MonoFamily,
                    fontSize = 11.sp,
                    lineHeight = 17.sp,
                    color = TextDim,
                )
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun AboutLinkRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontFamily = SansFamily,
            fontSize = 14.5.sp,
            color = TextPrimary,
        )
        Text(
            text = value,
            fontFamily = MonoFamily,
            fontSize = 11.sp,
            letterSpacing = 0.2.sp,
            color = Gold,
        )
    }
}

@Composable
private fun AboutChip(label: String, value: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Gold.copy(alpha = 0.10f))
            .border(1.dp, Gold.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Row {
            Text(
                text = "$label  ",
                fontFamily = MonoFamily,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                color = TextDim,
            )
            Text(
                text = value,
                fontFamily = MonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                color = Gold,
            )
        }
    }
}

private fun updateStatusText(update: UpdateUiState): String =
    when {
        update.checking -> "Checking GitHub releases..."
        update.downloading -> update.message ?: "Downloading update..."
        update.error != null -> update.error
        update.installPermissionRequired -> update.message ?: "Install permission is required."
        update.updateAvailable -> update.message ?: "Update ${update.latestVersion.orEmpty()} is available."
        update.message != null -> update.message
        else -> "Install updates from GitHub Releases when a newer APK is published."
    }

private fun updateActionLabel(update: UpdateUiState): String =
    when {
        update.checking -> "CHECKING"
        update.downloading -> "DOWNLOADING"
        update.updateAvailable || update.downloaded -> "INSTALL UPDATE"
        else -> "CHECK FOR UPDATES"
    }

@Composable
private fun UpdateAction(
    label: String,
    enabled: Boolean,
    secondary: Boolean = false,
    onClick: () -> Unit,
) {
    val borderColor = if (secondary) Border else Gold.copy(alpha = if (enabled) 0.55f else 0.22f)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (secondary) PanelLow else Gold.copy(alpha = if (enabled) 0.16f else 0.07f))
            .border(1.dp, borderColor, RoundedCornerShape(999.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            fontFamily = MonoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 1.2.sp,
            color = if (enabled) Gold else TextDim,
        )
    }
}
