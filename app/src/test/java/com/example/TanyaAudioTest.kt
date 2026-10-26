package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.tanya.TanyaAudioMatch
import com.example.data.tanya.TanyaRepository
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TanyaAudioTest {
    private fun source(name: String) = JSONObject(javaClass.classLoader!!.getResourceAsStream(name)!!.bufferedReader().use { it.readText() })
    @Test fun realSourceTextMatchesDailyLessonDespiteSpellingAndAbbreviations() = runBlocking {
        val lesson = TanyaRepository(ApplicationProvider.getApplicationContext<Context>()).getDailyTanyaLesson(LocalDate.of(2026, 10, 15))!!
        val row = source("tanya_audio_oct15.json")
        assertTrue(TanyaAudioMatch.sameDailyText(row.getString("date"), row.getString("text"), lesson))
        assertFalse(TanyaAudioMatch.sameDailyText("2026-10-16", row.getString("text"), lesson))
        val tomorrow = source("tanya_audio_oct16.json")
        assertFalse(TanyaAudioMatch.sameDailyText(lesson.date, tomorrow.getString("text"), lesson))
        assertFalse(TanyaAudioMatch.sameDailyText(lesson.date, "", lesson))
    }
    @Test fun cheshvan12RecordingAllowsPublisherIntroductionButRejectsAdjacentDay() = runBlocking {
        val repo = TanyaRepository(ApplicationProvider.getApplicationContext<Context>())
        val lesson = repo.getDailyTanyaLesson(LocalDate.of(2026, 10, 23))!!
        val row = source("tanya_audio_oct23.json")
        assertTrue(TanyaAudioMatch.sameDailyText(row.getString("date"), row.getString("text"), lesson))
        val next = repo.getDailyTanyaLesson(LocalDate.of(2026, 10, 24))!!
        assertFalse(TanyaAudioMatch.sameDailyText(next.date, row.getString("text"), next))
        assertFalse(TanyaAudioMatch.sameDailyText(lesson.date, row.getString("text").take(400), lesson))
    }
    @Test fun chapterFallbackNeverCrossesTheBooksOfTanya() {
        assertEquals("Tanya, Part IV; Iggeret HaKodesh 26", TanyaAudioMatch.chapterKey("Tanya, Part IV; Iggeret HaKodesh 26:1-10"))
        assertNotEquals(TanyaAudioMatch.chapterKey("Tanya, Part I; Likkutei Amarim 26:1-10"), TanyaAudioMatch.chapterKey("Tanya, Part IV; Iggeret HaKodesh 26:1-10"))
        val lesson = com.example.domain.tanya.DailyTanyaLesson("2026-10-15", "", "Tanya, Part IV; Iggeret HaKodesh 26:1-10", "", "", "", emptyList())
        val otherDayExcerpt = com.example.data.tanya.TanyaAudioClip("test", "", "https://tanyayomi.com/example.mp3", "2026-10-16", lesson.fullRef, "")
        assertFalse(TanyaAudioMatch.canUseFullChapter(otherDayExcerpt, lesson))
        assertTrue(TanyaAudioMatch.canUseFullChapter(otherDayExcerpt.copy(fullChapter = true), lesson))
        assertFalse(TanyaAudioMatch.canUseFullChapter(otherDayExcerpt.copy(fullChapter = true, reference = "Tanya, Part I; Likkutei Amarim 26:1-10"), lesson))
        assertNull(TanyaAudioMatch.chapterKey("unverified chapter title"))
        assertNull(TanyaAudioMatch.chapterKey("Tanya, Part I; Likkutei Amarim 26:1-27:2"))
    }
}
