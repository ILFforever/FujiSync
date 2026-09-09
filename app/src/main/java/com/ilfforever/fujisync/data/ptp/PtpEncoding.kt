package com.ilfforever.fujisync.data.ptp

import java.nio.ByteBuffer
import java.nio.ByteOrder

fun encodePtpString(value: String): ByteArray {
    val chars = value.take(254).toCharArray()
    if (chars.isEmpty()) return byteArrayOf(0)

    val buffer = ByteBuffer
        .allocate(1 + ((chars.size + 1) * Short.SIZE_BYTES))
        .order(ByteOrder.LITTLE_ENDIAN)

    buffer.put((chars.size + 1).toByte())
    chars.forEach { buffer.putShort(it.code.toShort()) }
    buffer.putShort(0)
    return buffer.array()
}

/** PTP string format: 1 byte char count (incl. null terminator) + that many UTF-16LE code units. */
fun decodePtpString(payload: ByteArray): String? {
    if (payload.isEmpty()) return null
    val count = payload[0].toInt() and 0xFF
    if (count == 0) return ""
    val needed = 1 + count * Short.SIZE_BYTES
    if (payload.size < needed) return null
    val chars = CharArray(count - 1)
    val buffer = ByteBuffer.wrap(payload, 1, count * Short.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN)
    for (i in 0 until count - 1) chars[i] = buffer.short.toInt().toChar()
    return String(chars)
}

fun decodeInt16Le(payload: ByteArray): Int? {
    if (payload.size < Short.SIZE_BYTES) return null
    return ByteBuffer.wrap(payload)
        .order(ByteOrder.LITTLE_ENDIAN)
        .short
        .toInt()
}

fun decodeUInt16Le(payload: ByteArray): Int? =
    decodeInt16Le(payload)?.and(0xFFFF)

/**
 * Reads a little-endian uint32 as a [Long]. Returned unsigned because PTP container lengths are
 * compared against limits, and a length near 2^31 read as a negative Int would pass a range check
 * it should fail.
 */
fun readUInt32Le(bytes: ByteArray, offset: Int = 0): Long {
    require(offset >= 0 && offset + 4 <= bytes.size) {
        "Cannot read a uint32 at offset $offset of a ${bytes.size}-byte array."
    }
    return (bytes[offset].toLong() and 0xFF) or
        ((bytes[offset + 1].toLong() and 0xFF) shl 8) or
        ((bytes[offset + 2].toLong() and 0xFF) shl 16) or
        ((bytes[offset + 3].toLong() and 0xFF) shl 24)
}

fun hexDump(bytes: ByteArray, maxBytes: Int = 32): String {
    if (bytes.isEmpty()) return "<empty>"
    val limit = minOf(maxBytes, bytes.size)
    val sb = StringBuilder(limit * 3)
    for (i in 0 until limit) {
        if (i > 0) sb.append(' ')
        sb.append(HEX[(bytes[i].toInt() ushr 4) and 0xF])
        sb.append(HEX[bytes[i].toInt() and 0xF])
    }
    if (bytes.size > limit) sb.append(" …(+").append(bytes.size - limit).append(")")
    return sb.toString()
}

private val HEX = charArrayOf(
    '0', '1', '2', '3', '4', '5', '6', '7',
    '8', '9', 'A', 'B', 'C', 'D', 'E', 'F',
)
