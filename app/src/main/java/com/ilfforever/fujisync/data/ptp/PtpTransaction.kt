package com.ilfforever.fujisync.data.ptp

data class PtpTransaction(
    val data: PtpContainer?,
    val response: PtpContainer,
) {
    val isOk: Boolean
        get() = response.code == PtpConstants.RESPONSE_OK
}

class PtpProtocolException(message: String) : IllegalStateException(message)

/**
 * The USB pipe failed, or the bytes coming back no longer parse as PTP.
 *
 * Kept separate from a camera answering with a rejection code: a rejection is an answer, and the
 * connection is still good afterwards. This means the connection itself can no longer be trusted,
 * so throwing one poisons the session rather than letting the next command read someone else's
 * reply.
 */
class PtpTransportException(
    val kind: Kind,
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause) {
    enum class Kind {
        /** The transfer itself failed or timed out. */
        BulkIo,

        /** Bytes arrived, but not the ones this transaction was waiting for. */
        Desync,
    }
}
