package com.ilfforever.fujirecipes.ui

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujirecipes.ui.components.IconClose
import com.ilfforever.fujirecipes.ui.components.PrimaryCTA
import com.ilfforever.fujirecipes.ui.haptics.FujiHapticEffect
import com.ilfforever.fujirecipes.ui.haptics.FujiHaptics
import com.ilfforever.fujirecipes.ui.theme.Bg
import com.ilfforever.fujirecipes.ui.theme.Border
import com.ilfforever.fujirecipes.ui.theme.Gold
import com.ilfforever.fujirecipes.ui.theme.MonoFamily
import com.ilfforever.fujirecipes.ui.theme.PanelLow
import com.ilfforever.fujirecipes.ui.theme.SansFamily
import com.ilfforever.fujirecipes.ui.theme.TextDim
import com.ilfforever.fujirecipes.ui.theme.TextMuted
import com.ilfforever.fujirecipes.ui.theme.TextPrimary

@Composable
fun ImportFromPhotoGuide(
    onClose: () -> Unit,
    onChoosePhoto: () -> Unit,
) {
    BackHandler(onBack = onClose)
    val context = LocalContext.current
    val view = LocalView.current

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
                    text = "IMPORT FROM PHOTO",
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
                text = "Read recipe\nsettings from\na JPEG.",
                fontFamily = SansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 34.sp,
                lineHeight = 38.sp,
                letterSpacing = 0.2.sp,
                color = TextPrimary,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Fujifilm X-series cameras embed the full recipe — film simulation, grain, colour, tone, sharpness — into every JPEG they produce. Choose one of those files and this app will read those settings out and save them to your library as a ready-to-use recipe.",
                fontFamily = SansFamily,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = TextMuted,
            )

            Spacer(Modifier.height(28.dp))

            // ── Steps ──────────────────────────────────────────────
            Text(
                text = "WHAT YOU NEED",
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
                GuideStepRow(
                    number = "01",
                    title = "A JPEG from your X-series",
                    body = "Any photo shot with an X-series camera will work — even a blank frame. The settings are embedded in every file the camera produces.",
                )
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Border))
                GuideStepRow(
                    number = "02",
                    title = "The original file",
                    body = "Use the JPEG straight from the camera or SD card. Most photo editors strip Fujifilm's recipe data when they re-export.",
                )
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Border))
                GuideStepRow(
                    number = "03",
                    title = "Transferred to this phone",
                    body = "Copy the file via USB, SD card reader, or share it from your camera roll if you already imported it.",
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Caveat note ────────────────────────────────────────
            Text(
                text = "RAF (raw) files are not supported — JPEGs only.",
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
                onClick = { FujiHaptics.perform(context, view, FujiHapticEffect.SoftConfirm); onChoosePhoto() },
            )
        }
    }
}

@Composable
private fun GuideStepRow(number: String, title: String, body: String) {
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
