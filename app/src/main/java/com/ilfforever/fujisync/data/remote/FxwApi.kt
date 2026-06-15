package com.ilfforever.fujisync.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object FxwApi {

    private const val BASE = "https://fujixweekly.com/wp-json/wp/v2"
    private const val FIELDS = "id,slug,link,title,content,date"
    private const val MAX_RESPONSE_BYTES = 4L * 1024L * 1024L

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
        search: String? = null,
        etag: String? = null,
        lastModified: String? = null,
    ): FetchResult =
        withContext(Dispatchers.IO) {
            val searchParam = search?.trim()?.takeIf { it.isNotBlank() }?.let { "&search=${java.net.URLEncoder.encode(it, "UTF-8")}" }.orEmpty()
            val orderPart = if (searchParam.isNotEmpty()) "orderby=relevance" else "orderby=date&order=desc"
            val url = URL("$BASE/posts?page=$page&per_page=$perPage&_fields=$FIELDS&$orderPart$searchParam")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("User-Agent", "FujiRecipes/${com.ilfforever.fujisync.BuildConfig.VERSION_NAME} Android")
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
                val body = conn.inputStream.use { input ->
                    val buffer = StringBuilder()
                    val chunk = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val read = input.read(chunk)
                        if (read < 0) break
                        total += read
                        if (total > MAX_RESPONSE_BYTES) throw IOException("Response too large (>${MAX_RESPONSE_BYTES / 1024 / 1024} MB)")
                        buffer.append(String(chunk, 0, read, Charsets.UTF_8))
                    }
                    buffer.toString()
                }
                val arr = JSONArray(body)
                val rawCount = arr.length()
                val recipes = (0 until rawCount)
                    .mapNotNull { runCatching { parsePost(arr.getJSONObject(it)) }.getOrNull() }
                    .filter { it.params["Film Simulation"]?.let { s -> s.isNotBlank() && !s.equals("Any", ignoreCase = true) } == true && it.params.size >= 3 }
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
        val allVariants = parseAllVariants(content)
        val primaryParams = allVariants.firstOrNull() ?: parseParams(content)
        val title = obj.getJSONObject("title").getString("rendered").decodeHtml()
        return FxwRecipe(
            id = obj.getInt("id"),
            slug = obj.getString("slug"),
            title = title,
            postUrl = obj.getString("link"),
            imageUrls = extractImages(content),
            date = obj.getString("date").take(10),
            params = primaryParams,
            articleText = extractArticleText(content),
            variants = if (allVariants.size > 1) {
                // Label each variant by its film simulation; disambiguate duplicates with an index
                val simCounts = mutableMapOf<String, Int>()
                allVariants.mapIndexed { i, params ->
                    val sim = params["Film Simulation"]?.substringBefore("(")?.trim().orEmpty().ifBlank { "Recipe ${i + 1}" }
                    val seen = simCounts.getOrDefault(sim, 0) + 1
                    simCounts[sim] = seen
                    val label = if (allVariants.count { it["Film Simulation"]?.substringBefore("(")?.trim() == sim } > 1) "$sim ($seen)" else sim
                    FxwRecipeVariant(label = label, params = params)
                }
            } else emptyList(),
            xTransGen = if (allVariants.size <= 1) {
                val gens = detectAllXTransGens(title).ifEmpty { detectAllXTransGens(content) }
                gens.joinToString(", ").ifBlank { null }
            } else null,
        )
    }

    // Returns the params for every recipe block in the post, in order.
    // Per-variant labels/generations are intentionally NOT inferred from prose —
    // the article text cross-references other generations, making it unreliable.
    // Callers label variants by film simulation and direct users to the original
    // post for full camera-compatibility details.
    internal fun parseAllVariants(html: String): List<Map<String, String>> {
        val results = mutableListOf<Map<String, String>>()
        var from = 0
        while (true) {
            val markerIdx = html.indexOf("Film Simulation:", from).takeIf { it >= 0 } ?: break
            val params = parseParamsFromMarker(html, markerIdx, null)
            if (params.size >= 3) results += params
            from = (html.indexOf("</p>", markerIdx).takeIf { it >= 0 } ?: markerIdx) + 4
        }
        return results
    }

    private fun extractImages(html: String): List<String> {
        val storeLinkedSrcs = buildStoreLinkedImageUrls(html)
        return IMG_SRC.findAll(html)
            .mapNotNull { m ->
                val tag = m.value
                // skip tiny icons/avatars (width attribute < 200 if present)
                val w = IMG_WIDTH.find(tag)?.groupValues?.get(1)?.toIntOrNull()
                if (w != null && w < 200) return@mapNotNull null
                val raw = m.groupValues[1].takeIf { it.isNotBlank() } ?: return@mapNotNull null
                // skip images inside app-store / play-store anchor tags (catches generic filenames like img_5106.jpg)
                if (raw in storeLinkedSrcs) return@mapNotNull null
                // secondary: skip by URL keyword (catches badges hosted at recognizable paths)
                val rawLower = raw.lowercase()
                if (APP_STORE_URL_KEYWORDS.any { rawLower.contains(it) }) return@mapNotNull null
                if (raw.contains("wp.com") || raw.contains("wp-content")) {
                    raw.substringBefore("?") + "?w=1200&ssl=1"
                } else raw
            }
            .distinct()
            .toList()
    }

    // Collects the src values of every <img> nested inside an <a> that links to an app store.
    // This is reliable even when the image filename gives no hint (e.g. img_5106.jpg).
    private fun buildStoreLinkedImageUrls(html: String): Set<String> {
        val result = mutableSetOf<String>()
        val lower = html.lowercase()
        var pos = 0
        while (pos < lower.length) {
            val aOpen = lower.indexOf("<a ", pos).takeIf { it >= 0 } ?: break
            val aTagClose = lower.indexOf(">", aOpen).takeIf { it >= 0 } ?: break
            val aTag = lower.substring(aOpen, aTagClose + 1)
            if (APP_STORE_LINK_HOSTS.any { aTag.contains(it) }) {
                val aEnd = lower.indexOf("</a>", aTagClose).takeIf { it >= 0 } ?: break
                IMG_SRC.findAll(html.substring(aOpen, aEnd + 4)).forEach { imgMatch ->
                    result.add(imgMatch.groupValues[1])
                }
                pos = aEnd + 4
            } else {
                pos = aTagClose + 1
            }
        }
        return result
    }

    private val APP_STORE_URL_KEYWORDS = listOf(
        "app-store", "appstore", "play-store", "playstore", "google-play", "googleplay",
        "apple-store", "itunes-badge", "play-badge", "download-on-the",
    )
    private val APP_STORE_LINK_HOSTS = listOf(
        "apps.apple.com", "itunes.apple.com", "play.google.com",
    )

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

    internal fun parseParams(html: String): Map<String, String> {
        // Modern format: "Film Simulation: Velvia\nDynamic Range: ..." inside a <p>
        val modernMarker = "Film Simulation:"
        val modernIdx = html.indexOf(modernMarker)

        // Legacy format: "<strong>Classic Chrome</strong>\n<strong>Dynamic Range: ..."
        // The film sim name appears as a standalone bold token with no key label.
        val legacyFilmSims = listOf(
            "Classic Chrome", "Provia/STD", "Provia/Standard", "Provia", "Velvia", "Astia",
            "Classic Neg", "Classic Negative",
            "Nostalgic Neg.", "Nostalgic Neg", "Nostalgic Negative",
            "Pro Neg Hi", "Pro Neg Std", "PRO Neg. Hi", "PRO Neg. Std",
            "Eterna", "Eterna Bleach Bypass", "Bleach Bypass",
            "Acros", "Monochrome", "Sepia", "Reala Ace",
        )
        val legacyIdx = legacyFilmSims.firstNotNullOfOrNull { sim ->
            // Variant B: sim in its own closing bold tag — sim name must not be followed by alphanumeric
            val patternB = Regex("<(?:strong|b)>\\s*${Regex.escape(sim)}(?![\\w/])[^<]*</(?:strong|b)>", RegexOption.IGNORE_CASE)
            // Variant A: sim as first line of bold block — sim name must not be followed by alphanumeric
            val patternA = Regex("<(?:strong|b)>\\s*${Regex.escape(sim)}(?![\\w/])[^<]*<br", RegexOption.IGNORE_CASE)
            val match = patternB.find(html) ?: patternA.find(html) ?: return@firstNotNullOfOrNull null
            val idx = match.range.first
            // Reject if the <strong> is inside an <a> link (roundup/listicle posts)
            val precedingSlice = html.substring(maxOf(0, idx - 100), idx)
            if (precedingSlice.contains("<a ") && !precedingSlice.contains("</a>")) return@firstNotNullOfOrNull null
            idx
        }

        return when {
            modernIdx >= 0 -> parseParamsFromMarker(html, modernIdx, null)
            legacyIdx != null -> parseParamsFromLegacyBlock(html, legacyIdx, legacyFilmSims)
            else -> emptyMap()
        }
    }

    private fun parseParamsFromMarker(html: String, markerIdx: Int, filmSim: String?): Map<String, String> {
        val pStart = html.lastIndexOf("<p", markerIdx).takeIf { it >= 0 } ?: return emptyMap()
        val pEnd = html.indexOf("</p>", markerIdx).takeIf { it >= 0 } ?: return emptyMap()
        val block = html.substring(pStart, pEnd + 4)
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), "")
            .decodeHtml()
        return buildMap {
            filmSim?.let { put("Film Simulation", it) }
            block.lines().forEach { line ->
                val colon = line.indexOf(':')
                if (colon < 1) return@forEach
                val key = line.substring(0, colon).trim()
                val value = line.substring(colon + 1).trim()
                if (key.isNotBlank() && value.isNotBlank()) put(key, value)
            }
        }
    }

    private fun parseParamsFromLegacyBlock(html: String, simTagIdx: Int, knownSims: List<String>): Map<String, String> {
        val strongContent = run {
            val start = html.indexOf('>', simTagIdx).takeIf { it >= 0 }?.plus(1) ?: return emptyMap()
            val end = html.indexOf("</strong>", start).takeIf { it >= 0 } ?: return emptyMap()
            html.substring(start, end)
        }
        // Extract film sim name: everything before the first <br> (variant A) or the whole content (variant B)
        val brIdx = strongContent.indexOfFirst { it == '<' }.takeIf { it >= 0 }
        val simName = if (brIdx != null) {
            strongContent.substring(0, brIdx).replace(Regex("<[^>]+>"), "").decodeHtml().trim()
        } else {
            strongContent.replace(Regex("<[^>]+>"), "").decodeHtml().trim()
        }

        // Params block: either rest of this <strong> after the film sim line (variant A),
        // or the next <strong> block (variant B)
        val paramsBlock = if (brIdx != null && strongContent.length > brIdx) {
            // Variant A: params are in the same strong after the first <br>
            strongContent.substring(brIdx)
        } else {
            // Variant B: params are in the very next <strong> block
            val simTagEnd = html.indexOf("</strong>", simTagIdx).takeIf { it >= 0 } ?: return buildMap { put("Film Simulation", simName) }
            val nextStart = html.indexOf("<strong>", simTagEnd).takeIf { it >= 0 } ?: return buildMap { put("Film Simulation", simName) }
            val nextEnd = html.indexOf("</strong>", nextStart).takeIf { it >= 0 } ?: return buildMap { put("Film Simulation", simName) }
            html.substring(nextStart, nextEnd + 9)
        }

        val text = paramsBlock
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), "")
            .decodeHtml()

        return buildMap {
            put("Film Simulation", simName)
            text.lines().forEach { line ->
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
        .replace("&apos;", "’")
        .replace("&#39;", "’")
        .replace("&#x27;", "’")
        .replace("&#8217;", "’")
        .replace("&#8216;", "‘")
        .replace("&#8220;", """)
        .replace("&#8221;", """)
        .replace("&#8211;", "–")
        .replace("&#8212;", "—")
        .replace("&#8230;", "…")
        .replace("&hellip;", "…")
        .replace("&#8226;", "•")
        .replace("&bull;", "•")
        .replace("&ndash;", "–")
        .replace("&mdash;", "—")
        .replace("&lsquo;", "‘")
        .replace("&rsquo;", "’")
        .replace("&ldquo;", """)
        .replace("&rdquo;", """)
        .replace("&#160;", " ")
        .replace("&nbsp;", " ")
}
