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
