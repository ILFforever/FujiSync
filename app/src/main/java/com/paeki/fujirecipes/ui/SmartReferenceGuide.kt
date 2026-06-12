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
fun SmartReferenceGuide(
    onClose: () -> Unit,
    onChoosePhoto: () -> Unit,
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
                    text = "SMART REFERENCE",
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
                text = "Tag a JPEG to a\nrecipe already\nin your library.",
                fontFamily = SansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 34.sp,
                lineHeight = 38.sp,
                letterSpacing = 0.2.sp,
                color = TextPrimary,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Pick any JPEG shot on your X-series. The app reads the embedded recipe settings, finds the closest match in your library, and adds that photo as a reference image — no new recipe created.",
                fontFamily = SansFamily,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = TextMuted,
            )

            Spacer(Modifier.height(28.dp))

            // ── How it works ───────────────────────────────────────
            Text(
                text = "HOW IT WORKS",
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
                SmartRefStepRow(
                    number = "01",
                    title = "CHOOSE A JPEG",
                    body = "Pick any original JPEG from an X-series camera — a portrait, a landscape, even a test shot. The recipe is embedded in the file.",
                )
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Border))
                SmartRefStepRow(
                    number = "02",
                    title = "SETTINGS ARE MATCHED",
                    body = "The app extracts the film simulation and recipe settings from the EXIF data and finds the closest recipe in your library.",
                )
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Border))
                SmartRefStepRow(
                    number = "03",
                    title = "CONFIRM & ATTACH",
                    body = "You'll see which recipe was matched before anything is saved. Confirm to add the photo as a reference image on that recipe.",
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Works best with photos shot using the exact recipe you saved. Edited JPEGs or screenshots won't contain recipe data.",
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
                label = "Choose Photo",
                onClick = onChoosePhoto,
            )
        }
    }
}

@Composable
private fun SmartRefStepRow(number: String, title: String, body: String) {
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
