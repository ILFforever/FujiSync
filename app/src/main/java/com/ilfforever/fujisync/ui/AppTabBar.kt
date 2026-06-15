package com.ilfforever.fujisync.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujisync.ui.components.IconCamera
import com.ilfforever.fujisync.ui.components.IconFolder
import com.ilfforever.fujisync.ui.components.IconProfile
import com.ilfforever.fujisync.ui.components.IconSearch
import com.ilfforever.fujisync.ui.haptics.FujiHapticEffect
import com.ilfforever.fujisync.ui.haptics.FujiHaptics
import com.ilfforever.fujisync.ui.theme.Bg
import com.ilfforever.fujisync.ui.theme.Border
import com.ilfforever.fujisync.ui.theme.Gold
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.TextMuted

// ── Tab bar ───────────────────────────────────────────────────────
private data class TabItem(val id: AppTab, val label: String, val icon: ImageVector)

@Composable
internal fun AppTabBar(tab: AppTab, onTabChange: (AppTab) -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val tabs = listOf(
        TabItem(AppTab.Camera, "CAMERA", IconCamera),
        TabItem(AppTab.Library, "LIBRARY", IconFolder),
        TabItem(AppTab.Discover, "DISCOVER", IconSearch),
        TabItem(AppTab.Profile, "PROFILE", IconProfile),
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Bg)
            .navigationBarsPadding()
            .padding(bottom = 10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Border),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 2.dp),
        ) {
            tabs.forEach { t ->
                val active = tab == t.id
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (tab != t.id) {
                                FujiHaptics.perform(context, view, FujiHapticEffect.Selection)
                            }
                            onTabChange(t.id)
                        }
                        .padding(top = 5.dp, bottom = 3.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            t.icon,
                            contentDescription = t.label,
                            tint = if (active) Gold else TextMuted,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = t.label,
                            fontFamily = MonoFamily,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                            fontSize = 9.5.sp,
                            letterSpacing = 1.2.sp,
                            color = if (active) Gold else TextMuted,
                        )
                        Spacer(Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height(2.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (active) Gold else Color.Transparent),
                        )
                    }
                }
            }
        }
    }
}
