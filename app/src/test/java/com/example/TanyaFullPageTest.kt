package com.example

import com.example.domain.tanya.VerifiedTanyaPrint
import org.junit.Assert.*
import org.junit.Test

class TanyaFullPageTest {
    private fun ref(part: String) = "Tanya, Part IV; Iggeret HaKodesh $part"

    @Test fun `daily excerpts on same source page always show identical full page text`() {
        val first = VerifiedTanyaPrint.fullPagesForReference(ref("23:1-3"))
        val second = VerifiedTanyaPrint.fullPagesForReference(ref("23:4-7"))
        assertEquals(first.filter { it.page == 271 }.map { it.text }, second.filter { it.page == 271 }.map { it.text })
        assertTrue(first.first().text.startsWith("מעשי דבר"))
        assertEquals(0, first.first().greyFromWord)
        assertTrue(first.last().text == "בנפשנו")
        assertEquals(0, first.last().greyFromWord)
        assertTrue(first.indexOfFirst { VerifiedTanyaPrint.hasDailyWords(it) } > 0)
    }

    @Test fun `last daily lesson includes next chapter on same page in grey`() {
        val lines = VerifiedTanyaPrint.fullPagesForReference(ref("24:1-9"))
        assertEquals("ולא", lines.last().text)
        val next = lines.first { it.text.startsWith("כה להבין") }
        assertEquals(0, next.greyFromWord)
        assertFalse(VerifiedTanyaPrint.hasDailyWords(next))
        assertEquals(2, next.largeOpeningWords)
        assertEquals(2, lines.first().largeOpeningWords)
        assertTrue(lines[1].rightIndentFraction > 0f)
    }

    @Test fun `saved excerpt index migrates to same source line on expanded page`() {
        val reference = ref("23:4-7")
        val old = VerifiedTanyaPrint.forReference(reference)[4]
        val index = VerifiedTanyaPrint.restoredIndex(reference, 5, "section_5")
        val full = VerifiedTanyaPrint.fullPagesForReference(reference)
        assertEquals(old.text, full[index - 1].text)
        assertEquals(index, VerifiedTanyaPrint.restoredIndex(reference, index, VerifiedTanyaPrint.stableId(full[index - 1])))
    }
}
