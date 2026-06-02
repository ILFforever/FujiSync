package com.paeki.fujirecipes.ui.profile

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paeki.fujirecipes.ui.components.SectionLabel
import com.paeki.fujirecipes.ui.model.AppSettings
import com.paeki.fujirecipes.ui.theme.Bg
import com.paeki.fujirecipes.ui.theme.Border
import com.paeki.fujirecipes.ui.theme.Gold
import com.paeki.fujirecipes.ui.theme.MonoFamily
import com.paeki.fujirecipes.ui.theme.PanelLow
import com.paeki.fujirecipes.ui.theme.SansFamily
import com.paeki.fujirecipes.ui.theme.TextDim
import com.paeki.fujirecipes.ui.theme.TextMuted
import com.paeki.fujirecipes.ui.theme.TextPrimary

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onToggleLibraryShowImages: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Header
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
                text = "SETTINGS",
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

            // ── Library ───────────────────────────────────────────────
            SectionLabel(text = "Library")
            Spacer(Modifier.height(8.dp))
            SettingsGroup {
                SettingsToggleRow(
                    label = "Show recipe images",
                    description = "Display the first reference photo on each library card",
                    enabled = settings.showLibraryImages,
                    onClick = onToggleLibraryShowImages,
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PanelLow)
            .border(1.dp, Border, RoundedCornerShape(14.dp)),
    ) {
        content()
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontFamily = SansFamily,
                fontSize = 14.5.sp,
                color = TextPrimary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                fontFamily = SansFamily,
                fontSize = 12.sp,
                color = TextDim,
                lineHeight = 16.sp,
            )
        }
        // Pill toggle
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(if (enabled) Gold.copy(alpha = 0.18f) else Bg)
                .border(1.dp, if (enabled) Gold.copy(alpha = 0.6f) else Border, RoundedCornerShape(999.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (enabled) "ON" else "OFF",
                fontFamily = MonoFamily,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                color = if (enabled) Gold else TextMuted,
            )
        }
    }
}
