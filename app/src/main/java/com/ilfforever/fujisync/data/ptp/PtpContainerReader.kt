package com.ilfforever.fujisync.data.ptp

/**
 * Reassembles PTP containers out of a USB bulk-IN byte stream.
 *
 * Bulk reads have nothing to do with container boundaries. One container can arrive split across
 * several reads, two containers can arrive in one read, and a read can end mid-header. Framing
 * therefore has to be driven by the length field in the stream, never by how much a single read
 * happened to return — and whatever a read delivered past the end of one container has to survive
 * until the next call, or the following exchange starts mid-container.
 *
 * This lives apart from the USB code deliberately: framing is where desync bugs hide, and a pure
 * byte-in/container-out class can be tested one byte at a time without a camera attached.
 */
class PtpContainerReader(
    private val maxContainerBytes: Int = PtpConstants.MAX_SMALL_CONTAINER_BYTES,
) {
    private var buffer = ByteArray(INITIAL_CAPACITY)
    private var start = 0
    private var end = 0

    /** Bytes held that do not yet form a complete container. */
    val buffered: Int get() = end - start

    /** Adds the first [length] bytes of [bytes] — the shape a `bulkTransfer` result comes back in. */
    fun append(bytes: ByteArray, length: Int = bytes.size) {
        require(length >= 0 && length <= bytes.size) {
            "Cannot append $length bytes from a ${bytes.size}-byte array."
        }
        if (length == 0) return

        ensureCapacity(length)
        bytes.copyInto(buffer, end, 0, length)
        end += length

        if (buffered > maxContainerBytes) {
            throw PtpTransportException(
                PtpTransportException.Kind.Desync,
                "Buffered PTP input reached $buffered bytes without forming a container.",
            )
        }
    }

    /**
     * The next complete container, or null when more bytes are needed. Throws only when the stream
     * cannot be PTP at all — a length that is impossible means the two sides have lost sync, and
     * reading further would compound it.
     */
    fun next(): PtpContainer? {
        if (buffered < PtpContainer.HEADER_BYTES) return null

        val length = readUInt32Le(buffer, start)
        if (length < PtpContainer.HEADER_BYTES || length > maxContainerBytes) {
            throw PtpTransportException(
                PtpTransportException.Kind.Desync,
                "Invalid PTP container length: $length.",
            )
        }

        if (buffered < length) return null

        val size = length.toInt()
        val container = PtpContainer.parse(buffer.copyOfRange(start, start + size))
        start += size
        if (start == end) {
            start = 0
            end = 0
        }
        return container
    }

    /** Drops everything buffered. Used after a reset, when leftover bytes describe the old world. */
    fun reset() {
        start = 0
        end = 0
    }

    private fun ensureCapacity(extra: Int) {
        if (end + extra <= buffer.size) return

        val used = buffered
        if (start > 0 && used + extra <= buffer.size) {
            buffer.copyInto(buffer, 0, start, end)
            start = 0
            end = used
            return
        }

        var capacity = maxOf(buffer.size, INITIAL_CAPACITY)
        while (capacity < used + extra) capacity *= 2
        val grown = ByteArray(capacity)
        buffer.copyInto(grown, 0, start, end)
        buffer = grown
        start = 0
        end = used
    }

    private companion object {
        const val INITIAL_CAPACITY = 32 * 1024
    }
}
