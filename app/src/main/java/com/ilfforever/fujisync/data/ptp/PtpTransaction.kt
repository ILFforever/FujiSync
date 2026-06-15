package com.ilfforever.fujisync.data.ptp

data class PtpTransaction(
    val data: PtpContainer?,
    val response: PtpContainer,
) {
    val isOk: Boolean
        get() = response.code == PtpConstants.RESPONSE_OK
}

class PtpProtocolException(message: String) : IllegalStateException(message)
