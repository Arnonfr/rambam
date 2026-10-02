package com.example

import com.example.domain.tanya.VerifiedTanyaPrint
import org.junit.Assert.*
import org.junit.Test

class VerifiedTanyaPrintTest {
    @Test fun `source page boundary and distinctive printed line endings are preserved`() {
        val lines = VerifiedTanyaPrint.forReference("Tanya, Part IV; Iggeret HaKodesh 23:1-3")
        assertEquals("חכמי המשנה ע״ה ששנו", lines[1].text)
        assertEquals("בתוך בנ״י ע״י עסק התורה והמצות בעשרה דוקא", lines[8].text)
        assertEquals("כמ״ש", lines[9].text)
        assertEquals(271, lines[10].page)
        assertEquals(2, lines.last().greyFromWord)
    }
    @Test fun `unmapped lessons never receive invented printed lines`() {
        assertTrue(VerifiedTanyaPrint.forReference("Tanya, Part IV; Iggeret HaKodesh 24:1").isEmpty())
    }
    @Test fun `following lesson crosses verified page boundary with context muted`() {
        val lines = VerifiedTanyaPrint.forReference("Tanya, Part IV; Iggeret HaKodesh 23:4-7")
        assertEquals(listOf(271, 272), lines.map { it.page }.distinct())
        assertEquals(2, lines.first().greyBeforeWord)
        assertEquals(6, lines.last().greyFromWord)
        assertEquals(listOf(1, 2, 3, 4), lines.map { it.section }.distinct())
        assertEquals("בנפשנו", lines.first { !it.justified }.text)
    }
    @Test fun `22 Tishrei starts and ends on original shared lines`() {
        val lines = VerifiedTanyaPrint.forReference("Tanya, Part IV; Iggeret HaKodesh 23:8-9")
        assertEquals(15, lines.size)
        assertTrue(lines.all { it.page == 272 })
        assertEquals(6, lines.first().greyBeforeWord)
        assertEquals(4, lines.last().greyFromWord)
        assertEquals("בגלותא על דדחקין לשכינתא ועל דעבדין קלנא", lines[13].text)
    }
    @Test fun `23 Tishrei includes whole final page and all five sections`() {
        val lines = VerifiedTanyaPrint.forReference("Tanya, Part IV; Iggeret HaKodesh 23:10-14")
        assertEquals(listOf(272, 273), lines.map { it.page }.distinct())
        assertEquals(27, lines.count { it.page == 273 })
        assertEquals(listOf(1, 2, 3, 4, 5), lines.map { it.section }.distinct())
        assertEquals(4, lines.first().greyBeforeWord)
        assertEquals("ורעי", lines[1].text)
        assertFalse(lines[1].justified)
        assertEquals("וירושלים תשכון לבטח אמן כן יהי רצון:", lines.last().text)
        assertNull(lines.last().greyFromWord)
    }
    @Test fun `24 Tishrei preserves both printed pages and complete epistle`() {
        val lines = VerifiedTanyaPrint.forReference("Tanya, Part IV; Iggeret HaKodesh 24:1-9")
        assertEquals(listOf(274, 275), lines.map { it.page }.distinct())
        assertEquals((1..9).toList(), lines.map { it.section }.distinct())
        assertEquals("בנפשו", lines.first { !it.justified }.text)
        assertTrue(lines.last().text.endsWith("ולישרים בלבותם:"))
        assertFalse(lines.any { Regex("[\u0591-\u05BD\u05BF-\u05C2\u05C4-\u05C5\u05C7]").containsMatchIn(it.text) })
    }
}
