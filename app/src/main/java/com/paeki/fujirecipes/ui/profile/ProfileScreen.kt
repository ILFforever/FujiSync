package com.paeki.fujirecipes.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.paeki.fujirecipes.ui.theme.Bg
import com.paeki.fujirecipes.ui.theme.Border
import com.paeki.fujirecipes.ui.theme.Gold
import com.paeki.fujirecipes.ui.theme.MonoFamily
import com.paeki.fujirecipes.ui.theme.PanelLow
import com.paeki.fujirecipes.ui.theme.SansFamily
import com.paeki.fujirecipes.ui.theme.TextMuted
import com.paeki.fujirecipes.ui.theme.TextPrimary

@Composable
fun ProfileScreen(
    cameraModel: String,
    connected: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(
            text = "PROFILE",
            fontFamily = SansFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            letterSpacing = 0.4.sp,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 24.dp, top = 8.dp),
        )

        // Camera card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(PanelLow)
                .border(1.dp, Border, RoundedCornerShape(14.dp))
                .padding(18.dp),
        ) {
            SectionLabel(text = "Camera")
            Spacer(Modifier.height(8.dp))
            Text(
                text = cameraModel,
                fontFamily = SansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = TextPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (connected) "● CONNECTED VIA USB" else "○ OFFLINE",
                fontFamily = MonoFamily,
                fontSize = 11.sp,
                letterSpacing = 1.4.sp,
                color = if (connected) Gold else TextMuted,
            )
        }

        Spacer(Modifier.height(16.dp))

        // Settings rows
        val rows = listOf(
            "Account" to "m.takahashi@studio.jp",
            "Cloud sync" to "Off",
            "Default film sim" to "Classic Chrome",
            "Units" to "Metric",
            "App version" to "0.4.1 (preview)",
            "Open source licenses" to "›",
            "Send feedback" to "›",
        )

        rows.forEach { (key, value) ->
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = key,
                        fontFamily = SansFamily,
                        fontSize = 14.5.sp,
                        color = TextPrimary,
                    )
                    Text(
                        text = value,
                        fontFamily = MonoFamily,
                        fontSize = 12.sp,
                        color = TextMuted,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Border),
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}
