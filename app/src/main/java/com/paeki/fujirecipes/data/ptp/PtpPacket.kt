package com.paeki.fujirecipes.data.ptp

import java.nio.ByteBuffer
import java.nio.ByteOrder

data class PtpContainer(
    val length: Int,
    val type: Int,
    val code: Int,
    val transactionId: Int,
    val payload: ByteArray = ByteArray(0),
) {
    init {
        require(length >= HEADER_BYTES) { "PTP container length must include the 12-byte header." }
        require(type in 1..4) { "Unknown PTP container type: $type" }
    }

    companion object {
        const val HEADER_BYTES = 12

        fun parse(bytes: ByteArray): PtpContainer {
            require(bytes.size >= HEADER_BYTES) { "PTP container is too short." }

            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val length = buffer.int
            require(length in HEADER_BYTES..bytes.size) { "Invalid PTP container length: $length" }

            val type = buffer.short.toInt() and 0xFFFF
            val code = buffer.short.toInt() and 0xFFFF
            val transactionId = buffer.int
            val payload = bytes.copyOfRange(HEADER_BYTES, length)

            return PtpContainer(length, type, code, transactionId, payload)
        }
    }
}

fun buildCommandPacket(code: Int, transactionId: Int, params: List<Int> = emptyList()): ByteArray {
    val length = PtpContainer.HEADER_BYTES + (params.size * Int.SIZE_BYTES)
    val buffer = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN)
    buffer.putInt(length)
    buffer.putShort(PtpConstants.CONTAINER_COMMAND.toShort())
    buffer.putShort(code.toShort())
    buffer.putInt(transactionId)
    params.forEach(buffer::putInt)
    return buffer.array()
}

fun buildDataOutPacket(code: Int, transactionId: Int, payload: ByteArray): ByteArray {
    val length = PtpContainer.HEADER_BYTES + payload.size
    val buffer = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN)
    buffer.putInt(length)
    buffer.putShort(PtpConstants.CONTAINER_DATA.toShort())
    buffer.putShort(code.toShort())
    buffer.putInt(transactionId)
    buffer.put(payload)
    return buffer.array()
}

fun uint16Le(value: Int): ByteArray =
    ByteBuffer.allocate(Short.SIZE_BYTES)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putShort(value.toShort())
        .array()
