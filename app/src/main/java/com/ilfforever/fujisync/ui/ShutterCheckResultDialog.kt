package com.ilfforever.fujisync.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ilfforever.fujisync.ui.theme.Border
import com.ilfforever.fujisync.ui.theme.Gold
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.PanelLow
import com.ilfforever.fujisync.ui.theme.SansFamily
import com.ilfforever.fujisync.ui.theme.TextDim
import com.ilfforever.fujisync.ui.theme.TextMuted
import com.ilfforever.fujisync.ui.theme.TextPrimary

// ── Shutter check result ──────────────────────────────────────────

@Composable
internal fun ShutterCheckResultDialog(count: Int, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(PanelLow)
                .border(1.dp, Border, RoundedCornerShape(16.dp))
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "SHUTTER COUNT",
                fontFamily = MonoFamily,
                fontSize = 10.sp,
                letterSpacing = 2.4.sp,
                color = Gold,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = java.text.NumberFormat.getIntegerInstance().format(count),
                fontFamily = SansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 48.sp,
                letterSpacing = (-1).sp,
                color = TextPrimary,
            )
            Text(
                text = "actuations",
                fontFamily = SansFamily,
                fontSize = 13.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 2.dp),
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Fujifilm X-series mechanical shutters are typically rated for 150,000 actuations or more.",
                fontFamily = SansFamily,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = TextDim,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Gold.copy(alpha = 0.10f))
                    .border(1.dp, Gold.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Done",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = Gold,
                )
            }
        }
    }
}
