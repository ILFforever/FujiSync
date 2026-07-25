package com.ilfforever.fujisync.ui.transfer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujisync.ui.components.IconStar
import com.ilfforever.fujisync.ui.theme.Border
import com.ilfforever.fujisync.ui.theme.Gold
import com.ilfforever.fujisync.ui.theme.GoldFaint
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.SansFamily
import com.ilfforever.fujisync.ui.theme.TextDim

/**
 * Standing reminder on the Transfer screen that imported recipes belong to whoever made them.
 * Deliberately quiet — a faint gold panel, not a warning.
 */
@Composable
internal fun TransferCreditNote() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(GoldFaint)
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Icon(
            imageVector = IconStar,
            contentDescription = null,
            tint = Gold,
            modifier = Modifier.size(16.dp),
        )
        Column {
            Text(
                text = "CREDIT THE MAKER",
                fontFamily = MonoFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 9.5.sp,
                letterSpacing = 1.4.sp,
                color = Gold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Recipes are someone's work. If you didn't create it, keep the " +
                    "creator's name on it when you save or share — and respect creators " +
                    "who ask that their recipes not be passed around.",
                fontFamily = SansFamily,
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                color = TextDim,
            )
        }
    }
}
