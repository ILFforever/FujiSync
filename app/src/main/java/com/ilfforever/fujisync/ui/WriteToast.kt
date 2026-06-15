package com.ilfforever.fujisync.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.ilfforever.fujisync.ui.theme.Gold
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.TextMuted

// ── Write toast ───────────────────────────────────────────────────
@Composable
fun WriteToast(
    slot: String,
    name: String,
    savedToLibrary: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(com.ilfforever.fujisync.ui.theme.PanelHigh)
            .border(1.dp, Gold, RoundedCornerShape(12.dp))
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "✓", color = Gold, fontSize = 16.sp)
        Column {
            Text(
                text = if (savedToLibrary) "Saved $name" else "Wrote $name → $slot",
                fontFamily = com.ilfforever.fujisync.ui.theme.SansFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = com.ilfforever.fujisync.ui.theme.TextPrimary,
            )
            Text(
                text = if (savedToLibrary) "ADDED TO LIBRARY" else "RECIPE LIVE ON CAMERA",
                fontFamily = MonoFamily,
                fontSize = 10.5.sp,
                letterSpacing = 1.sp,
                color = TextMuted,
            )
        }
    }
}
