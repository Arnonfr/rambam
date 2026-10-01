package com.example

import com.example.ui.util.HebrewTextNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.Normalizer

class HebrewTextNormalizerTest {
    @Test
    fun `normalizes canonically equivalent Hebrew marks`() {
        val input = "ש\u05C1\u05B8לוֹם"
        val normalized = HebrewTextNormalizer.forDisplay(input)

        assertTrue(Normalizer.isNormalized(normalized, Normalizer.Form.NFC))
    }

    @Test
    fun `preserves combining grapheme joiner`() {
        val input = "י\u05B4\u034F\u05B7ם"

        assertTrue(HebrewTextNormalizer.forDisplay(input).contains('\u034F'))
    }

    @Test
    fun `detects marks that require a fully featured Hebrew font`() {
        assertTrue(HebrewTextNormalizer.containsCantillationOrRareMarks("בְּרֵאשִׁ֖ית"))
        assertTrue(HebrewTextNormalizer.containsCantillationOrRareMarks("מֶֽלֶךְ"))
        assertFalse(HebrewTextNormalizer.containsCantillationOrRareMarks("שָׁלוֹם"))
    }

    @Test
    fun `strips Hebrew marks without removing letters`() {
        assertEquals("שלום", HebrewTextNormalizer.stripMarks("שָׁלוֹם"))
    }
}
