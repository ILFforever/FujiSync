package com.ilfforever.fujisync.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujisync.ui.components.IconClose
import com.ilfforever.fujisync.ui.theme.Bg
import com.ilfforever.fujisync.ui.theme.Border
import com.ilfforever.fujisync.ui.theme.Gold
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.PanelLow
import com.ilfforever.fujisync.ui.theme.SansFamily
import com.ilfforever.fujisync.ui.theme.TextDim
import com.ilfforever.fujisync.ui.theme.TextMuted
import com.ilfforever.fujisync.ui.theme.TextPrimary

@Composable
fun ScanTileGuide(onClose: () -> Unit) {
    BackHandler(onBack = onClose)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding(),
    ) {
        // ── Header ─────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onClose)
                    .padding(horizontal = 4.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(IconClose, contentDescription = "Close", tint = TextPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "SCAN RECIPE TILE",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    letterSpacing = 1.6.sp,
                    color = TextPrimary,
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Hero ───────────────────────────────────────────────
            Text(
                text = "Scan any recipe,\nwithout leaving\nthe app you're in.",
                fontFamily = SansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 34.sp,
                lineHeight = 38.sp,
                letterSpacing = 0.2.sp,
                color = TextPrimary,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Add the Scan Recipe tile to your notification shade. Tap it while viewing any recipe — a website, another app, or your camera's settings screen — and FujiSync captures and reads it on the spot. No switching apps, no file picker.",
                fontFamily = SansFamily,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = TextMuted,
            )

            Spacer(Modifier.height(28.dp))

            // ── Steps ──────────────────────────────────────────────
            Text(
                text = "HOW TO SET IT UP",
                fontFamily = MonoFamily,
                fontSize = 10.sp,
                letterSpacing = 2.4.sp,
                color = TextDim,
            )
            Spacer(Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(PanelLow)
                    .border(1.dp, Border, RoundedCornerShape(16.dp)),
            ) {
                TileGuideRow(
                    number = "01",
                    title = "ADD THE TILE",
                    body = "Pull down your notification shade and tap the edit icon. Find \"Scan Recipe\" in the available tiles and drag it into your active panel.",
                )
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Border))
                TileGuideRow(
                    number = "02",
                    title = "NAVIGATE TO A RECIPE",
                    body = "Open any recipe in a browser, another app, or browse your camera's custom settings menu. The full screen is captured — no cropping needed.",
                )
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Border))
                TileGuideRow(
                    number = "03",
                    title = "TAP THE TILE",
                    body = "Pull down the shade and tap Scan Recipe. Android will ask permission to capture your screen — allow it. The permission prompt only appears once per session.",
                )
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Border))
                TileGuideRow(
                    number = "04",
                    title = "REVIEW AND SAVE",
                    body = "FujiSync opens straight to the recipe editor with the scanned settings pre-filled. Any fields it couldn't read are flagged in the description.",
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "The tile uses on-device text recognition — no internet required. It works with any legible screenshot, website, or menu screen.",
                fontFamily = SansFamily,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = TextDim,
                modifier = Modifier.padding(horizontal = 2.dp),
            )

            Spacer(Modifier.height(32.dp))
        }

        // ── CTA ────────────────────────────────────────────────────
    }
}

@Composable
private fun TileGuideRow(number: String, title: String, body: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp, vertical = 16.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(13.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = number,
            fontFamily = MonoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 1.4.sp,
            color = Gold,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = SansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.2.sp,
                color = TextPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = body,
                fontFamily = SansFamily,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = TextMuted,
            )
        }
    }
}
