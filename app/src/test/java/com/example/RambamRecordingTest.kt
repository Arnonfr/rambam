package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.data.speech.*
import com.example.domain.schedule.DailyScheduleEngine
import com.example.domain.speech.*
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class RambamRecordingTest {
    private fun html(track: String) = javaClass.classLoader!!.getResourceAsStream("chabad_rambam_${track}_oct23.html")!!.bufferedReader().use { it.readText() }
    private fun day(track: String, date: String = "2026-10-23") = DailyScheduleEngine(ApplicationProvider.getApplicationContext())
        .getDailyLesson(Instant.parse("${date}T12:00:00Z"), ZoneId.of("UTC"),track)
    private fun archive() = javaClass.classLoader!!.getResourceAsStream("nadav_rambam_nov25_27.json")!!.bufferedReader().use { it.readText() }
    private fun media(chapter: Int, hebrew: String) = "https://tanyayomi.com/רמבם-05-קדושה-02-מאכלות-$chapter-$hebrew.m4a"

    @Test fun nadavRecordingMatchesDisplayedChapterOnThreeConsecutiveDays() {
        listOf(Triple(25, 15, "טו"), Triple(26, 16, "טז"), Triple(27, 17, "יז")).forEachIndexed { index, (date, chapter, hebrew) ->
            val assignment = day("one", "2026-11-$date")
            val candidate = RambamRecordingRepository.parseNadav(archive(), assignment)
            val plan = RambamRecordingRepository.nadavPlan(assignment, candidate, media(chapter, hebrew))
            assertEquals("nadav", plan.source)
            assertEquals("rambam-2026-11-$date-one-nadav-archive-${4958 + index}", plan.id)
            assertEquals("maachalot_assurot_$chapter", plan.parts.single().chapterId)
            assertEquals(media(chapter, hebrew), plan.parts.single().audioUrl)
        }
    }

    @Test fun repositoryUsesStudyDateAndChecksTheResolvedAudioBeforeReturningPlay() {
        val assignment = day("one", "2026-11-26").copy(civilDate = "2026-11-25")
        val calls = mutableListOf<String>()
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            assertTrue(request.url.isHttps)
            val target = "${request.method} ${request.url.host}${request.url.encodedPath}"
            calls += target
            val response = Response.Builder().request(request).protocol(Protocol.HTTP_1_1).message("test")
            when (target) {
                "GET tanyayomi.com/yy/yyGetToday.php" -> {
                    assertEquals("2026-11-26", request.url.queryParameter("day"))
                    response.code(200).body(archive().toResponseBody("application/json".toMediaType()))
                }
                "HEAD bit.ly/3vAJYQe" -> response.code(301).header("Location", media(16, "טז").replaceFirst("https:", "http:").toHttpUrl().toString()).body("".toResponseBody())
                else -> {
                    assertEquals("HEAD", request.method)
                    assertEquals("tanyayomi.com", request.url.host)
                    response.code(200).header("Content-Type", "audio/mp4").body("".toResponseBody())
                }
            }.build()
        }.build()
        val repo = RambamRecordingRepository(ApplicationProvider.getApplicationContext(), client)
        val plan = runBlocking { repo.resolve(assignment) }
        assertEquals("maachalot_assurot_16", plan.parts.single().chapterId)
        assertEquals(listOf("GET", "HEAD", "HEAD"), calls.map { it.substringBefore(' ') })
    }

    @Test fun nadavRejectsWrongDayChapterBookOrMislabeledFile() {
        val today = day("one", "2026-11-26")
        assertThrows(IllegalStateException::class.java) { RambamRecordingRepository.parseNadav(archive(), day("one","2026-11-28")) }
        assertThrows(IllegalStateException::class.java) { RambamRecordingRepository.parseNadav(archive().replace("הלכות מאכלות אסורות","הלכות שחיטה"), today) }
        assertThrows(IllegalStateException::class.java) { RambamRecordingRepository.parseNadav(archive().replace("פרק טז","פרק טו"), today) }
        assertThrows(IllegalStateException::class.java) { RambamRecordingRepository.parseNadav(archive().replace("https://bit.ly/3vAJYQe","https://example.com/wrong.m4a"), today) }
        val candidate = RambamRecordingRepository.parseNadav(archive(), today)
        assertThrows(IllegalStateException::class.java) { RambamRecordingRepository.nadavPlan(today, candidate, media(15, "טו")) }
        assertThrows(IllegalStateException::class.java) { RambamRecordingRepository.nadavPlan(today, candidate, media(16, "טו")) }
        assertThrows(IllegalStateException::class.java) { RambamRecordingRepository.nadavPlan(today, candidate, "https://www.chabad.fm/mp3/rambamRabbiNadav/0298.mp3") }
    }

    @Test fun peretzPublisherPageStillMatchesTheThreeChapterAssignment() {
        val three=RambamRecordingRepository.parse(html("three"),day("three"))
        assertEquals("peretz",three.source)
        assertEquals("https://www.chabad.fm/mp3/rambam/shiur263.mp3",three.parts.single().audioUrl)
        assertEquals(3,three.parts.single().chapterId.split('+').size)
        assertThrows(IllegalStateException::class.java) { RambamRecordingRepository.parse(html("three"),day("three","2026-10-24")) }
        assertThrows(IllegalStateException::class.java) { RambamRecordingRepository.parse(html("three").replace("<font class=\"perek\">הלכות מכירה פרק ג</font>",""),day("three")) }
        assertThrows(IllegalStateException::class.java) { RambamRecordingRepository.parse(html("one"),day("one")) }
    }

    @Test fun savedPlansRejectUnsupportedUrlsAndStaleNadavFiles() {
        assertFalse(RambamRecordingRepository.allowedUrl("http://www.chabad.fm/mp3/rambam/shiur263.mp3", "peretz"))
        assertFalse(RambamRecordingRepository.allowedUrl("https://evil.example/mp3/rambam/shiur263.mp3", "peretz"))
        assertFalse(RambamRecordingRepository.allowedUrl("https://www.chabad.fm/mp3/rambam/shiur263.mp3?redirect=evil", "peretz"))
        val store=SpeechLessonStore(ApplicationProvider.getApplicationContext())
        val today = day("one", "2026-11-26")
        val plan = RambamRecordingRepository.nadavPlan(today, RambamRecordingRepository.parseNadav(archive(), today), media(16, "טז"))
        store.save(plan)
        assertEquals(plan,store.load(plan.id))
        store.save(plan.copy(parts=listOf(plan.parts.single().copy(audioUrl="https://www.chabad.fm/mp3/rambamRabbiNadav/0298.mp3"))))
        assertThrows(IllegalArgumentException::class.java) { store.load(plan.id) }
    }
}
