package com.paeki.fujirecipes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    sheetExpanded: Boolean,
    onReconnect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Bg)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Wordmark()
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (connected) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Glow dot: shadow behind + solid dot on top
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Gold.copy(alpha = 0.35f))
                                .blur(6.dp),
                        )
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Gold),
                        )
                    }
                    if (sheetExpanded) {
                        Text(
                            text = cameraModel,
                            fontFamily = MonoFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.5.sp,
                            letterSpacing = 1.sp,
                            color = TextPrimary,
                        )
                    }
                }
            } else {
                Text(
                    text = "OFFLINE",
                    fontFamily = MonoFamily,
                    fontSize = 10.sp,
                    letterSpacing = 1.8.sp,
                    color = TextDim,
                )
            }
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
) {
    val icon = PROP_ICONS[label]
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
                    tint = Gold,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            fontFamily = SansFamily,
            fontSize = 14.5.sp,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            fontFamily = MonoFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            letterSpacing = 0.2.sp,
            color = TextPrimary,
        )
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
