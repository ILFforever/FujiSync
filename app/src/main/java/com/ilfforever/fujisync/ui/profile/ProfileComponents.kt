package com.ilfforever.fujisync.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujisync.ui.theme.Border
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.SansFamily
import com.ilfforever.fujisync.ui.theme.TextDim
import com.ilfforever.fujisync.ui.theme.TextMuted
import com.ilfforever.fujisync.ui.theme.TextPrimary

/**
 * Shared navigation row used across the Profile screens (Profile, About, Dev tools).
 * Renders a label with an optional subtitle/badge and a trailing chevron.
 */
@Composable
internal fun ProfileNavRow(
    label: String,
    onClick: () -> Unit,
    inCard: Boolean = false,
    badge: String? = null,
    subtitle: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = if (inCard) 16.dp else 4.dp,
                vertical = if (subtitle != null) 12.dp else 16.dp,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontFamily = SansFamily,
                fontSize = 14.5.sp,
                color = TextPrimary,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontFamily = SansFamily,
                    fontSize = 12.sp,
                    color = TextDim,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        if (badge != null) {
            Text(
                text = badge,
                fontFamily = MonoFamily,
                fontSize = 11.sp,
                letterSpacing = 0.4.sp,
                color = TextDim,
                modifier = Modifier.padding(end = 10.dp),
            )
        }
        Text(
            text = "›",
            fontFamily = MonoFamily,
            fontSize = 14.sp,
            color = TextMuted,
        )
    }
}

@Composable
internal fun ProfileDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Border),
    )
}
