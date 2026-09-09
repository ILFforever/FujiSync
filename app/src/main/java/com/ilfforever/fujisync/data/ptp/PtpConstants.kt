package com.ilfforever.fujisync.data.ptp

object PtpConstants {
    const val FUJI_VENDOR_ID = 0x04CB
    const val PTP_INTERFACE_CLASS = 0x06
    const val MASS_STORAGE_INTERFACE_CLASS = 0x08

    const val CONTAINER_COMMAND = 1
    const val CONTAINER_DATA = 2
    const val CONTAINER_RESPONSE = 3

    const val GET_DEVICE_INFO = 0x1001
    const val OPEN_SESSION = 0x1002
    const val CLOSE_SESSION = 0x1003
    const val GET_DEVICE_PROP_DESC = 0x1014
    const val GET_DEVICE_PROP_VALUE = 0x1015
    const val SET_DEVICE_PROP_VALUE = 0x1016

    const val RESPONSE_OK = 0x2001
    const val RESPONSE_SESSION_NOT_OPEN = 0x2003
    const val RESPONSE_DEVICE_BUSY = 0x2019
    const val RESPONSE_SESSION_ALREADY_OPEN = 0x201E

    /**
     * Still Image class device reset — USB Still Image Capture Device definition §5.2.4, a class
     * request on the interface. Tells the camera to drop whatever transaction state it is holding,
     * which is the only way back from a desync short of unplugging the cable.
     */
    const val STILL_IMAGE_RESET_REQUEST_TYPE = 0x21
    const val STILL_IMAGE_RESET_REQUEST = 0x66
    const val STILL_IMAGE_RESET_TIMEOUT_MS = 1_000

    /**
     * A camera that has just enumerated will refuse OpenSession for a moment, and one that has just
     * been reset needs longer still. Polling costs nothing when the body is ready on the first try.
     */
    const val OPEN_SESSION_ATTEMPTS = 10
    const val OPEN_SESSION_POLL_MS = 200L
    const val POST_RESET_DELAY_MS = 500L

    /** GetDeviceInfo is the first real payload we ask for, and the one bodies most often fumble. */
    const val DEVICE_INFO_ATTEMPTS = 5
    const val DEVICE_INFO_RETRY_BASE_DELAY_MS = 200L

    const val CLOSE_SESSION_TIMEOUT_MS = 1_000

    const val FUJI_SLOT_SELECTOR = 0xD18C
    const val FUJI_PRESET_NAME = 0xD18D
    const val PRESET_BLOCK_START = 0xD18E
    const val PRESET_BLOCK_END = 0xD1A5

    const val BULK_CHUNK_SIZE = 16_384
    const val STANDARD_TIMEOUT_MS = 5_000
    const val HEARTBEAT_TIMEOUT_MS = 3_000
    const val MAX_SMALL_CONTAINER_BYTES = 4_194_304
}

/** Human-readable name for a PTP response code, so diagnostics don't just show a bare number. */
fun ptpResponseName(code: Int): String = when (code) {
    0x2001 -> "OK"
    0x2002 -> "GeneralError"
    0x2003 -> "SessionNotOpen"
    0x2004 -> "InvalidTransactionID"
    0x2005 -> "OperationNotSupported"
    0x2006 -> "ParameterNotSupported"
    0x2007 -> "IncompleteTransfer"
    0x200A -> "DevicePropNotSupported"
    0x200F -> "AccessDenied"
    0x2013 -> "StoreNotAvailable"
    0x2019 -> "DeviceBusy"
    0x201B -> "InvalidDevicePropFormat"
    0x201C -> "InvalidDevicePropValue"
    0x201D -> "InvalidParameter"
    0x201E -> "SessionAlreadyOpen"
    else -> "0x%04X".format(code)
}

internal val MONO_SIM_CODES: Set<Int> = setOf(6, 7, 8, 9, 10, 12, 13, 14, 15)
