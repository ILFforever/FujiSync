package com.ilfforever.fujisync.data.usb

import com.ilfforever.fujisync.data.ptp.PtpConstants
import com.ilfforever.fujisync.data.ptp.hexDump
import com.ilfforever.fujisync.data.ptp.ptpResponseName

// Captures exactly what a camera sends back for one operation, without assuming how many containers
// arrive. Used to tell "this camera rejects the operation" apart from "we are forming the request
// wrongly" — the two look identical if you only ever see a response code.

data class RawContainerLine(
    val type: Int,
    val code: Int,
    val transactionId: Int,
    val payloadBytes: Int,
    val payloadHex: String,
) {
    val typeName: String
        get() = when (type) {
            PtpConstants.CONTAINER_COMMAND -> "COMMAND"
            PtpConstants.CONTAINER_DATA -> "DATA"
            PtpConstants.CONTAINER_RESPONSE -> "RESPONSE"
            else -> "TYPE$type"
        }

    override fun toString(): String = buildString {
        append("%-8s code=0x%04X".format(typeName, code))
        if (type == PtpConstants.CONTAINER_RESPONSE) append(" (${ptpResponseName(code)})")
        append(" txn=$transactionId bytes=$payloadBytes")
        if (payloadBytes > 0) append("\n           $payloadHex")
    }
}

data class OperationProbe(
    val label: String,
    val opcode: Int,
    val param: Int,
    val containers: List<RawContainerLine>,
    val error: String? = null,
) {
    val summary: String
        get() = when {
            error != null -> "exception: $error"
            containers.isEmpty() -> "no reply at all"
            else -> containers.joinToString("  |  ") {
                if (it.type == PtpConstants.CONTAINER_RESPONSE) ptpResponseName(it.code)
                else "${it.typeName}(${it.payloadBytes}B)"
            }
        }
}

private fun probe(
    connection: OpenPtpConnection,
    label: String,
    opcode: Int,
    param: Int,
): OperationProbe = runCatching {
    val containers = connection.rawExchange(opcode, listOf(param)).map {
        RawContainerLine(it.type, it.code, it.transactionId, it.payload.size, hexDump(it.payload, maxBytes = 48))
    }
    OperationProbe(label, opcode, param, containers)
}.getOrElse {
    OperationProbe(label, opcode, param, emptyList(), it.message ?: it::class.java.simpleName)
}

/**
 * Runs the same property through the operation we suspect is broken and the one known to work.
 * If GetDevicePropValue succeeds where GetDevicePropDesc fails on the *same* property in the *same*
 * session, the request plumbing is fine and the camera is genuinely refusing that operation.
 */
fun runOperationDiagnostic(connection: OpenPtpConnection): List<OperationProbe> = listOf(
    // Control: this is the exact call the app makes successfully on every recipe read.
    probe(connection, "GetDevicePropValue on 0xD18C (control, known good)", PtpConstants.GET_DEVICE_PROP_VALUE, 0xD18C),
    probe(connection, "GetDevicePropValue on 0x5005 (PTP standard)", PtpConstants.GET_DEVICE_PROP_VALUE, 0x5005),

    // The operation under suspicion, against a Fuji code and a PTP-standard code.
    probe(connection, "GetDevicePropDesc on 0xD18C (Fuji vendor)", PtpConstants.GET_DEVICE_PROP_DESC, 0xD18C),
    probe(connection, "GetDevicePropDesc on 0x5005 (PTP standard)", PtpConstants.GET_DEVICE_PROP_DESC, 0x5005),
    probe(connection, "GetDevicePropDesc on 0xD007 (live Dynamic Range)", PtpConstants.GET_DEVICE_PROP_DESC, 0xD007),

    // Does the session survive a rejected operation, or is everything after it broken?
    probe(connection, "GetDevicePropValue on 0xD18C (again, after the failures)", PtpConstants.GET_DEVICE_PROP_VALUE, 0xD18C),
)

fun formatOperationDiagnostic(probes: List<OperationProbe>): String = buildString {
    appendLine("OPERATION DIAGNOSTIC")
    probes.forEach { p ->
        appendLine()
        appendLine("${p.label}")
        appendLine("  request: opcode=0x%04X param=0x%04X".format(p.opcode, p.param))
        appendLine("  result : ${p.summary}")
        p.containers.forEach { appendLine("  <- $it") }
    }
}
