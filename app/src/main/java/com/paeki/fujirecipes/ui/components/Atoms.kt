package com.paeki.fujirecipes.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paeki.fujirecipes.ui.theme.Bg
import com.paeki.fujirecipes.ui.theme.Border
import com.paeki.fujirecipes.ui.theme.Gold
import com.paeki.fujirecipes.ui.theme.GoldDim
import com.paeki.fujirecipes.ui.theme.GoldFaint
import com.paeki.fujirecipes.ui.theme.MonoFamily
import com.paeki.fujirecipes.ui.theme.PanelHigh
import com.paeki.fujirecipes.ui.theme.PanelLow
import com.paeki.fujirecipes.ui.theme.SansFamily
import com.paeki.fujirecipes.ui.theme.TextDim
import com.paeki.fujirecipes.ui.theme.TextMuted
import com.paeki.fujirecipes.ui.theme.TextPrimary

// ── Wordmark ─────────────────────────────────────────────────────
@Composable
fun Wordmark() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Gold square with cutout centre + filled dot (3-layer stack)
        Box(modifier = Modifier.size(14.dp)) {
            // Gold outer
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(2.dp))
                    .background(Gold),
            )
            // Bg cutout ring
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(3.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(Bg),
            )
            // Gold centre dot
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(5.dp)
                    .background(Gold),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "FUJISYNC",
            fontFamily = MonoFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            letterSpacing = 1.1.sp,
            color = TextPrimary,
        )
        Text(
            text = ".",
            fontFamily = MonoFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = Gold,
        )
    }
}

// ── App header ────────────────────────────────────────────────────
@Composable
fun AppHeader(
    connected: Boolean,
    cameraModel: String,
    sheetRevealProgress: Float,
    onReconnect: () -> Unit,
    showConnectionStatus: Boolean = true,
    showDisconnectedStatus: Boolean = true,
    showReconnectButton: Boolean = true,
) {
    val cameraModelAlpha = ((sheetRevealProgress - 0.08f) / 0.72f).coerceIn(0f, 1f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Bg)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Wordmark()
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(28.dp)) {
            if (showConnectionStatus && connected) {
                Text(
                    text = cameraModel,
                    fontFamily = MonoFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.5.sp,
                    letterSpacing = 1.sp,
                    color = TextPrimary.copy(alpha = cameraModelAlpha),
                )
            } else if (showConnectionStatus && showDisconnectedStatus) {
                Text(
                    text = "OFFLINE",
                    fontFamily = MonoFamily,
                    fontSize = 10.sp,
                    letterSpacing = 1.8.sp,
                    color = TextDim,
                )
            }
            if (showReconnectButton) {
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onReconnect, modifier = Modifier.size(28.dp)) {
                    Icon(
                        IconRefresh,
                        contentDescription = "Reconnect",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

// ── Section label ─────────────────────────────────────────────────
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        fontFamily = SansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 2.4.sp,
        color = TextMuted,
        modifier = modifier,
    )
}

// ── Pill chip ─────────────────────────────────────────────────────
@Composable
fun Pill(
    text: String,
    large: Boolean = false,
    active: Boolean = false,
) {
    val bg = if (active) GoldDim else PanelHigh
    val fg = if (active) Gold else TextPrimary
    val borderColor = if (active) Gold else Color.Transparent

    Text(
        text = text,
        fontFamily = MonoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = if (large) 11.sp else 10.5.sp,
        letterSpacing = 0.6.sp,
        color = fg,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(999.dp))
            .padding(
                horizontal = if (large) 12.dp else 10.dp,
                vertical = if (large) 8.dp else 6.dp,
            ),
    )
}

// ── Property row (gold icon + label + right-aligned value) ────────
@Composable
fun PropRow(
    label: String,
    value: String,
    isLast: Boolean = false,
    inactive: Boolean = false,
    inactiveValue: String? = null,
) {
    val icon = PROP_ICONS[label]
    val iconTint = if (inactive) TextDim else Gold
    val labelColor = if (inactive) TextMuted else TextPrimary
    val valueColor = if (inactive) Gold.copy(alpha = 0.72f) else TextPrimary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Gold line-icon slot (always 18dp wide, empty if no icon)
        Box(modifier = Modifier.size(18.dp), contentAlignment = Alignment.Center) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            fontFamily = SansFamily,
            fontSize = 14.5.sp,
            color = labelColor,
            modifier = Modifier.weight(1f),
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = if (inactive) (inactiveValue ?: "DRP CONTROLLED") else value,
                fontFamily = MonoFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = if (inactive) 12.5.sp else 14.sp,
                letterSpacing = if (inactive) 0.8.sp else 0.2.sp,
                color = valueColor,
            )
            if (inactive) {
                Text(
                    text = value,
                    fontFamily = MonoFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.5.sp,
                    letterSpacing = 0.3.sp,
                    color = TextDim,
                )
            }
        }
    }
}

fun recipePropertyRows(data: Map<String, String>): List<Pair<String, String>> {
    val dRangePriority = data["D Range Priority"]
        ?.takeIf { it != "Off" && it != "—" }
    val hasDynamicRange = data.containsKey("Dynamic Range")

    return buildList {
        data.forEach { (key, value) ->
            when {
                key == "Dynamic Range" && dRangePriority != null ->
                    add("D Range Priority" to dRangePriority)
                key == "D Range Priority" && (dRangePriority == null || hasDynamicRange) ->
                    Unit
                else ->
                    add(key to value)
            }
        }
    }
}

// ── Slot chip ─────────────────────────────────────────────────────
@Composable
fun SlotChip(
    slot: String,
    name: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (active) PanelHigh else PanelLow
    val borderColor = if (active) Gold else Border
    val nameColor = if (active) TextPrimary else TextMuted

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = slot,
            fontFamily = MonoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 0.4.sp,
            color = Gold,
        )
        Spacer(Modifier.width(9.dp))
        Text(
            text = name.uppercase(),
            fontFamily = SansFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.5.sp,
            letterSpacing = 0.5.sp,
            color = nameColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ── Drag handle ───────────────────────────────────────────────────
@Composable
fun DragHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0x29FFFFFF)),
        )
    }
}

// ── Primary CTA ───────────────────────────────────────────────────
@Composable
fun PrimaryCTA(
    label: String,
    onClick: () -> Unit,
    busy: Boolean = false,
    enabled: Boolean = true,
    secondary: Boolean = false,
) {
    val bg = when {
        secondary -> Color.Transparent
        !enabled -> PanelHigh
        else -> Gold
    }
    val fg = when {
        secondary -> Gold
        !enabled -> TextMuted
        else -> Bg
    }
    val borderColor = if (secondary) Gold else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 17.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (busy) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                color = fg,
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = label.uppercase(),
            fontFamily = SansFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.5.sp,
            letterSpacing = 2.4.sp,
            color = fg,
        )
    }
}

// ── Meta row (label + value, used in camera card) ─────────────────
@Composable
fun MetaRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label.uppercase(),
            fontFamily = SansFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 10.5.sp,
            letterSpacing = 2.2.sp,
            color = TextDim,
            modifier = Modifier.width(92.dp),
        )
        Text(
            text = value,
            fontFamily = MonoFamily,
            fontSize = 12.sp,
            color = TextMuted,
        )
    }
}

@Composable
fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -600f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer-x",
    )
    return Brush.linearGradient(
        colors = listOf(
            Color(0xFF1A1612),
            Color(0xFF262119),
            Color(0xFF1A1612),
        ),
        start = Offset(x, 0f),
        end = Offset(x + 600f, 0f),
    )
}

data class ImageLoadState(val preview: ImageBitmap?, val full: ImageBitmap?)

@Composable
fun BlurredImagePlaceholder(
    state: ImageLoadState,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    blurEnabled: Boolean = true,
) {
    val fullAlpha by animateFloatAsState(
        targetValue = if (state.full != null) 1f else 0f,
        animationSpec = tween(380, easing = FastOutSlowInEasing),
        label = "img-full-alpha",
    )
    Box(modifier = modifier) {
        if (blurEnabled && state.preview != null) {
            Image(
                bitmap = state.preview,
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize().blur(28.dp),
            )
        }
        state.full?.let { full ->
            Image(
                bitmap = full,
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = fullAlpha },
            )
        }
    }
}

private val memCache = HashMap<String, ImageBitmap>()

private fun diskCacheFile(context: Context, key: String): java.io.File {
    val dir = java.io.File(context.cacheDir, "bmp").also { it.mkdirs() }
    return java.io.File(dir, "${key.hashCode().and(0x7FFFFFFF)}.webp")
}

internal fun decodeSampledBitmap(
    context: Context,
    uri: Uri,
    maxPx: Int = 512,
): ImageBitmap? {
    val key = "$uri@$maxPx"

    // L1: memory
    memCache[key]?.let { return it }

    // L2: disk
    val cacheFile = diskCacheFile(context, key)
    if (cacheFile.exists()) {
        BitmapFactory.decodeFile(cacheFile.absolutePath)?.asImageBitmap()?.let { cached ->
            memCache[key] = cached
            return cached
        }
    }

    // decode from source, persist to disk and memory
    return runCatching {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        val scale = maxOf(opts.outWidth, opts.outHeight).coerceAtLeast(1)
        opts.inSampleSize = Integer.highestOneBit(scale / maxPx).coerceAtLeast(1)
        opts.inJustDecodeBounds = false
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }?.asImageBitmap()
    }.getOrNull()?.also { bmp ->
        memCache[key] = bmp
        runCatching {
            cacheFile.outputStream().use { out ->
                bmp.asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.WEBP_LOSSY, 85, out)
            }
        }
    }
}
