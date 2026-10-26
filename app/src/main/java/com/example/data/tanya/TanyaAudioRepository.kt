package com.example.data.tanya

import android.content.Context
import com.example.domain.tanya.DailyTanyaLesson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

data class TanyaAudioClip(val id: String, val title: String, val url: String, val date: String,
    val reference: String, val textHash: String, val chapterLesson: Boolean = false, val fullChapter: Boolean = false,
    val teacher: String = "הרב נדב כהן", val sourceUrl: String = "")

/** Conservative text-boundary matching: shared chapter titles alone cannot establish a daily match. */
object TanyaAudioMatch {
    fun words(text: String) = text.replace(Regex("<[^>]*>"), " ")
        .replace(Regex("[\\u0591-\\u05BD\\u05BF-\\u05C2\\u05C4-\\u05C5\\u05C7]"), "")
        .replace('ך', 'כ').replace('ם', 'מ').replace('ן', 'נ').replace('ף', 'פ').replace('ץ', 'צ')
        .split(Regex("[^א-ת]+" )).filter { it.isNotBlank() }
    fun hash(lesson: DailyTanyaLesson): String = MessageDigest.getInstance("SHA-256")
        .digest(words(lesson.sections.joinToString(" ") { it.textPlain }).joinToString(" ").toByteArray())
        .joinToString("") { "%02x".format(it) }
    fun chapterKey(reference: String): String? = Regex("^(.+? \\d+):\\d+(?:-\\d+)?$")
        .matchEntire(reference)?.groupValues?.get(1)

    fun canUseFullChapter(clip: TanyaAudioClip, lesson: DailyTanyaLesson): Boolean =
        clip.fullChapter && chapterKey(lesson.fullRef)?.let { it == chapterKey(clip.reference) } == true

    fun sameDailyText(sourceDate: String, sourceText: String, lesson: DailyTanyaLesson): Boolean {
        if (sourceDate != lesson.date || lesson.fullRef.isBlank()) return false
        val a = words(sourceText)
        val b = words(lesson.sections.joinToString(" ") { it.textPlain })
        if (a.size < 12 || b.size < 12 || minOf(a.size, b.size).toFloat() / maxOf(a.size, b.size) < .6f) return false
        fun coverage(x: List<String>, y: List<String>): Float {
            val row = IntArray(y.size + 1)
            x.forEach { word ->
                var previous = 0
                y.forEachIndexed { j, other ->
                    val old = row[j + 1]
                    row[j + 1] = if (word == other) previous + 1 else maxOf(row[j + 1], row[j])
                    previous = old
                }
            }
            return row.last().toFloat() / maxOf(x.size, y.size)
        }
        // The publisher can prepend the letter heading/dedication (e.g. 12 Cheshvan).
        // Align only within a short introduction; still require both daily boundaries
        // and comparable total lengths, never a chapter/date-only fallback.
        val opening = b.take(40)
        val openingCoverage = (0..minOf(60, (a.size - opening.size).coerceAtLeast(0)))
            .maxOf { offset -> coverage(a.drop(offset).take(opening.size), opening) }
        return openingCoverage >= .65f && coverage(a.takeLast(40), b.takeLast(40)) >= .75f
    }
}

class TanyaAudioRepository private constructor(context: Context) {
    private val folder = File(context.applicationContext.cacheDir, "tanya_audio_catalog")
    private val mutex = Mutex()
    private val client = OkHttpClient.Builder().connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS).callTimeout(9, TimeUnit.SECONDS).build()

    suspend fun resolve(lesson: DailyTanyaLesson, selectedDate: String, refresh: Boolean = false): TanyaAudioClip? = withContext(Dispatchers.IO) {
        if (lesson.date != selectedDate) return@withContext null
        mutex.withLock {
            val hash = TanyaAudioMatch.hash(lesson)
            val file = File(folder, "${lesson.date}.json")
            read(file)?.takeIf { !refresh && it.reference == lesson.fullRef && it.textHash == hash && fresh(file) }?.let { return@withLock it }
            var result: TanyaAudioClip? = null
            try {
                val request = Request.Builder().url("https://tanyayomi.com/yy/yyGetToday.php?day=${lesson.date}").build()
                val body = client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) null else response.body?.string()
                }
                coroutineContext.ensureActive()
                if (body != null) {
                    val rows = JSONArray(body)
                    val candidates = (0 until rows.length()).map { rows.getJSONObject(it) }.filter {
                        it.optString("sug") == "t" && TanyaAudioMatch.sameDailyText(it.optString("date"), it.optString("text"), lesson)
                    }
                    if (candidates.size == 1) {
                        val row = candidates.single()
                        val shortUrl = row.optString("audio")
                        if (shortUrl.startsWith("https://")) {
                            // Resolve redirects before play; no media body is downloaded here.
                            val direct = client.newCall(Request.Builder().url(shortUrl).head().build()).execute().use {
                                val url = it.request.url
                                val type = it.header("Content-Type").orEmpty().substringBefore(';').lowercase()
                                val audio = type.startsWith("audio/") || (type == "application/octet-stream" && url.encodedPath.endsWith(".mp3", ignoreCase = true))
                                if (it.isSuccessful && audio && url.isHttps && (url.host == "tanyayomi.com" || url.host == "www.tanyayomi.com")) url.toString() else null
                            }
                            if (direct != null) result = TanyaAudioClip("nadav:${row.getString("lid")}", row.optString("title"), direct, lesson.date, lesson.fullRef, hash)
                        }
                    }
                }
                coroutineContext.ensureActive()
                result?.let { clip ->
                    folder.mkdirs()
                    file.writeText(JSONObject().put("id", clip.id).put("title", clip.title).put("url", clip.url)
                        .put("date", clip.date).put("ref", clip.reference).put("hash", clip.textHash).toString())
                    folder.listFiles()?.filterNot { fresh(it) }?.forEach { it.delete() }
                }
            } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e }
            // Broader matching requires an explicitly verified FULL chapter, not another daily excerpt.
            result ?: TanyaAudioMatch.chapterKey(lesson.fullRef)?.let { chapter ->
                folder.listFiles()?.sortedByDescending { it.lastModified() }?.firstNotNullOfOrNull { f ->
                    if (fresh(f)) read(f)?.takeIf { TanyaAudioMatch.canUseFullChapter(it, lesson) }?.copy(chapterLesson = true) else null
                }
            }
        }
    }
    private fun fresh(file: File) = file.exists() && System.currentTimeMillis() - file.lastModified() < TimeUnit.DAYS.toMillis(7)
    private fun read(file: File): TanyaAudioClip? = runCatching {
        val o = JSONObject(file.readText())
        val url = okhttp3.HttpUrl.Builder().scheme("https").host("tanyayomi.com").build().resolve(o.getString("url")) ?: return null
        if (!url.isHttps || url.host !in setOf("tanyayomi.com", "www.tanyayomi.com")) return null
        TanyaAudioClip(o.getString("id"), o.getString("title"), url.toString(), o.getString("date"), o.getString("ref"), o.getString("hash"), fullChapter = o.optBoolean("fullChapter", false))
    }.getOrNull()

    companion object {
        @Volatile private var instance: TanyaAudioRepository? = null
        fun get(context: Context): TanyaAudioRepository = instance ?: synchronized(this) {
            instance ?: TanyaAudioRepository(context).also { instance = it }
        }
    }
}
