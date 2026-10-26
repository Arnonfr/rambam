package com.example.data.tanya

import android.content.Context
import android.util.Xml
import com.example.domain.tanya.DailyTanyaLesson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

data class AshkenaziTanyaLesson(val id: String, val title: String, val subtitle: String,
    val order: Int, val chapters: List<Int>, val website: String)

/** The public podcast is Likkutei Amarim. Chapter numbers in other books must not match it. */
class AshkenaziTanyaRepository private constructor(context: Context) {
    private val app = context.applicationContext
    private val file = File(app.cacheDir, "ashkenazi_tanya_rss.xml")
    private val mutex = Mutex()
    private val client = OkHttpClient.Builder().connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS).callTimeout(12, TimeUnit.SECONDS).build()
    val lessons: List<AshkenaziTanyaLesson> by lazy {
        val rows = JSONObject(app.assets.open("tanya_ashkenazi_catalog.json").bufferedReader().use { it.readText() }).getJSONArray("lessons")
        (0 until rows.length()).map { index ->
            val row = rows.getJSONObject(index)
            val chapters = row.getJSONArray("chapters")
            AshkenaziTanyaLesson(row.getString("id"), row.getString("title"), row.getString("subtitle"),
                row.getInt("order"), (0 until chapters.length()).map { chapters.getInt(it) }, row.getString("website"))
        }.sortedBy { it.order }
    }

    // Refresh signed enclosure URLs from the publisher, never ship expiring URLs in the APK.
    suspend fun resolve(id: String, refresh: Boolean = false): TanyaAudioClip? = withContext(Dispatchers.IO) {
        val lesson = lessons.find { it.id == id } ?: return@withContext null
        mutex.withLock {
            try {
                val fresh = file.exists() && System.currentTimeMillis() - file.lastModified() < TimeUnit.HOURS.toMillis(12)
                val xml = if (!refresh && fresh) file.readText() else {
                    val response = client.newCall(Request.Builder().url(FEED).build()).execute().use {
                        if (!it.isSuccessful) return@withLock null
                        it.body?.string() ?: return@withLock null
                    }
                    coroutineContext.ensureActive()
                    if (enclosures(response).isEmpty()) return@withLock null
                    file.writeText(response)
                    response
                }
                val url = enclosures(xml)[id] ?: return@withLock null
                // Some publisher entries currently point to missing files. Keep a direct official
                // lesson-page fallback instead of offering a play button for a known HTTP failure.
                val playable = client.newCall(Request.Builder().url(url).head().build()).execute().use {
                    it.isSuccessful && it.header("Content-Type").orEmpty().startsWith("audio/")
                }
                if (!playable) return@withLock null
                coroutineContext.ensureActive()
                TanyaAudioClip("ashkenazi:$id", lesson.title, url, "",
                    lesson.chapters.firstOrNull()?.let { "Tanya, Part I; Likkutei Amarim $it:1" }.orEmpty(), "",
                    chapterLesson = true, teacher = "הרב שניאור אשכנזי", sourceUrl = lesson.website)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                null
            }
        }
    }

    companion object {
        const val SOURCE = "https://he.chabad.org/library/article_cdo/aid/5287961"
        const val FEED = "https://www.chabad.org/tools/rss/itunes/seriesrss_cdo/scope/5287961/variant/30977"
        fun matchingChapter(lesson: DailyTanyaLesson): Int? =
            Regex("^Tanya, Part I; Likkutei Amarim (\\d+):\\d+(?:-\\d+)?$")
                .matchEntire(lesson.fullRef)?.groupValues?.get(1)?.toIntOrNull()

        internal fun enclosures(xml: String): Map<String, String> {
            require(xml.length <= 2_000_000 && !xml.contains("<!DOCTYPE", ignoreCase = true))
            val parser = Xml.newPullParser().apply { setInput(xml.reader()) }
            val result = linkedMapOf<String, String>()
            var inItem = false
            var id: String? = null
            var url: String? = null
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                when (parser.eventType) {
                    XmlPullParser.START_TAG -> when (parser.name) {
                        "item" -> { inItem = true; id = null; url = null }
                        "guid" -> if (inItem) id = parser.nextText().trim()
                        "enclosure" -> if (inItem && parser.getAttributeValue(null, "type")?.startsWith("audio/") == true) {
                            val candidate = parser.getAttributeValue(null, "url")?.toHttpUrlOrNull()
                            if (candidate != null && candidate.isHttps && candidate.host == "http-vod-cdn.chabad.org"
                                && candidate.encodedPath.endsWith(".mp3")) url = candidate.toString()
                        }
                    }
                    XmlPullParser.END_TAG -> if (parser.name == "item") {
                        if (inItem && id != null && url != null) result[id] = url
                        inItem = false
                    }
                }
                parser.next()
            }
            return result
        }
        @Volatile private var instance: AshkenaziTanyaRepository? = null
        fun get(context: Context): AshkenaziTanyaRepository = instance ?: synchronized(this) {
            instance ?: AshkenaziTanyaRepository(context).also { instance = it }
        }
    }
}
