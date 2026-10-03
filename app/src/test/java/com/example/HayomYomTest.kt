package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.data.hayomyom.HayomYomCalendar
import com.example.data.hayomyom.HayomYomRepository
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HayomYomTest {
    @Test fun datesDoNotDependOnRambamScheduleOrDeviceTimeZone() {
        assertEquals("tishrei-22", HayomYomCalendar.key(LocalDate.of(2026,10,3)))
        assertEquals("tishrei-1", HayomYomCalendar.key(LocalDate.of(2026,9,12)))
        assertEquals("adar1-1", HayomYomCalendar.key(LocalDate.of(2024,2,10)))
        assertEquals("adar2-1", HayomYomCalendar.key(LocalDate.of(2024,3,11)))
        assertEquals("adar-1", HayomYomCalendar.key(LocalDate.of(2025,3,1)))
    }

    @Test fun everyDayInCommonAndLeapYearsHasItsOwnOfflineText() {
        val repository = HayomYomRepository(ApplicationProvider.getApplicationContext())
        val start = LocalDate.of(2023,9,16)
        repeat(1120) { offset ->
            val date = start.plusDays(offset.toLong())
            val lesson = repository.lessonFor(date)
            assertNotNull("Missing ${HayomYomCalendar.key(date)} on $date", lesson)
            assertEquals(date, lesson!!.date)
            assertTrue(lesson.paragraphs.all { it.isNotBlank() && !it.contains("{{") })
            assertTrue(lesson.revisionId > 0)
        }
    }

    @Test fun readerAdapterKeepsTextAndIndependentContentIdentity() {
        val lesson = HayomYomRepository(ApplicationProvider.getApplicationContext())
            .lessonFor(LocalDate.of(2026,10,1))!!
        val reader = lesson.asReaderLesson()
        assertEquals("היום יום", reader.bookTitle)
        assertTrue(reader.fullRef.startsWith("hayom_yom:"))
        assertEquals(lesson.paragraphs, reader.sections.map { it.textWithNikud })
    }
}
