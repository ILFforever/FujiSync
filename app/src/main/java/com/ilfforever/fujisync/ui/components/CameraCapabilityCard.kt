package com.ilfforever.fujisync.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujisync.ui.model.CapabilityProfile
import com.ilfforever.fujisync.ui.theme.Gold
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.SansFamily
import com.ilfforever.fujisync.ui.theme.TextDim
import com.ilfforever.fujisync.ui.theme.TextMuted
import com.ilfforever.fujisync.ui.theme.TextPrimary

/**
 * What this body can do with a recipe — the film-simulation ceiling, whether its tone dials have
 * half steps, how many white-balance modes it has, and which recipe settings it does not have.
 *
 * Shown wherever a camera is described, so the limits are visible before a write rather than only
 * after one is refused.
 */
@Composable
fun CameraCapabilityCard(
    profile: CapabilityProfile,
    modifier: Modifier = Modifier,
    background: Color,
    borderColor: Color,
) {
    if (!profile.isKnown) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = "WHAT THIS BODY TAKES",
            fontFamily = MonoFamily,
            fontSize = 9.sp,
            letterSpacing = 1.6.sp,
            color = TextMuted,
        )

        profile.deviceKey?.let { key ->
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = key,
                    fontFamily = MonoFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    letterSpacing = 0.8.sp,
                    color = Gold,
                )
                Text(
                    text = profile.keyNote,
                    fontFamily = SansFamily,
                    fontSize = 11.sp,
                    color = TextDim,
                )
            }
        }

        if (profile.facts.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Divider(borderColor)
            Spacer(Modifier.height(10.dp))
            profile.facts.forEach { fact ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = fact.label,
                        fontFamily = SansFamily,
                        fontSize = 12.5.sp,
                        color = TextMuted,
                    )
                    Text(
                        text = fact.value,
                        fontFamily = SansFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.5.sp,
                        color = TextPrimary,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
            }
        }

        CapabilityGap("NOT ON THIS BODY", profile.missing, borderColor)
        // Kept apart from the list above on purpose: this is Fuji's shipped compatibility data,
        // which describes their desktop app rather than camera firmware. It has never been
        // verified against a non-X-Trans-V body, so it is reported as expectation, not fact.
        CapabilityGap("FUJI'S DATA ALSO EXPECTS MISSING", profile.doubtful, borderColor)
    }
}

@Composable
private fun CapabilityGap(label: String, items: List<String>, borderColor: Color) {
    if (items.isEmpty()) return

    Spacer(Modifier.height(10.dp))
    Divider(borderColor)
    Spacer(Modifier.height(10.dp))
    Text(
        text = label,
        fontFamily = MonoFamily,
        fontSize = 9.sp,
        letterSpacing = 1.4.sp,
        color = TextMuted,
    )
    Spacer(Modifier.height(5.dp))
    Text(
        text = items.joinToString(" · "),
        fontFamily = SansFamily,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        color = TextDim,
    )
}

@Composable
private fun Divider(color: Color) {
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(color))
}
