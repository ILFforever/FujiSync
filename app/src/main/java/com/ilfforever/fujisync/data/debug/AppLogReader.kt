package com.ilfforever.fujisync.data.debug

import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Reads this app's own recent log output via `logcat`. Android restricts a non-privileged app to
 * seeing only its own UID's log lines this way — no READ_LOGS permission, root, or adb needed.
 * Same mechanism most in-app "send us your logs" screens use.
 */
object AppLogReader {
    private const val DEFAULT_MAX_LINES = 1000

    fun readRecent(maxLines: Int = DEFAULT_MAX_LINES): String = runCatching {
        val process = ProcessBuilder("logcat", "-d", "-v", "time")
            .redirectErrorStream(true)
            .start()

        val lines = BufferedReader(InputStreamReader(process.inputStream)).use { it.readLines() }
        process.waitFor()

        lines.takeLast(maxLines).joinToString("\n")
    }.getOrElse { error ->
        "Could not read the device log: ${error.message ?: error::class.java.simpleName}"
    }
}
