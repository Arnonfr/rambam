package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.data.speech.*
import com.example.domain.schedule.DailyScheduleEngine
import com.example.domain.speech.*
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
    @Test fun publisherPagesMatchTheActualDailyAssignmentsForBothTracks() {
        val one=RambamRecordingRepository.parse(html("one"),day("one"))
        assertEquals("nadav",one.source)
        assertEquals("https://www.chabad.fm/mp3/rambamRabbiNadav/0264.mp3",one.parts.single().audioUrl)
        val three=RambamRecordingRepository.parse(html("three"),day("three"))
        assertEquals("peretz",three.source)
        assertEquals("https://www.chabad.fm/mp3/rambam/shiur263.mp3",three.parts.single().audioUrl)
        assertEquals(3,three.parts.single().chapterId.split('+').size)
    }
    @Test fun wrongDateMissingChapterOrDifferentBookNeverBecomesAPlayableRecording() {
        assertThrows(IllegalStateException::class.java) { RambamRecordingRepository.parse(html("one"),day("one","2026-10-24")) }
        assertThrows(IllegalStateException::class.java) { RambamRecordingRepository.parse(html("one").replace("איסורי ביאה","מכירה"),day("one")) }
        assertThrows(IllegalStateException::class.java) { RambamRecordingRepository.parse(html("three").replace("<font class=\"perek\">הלכות מכירה פרק ג</font>",""),day("three")) }
        assertThrows(IllegalStateException::class.java) { RambamRecordingRepository.parse(html("one").replace("data-id-nadav=\"0264\"","data-id-nadav=\"-1\""),day("one")) }
    }
    @Test fun savedRecordingPlansAllowOnlyThePublishersExactHttpsMediaPaths() {
        assertFalse(RambamRecordingRepository.allowedUrl("http://www.chabad.fm/mp3/rambam/shiur263.mp3"))
        assertFalse(RambamRecordingRepository.allowedUrl("https://evil.example/mp3/rambam/shiur263.mp3"))
        assertFalse(RambamRecordingRepository.allowedUrl("https://www.chabad.fm/mp3/rambam/shiur263.mp3?redirect=evil"))
        val store=SpeechLessonStore(ApplicationProvider.getApplicationContext())
        val plan=RambamRecordingRepository.parse(html("one"),day("one"))
        store.save(plan);assertEquals(plan,store.load(plan.id))
        store.save(plan.copy(parts=listOf(plan.parts.single().copy(audioUrl="https://evil.example/audio.mp3"))))
        assertThrows(IllegalArgumentException::class.java) { store.load(plan.id) }
    }
}
