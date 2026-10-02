package com.example

import com.example.domain.tanya.VerifiedTanyaPrint
import org.junit.Assert.*
import org.junit.Test

class TanyaPrintExtensionTest {
    private fun ref(part: String) = "Tanya, Part IV; Iggeret HaKodesh $part"

    @Test fun `twenty consecutive new pages are available without gaps`() {
        val all = listOf("25:1-40", "26:1-36", "27:1-19", "28:1-5")
            .flatMap { VerifiedTanyaPrint.fullPagesForReference(ref(it)) }
        assertEquals((276..295).toSet(), all.map { it.page }.filter { it >= 276 }.toSet())
        assertEquals(556, all.filter { it.page >= 276 }.distinctBy { it.page to it.text }.size)
        assertTrue(all.none { it.text.contains('\t') || it.text.contains("confidence") || it.text.contains("[25:") })
    }

    @Test fun `new day starts inside printed row without discarding surrounding words`() {
        val earlier = VerifiedTanyaPrint.fullPagesForReference(ref("25:1-5"))
        val later = VerifiedTanyaPrint.fullPagesForReference(ref("25:6-10"))
        assertEquals(earlier.filter { it.page == 276 }.map { it.text }, later.filter { it.page == 276 }.map { it.text })
        val shared = later.first { it.text.startsWith("ממש קלל את דוד") }
        assertEquals(8, shared.greyBeforeWord)
        assertEquals(1, shared.section)
        val previousShared = earlier.first { it.text == shared.text }
        assertEquals(8, previousShared.greyFromWord)
        assertTrue(later.takeWhile { it != shared }.all { !VerifiedTanyaPrint.hasDailyWords(it) })
    }

    @Test fun `chapter begins with large letters and previous chapter remains grey`() {
        val lines = VerifiedTanyaPrint.fullPagesForReference(ref("26:1-10"))
        assertEquals(283, lines.first().page)
        assertEquals(0, lines.first().greyFromWord)
        val opening = lines.first { it.text.startsWith("כו בר״מ") }
        assertEquals(2, opening.largeOpeningWords)
        assertTrue(VerifiedTanyaPrint.hasDailyWords(opening))
        assertEquals(.35f, lines[lines.indexOf(opening)+1].rightIndentFraction, .001f)
    }

    @Test fun `first expanded daily assignment shows all previous epistle on page 275`() {
        val lines = VerifiedTanyaPrint.fullPagesForReference(ref("25:1-5"))
        assertEquals("בנפשו למלך על הראות קלונו ובזיונו את המלך לעין", lines.first().text)
        assertEquals(27, lines.count { it.page == 275 })
        assertFalse(VerifiedTanyaPrint.hasDailyWords(lines.first()))
    }

    @Test fun `unverified final lesson is never silently truncated`() {
        assertTrue(VerifiedTanyaPrint.fullPagesForReference(ref("28:6-9")).isEmpty())
        assertFalse(VerifiedTanyaPrint.fullPagesForReference(ref("28:6")).isEmpty())
        val lastCovered = VerifiedTanyaPrint.fullPagesForReference(ref("28:1-5"))
        assertEquals("ואתהפכא", lastCovered.last().text)
        assertFalse(VerifiedTanyaPrint.hasDailyWords(lastCovered.last()))
    }

    @Test fun `small editorial insert and centered catchword remain separately styled`() {
        val lines = VerifiedTanyaPrint.fullPagesForReference(ref("28:1-5"))
        val small = lines.first { it.text.startsWith("ומן תיבת לשלש") }
        assertEquals((0..7).toSet(), small.wordFontScales.keys)
        assertFalse(8 in small.wordFontScales)
        assertTrue(lines.last().centered)
        assertTrue(lines.first { it.text.startsWith("כח מה שכתב") }.fontScale < 1f)
        assertTrue(lines.first { it.text == "המאיר מתחת לארץ לששים רבוא כוכבים:" }.centered)
        assertFalse(lines.first { it.text == "המאיר מתחת לארץ לששים רבוא כוכבים:" }.justified)
    }
}
