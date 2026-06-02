package com.paeki.fujirecipes.ui

import androidx.activity.compose.BackHandler
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
import com.paeki.fujirecipes.ui.components.IconClose
import com.paeki.fujirecipes.ui.components.PrimaryCTA
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
fun ImportFromScreenshotGuide(
    onClose: () -> Unit,
    onChooseScreenshot: () -> Unit,
) {
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
                    text = "IMPORT FROM SCREENSHOT",
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

            // ── Hero text ──────────────────────────────────────────
            Text(
                text = "Read recipe\nsettings from\nany screenshot.",
                fontFamily = SansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 34.sp,
                lineHeight = 38.sp,
                letterSpacing = 0.2.sp,
                color = TextPrimary,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Pick a screenshot of any Fujifilm recipe — from a recipe website, another app, or your camera's menu — and this app will read the parameter labels directly from the image using on-device text recognition. No internet required.",
                fontFamily = SansFamily,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = TextMuted,
            )

            Spacer(Modifier.height(28.dp))

            // ── Steps ──────────────────────────────────────────────
            Text(
                text = "WHAT WORKS BEST",
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
                OcrGuideRow(
                    number = "01",
                    title = "CLEAR, READABLE TEXT",
                    body = "Screenshots work better than photos of a screen. If you're photographing a monitor or LCD, shoot straight-on with the room lights off.",
                )
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Border))
                OcrGuideRow(
                    number = "02",
                    title = "LABELLED PARAMETERS",
                    body = "The image needs to show the parameter name next to its value — \"Grain Effect: Weak, Small\" not just \"Weak, Small\". Recipe app screenshots and camera menu shots both work.",
                )
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Border))
                OcrGuideRow(
                    number = "03",
                    title = "REVIEW BEFORE SAVING",
                    body = "The app flags any settings it couldn't confidently read. Always check the editor before saving — especially grain size and white balance shifts.",
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Unrecognised fields are left at their defaults and noted in the recipe description so you know what to fill in manually.",
                fontFamily = SansFamily,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = TextDim,
                modifier = Modifier.padding(horizontal = 2.dp),
            )

            Spacer(Modifier.height(32.dp))
        }

        // ── CTA ────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .navigationBarsPadding(),
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Border))
            Spacer(Modifier.height(12.dp))
            PrimaryCTA(
                label = "Choose Screenshot",
                onClick = onChooseScreenshot,
            )
        }
    }
}

@Composable
private fun OcrGuideRow(number: String, title: String, body: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
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
