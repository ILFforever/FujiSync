package com.paeki.fujirecipes.capture

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CaptureDiag {
    private const val PREFS = "capture_diag"
    private const val KEY = "log"
    private val fmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    fun log(context: Context, msg: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY, "") ?: ""
        val line = "[${fmt.format(Date())}] $msg"
        prefs.edit().putString(KEY, if (existing.isBlank()) line else "$existing\n$line").apply()
    }

    fun read(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "") ?: ""

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply()
    }
}
