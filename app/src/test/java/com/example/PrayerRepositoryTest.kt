package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.prayers.PrayerRepository
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
}
