package com.ilfforever.fujisync.ui

import androidx.compose.ui.graphics.vector.ImageVector
import com.ilfforever.fujisync.BuildConfig
import com.ilfforever.fujisync.ui.components.IconCamera
import com.ilfforever.fujisync.ui.components.IconFolder
import com.ilfforever.fujisync.ui.components.IconProfile
import com.ilfforever.fujisync.ui.components.IconSearch
import com.ilfforever.fujisync.ui.components.IconTransfer

internal data class TabItem(val id: AppTab, val label: String, val icon: ImageVector)

/**
 * The navigation destinations, shared by [AppTabBar] (phone) and [AppNavRail] (tablet) so
 * the two can never drift apart. Discover and Transfer occupy the same slot — full builds
 * get Discover, lean builds get Transfer.
 */
internal fun appTabs(): List<TabItem> = buildList {
    add(TabItem(AppTab.Camera, "CAMERA", IconCamera))
    add(TabItem(AppTab.Library, "LIBRARY", IconFolder))
    if (BuildConfig.DISCOVER_ENABLED) {
        add(TabItem(AppTab.Discover, "DISCOVER", IconSearch))
    } else {
        add(TabItem(AppTab.Transfer, "TRANSFER", IconTransfer))
    }
    add(TabItem(AppTab.Profile, "PROFILE", IconProfile))
}
