package com.ilfforever.fujisync.ui.transfer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujisync.ui.components.IconChevronRight
import com.ilfforever.fujisync.ui.theme.Border
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.PanelHigh
import com.ilfforever.fujisync.ui.theme.SansFamily
import com.ilfforever.fujisync.ui.theme.TextDim
import com.ilfforever.fujisync.ui.theme.TextMuted
import com.ilfforever.fujisync.ui.theme.TextPrimary

/**
 * One action inside a [TransferSection] — label, subtitle, gold line-icon, optional trailing
 * tag (e.g. "OCR", "EXIF"). Disabled rows drop to dim text and stop responding to taps.
 */
@Composable
internal fun TransferActionRow(
    label: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    tag: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) iconTint else TextDim,
            modifier = Modifier.size(18.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontFamily = SansFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = if (enabled) TextPrimary else TextDim,
            )
            Text(
                text = subtitle,
                fontFamily = SansFamily,
                fontSize = 12.sp,
                color = TextDim,
            )
        }
        if (tag != null) {
            Text(
                text = tag,
                fontFamily = MonoFamily,
                fontSize = 9.sp,
                letterSpacing = 1.4.sp,
                color = if (enabled) TextMuted else TextDim,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(PanelHigh)
                    .border(1.dp, Border, RoundedCornerShape(999.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        Icon(
            imageVector = IconChevronRight,
            contentDescription = null,
            tint = if (enabled) TextDim else Border,
            modifier = Modifier.size(14.dp),
        )
    }
}
