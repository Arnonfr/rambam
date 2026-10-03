package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.tanya.TanyaRepository
import com.example.domain.tanya.VerifiedTanyaPrint
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TanyaBundledLessonTest {
    @Test fun `all remaining assignments are complete and mapped through last page`() = runBlocking {
        val repo = TanyaRepository(ApplicationProvider.getApplicationContext<Context>())
        val first = LocalDate.of(2026, 10, 29)
        repeat(31) { offset ->
            val date = first.plusDays(offset.toLong())
            val lesson = repo.getDailyTanyaLesson(date)!!
            assertEquals(date.toString(), lesson.date)
            val lines = VerifiedTanyaPrint.fullPagesForReference(lesson.fullRef)
            assertTrue("Missing ${lesson.fullRef}", lines.isNotEmpty())
            assertEquals("Truncated ${lesson.fullRef}", lesson.sections.size, lines.maxOf { it.section })
            assertTrue(lesson.sections.none { it.textPlain.contains("[פ:") || it.textPlain.contains("[מ:") })
        }
        val last = repo.getDailyTanyaLesson(LocalDate.of(2026, 11, 28))!!
        assertEquals("Tanya, Part V; Kuntres Acharon 9:1-12", last.fullRef)
        assertEquals(325, VerifiedTanyaPrint.fullPagesForReference(last.fullRef).last().page)
    }

    @Test fun `expanded lessons through epistle 28 load offline with full print mapping`() = runBlocking {
        val repo = TanyaRepository(ApplicationProvider.getApplicationContext<Context>())
        (6..28).forEach { day ->
            val lesson = repo.getDailyTanyaLesson(LocalDate.of(2026, 10, day))!!
            assertEquals("2026-10-${day.toString().padStart(2, '0')}", lesson.date)
            val lines = VerifiedTanyaPrint.fullPagesForReference(lesson.fullRef)
            assertTrue("Missing print mapping for ${lesson.fullRef}", lines.isNotEmpty())
            assertEquals(lesson.sections.size, lines.maxOf { it.section })
        }
        val first26 = repo.getDailyTanyaLesson(LocalDate.of(2026, 10, 15))!!
        assertEquals(10, first26.sections.size)
        val last25 = repo.getDailyTanyaLesson(LocalDate.of(2026, 10, 14))!!
        assertEquals(5, last25.sections.size)
    }

    @Test fun `end of epistle lessons load complete offline and map to printed lines`() = runBlocking {
        val repo = TanyaRepository(ApplicationProvider.getApplicationContext<Context>())
        val lesson23 = repo.getDailyTanyaLesson(LocalDate.of(2026, 10, 4))!!
        assertEquals(5, lesson23.sections.size)
        assertEquals("Tanya, Part IV; Iggeret HaKodesh 23:10-14", lesson23.fullRef)
        assertTrue(lesson23.sections.last().textPlain.contains("אמן"))
        val lesson24 = repo.getDailyTanyaLesson(LocalDate.of(2026, 10, 5))!!
        assertEquals(9, lesson24.sections.size)
        assertEquals("Tanya, Part IV; Iggeret HaKodesh 24:1-9", lesson24.fullRef)
        assertTrue(lesson24.sections.last().textPlain.contains("הטיבה"))
        listOf(lesson23, lesson24).forEach { lesson ->
            val lines = VerifiedTanyaPrint.forReference(lesson.fullRef)
            assertTrue(lines.isNotEmpty())
            assertEquals(lesson.sections.size, lines.maxOf { it.section })
        }
    }
}
