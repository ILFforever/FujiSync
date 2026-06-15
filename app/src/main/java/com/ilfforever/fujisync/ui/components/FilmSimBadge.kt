package com.ilfforever.fujirecipes.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujirecipes.R
import com.ilfforever.fujirecipes.ui.theme.Gold
import com.ilfforever.fujirecipes.ui.theme.MonoFamily
import com.ilfforever.fujirecipes.ui.theme.PanelHigh
import com.ilfforever.fujirecipes.ui.theme.TextMuted
import com.ilfforever.fujirecipes.ui.theme.TextPrimary

@DrawableRes
fun filmSimulationBadgeRes(sim: String): Int? = when (sim.normalizedFilmSim()) {
    "acros" -> R.drawable.film_sim_badge_acros
    "acrosg", "acrosge", "acrosgreen", "acrosgreenfilter" -> R.drawable.film_sim_badge_acros_green_filter
    "acrosr", "acrosre", "acrosred", "acrosredfilter" -> R.drawable.film_sim_badge_acros_red_filter
    "acrosy", "acrosye", "acrosyellow", "acrosyellowfilter" -> R.drawable.film_sim_badge_acros_yellow_filter
    "astia", "astiasoft" -> R.drawable.film_sim_badge_astia_soft
    "classicchrome" -> R.drawable.film_sim_badge_classic_chrome
    "classicneg", "classicnegative" -> R.drawable.film_sim_badge_classic_negative
    "eterna", "eternacinema" -> R.drawable.film_sim_badge_eterna_cinema
    "eternableachbypass" -> R.drawable.film_sim_badge_eterna_bleach_bypass
    "monochrome" -> R.drawable.film_sim_badge_monochrome
    "monochromeg", "monochromegreen", "monochromegreenfilter" -> R.drawable.film_sim_badge_monochrome_green_filter
    "monochromer", "monochromered", "monochromeredfilter" -> R.drawable.film_sim_badge_monochrome_red_filter
    "monochromey", "monochromeyellow", "monochromeyellowfilter" -> R.drawable.film_sim_badge_monochrome_yellow_filter
    "nostalgicneg", "nostalgicnegative" -> R.drawable.film_sim_badge_nostalgic_negative
    "proneghi" -> R.drawable.film_sim_badge_pro_neg_hi
    "pronegstd", "pronegstandard" -> R.drawable.film_sim_badge_pro_neg_standard
    "provia", "proviastandard", "proviastd" -> R.drawable.film_sim_badge_provia_standard
    "realaace" -> R.drawable.film_sim_badge_reala_ace
    "sepia" -> R.drawable.film_sim_badge_sepia
    "velvia", "velviavivid" -> R.drawable.film_sim_badge_velvia_vivid
    else -> null
}

@Composable
fun FilmSimBadgeImage(
    sim: String,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
) {
    val badge = filmSimulationBadgeRes(sim) ?: return
    Image(
        painter = painterResource(badge),
        contentDescription = sim,
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size),
    )
}

@Composable
fun FilmSimInlineLabel(
    sim: String,
    modifier: Modifier = Modifier,
    imageSize: Dp = 28.dp,
) {
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.105f),
                    0.38f to Color.White.copy(alpha = 0.055f),
                    1f to Color.White.copy(alpha = 0.028f),
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.12f), shape)
    ) {
        Row(
            modifier = Modifier.padding(start = 7.dp, end = 11.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilmSimBadgeImage(sim = sim, size = imageSize)
            Text(
                text = sim.trimEnd('.').uppercase(),
                fontFamily = MonoFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                letterSpacing = 1.4.sp,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun FilmSimLabel(
    sim: String,
    modifier: Modifier = Modifier,
    imageSize: Dp = 24.dp,
    quiet: Boolean = false,
) {
    val borderColor = if (quiet) Gold.copy(alpha = 0.62f) else Gold
    val textColor = if (quiet) TextMuted else TextPrimary

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(PanelHigh)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .padding(start = 6.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FilmSimBadgeImage(sim = sim, size = imageSize)
        Text(
            text = sim.trimEnd('.').uppercase(),
            fontFamily = MonoFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 9.5.sp,
            letterSpacing = 1.2.sp,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun String.normalizedFilmSim(): String =
    replace(Regex("\\s*\\(\\d+\\)"), "")  // strip "(1)", "(2)" variant suffixes
        .lowercase()
        .replace("+", "")
        .replace("-", "")
        .replace("_", "")
        .replace("/", "")
        .replace(".", "")
        .replace(" ", "")
