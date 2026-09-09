package com.ilfforever.fujisync.ui.model

/**
 * The one place white-balance display labels meet their PTP wire values.
 *
 * `0x8020` and `0x8021` (the two auto-priority modes) exist only on later bodies, and Custom 1-3
 * recall a measurement stored on the camera, so a rejection there says nothing about whether the
 * mode exists. Both facts matter to capability checks, which is why the mapping is shared rather
 * than repeated per screen.
 */
object WhiteBalanceCodes {

    const val COLOR_TEMPERATURE = 0x8007

    /** Label shown in the editor → wire value. Ordered as the editor lists them. */
    val byLabel: Map<String, Int> = linkedMapOf(
        "Auto" to 0x0002,
        "Auto White Priority" to 0x8020,
        "Ambience Priority" to 0x8021,
        "Daylight" to 0x0004,
        "Incandescent" to 0x0006,
        "Underwater" to 0x0008,
        "Fluorescent 1" to 0x8001,
        "Fluorescent 2" to 0x8002,
        "Fluorescent 3" to 0x8003,
        "Shade" to 0x8006,
        "Color Temperature" to COLOR_TEMPERATURE,
    )

    /** Every documented mode, including the three custom slots the editor does not offer. */
    private val labelByValue: Map<Int, String> =
        byLabel.entries.associate { (label, value) -> value to label } + mapOf(
            0x8008 to "Custom 1",
            0x8009 to "Custom 2",
            0x800A to "Custom 3",
        )

    fun wireValue(label: String?): Int? = label?.let { byLabel[it] }

    fun label(wireValue: Int): String? = labelByValue[wireValue]
}
