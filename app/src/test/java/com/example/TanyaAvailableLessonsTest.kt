package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.tanya.TanyaAvailableLessons
import com.example.domain.tanya.DailyTanyaLesson
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TanyaAvailableLessonsTest {
    private fun lesson(ref: String) = DailyTanyaLesson("2026-10-16", "", ref, "", "", "", emptyList())

    @Test fun catalogSeparatesBooksAndNeverTreatsPodcastEpisodeNumbersAsChapters() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val records = TanyaAvailableLessons.get(context).bundled
        assertTrue(records.size > 100)
        assertEquals(records.size, records.map { it.id }.distinct().size)
        val igrot = lesson("Tanya, Part IV; Iggeret HaKodesh 26:1-10")
        val likkutei = lesson("Tanya, Part I; Likkutei Amarim 26:1-10")
        assertEquals(4, records.count { it.teacher == "wilhelm" && it.matches(igrot) })
        assertTrue(records.none { it.teacher == "teib" && it.matches(igrot) })
        assertTrue(records.any { it.teacher == "teib" && it.matches(likkutei) })
        assertTrue(records.filter { it.teacher == "wilhelm" && it.kind == "audio" }.all { it.chapters.isEmpty() && !it.matches(igrot) })
        assertTrue(records.filter { it.teacher == "nadav" }.all { !it.matches(igrot) && !it.matches(likkutei) })
    }

    @Test fun feedImportsAudioButDoesNotInferChapterFromEpisodeNumber() {
        val xml = """<rss><channel><item><title>שיעור מספר 26</title><guid>episode-26</guid>
            <description>בלימוד אגרת הקודש</description><enclosure type="audio/mpeg" url="https://podcasts.captivate.fm/media/a.mp3"/>
            </item><item><title>Untrusted audio</title><guid>evil</guid><description>אגרת הקודש</description>
            <enclosure type="audio/mpeg" url="https://evil.example/media.mp3"/></item></channel></rss>"""
        val rows = TanyaAvailableLessons.podcastEntries(xml)
        assertEquals(1, rows.size)
        assertEquals("wilhelm:episode-26", rows.single().id)
        assertTrue(rows.single().chapters.isEmpty())
    }

    @Test fun archiveKeepsOnlyTanyaForRequestedDateAndDoesNotInventChapterCoverage() {
        val rows = JSONArray("""[{"lid":"1","sug":"t","date":"2026-10-16","title":"סימן כו","audio":"http://bit.ly/abc"},
            {"lid":"2","sug":"r","date":"2026-10-16","audio":"https://bit.ly/a"},
            {"lid":"3","sug":"t","date":"2025-10-16","audio":"https://bit.ly/b"}]""")
        val result = TanyaAvailableLessons.dailyEntries(rows, "2026-10-16")
        assertEquals(1, result.size)
        assertEquals("https://bit.ly/abc", result.single().media)
        assertTrue(result.single().chapters.isEmpty())
    }

}
