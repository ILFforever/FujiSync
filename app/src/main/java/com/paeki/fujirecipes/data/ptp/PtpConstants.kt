package com.paeki.fujirecipes.data.ptp

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
    const val GET_DEVICE_PROP_VALUE = 0x1015
    const val SET_DEVICE_PROP_VALUE = 0x1016

    const val RESPONSE_OK = 0x2001
    const val RESPONSE_SESSION_ALREADY_OPEN = 0x201E

    const val FUJI_SLOT_SELECTOR = 0xD18C
    const val FUJI_PRESET_NAME = 0xD18D
    const val PRESET_BLOCK_START = 0xD18E
    const val PRESET_BLOCK_END = 0xD1A5

    const val BULK_CHUNK_SIZE = 16_384
    const val STANDARD_TIMEOUT_MS = 5_000
    const val HEARTBEAT_TIMEOUT_MS = 3_000
    const val MAX_SMALL_CONTAINER_BYTES = 4_194_304
}

internal val MONO_SIM_CODES: Set<Int> = setOf(6, 7, 8, 9, 10, 12, 13, 14, 15)
