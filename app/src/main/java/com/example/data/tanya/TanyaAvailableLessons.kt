package com.example.data.tanya

import android.content.Context
import android.util.Xml
import com.example.domain.tanya.DailyTanyaLesson
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

data class TanyaAvailableLesson(
    val id: String, val teacher: String, val title: String, val book: String,
    val chapters: List<Int>, val kind: String, val media: String, val source: String,
    val subtitle: String = ""
) {
    fun matches(lesson: DailyTanyaLesson): Boolean {
        val key = TanyaAudioMatch.chapterKey(lesson.fullRef) ?: return false
        val chapter = key.substringAfterLast(' ').toIntOrNull() ?: return false
        return book.isNotBlank() && key.substringBeforeLast(' ') == book && chapter in chapters
    }
}

/** Publisher metadata is bundled, so browsing does not wait for a network request. */
class TanyaAvailableLessons private constructor(context: Context) {
    private val app = context.applicationContext
    private val client = OkHttpClient.Builder().connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS).callTimeout(9, TimeUnit.SECONDS).build()
    val bundled: List<TanyaAvailableLesson> by lazy {
        parse(JSONObject(app.assets.open("tanya_available_catalog.json").bufferedReader().use { it.readText() }).getJSONArray("lessons"))
    }
    fun cached(teacher: String): List<TanyaAvailableLesson> {
        val additions = runCatching { parse(JSONArray(cache(teacher).readText())) }.getOrDefault(emptyList())
        return (additions + bundled.filter { it.teacher == teacher }).distinctBy { it.id }
    }
    private fun cache(teacher: String): File {
        require(teacher in setOf("nadav", "wilhelm", "teib"))
        return File(app.cacheDir, "tanya_available_$teacher.json")
    }

    suspend fun refresh(teacher: String): List<TanyaAvailableLesson> = withContext(Dispatchers.IO) {
        val file = cache(teacher)
        if (teacher == "teib" || (file.exists() && System.currentTimeMillis() - file.lastModified() < TimeUnit.HOURS.toMillis(12)))
            return@withContext cached(teacher)
        try {
            val updates = when (teacher) {
                "wilhelm" -> podcastEntries(fetch(WILHELM_FEED))
                "nadav" -> coroutineScope {
                    val slots = Semaphore(3)
                    (0L..6L).map { days -> async { slots.withPermit {
                        runCatching {
                            val date = LocalDate.now().minusDays(days).toString()
                            dailyEntries(JSONArray(fetch("https://tanyayomi.com/yy/yyGetToday.php?day=$date")), date)
                        }.getOrElse { if (it is CancellationException) throw it else emptyList() }
                    } } }.awaitAll().flatten()
                }
                else -> emptyList()
            }
            coroutineContext.ensureActive()
            if (updates.isNotEmpty()) {
                val merged = (updates + cached(teacher)).distinctBy { it.id }.take(500)
                file.writeText(JSONArray().apply { merged.forEach { put(toJson(it)) } }.toString())
            }
        } catch (e: Exception) { if (e is CancellationException) throw e }
        cached(teacher)
    }

    suspend fun resolve(record: TanyaAvailableLesson): TanyaAudioClip? = withContext(Dispatchers.IO) {
        if (record.kind != "audio") return@withContext null
        try {
            val url = record.media.toHttpUrlOrNull() ?: return@withContext null
            if (!url.isHttps || url.host !in setOf("bit.ly", "tanyayomi.com", "www.tanyayomi.com", "podcasts.captivate.fm")) return@withContext null
            val direct = client.newCall(Request.Builder().url(url).head().build()).execute().use { r ->
                val final = r.request.url
                val allowed = if (record.teacher == "wilhelm") final.host == "podcasts.captivate.fm"
                    else final.host in setOf("tanyayomi.com", "www.tanyayomi.com")
                if (r.isSuccessful && final.isHttps && allowed && r.header("Content-Type").orEmpty().startsWith("audio/")) final.toString() else null
            } ?: return@withContext null
            coroutineContext.ensureActive()
            TanyaAudioClip(record.id, record.title, direct, "", "", "", teacher = teacherName(record.teacher), sourceUrl = record.source)
        } catch (e: Exception) { if (e is CancellationException) throw e; null }
    }
    private fun fetch(url: String): String = client.newCall(Request.Builder().url(url).build()).execute().use {
        check(it.isSuccessful)
        val body = it.body ?: error("No body")
        body.source().request(2_000_001)
        check(body.source().buffer.size <= 2_000_000)
        body.string()
    }

    companion object {
        const val WILHELM_FEED = "https://feeds.captivate.fm/rbaruchwilhelm/"
        fun teacherName(key: String) = when (key) {
            "wilhelm" -> "הרב ברוך וילהלם"
            "teib" -> "הרב מיכאל טייב"
            "ashkenazi" -> "הרב שניאור אשכנזי"
            else -> "הרב נדב כהן"
        }
        internal fun parse(rows: JSONArray): List<TanyaAvailableLesson> = (0 until rows.length()).mapNotNull { index ->
            runCatching {
                val r = rows.getJSONObject(index)
                val kind = r.getString("kind")
                val teacher = r.getString("teacher")
                require(teacher in setOf("nadav", "wilhelm", "teib"))
                val media = r.getString("media")
                require(kind == "audio" || (kind == "youtube" && media.matches(Regex("[A-Za-z0-9_-]{11}"))))
                if (kind == "audio") require(media.toHttpUrlOrNull()?.let { it.isHttps && it.host in
                    setOf("bit.ly", "tanyayomi.com", "www.tanyayomi.com", "podcasts.captivate.fm") } == true)
                val source = r.getString("source").toHttpUrlOrNull() ?: error("Invalid source")
                require(source.isHttps && source.host in setOf("www.youtube.com", "tanyayomi.com", "rbaruchwilhelm.captivate.fm"))
                val ch = r.getJSONArray("chapters")
                TanyaAvailableLesson(r.getString("id"), teacher, r.getString("title"), r.optString("book"),
                    (0 until ch.length()).map { ch.getInt(it) }, kind, media, source.toString(), r.optString("subtitle"))
            }.getOrNull()
        }
        internal fun toJson(r: TanyaAvailableLesson) = JSONObject().put("id", r.id).put("teacher", r.teacher)
            .put("title", r.title).put("book", r.book).put("chapters", JSONArray(r.chapters)).put("kind", r.kind)
            .put("media", r.media).put("source", r.source).put("subtitle", r.subtitle)

        internal fun dailyEntries(rows: JSONArray, date: String): List<TanyaAvailableLesson> =
            (0 until rows.length()).mapNotNull { index ->
                val r = rows.getJSONObject(index)
                if (r.optString("sug") != "t" || r.optString("date") != date) return@mapNotNull null
                val media = r.optString("audio").replace("http://bit.ly/", "https://bit.ly/")
                val candidate = TanyaAvailableLesson("nadav-archive:${r.optString("lid")}:$date", "nadav", r.optString("title").trim(),
                    "", emptyList(), "audio", media, "https://tanyayomi.com/yy/ClientTania/Home", "הקלטה מהארכיון · $date")
                parse(JSONArray().put(toJson(candidate))).firstOrNull()
            }

        internal fun podcastEntries(xml: String): List<TanyaAvailableLesson> {
            require(xml.length <= 2_000_000 && !xml.contains("<!DOCTYPE", true))
            val parser = Xml.newPullParser().apply { setInput(xml.reader()) }
            val records = mutableListOf<TanyaAvailableLesson>()
            var inItem = false; var title = ""; var id = ""; var description = ""; var url = ""
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG) when (parser.name) {
                    "item" -> { inItem = true; title = ""; id = ""; description = ""; url = "" }
                    "title" -> if (inItem) title = parser.nextText()
                    "guid" -> if (inItem) id = parser.nextText()
                    "description" -> if (inItem) description = parser.nextText()
                    "enclosure" -> if (inItem && parser.getAttributeValue(null, "type")?.startsWith("audio/") == true)
                        url = parser.getAttributeValue(null, "url").orEmpty()
                }
                if (parser.eventType == XmlPullParser.END_TAG && parser.name == "item") {
                    if (id.isNotBlank() && title.isNotBlank() && description.contains("אגרת הקודש")) {
                        val r = TanyaAvailableLesson("wilhelm:$id", "wilhelm", title, "Tanya, Part IV; Iggeret HaKodesh",
                            emptyList(), "audio", url, "https://rbaruchwilhelm.captivate.fm", "אגרת הקודש · מתוך הפודקאסט")
                        records += parse(JSONArray().put(toJson(r)))
                    }
                    inItem = false
                }
                parser.next()
            }
            return records.distinctBy { it.id }
        }
        @Volatile private var instance: TanyaAvailableLessons? = null
        fun get(context: Context): TanyaAvailableLessons = instance ?: synchronized(this) {
            instance ?: TanyaAvailableLessons(context).also { instance = it }
        }
    }
}
