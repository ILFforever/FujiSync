package com.paeki.fujirecipes.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object FxwApi {

    private const val BASE = "https://fujixweekly.com/wp-json/wp/v2"
    private const val FIELDS = "id,slug,link,title,content,date"

    private val IMG_SRC = Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    private val IMG_WIDTH = Regex("""width=["']?(\d+)["']?""", RegexOption.IGNORE_CASE)

    data class FetchResult(
        val recipes: List<FxwRecipe>,
        val hasMore: Boolean,
        val etag: String? = null,
        val lastModified: String? = null,
        val notModified: Boolean = false,
    )

    suspend fun fetchRecipes(
        page: Int = 1,
        perPage: Int = 50,
        etag: String? = null,
        lastModified: String? = null,
    ): FetchResult =
        withContext(Dispatchers.IO) {
            val url = URL("$BASE/posts?page=$page&per_page=$perPage&_fields=$FIELDS&orderby=date&order=desc")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("User-Agent", "FujiSync/1.0 Android")
            etag?.takeIf { it.isNotBlank() }?.let { conn.setRequestProperty("If-None-Match", it) }
            lastModified?.takeIf { it.isNotBlank() }?.let { conn.setRequestProperty("If-Modified-Since", it) }
            try {
                val code = conn.responseCode
                val responseEtag = conn.getHeaderField("ETag")
                val responseLastModified = conn.getHeaderField("Last-Modified")
                if (code == HttpURLConnection.HTTP_NOT_MODIFIED) {
                    return@withContext FetchResult(
                        recipes = emptyList(),
                        hasMore = true,
                        etag = responseEtag ?: etag,
                        lastModified = responseLastModified ?: lastModified,
                        notModified = true,
                    )
                }
                if (code !in 200..299) {
                    val body = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    throw IOException("Fuji X Weekly returned HTTP $code${body.takeIf { it.isNotBlank() }?.let { ": $it" }.orEmpty()}")
                }
                val body = conn.inputStream.bufferedReader().readText()
                val arr = JSONArray(body)
                val rawCount = arr.length()
                val recipes = (0 until rawCount)
                    .mapNotNull { runCatching { parsePost(arr.getJSONObject(it)) }.getOrNull() }
                    .filter { it.params.containsKey("Film Simulation") }
                FetchResult(
                    recipes = recipes,
                    hasMore = rawCount >= perPage,
                    etag = responseEtag,
                    lastModified = responseLastModified,
                )
            } finally {
                conn.disconnect()
            }
        }

    private fun parsePost(obj: org.json.JSONObject): FxwRecipe {
        val content = obj.getJSONObject("content").getString("rendered")
        return FxwRecipe(
            id = obj.getInt("id"),
            slug = obj.getString("slug"),
            title = obj.getJSONObject("title").getString("rendered").decodeHtml(),
            postUrl = obj.getString("link"),
            imageUrls = extractImages(content),
            date = obj.getString("date").take(10),
            params = parseParams(content),
            articleText = extractArticleText(content),
        )
    }

    private fun extractImages(html: String): List<String> =
        IMG_SRC.findAll(html)
            .mapNotNull { m ->
                val tag = m.value
                // skip tiny icons/avatars (width attribute < 200 if present)
                val w = IMG_WIDTH.find(tag)?.groupValues?.get(1)?.toIntOrNull()
                if (w != null && w < 200) return@mapNotNull null
                val raw = m.groupValues[1].takeIf { it.isNotBlank() } ?: return@mapNotNull null
                if (raw.contains("wp.com") || raw.contains("wp-content")) {
                    raw.substringBefore("?") + "?w=1200&ssl=1"
                } else raw
            }
            .distinct()
            .toList()

    private fun extractArticleText(html: String): String {
        val recipeIdx = html.indexOf("Film Simulation:")
        if (recipeIdx < 0) return ""
        val beforeRecipe = html.substring(0, recipeIdx)
        return beforeRecipe
            .replace(Regex("<img[^>]+>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<[^>]+>"), " ")
            .decodeHtml()
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun parseParams(html: String): Map<String, String> {
        val marker = "Film Simulation:"
        val markerIdx = html.indexOf(marker).takeIf { it >= 0 } ?: return emptyMap()
        val pStart = html.lastIndexOf("<p", markerIdx).takeIf { it >= 0 } ?: return emptyMap()
        val pEnd = html.indexOf("</p>", markerIdx).takeIf { it >= 0 } ?: return emptyMap()

        val block = html.substring(pStart, pEnd + 4)
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), "")
            .decodeHtml()

        return buildMap {
            block.lines().forEach { line ->
                val colon = line.indexOf(':')
                if (colon < 1) return@forEach
                val key = line.substring(0, colon).trim()
                val value = line.substring(colon + 1).trim()
                if (key.isNotBlank() && value.isNotBlank()) put(key, value)
            }
        }
    }

    private fun String.decodeHtml(): String = this
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#8217;", "’")
        .replace("&#8216;", "‘")
        .replace("&#8220;", "“")
        .replace("&#8221;", "”")
        .replace("&#8211;", "–")
        .replace("&#8212;", "—")
        .replace("&nbsp;", " ")
}
