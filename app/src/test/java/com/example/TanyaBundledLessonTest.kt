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
