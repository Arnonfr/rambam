package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.mitzvot.SeferHamitzvotRepository
import com.example.domain.mitzvot.MitzvahAssignmentItem
import com.example.domain.mitzvot.MitzvahAssignmentType
import com.example.ui.util.HebrewTextNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SeferHamitzvotRepositoryTest {
    private val repository by lazy {
        SeferHamitzvotRepository(ApplicationProvider.getApplicationContext<Context>())
    }

    @Test
    fun `loads a positive commandment from the public domain pack`() {
        val text = repository.getText(
            MitzvahAssignmentItem(MitzvahAssignmentType.POSITIVE, number = 96)
        )!!

        assertEquals("Public Domain", text.license)
        assertTrue(text.paragraphs.isNotEmpty())
        assertTrue(HebrewTextNormalizer.stripMarks(text.paragraphs.joinToString()).contains("טומאת נבלה"))
    }

    @Test
    fun `all 613 commandments have readable pointed text`() {
        for ((type, count) in listOf(MitzvahAssignmentType.POSITIVE to 248, MitzvahAssignmentType.NEGATIVE to 365)) {
            for (number in 1..count) {
                val text = repository.getText(MitzvahAssignmentItem(type, number = number))!!
                val pointed = text.paragraphs.joinToString()
                assertTrue("$type $number", pointed.isNotBlank())
                assertTrue("$type $number contains vowels", pointed.any { it in '\u05B0'..'\u05BC' })
                assertTrue(HebrewTextNormalizer.stripMarks(pointed).none { it in '\u05B0'..'\u05BC' })
                val abbreviations = Regex("[א-ת\\u0591-\\u05C7]+['׳]").findAll(pointed)
                for (abbreviation in abbreviations) {
                    val plain = HebrewTextNormalizer.stripMarks(abbreviation.value)
                    if (Regex("[ובכלמש]?ה['׳]").matches(plain)) assertEquals(plain, abbreviation.value)
                }
            }
        }
    }

    @Test
    fun `contains all fourteen roots`() {
        for (number in 1..14) {
            assertTrue(repository.getRoot(number).isNotEmpty())
        }
    }
}
