package com.example.data.speech

import android.content.Context
import com.example.domain.schedule.DailyLessonResult
import com.example.domain.speech.*
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Public dated publisher pages. Both the date and the entire assignment must match. */
class RambamRecordingRepository(context: Context) {
    private val folder = File(context.filesDir, "rambam_recording_pages").apply { mkdirs() }
    private val client = OkHttpClient.Builder().callTimeout(20, java.util.concurrent.TimeUnit.SECONDS).build()
    suspend fun resolve(day: DailyLessonResult): DailySpeechLesson = withContext(Dispatchers.IO) {
        val file = File(folder, "${LocalDate.parse(day.studyDate)}-${day.track}.html")
        val cached = file.takeIf { it.exists() }?.readText()
        val html = cached ?: client.newCall(Request.Builder().url("https://www.chabad.fm/${if (day.track == "three") 157 else 156}/")
            .post(FormBody.Builder().add("date", LocalDate.parse(day.studyDate).format(usa)).build()).build()).execute().use {
                check(it.isSuccessful) { "לא ניתן להתחבר לאתר ההקלטות. בדקו את החיבור ונסו שוב." }
                val body = it.body ?: error("אתר ההקלטות לא החזיר מידע")
                body.source().request(2_000_001)
                check(body.source().buffer.size <= 2_000_000)
                body.string()
            }
        ensureActive()
        val plan = parse(html, day)
        // Validate the media before exposing Play. A missing recording never becomes system TTS.
        client.newCall(Request.Builder().url(plan.parts.single().audioUrl).head().build()).execute().use {
            check(it.isSuccessful && allowedUrl(it.request.url.toString()) && it.header("Content-Type").orEmpty().startsWith("audio/")) {
                "ההקלטה אינה זמינה כרגע. אפשר לנסות שוב או לבחור בהקראה של Michael."
            }
        }
        ensureActive()
        if (cached == null) file.writeText(html)
        folder.listFiles()?.sortedByDescending { it.lastModified() }?.drop(45)?.forEach { it.delete() }
        plan
    }

    companion object {
        private val usa = DateTimeFormatter.ofPattern("MM-dd-yyyy")
        fun allowedUrl(value: String): Boolean {
            val url = value.toHttpUrlOrNull() ?: return false
            return url.isHttps && url.host == "www.chabad.fm" && url.port == 443 && url.query == null && url.fragment == null &&
                url.encodedPath.matches(Regex("/mp3/(?:rambamRabbiNadav/[0-9]{4}|rambam/shiur[0-9]{1,4})\\.mp3"))
        }
        internal fun parse(html: String, day: DailyLessonResult): DailySpeechLesson {
            val date = LocalDate.parse(day.studyDate)
            val previous = Regex("data-previous=\"([^\"]+)\"").find(html)?.groupValues?.get(1)
            val next = Regex("data-next=\"([^\"]+)\"").find(html)?.groupValues?.get(1)
            check(previous == date.minusDays(1).format(usa) && next == date.plusDays(1).format(usa)) { "אתר ההקלטות החזיר יום אחר. לא ננגן שיעור שאינו תואם." }
            val headings = Regex("<font\\s+class=\"perek\">(.*?)</font>", RegexOption.DOT_MATCHES_ALL)
                .findAll(html).map { it.groupValues[1].replace(Regex("<[^>]*>"), "").trim() }.toList()
            check(headings.size == day.chapters.size && day.chapters.isNotEmpty()) { "ההקלטה אינה תואמת לכל פרקי הלימוד." }
            headings.zip(day.chapters).forEach { (heading, chapter) ->
                val parts = heading.split(" פרק ")
                val number = if (parts.size == 1) 1 else hebrewNumber(parts.last())
                check(normalize(parts.first()) == normalize(chapter.sectionNameHebrew) && number == chapter.chapterNumber) { "הפרקים באתר ההקלטות שונים מהלימוד שנבחר." }
            }
            val player = Regex("<div\\s+id=\"chitatPlayer\"[^>]*>").find(html)?.value.orEmpty()
            val three = day.track == "three"
            val type = Regex("data-type=\"(\\d+)\"").find(player)?.groupValues?.get(1)
            check(type == if (three) "6" else "7")
            val key = if (three) "data-id" else "data-id-nadav"
            val id = Regex("$key=\"([0-9]+)\"").find(player)?.groupValues?.get(1)
                ?: error("אין הקלטה זמינה ללימוד הזה. אפשר לבחור בהקראה של Michael.")
            val url = if (three) "https://www.chabad.fm/mp3/rambam/shiur$id.mp3" else "https://www.chabad.fm/mp3/rambamRabbiNadav/$id.mp3"
            check(allowedUrl(url))
            val source = if (three) "peretz" else "nadav"
            val title = headings.joinToString(" · ")
            return DailySpeechLesson("rambam-${day.studyDate}-${day.track}-$source-$id", "רמב״ם · ${day.hebrewDate}",
                listOf(SpeechPart(day.chapters.joinToString("+") { "${it.sectionId}_${it.chapterNumber}" }, title, "", unitLabel = "הקלטה", audioUrl = url)), source)
        }
        private fun normalize(value: String) = value.removePrefix("הלכות ").replace(Regex("[\\s\"'׳״־-]"), "")
        private fun hebrewNumber(value: String): Int {
            val alphabet = "אבגדהוזחטיכלמנסעפצקרשת"
            val values = listOf(1,2,3,4,5,6,7,8,9,10,20,30,40,50,60,70,80,90,100,200,300,400)
            return value.filter { it in alphabet }.sumOf { values[alphabet.indexOf(it)] }
        }
    }
}
