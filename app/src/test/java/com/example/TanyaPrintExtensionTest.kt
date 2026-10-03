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

    @Test fun `invalid range is never silently truncated`() {
        assertTrue(VerifiedTanyaPrint.fullPagesForReference(ref("28:6-10")).isEmpty())
        assertFalse(VerifiedTanyaPrint.fullPagesForReference(ref("28:6-9")).isEmpty())
        assertFalse(VerifiedTanyaPrint.fullPagesForReference(ref("28:6")).isEmpty())
        val lastCovered = VerifiedTanyaPrint.fullPagesForReference(ref("28:1-5"))
        assertEquals("ואתהפכא", lastCovered.last().text)
        assertFalse(VerifiedTanyaPrint.hasDailyWords(lastCovered.last()))
    }

    @Test fun `all thirty remaining pages reach end of Kuntres Acharon without gaps`() {
        val references = listOf("28:6-9", "29:1-27", "30:1-5", "31:1-9", "32:1-10").map(::ref) +
            listOf(1 to 6, 2 to 6, 3 to 7, 4 to 58, 5 to 10, 6 to 15, 7 to 6, 8 to 8, 9 to 12)
                .map { (chapter, end) -> "Tanya, Part V; Kuntres Acharon $chapter:1-$end" }
        val lines = references.flatMap(VerifiedTanyaPrint::fullPagesForReference)
        assertEquals((296..325).toSet(), lines.map { it.page }.filter { it >= 296 }.toSet())
        assertTrue(lines.none { Regex("\\[\\d+:\\d+\\]").containsMatchIn(it.text) })
        val final = VerifiedTanyaPrint.fullPagesForReference("Tanya, Part V; Kuntres Acharon 9:12")
        assertEquals(325, final.first().page)
        assertEquals("לעומת זה כו׳:", final.last().text)
        assertTrue(final.takeWhile { !VerifiedTanyaPrint.hasDailyWords(it) }.size > 20)
        assertFalse(VerifiedTanyaPrint.fullPagesForReference("Tanya, Part V; Kuntres Acharon 9:1-13").isNotEmpty())
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
