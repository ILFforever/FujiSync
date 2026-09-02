package com.ilfforever.fujisync.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujisync.ui.components.Wordmark
import com.ilfforever.fujisync.ui.haptics.FujiHapticEffect
import com.ilfforever.fujisync.ui.haptics.FujiHaptics
import com.ilfforever.fujisync.ui.theme.Bg
import com.ilfforever.fujisync.ui.theme.Border
import com.ilfforever.fujisync.ui.theme.Gold
import com.ilfforever.fujisync.ui.theme.GoldFaint
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.TextMuted

private val RailWidth = 96.dp

/**
 * Vertical navigation for tablets, replacing [AppTabBar]. A bottom bar on a 1280dp-wide
 * screen pushes its targets to the far corners; a rail keeps them together under the thumb
 * and hands the freed vertical space back to content.
 */
@Composable
internal fun AppNavRail(tab: AppTab, onTabChange: (AppTab) -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val tabs = appTabs()

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(RailWidth)
            .background(Bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.padding(bottom = 20.dp, top = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Wordmark(compact = true)
        }

        tabs.forEach { t ->
            val active = tab == t.id
            val tint by animateColorAsState(
                targetValue = if (active) Gold else TextMuted,
                animationSpec = tween(160),
                label = "rail-tint",
            )
            val pill by animateColorAsState(
                targetValue = if (active) GoldFaint else Color.Transparent,
                animationSpec = tween(160),
                label = "rail-pill",
            )
            Column(
                modifier = Modifier
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        if (tab != t.id) {
                            FujiHaptics.perform(context, view, FujiHapticEffect.Selection)
                        }
                        onTabChange(t.id)
                    }
                    .background(pill)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = t.icon,
                    contentDescription = t.label,
                    tint = tint,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = t.label,
                    fontFamily = MonoFamily,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 8.5.sp,
                    letterSpacing = 0.8.sp,
                    color = tint,
                )
            }
        }

        Spacer(Modifier.weight(1f))
    }

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(1.dp)
            .background(Border),
    )
}
