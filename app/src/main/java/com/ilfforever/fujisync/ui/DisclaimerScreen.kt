package com.ilfforever.fujisync.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujisync.ui.components.Wordmark
import com.ilfforever.fujisync.ui.theme.Bg
import com.ilfforever.fujisync.ui.theme.Border
import com.ilfforever.fujisync.ui.theme.Gold
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.PanelLow
import com.ilfforever.fujisync.ui.theme.SansFamily
import com.ilfforever.fujisync.ui.theme.TextDim
import com.ilfforever.fujisync.ui.theme.TextMuted
import com.ilfforever.fujisync.ui.theme.TextPrimary
import kotlinx.coroutines.delay

private const val COOLDOWN_SECONDS = 5

@Composable
fun DisclaimerScreen(
    onAccept: () -> Unit,
    actionLabel: String = "I UNDERSTAND — CONTINUE",
    showCooldown: Boolean = actionLabel == "I UNDERSTAND — CONTINUE",
) {
    var secondsLeft by remember { mutableIntStateOf(if (showCooldown) COOLDOWN_SECONDS else 0) }

    if (showCooldown) {
        LaunchedEffect(Unit) {
            while (secondsLeft > 0) {
                delay(1000L)
                secondsLeft--
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(32.dp))

            Wordmark()

            Spacer(Modifier.height(32.dp))

            Text(
                text = "BEFORE YOU CONTINUE",
                fontFamily = MonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 2.sp,
                color = Gold,
            )

            Spacer(Modifier.height(12.dp))

            DisclaimerCard(title = "No Affiliation") {
                "FujiSync is an independent, community-built tool. It is not made, endorsed, " +
                "licensed, or supported by FUJIFILM Corporation or any of its affiliates. " +
                "FUJIFILM Corporation bears no responsibility for this application."
            }

            Spacer(Modifier.height(12.dp))

            DisclaimerCard(title = "Warranty Risk") {
                "Connecting your camera to third-party software may void your Fujifilm " +
                "manufacturer warranty. Fujifilm has indicated this applies to all third-party " +
                "USB camera access, including PTP-based connections. FujiSync accepts no " +
                "responsibility for any effect on your warranty coverage."
            }

            Spacer(Modifier.height(12.dp))

            DisclaimerCard(title = "No Liability") {
                "FujiSync is provided \"as is\" without warranty of any kind. The developer is " +
                "not liable for any damage to your camera, loss of recipes or data, voided " +
                "warranties, failed writes, incorrect settings, or any other direct or indirect " +
                "loss arising from the use of this application. You assume all risk."
            }

            Spacer(Modifier.height(12.dp))

            DisclaimerCard(title = "How It Works") {
                "This app communicates with your camera over PTP (Picture Transfer Protocol) " +
                "via USB-C OTG — the same mode your camera uses for standard backup " +
                "(USB RAW Conv. / Backup Restore). No Fujifilm SDK or licensed library is used. " +
                "Always back up your camera settings before making changes."
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text = "By continuing you confirm that you have read and understood the above, " +
                    "and that you accept full responsibility for your use of this application.",
                fontFamily = MonoFamily,
                fontSize = 10.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp,
                color = TextDim,
            )

            Spacer(Modifier.height(16.dp))

            DisclaimerAcceptButton(
                label = actionLabel,
                secondsLeft = secondsLeft,
                enabled = secondsLeft == 0,
                onClick = onAccept,
            )

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun DisclaimerCard(title: String, body: () -> String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PanelLow)
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .padding(18.dp),
    ) {
        Text(
            text = title,
            fontFamily = SansFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = TextPrimary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = body(),
            fontFamily = SansFamily,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = TextMuted,
        )
    }
}

@Composable
private fun DisclaimerAcceptButton(label: String, secondsLeft: Int, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Gold.copy(alpha = if (enabled) 0.15f else 0.06f))
            .border(1.dp, Gold.copy(alpha = if (enabled) 0.5f else 0.2f), RoundedCornerShape(12.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (secondsLeft > 0) {
                Text(
                    text = "$secondsLeft",
                    fontFamily = MonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.5.sp,
                    color = Gold.copy(alpha = 0.35f),
                    modifier = Modifier.padding(end = 10.dp),
                )
            }
            Text(
                text = label,
                fontFamily = MonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
                color = if (enabled) Gold else Gold.copy(alpha = 0.35f),
            )
        }
    }
}
