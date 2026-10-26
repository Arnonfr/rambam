package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.tanya.AshkenaziTanyaRepository
import com.example.domain.tanya.DailyTanyaLesson
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AshkenaziTanyaTest {
    private fun lesson(ref: String) = DailyTanyaLesson("2026-10-15", "", ref, "", "", "", emptyList())

    @Test fun chapterNumbersNeverCrossTanyaBooksOrChapterRanges() {
        assertEquals(26, AshkenaziTanyaRepository.matchingChapter(lesson("Tanya, Part I; Likkutei Amarim 26:1-10")))
        assertNull(AshkenaziTanyaRepository.matchingChapter(lesson("Tanya, Part IV; Iggeret HaKodesh 26:1-10")))
        assertNull(AshkenaziTanyaRepository.matchingChapter(lesson("Tanya, Part I; Likkutei Amarim 26:1-27:2")))
    }

    @Test fun catalogKeepsAllPartsInPublisherOrderAndSupportsMultiChapterLectures() {
        val catalog = AshkenaziTanyaRepository.get(ApplicationProvider.getApplicationContext<Context>()).lessons
        assertEquals(77, catalog.size)
        assertEquals(catalog.size, catalog.distinctBy { it.id }.size)
        assertEquals(listOf(51, 52, 53), catalog.filter { 26 in it.chapters }.map { it.order })
        assertEquals(listOf(20, 21), catalog.single { it.order == 44 }.chapters)
        assertEquals(listOf(16, 17), catalog.single { it.order == 38 }.chapters)
        assertTrue(catalog.all { it.website.startsWith("https://haravshneor.co.il/") })
    }

    @Test fun podcastParsingPreservesSignedUrlsAndRejectsUntrustedMedia() {
        val xml = """<rss><channel>
          <item><guid>one</guid><enclosure type="audio/mpeg" url="https://http-vod-cdn.chabad.org/api/utils/file/12177010.mp3?id=publisher&amp;sig=x%2By%3D" /></item>
          <item><guid>bad</guid><enclosure type="audio/mpeg" url="https://unrelated.example/file.mp3" /></item>
          <item><guid>insecure</guid><enclosure type="audio/mpeg" url="http://http-vod-cdn.chabad.org/file.mp3" /></item>
        </channel></rss>"""
        val enclosures = AshkenaziTanyaRepository.enclosures(xml)
        assertEquals(setOf("one"), enclosures.keys)
        assertTrue(enclosures.getValue("one").endsWith("?id=publisher&sig=x%2By%3D"))
    }
}
