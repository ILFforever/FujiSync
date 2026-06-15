package com.ilfforever.fujisync.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilfforever.fujisync.ui.components.SectionLabel
import com.ilfforever.fujisync.ui.theme.Bg
import com.ilfforever.fujisync.ui.theme.Border
import com.ilfforever.fujisync.ui.theme.Gold
import com.ilfforever.fujisync.ui.theme.GoldDim
import com.ilfforever.fujisync.ui.theme.MonoFamily
import com.ilfforever.fujisync.ui.theme.PanelHigh
import com.ilfforever.fujisync.ui.theme.PanelLow
import com.ilfforever.fujisync.ui.theme.SansFamily
import com.ilfforever.fujisync.ui.theme.TextDim
import com.ilfforever.fujisync.ui.theme.TextMuted
import com.ilfforever.fujisync.ui.theme.TextPrimary

@Composable
internal fun EditorTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minHeight: androidx.compose.ui.unit.Dp = 0.dp,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        textStyle = textStyle,
        cursorBrush = androidx.compose.ui.graphics.SolidColor(Gold),
        modifier = modifier
            .fillMaxWidth()
            .then(if (minHeight > 0.dp) Modifier.heightIn(min = minHeight) else Modifier),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.TopStart) {
                if (value.isBlank()) {
                    Text(
                        text = placeholder,
                        fontFamily = textStyle.fontFamily,
                        fontWeight = textStyle.fontWeight,
                        fontSize = textStyle.fontSize,
                        lineHeight = textStyle.lineHeight,
                        color = TextDim,
                    )
                }
                inner()
            }
        },
    )
}

@Composable
internal fun EditorSection(
    title: String,
    optional: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        SectionLabel(text = title)
        if (optional) {
            Text(
                text = "OPTIONAL",
                fontFamily = MonoFamily,
                fontSize = 9.sp,
                letterSpacing = 1.4.sp,
                color = TextDim,
            )
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PanelLow)
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
    Spacer(Modifier.height(16.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FilmSimulationPicker(
    selected: String,
    selectedFamily: String,
    onFamilySelect: (String) -> Unit,
    onSelect: (String) -> Unit,
) {
    val family = filmSimFamilies.firstOrNull { it.label == selectedFamily } ?: filmSimFamilyFor(selected)

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        filmSimFamilies.forEach { item ->
            val active = item.label == family.label
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (active) GoldDim else Color.Transparent)
                    .border(1.dp, if (active) Gold else Border, RoundedCornerShape(999.dp))
                    .clickable { onFamilySelect(item.label) }
                    .padding(horizontal = 11.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    text = item.label.uppercase(),
                    fontFamily = MonoFamily,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 10.sp,
                    letterSpacing = 1.1.sp,
                    color = if (active) Gold else TextMuted,
                )
                Text(
                    text = item.sims.size.toString(),
                    fontFamily = MonoFamily,
                    fontSize = 10.sp,
                    color = if (active) Gold else TextDim,
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Border),
    )

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        family.sims.forEach { option ->
            val active = option == selected
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (active) Gold else PanelHigh)
                    .border(1.dp, if (active) Gold else Border, RoundedCornerShape(10.dp))
                    .clickable { onSelect(option) }
                    .padding(start = 10.dp, end = 13.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (active) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(Bg),
                    )
                }
                Text(
                    text = option,
                    fontFamily = SansFamily,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 13.5.sp,
                    color = if (active) Bg else TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GrainEffectControl(
    icon: ImageVector,
    selected: String,
    onSelect: (String) -> Unit,
) {
    val strength = when {
        selected.startsWith("Strong") -> "Strong"
        selected.startsWith("Weak") -> "Weak"
        else -> "Off"
    }
    val size = when {
        selected.endsWith("Large") -> "Large"
        else -> "Small"
    }

    ControlLabel(label = "Grain Effect", icon = icon)
    ChipGrid(options = listOf("Off", "Weak", "Strong"), selected = strength) { newStrength ->
        if (newStrength == "Off") onSelect("Off")
        else onSelect("$newStrength $size")
    }
    Spacer(Modifier.height(6.dp))
    ChipGrid(options = listOf("Small", "Large"), selected = size, enabled = strength != "Off") { newSize ->
        onSelect("$strength $newSize")
    }
}

@Composable
internal fun ChipControl(
    label: String,
    icon: ImageVector,
    options: List<String>,
    selected: String,
    enabled: Boolean = true,
    onSelect: (String) -> Unit,
) {
    ControlLabel(label = label, icon = icon, enabled = enabled)
    ChipGrid(options = options, selected = selected, enabled = enabled, onSelect = onSelect)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChipGrid(
    options: List<String>,
    selected: String,
    enabled: Boolean = true,
    onSelect: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val active = option == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when {
                            !enabled && active -> GoldDim.copy(alpha = 0.28f)
                            !enabled -> PanelLow
                            active -> Gold
                            else -> PanelHigh
                        },
                    )
                    .border(
                        1.dp,
                        when {
                            !enabled -> Color.Transparent
                            active -> Gold
                            else -> Border
                        },
                        RoundedCornerShape(8.dp),
                    )
                    .clickable(enabled = enabled) { onSelect(option) }
                    .padding(horizontal = 13.dp, vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option,
                    fontFamily = SansFamily,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 13.5.sp,
                    color = when {
                        !enabled && active -> Gold.copy(alpha = 0.82f)
                        !enabled -> TextDim
                        active -> Bg
                        else -> TextMuted
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
internal fun StepperControl(
    label: String,
    icon: ImageVector?,
    value: Int,
    min: Int,
    max: Int,
    step: Int = 1,
    valueText: String = value.signedDisplay(),
    modifier: Modifier = Modifier,
    onValueChange: (Int) -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ControlLabel(label = label, icon = icon, modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(9.dp))
                .border(1.dp, Border, RoundedCornerShape(9.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepButton("−", enabled = value > min) { onValueChange((value - step).coerceAtLeast(min)) }
            Box(
                modifier = Modifier
                    .width(68.dp)
                    .height(42.dp)
                    .background(Color(0xFF0A0908)),
                contentAlignment = Alignment.Center,
            ) {
                Text(valueText, fontFamily = MonoFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
            }
            StepButton("+", enabled = value < max) { onValueChange((value + step).coerceAtMost(max)) }
        }
    }
}

@Composable
internal fun HalfStepControl(
    label: String,
    icon: ImageVector?,
    value: Float,
    min: Float,
    max: Float,
    modifier: Modifier = Modifier,
    onValueChange: (Float) -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ControlLabel(label = label, icon = icon, modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(9.dp))
                .border(1.dp, Border, RoundedCornerShape(9.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepButton("−", enabled = value > min) { onValueChange((value - 0.5f).coerceAtLeast(min)) }
            Box(
                modifier = Modifier
                    .width(68.dp)
                    .height(42.dp)
                    .background(Color(0xFF0A0908)),
                contentAlignment = Alignment.Center,
            ) {
                Text(value.signedDisplay(), fontFamily = MonoFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
            }
            StepButton("+", enabled = value < max) { onValueChange((value + 0.5f).coerceAtMost(max)) }
        }
    }
}

@Composable
internal fun StepButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 42.dp)
            .background(if (enabled) PanelHigh else PanelLow)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontFamily = SansFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = if (enabled) TextPrimary else TextDim)
    }
}

@Composable
internal fun ControlLabel(
    label: String,
    icon: ImageVector?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = if (enabled) Gold else TextDim, modifier = Modifier.size(18.dp))
        }
        Text(label, fontFamily = SansFamily, fontSize = 14.5.sp, color = if (enabled) TextPrimary else TextDim)
    }
}

@Composable
internal fun OptionalFieldHeader(
    label: String,
    icon: ImageVector,
    isSet: Boolean,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ControlLabel(label = label, icon = icon)
        if (isSet) {
            Text(
                text = "CLEAR",
                fontFamily = MonoFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                color = TextMuted,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onClear)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SensorGenCheckboxes(
    selected: List<Int>,
    onChanged: (List<Int>) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        (1..5).forEach { gen ->
            val active = gen in selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (active) Gold else PanelHigh)
                    .border(1.dp, if (active) Gold else Border, RoundedCornerShape(8.dp))
                    .clickable {
                        onChanged(
                            if (active) selected - gen else (selected + gen).sorted()
                        )
                    }
                    .padding(horizontal = 13.dp, vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "X-Trans $gen",
                    fontFamily = SansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.5.sp,
                    color = if (active) Bg else TextMuted,
                    maxLines = 1,
                )
            }
        }
    }
}
