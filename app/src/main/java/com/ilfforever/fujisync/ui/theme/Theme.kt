@file:Suppress("DEPRECATION", "DEPRECATION_ERROR")

package com.ilfforever.fujisync.ui.theme

import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationInstance
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.drawscope.ContentDrawScope

private val FujiDarkScheme = darkColorScheme(
    background = Bg,
    onBackground = TextPrimary,
    surface = PanelLow,
    onSurface = TextPrimary,
    surfaceVariant = PanelHigh,
    onSurfaceVariant = TextMuted,
    primary = Gold,
    onPrimary = Bg,
    outline = Border,
    outlineVariant = BorderStrong,
    error = androidx.compose.ui.graphics.Color(0xFFD94040),
)

private object NoClickIndication : Indication {
    @Composable
    override fun rememberUpdatedInstance(interactionSource: InteractionSource): IndicationInstance =
        NoClickIndicationInstance
}

private object NoClickIndicationInstance : IndicationInstance {
    override fun ContentDrawScope.drawIndication() {
        drawContent()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FujiRecipesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FujiDarkScheme,
        typography = FujiTypography,
    ) {
        CompositionLocalProvider(
            LocalIndication provides NoClickIndication,
            LocalRippleConfiguration provides null,
            content = content,
        )
    }
}
