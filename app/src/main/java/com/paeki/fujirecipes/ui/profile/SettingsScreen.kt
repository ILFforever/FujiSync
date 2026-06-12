package com.paeki.fujirecipes.ui.profile

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paeki.fujirecipes.ui.components.SectionLabel
import com.paeki.fujirecipes.ui.model.AppSettings
import com.paeki.fujirecipes.ui.theme.Bg
import com.paeki.fujirecipes.ui.theme.Border
import com.paeki.fujirecipes.ui.theme.BorderStrong
import com.paeki.fujirecipes.ui.theme.Gold
import com.paeki.fujirecipes.ui.theme.GoldDim
import com.paeki.fujirecipes.ui.theme.PanelLow
import com.paeki.fujirecipes.ui.theme.SansFamily
import com.paeki.fujirecipes.ui.theme.TextDim
import com.paeki.fujirecipes.ui.theme.TextPrimary

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onToggleLibraryShowImages: () -> Unit,
    onToggleReferenceImageBlur: () -> Unit = {},
    onToggleFavoritesOnTop: () -> Unit = {},
    onToggleHaptics: () -> Unit = {},
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

            // ── Feedback ──────────────────────────────────────────────
            SectionLabel(text = "Feedback")
            Spacer(Modifier.height(8.dp))
            SettingsGroup {
                SettingsToggleRow(
                    label = "Haptic feedback",
                    description = "Vibrate on taps, drags, and camera events",
                    enabled = settings.hapticsEnabled,
                    onClick = onToggleHaptics,
                )
            }

            Spacer(Modifier.height(24.dp))

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
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Border))
                SettingsToggleRow(
                    label = "Blur on image expand",
                    description = "Show a blurred preview while the full image loads in the lightbox",
                    enabled = settings.showReferenceImageBlur,
                    onClick = onToggleReferenceImageBlur,
                )
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Border))
                SettingsToggleRow(
                    label = "Favorites on top",
                    description = "Pin favorited recipes to the top of the library list",
                    enabled = settings.favoritesOnTop,
                    onClick = onToggleFavoritesOnTop,
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
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
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
        Spacer(Modifier.width(12.dp))
        FujiToggle(enabled = enabled, onClick = onClick)
    }
}

// Track: 48×26dp  |  Thumb: 20dp circle  |  travel: 22dp
private val TrackW = 48.dp
private val TrackH = 26.dp
private val ThumbD = 20.dp
private val ThumbTravel = TrackW - ThumbD - 6.dp // 3dp pad each side → 22dp

@Composable
private fun FujiToggle(enabled: Boolean, onClick: () -> Unit) {
    val thumbProgress by animateFloatAsState(
        targetValue = if (enabled) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "toggle",
    )

    Box(
        modifier = Modifier
            .size(width = TrackW, height = TrackH)
            .clip(RoundedCornerShape(999.dp))
            .background(if (enabled) GoldDim else Bg)
            .border(
                width = 1.dp,
                color = if (enabled) Gold.copy(alpha = 0.55f) else BorderStrong,
                shape = RoundedCornerShape(999.dp),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .padding(start = 3.dp)
                .offset(x = ThumbTravel * thumbProgress)
                .size(ThumbD)
                .clip(CircleShape)
                .background(
                    if (enabled) Gold
                    else Gold.copy(alpha = 0.25f),
                )
                .graphicsLayer {
                    // subtle scale-in on enable for snap feel
                    scaleX = 0.88f + 0.12f * thumbProgress
                    scaleY = 0.88f + 0.12f * thumbProgress
                },
        )
    }
}
