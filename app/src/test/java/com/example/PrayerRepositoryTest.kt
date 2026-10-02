package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.prayers.PrayerRepository
import com.example.data.prayers.ShirShelYom
import java.time.LocalDate
import java.time.DayOfWeek
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PrayerRepositoryTest {
    private val book by lazy { PrayerRepository(ApplicationProvider.getApplicationContext<Context>()).load() }

    @Test fun `all available prayers are bundled and attributed`() {
        assertTrue(book.edition.contains("תורה אור"))
        assertTrue(book.sections.size >= 9)
        assertEquals(book.sections.size, book.sections.map { it.id }.distinct().size)
        book.sections.forEach {
            assertTrue(it.source.startsWith("https://he.wikisource.org/wiki/"))
            assertTrue(it.paragraphs.isNotEmpty())
            assertFalse(it.paragraphs.any { paragraph -> paragraph.contains("שגיאת ציטוט") || paragraph.contains("<references") })
        }
    }

    @Test fun `morning blessings shema and amidah retain actual text`() {
        fun plain(id: String) = book.sections.single { it.id == id }.paragraphs.joinToString(" ")
            .replace(Regex("[\\u0591-\\u05BD\\u05BF-\\u05C2\\u05C4-\\u05C5\\u05C7]"), "")
        assertTrue(plain("blessings").contains("אשר יצר"))
        assertTrue(plain("shema").contains("שמע ישראל"))
        assertTrue(plain("amidah").contains("ברוך אתה"))
    }

    @Test fun `reader excludes halachic essays but preserves recited korbanot`() {
        assertFalse(book.sections.any { it.id == "washing" })
        val morning = book.sections.single { it.id == "morning" }
        assertEquals(1, morning.paragraphs.size)
        assertTrue(morning.paragraphs.single().startsWith("מוֹדֶה"))
        val all = book.sections.flatMap { it.paragraphs }.joinToString(" ")
        listOf("מודעת זאת מעלת", "כל הברכות הללו מברך", "ברכת התורה צריך ליזהר", "מנהג ספרד שבכל יום", "וכל זה כשנזכר").forEach {
            assertFalse(it, all.contains(it))
        }
        assertTrue(book.sections.single { it.id == "offerings" }.paragraphs.any { it.startsWith("אֵיזֶהוּ") })
    }

    @Test fun `shir shel yom follows every weekday and includes Wednesday addition`() {
        val expected = listOf(24, 48, 82, 94, 81, 93, 92)
        val sunday = LocalDate.of(2026, 9, 27)
        val repo = PrayerRepository(ApplicationProvider.getApplicationContext<Context>())
        (0..6).forEach { offset ->
            val date = sunday.plusDays(offset.toLong())
            assertEquals(expected[offset], ShirShelYom.chapter(date.dayOfWeek))
            val shir = repo.load(date).sections.single { it.id == "shir_shel_yom" }
            assertTrue(shir.paragraphs.size > 5)
            assertEquals("shir_$date", shir.bookmarkKey)
            assertEquals(date.dayOfWeek == DayOfWeek.WEDNESDAY,
                shir.paragraphs.any { it.contains("לְכוּ") && it.contains("נְרַנְּנָה") })
            assertTrue(shir.paragraphs.last().startsWith("הוֹשִׁיעֵנוּ"))
        }
    }
}
