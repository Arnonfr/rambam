package com.example.data.speech

import android.content.Context
import com.example.domain.schedule.DailyLessonResult
import com.example.domain.speech.*
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** A recording must match the date and every assigned chapter before it can play. */
class RambamRecordingRepository internal constructor(context: Context, private val client: OkHttpClient) {
    constructor(context: Context) : this(
        context, OkHttpClient.Builder().callTimeout(20, java.util.concurrent.TimeUnit.SECONDS).build()
    )
    private val folder = File(context.filesDir, "rambam_recording_pages").apply { mkdirs() }
    suspend fun resolve(day: DailyLessonResult): DailySpeechLesson = withContext(Dispatchers.IO) {
        val date = LocalDate.parse(day.studyDate)
        if (day.track == "one") return@withContext resolveNadav(day, date)
        check(day.track == "three")
        val file = File(folder, "$date-three.html")
        val cached = file.takeIf { it.exists() }?.readText()
        val html = cached ?: client.newCall(Request.Builder().url("https://www.chabad.fm/157/")
            .post(FormBody.Builder().add("date", date.format(usa)).build()).build()).execute().use {
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
            check(it.isSuccessful && allowedUrl(it.request.url.toString(), plan.source) && it.header("Content-Type").orEmpty().startsWith("audio/")) {
                "ההקלטה אינה זמינה כרגע. אפשר לנסות שוב מאוחר יותר."
            }
        }
        ensureActive()
        if (cached == null) file.writeText(html)
        folder.listFiles()?.sortedByDescending { it.lastModified() }?.drop(45)?.forEach { it.delete() }
        plan
    }

    private suspend fun resolveNadav(day: DailyLessonResult, date: LocalDate): DailySpeechLesson {
        // Chabad FM's daily HTML can name the correct chapter while its Nadav file plays
        // the previous one. Use the teacher's chapter-labelled archive, not a day or ID offset.
        val request = Request.Builder().url("https://tanyayomi.com/yy/yyGetToday.php?day=$date").build()
        val json = client.newCall(request).execute().use {
            check(it.isSuccessful) { "לא ניתן להתחבר לאתר ההקלטות. בדקו את החיבור ונסו שוב." }
            val body = it.body ?: error("אתר ההקלטות לא החזיר מידע")
            body.source().request(2_000_001)
            check(body.source().buffer.size <= 2_000_000)
            body.string()
        }
        currentCoroutineContext().ensureActive()
        val candidate = parseNadav(json, day)
        // Validate each redirect before following it. No stale Chabad FM URL is used as a fallback.
        val headClient = client.newBuilder().followRedirects(false).followSslRedirects(false).build()
        var url = candidate.sourceUrl.toHttpUrlOrNull() ?: error(noNadavMatch)
        repeat(4) {
            check(isNadavMediaUrl(url) || isNadavShortUrl(url)) { noNadavMatch }
            val next = headClient.newCall(Request.Builder().url(url).head().build()).execute().use { response ->
                if (response.isRedirect) {
                    val location = response.header("Location")?.let(url::resolve) ?: error(noNadavMatch)
                    secureArchiveUrl(location)
                }
                else {
                    check(response.isSuccessful && isNadavMediaUrl(url) &&
                        response.header("Content-Type").orEmpty().startsWith("audio/", ignoreCase = true)) { noNadavMatch }
                    currentCoroutineContext().ensureActive()
                    return nadavPlan(day, candidate, url.toString())
                }
            }
            url = next
        }
        error(noNadavMatch)
    }

    companion object {
        private const val noNadavMatch = "לא נמצאה הקלטה שתואמת לפרק הזה. אפשר לנסות שוב מאוחר יותר."
        private val chapterSuffix = Regex("""\s+פרק\s+([א-ת'׳"״0-9]+)\s*$""")
        private val archiveHosts = setOf("tanyayomi.com", "www.tanyayomi.com")

        internal data class NadavCandidate(val id: String, val sourceUrl: String)

        private fun isCleanHttps(url: HttpUrl) =
            url.isHttps && url.port == 443 && url.username.isEmpty() && url.password.isEmpty() &&
                url.query == null && url.fragment == null

        internal fun isNadavMediaUrl(url: HttpUrl) = isCleanHttps(url) && url.host in archiveHosts &&
            url.pathSegments.size == 1 && url.pathSegments.single().let {
                it.startsWith("רמבם-", ignoreCase = true) && '/' !in it && '\\' !in it &&
                    (it.endsWith(".m4a", ignoreCase = true) || it.endsWith(".mp3", ignoreCase = true))
            }

        // Some legacy archive links advertise HTTP even though the identical media
        // path is available over HTTPS. Upgrade before sending any request.
        private fun secureArchiveUrl(url: HttpUrl): HttpUrl =
            if (url.scheme == "http" && url.port == 80 && url.host in archiveHosts) {
                url.newBuilder().scheme("https").port(443).build().takeIf(::isNadavMediaUrl) ?: url
            } else url

        private fun isNadavShortUrl(url: HttpUrl) = isCleanHttps(url) && url.host == "bit.ly" &&
            url.encodedPath.matches(Regex("/[A-Za-z0-9]+"))

        internal fun parseNadav(json: String, day: DailyLessonResult): NadavCandidate {
            check(day.track == "one" && day.chapters.size == 1)
            val chapter = day.chapters.single()
            val rows = JSONArray(json)
            val matches = (0 until rows.length()).mapNotNull { index ->
                val row = rows.optJSONObject(index) ?: return@mapNotNull null
                if (row.optString("sug") != "r" || row.optString("date") != day.studyDate) return@mapNotNull null
                val title = row.optString("title").substringAfterLast(',').trim().replace(Regex("\\s+"), " ")
                val inTitle = chapterSuffix.find(title)
                val inDescription = chapterSuffix.find(" " + row.optString("mydesc").trim())
                val numbers = listOfNotNull(inTitle, inDescription).map {
                    val value = it.groupValues[1]
                    value.toIntOrNull() ?: hebrewNumber(value)
                }
                val section = inTitle?.let { title.substring(0, it.range.first) } ?: title
                if (numbers.isEmpty() || numbers.any { it != chapter.chapterNumber } ||
                    normalize(section) != normalize(chapter.sectionNameHebrew)) return@mapNotNull null
                val id = row.optString("lid")
                if (!id.matches(Regex("[0-9]{1,12}"))) return@mapNotNull null
                val sourceUrl = sequenceOf(row.optString("audio"), row.optString("fullUrl"))
                    .mapNotNull { it.toHttpUrlOrNull()?.let(::secureArchiveUrl) }.firstOrNull { isNadavMediaUrl(it) || isNadavShortUrl(it) }
                    ?: return@mapNotNull null
                NadavCandidate(id, sourceUrl.toString())
            }.distinct()
            return matches.singleOrNull() ?: error(noNadavMatch)
        }

        internal fun nadavPlan(day: DailyLessonResult, candidate: NadavCandidate, url: String): DailySpeechLesson {
            val chapter = day.chapters.single()
            val parsed = url.toHttpUrlOrNull()
            check(parsed != null && isNadavMediaUrl(parsed)) { noNadavMatch }
            // The publisher's media filenames carry both the numeric and Hebrew chapter.
            // Check the file itself too, not just the dated row that linked to it.
            val name = parsed.pathSegments.single().substringBeforeLast('.')
            val fields = name.split('-')
            check(fields.size >= 3 && fields[fields.lastIndex - 1].toIntOrNull() == chapter.chapterNumber &&
                hebrewNumber(fields.last()) == chapter.chapterNumber) { noNadavMatch }
            val title = "${chapter.sectionNameHebrew} · ${chapter.chapterHebrew}"
            return DailySpeechLesson("rambam-${day.studyDate}-one-nadav-archive-${candidate.id}", "רמב״ם · ${day.hebrewDate}",
                listOf(SpeechPart("${chapter.sectionId}_${chapter.chapterNumber}", title, "", unitLabel = "הקלטה", audioUrl = url)), "nadav")
        }

        private val usa = DateTimeFormatter.ofPattern("MM-dd-yyyy")
        fun allowedUrl(value: String, source: String): Boolean {
            val url = value.toHttpUrlOrNull() ?: return false
            return when (source) {
                "nadav" -> isNadavMediaUrl(url)
                "peretz" -> isCleanHttps(url) && url.host == "www.chabad.fm" &&
                    url.encodedPath.matches(Regex("/mp3/rambam/shiur[0-9]{1,4}\\.mp3"))
                else -> false
            }
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
            check(day.track == "three")
            val type = Regex("data-type=\"(\\d+)\"").find(player)?.groupValues?.get(1)
            check(type == "6")
            val id = Regex("data-id=\"([0-9]+)\"").find(player)?.groupValues?.get(1)
                ?: error("אין הקלטה זמינה ללימוד הזה. נסו שוב מאוחר יותר.")
            val url = "https://www.chabad.fm/mp3/rambam/shiur$id.mp3"
            val source = "peretz"
            check(allowedUrl(url, source))
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
