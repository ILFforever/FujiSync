package com.ilfforever.fujisync.data.ptp

import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder

// Standard PTP DevicePropDesc (ISO 15740, opcode 0x1014). Parsed so a bench can learn what a
// camera will actually accept for a property — its type, whether it is writable at all, and the
// legal value set — instead of guessing and writing something the body rejects.
//
// Layout:
//   uint16 propCode | uint16 dataType | uint8 getSet | DTS factoryDefault | DTS current
//   uint8 formFlag  | form: 0 = none, 1 = range (min/max/step), 2 = enum (uint16 count + values)
// DTS width follows dataType. Only the 2-byte integer types are handled here, which is every
// property this app touches; anything else is reported as unsupported rather than mis-parsed.

const val PTP_TYPE_INT16 = 0x0003
const val PTP_TYPE_UINT16 = 0x0004

enum class PtpPropForm { None, Range, Enum }

data class PtpPropDesc(
    val propCode: Int,
    val dataType: Int,
    val writable: Boolean,
    val factoryDefault: Int,
    val current: Int,
    val form: PtpPropForm,
    val min: Int? = null,
    val max: Int? = null,
    val step: Int? = null,
    val enumValues: List<Int> = emptyList(),
) {
    val signed: Boolean get() = dataType == PTP_TYPE_INT16

    val typeName: String
        get() = when (dataType) {
            PTP_TYPE_INT16 -> "INT16"
            PTP_TYPE_UINT16 -> "UINT16"
            else -> "0x%04X".format(dataType)
        }

    /** How the legal value set reads in a UI, e.g. "-4..4 step 1" or "1 of 8 values". */
    val formSummary: String
        get() = when (form) {
            PtpPropForm.Range -> "$min..$max step $step"
            PtpPropForm.Enum -> enumValues.joinToString(", ") { it.toString() }
            PtpPropForm.None -> "no declared range"
        }

    /**
     * A legal value different from [current], or null if none can be chosen safely.
     * Returns null for a property with no declared form — writing an undeclared value is exactly
     * the guess this parsing exists to avoid.
     */
    fun alternateValue(): Int? = when (form) {
        PtpPropForm.Enum -> enumValues.firstOrNull { it != current }
        PtpPropForm.Range -> {
            val s = step ?: 1
            val lo = min ?: return null
            val hi = max ?: return null
            when {
                s <= 0 -> null
                current + s <= hi -> current + s
                current - s >= lo -> current - s
                else -> null
            }
        }
        PtpPropForm.None -> null
    }
}

private fun ByteBuffer.readValue(dataType: Int): Int = when (dataType) {
    PTP_TYPE_INT16 -> short.toInt()
    PTP_TYPE_UINT16 -> short.toInt() and 0xFFFF
    else -> throw IllegalArgumentException("unsupported DevicePropDesc dataType 0x%04X".format(dataType))
}

fun parsePropDesc(payload: ByteArray): PtpPropDesc? {
    if (payload.size < 5) return null
    return try {
        val b = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
        val propCode = b.short.toInt() and 0xFFFF
        val dataType = b.short.toInt() and 0xFFFF
        if (dataType != PTP_TYPE_INT16 && dataType != PTP_TYPE_UINT16) return null
        val writable = (b.get().toInt() and 0xFF) == 1
        val factoryDefault = b.readValue(dataType)
        val current = b.readValue(dataType)

        when (b.get().toInt() and 0xFF) {
            1 -> PtpPropDesc(
                propCode, dataType, writable, factoryDefault, current, PtpPropForm.Range,
                min = b.readValue(dataType),
                max = b.readValue(dataType),
                step = b.readValue(dataType),
            )
            2 -> {
                val count = b.short.toInt() and 0xFFFF
                val values = ArrayList<Int>(count)
                repeat(count) { values.add(b.readValue(dataType)) }
                PtpPropDesc(
                    propCode, dataType, writable, factoryDefault, current,
                    PtpPropForm.Enum, enumValues = values,
                )
            }
            else -> PtpPropDesc(propCode, dataType, writable, factoryDefault, current, PtpPropForm.None)
        }
    } catch (_: BufferUnderflowException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}
