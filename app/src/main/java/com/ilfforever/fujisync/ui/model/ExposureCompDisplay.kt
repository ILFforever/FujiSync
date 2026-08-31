package com.ilfforever.fujisync.ui.model

import kotlin.math.abs
import kotlin.math.roundToInt

internal fun formatExposureComp(value: Float): String {
    val thirds = (value * 3f).roundToInt()
    val sign = if (thirds < 0) "−" else "+"
    val magnitude = abs(thirds)
    val whole = magnitude / 3
    val remainder = magnitude % 3
    val amount = when {
        remainder == 0 -> whole.toString()
        whole == 0 -> "$remainder/3"
        else -> "$whole $remainder/3"
    }
    return "$sign$amount"
}
