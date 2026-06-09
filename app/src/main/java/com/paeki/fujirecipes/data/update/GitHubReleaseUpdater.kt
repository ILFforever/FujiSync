package com.paeki.fujirecipes.data.update

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class AppUpdateRelease(
    val versionName: String,
    val tagName: String,
    val releaseName: String,
    val body: String,
    val assetName: String,
    val downloadUrl: String,
)

data class DownloadedUpdate(
    val release: AppUpdateRelease,
    val uri: Uri,
)

class GitHubReleaseUpdater(
    private val context: Context,
    private val repo: String,
) {
    fun fetchLatestRelease(): Result<AppUpdateRelease> = runCatching {
        val json = getJson("https://api.github.com/repos/$repo/releases/latest")
        val asset = json.getJSONArray("assets")
            .asSequence()
            .mapNotNull { it as? JSONObject }
            .filter { asset ->
                val name = asset.optString("name")
                val contentType = asset.optString("content_type")
                name.endsWith(".apk", ignoreCase = true) ||
                    contentType == "application/vnd.android.package-archive"
            }
            .sortedWith(
                compareBy<JSONObject> {
                    val name = it.optString("name").lowercase(Locale.US)
                    name.contains("debug") || name.contains("unsigned")
                }.thenBy { it.optString("name") }
            )
            .firstOrNull()
            ?: error("Latest GitHub release does not include an APK asset.")

        val tag = json.getString("tag_name")
        AppUpdateRelease(
            versionName = tag.trim().removePrefix("v").removePrefix("V"),
            tagName = tag,
            releaseName = json.optString("name").ifBlank { tag },
            body = json.optString("body"),
            assetName = asset.getString("name"),
            downloadUrl = asset.getString("browser_download_url"),
        )
    }

    fun download(release: AppUpdateRelease): Result<DownloadedUpdate> = runCatching {
        val dir = File(context.cacheDir, "github_updates").also { it.mkdirs() }
        dir.listFiles()?.forEach { file -> if (file.isFile) file.delete() }
        val outFile = File(dir, safeFileName(release.assetName))

        val connection = (URL(release.downloadUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 60_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/octet-stream")
            setRequestProperty("User-Agent", "FujiSync")
        }
        try {
            if (connection.responseCode !in 200..299) {
                error("GitHub download failed with HTTP ${connection.responseCode}.")
            }
            connection.inputStream.use { input ->
                outFile.outputStream().use { output -> input.copyTo(output) }
            }
        } finally {
            connection.disconnect()
        }

        DownloadedUpdate(
            release = release,
            uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outFile,
            ),
        )
    }

    private fun getJson(url: String): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 20_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "FujiSync")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        }
        return try {
            if (connection.responseCode !in 200..299) {
                error("GitHub update check failed with HTTP ${connection.responseCode}.")
            }
            JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }
}

fun isRemoteVersionNewer(current: String, remote: String): Boolean {
    val currentParts = current.versionParts()
    val remoteParts = remote.versionParts()
    val max = maxOf(currentParts.size, remoteParts.size)
    for (i in 0 until max) {
        val c = currentParts.getOrElse(i) { 0 }
        val r = remoteParts.getOrElse(i) { 0 }
        if (r != c) return r > c
    }
    return false
}

private fun String.versionParts(): List<Int> =
    trim()
        .substringBefore('-')
        .removePrefix("v")
        .removePrefix("V")
        .split('.')
        .map { part -> part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }

private fun safeFileName(name: String): String =
    name.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "fujisync-update.apk" }

private fun org.json.JSONArray.asSequence(): Sequence<Any?> = sequence {
    for (i in 0 until length()) yield(opt(i))
}
