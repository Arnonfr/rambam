package com.example
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.tanya.TanyaRepository
import com.example.domain.tanya.TanyaSourceLine
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
class TanyaCycleStartTest {
    private fun leaves(line: TanyaSourceLine): List<TanyaSourceLine> =
        if (line.columns.isEmpty()) listOf(line) else line.columns.flatten().flatMap { leaves(it) }
    private fun active(lines: List<TanyaSourceLine>) = lines.flatMap { leaves(it) }
        .filterNot { it.isCatchword }.flatMap { l -> l.text.split(' ').filterIndexed { i, _ -> i in (l.activeWordIndices ?: emptySet()) } }.joinToString(" ")
    @Test fun sixtyCompleteOfflineDaysFollowTheLeapYearCycle() = runBlocking {
        val repo = TanyaRepository(ApplicationProvider.getApplicationContext<Context>())
        val start = LocalDate.of(2026, 11, 29)
        repeat(60) { offset ->
            val date = start.plusDays(offset.toLong())
            val lesson = repo.getDailyTanyaLesson(date)!!
            assertEquals(date.toString(), lesson.date)
            val lines = VerifiedTanyaPrint.fullPagesForReference(lesson.fullRef)
            assertTrue("No print mapping: " + lesson.fullRef, lines.isNotEmpty())
            val all = lines.flatMap { leaves(it) }
            assertTrue("Empty daily text: " + lesson.fullRef, active(lines).isNotBlank())
            assertTrue(all.none { it.text.contains("[מ:") || it.text.contains("[פ:") })
            assertTrue(lesson.sections.none { it.textPlain.contains("[מ:") || it.textPlain.contains("[פ:") })
        }
        assertEquals("Tanya, Part I; Likkutei Amarim, Compiler's Foreword 13-15", repo.getDailyTanyaLesson(LocalDate.of(2026, 12, 3))!!.fullRef)
        assertEquals("Tanya, Part I; Likkutei Amarim 1:1-12", repo.getDailyTanyaLesson(LocalDate.of(2026, 12, 4))!!.fullRef)
        val last = repo.getDailyTanyaLesson(LocalDate.of(2027, 1, 27))!!
        assertEquals("יט שבט", last.hebrewDate)
        assertEquals("Tanya, Part I; Likkutei Amarim 23:4", last.fullRef)
    }
    @Test fun titleDayIncludesBothApprobationsAndAllTwentyParagraphs() = runBlocking {
        val lesson = TanyaRepository(ApplicationProvider.getApplicationContext<Context>()).getDailyTanyaLesson(LocalDate.of(2026, 11, 29))!!
        assertEquals(20, lesson.sections.size)
        val lines = VerifiedTanyaPrint.fullPagesForReference(lesson.fullRef)
        assertEquals(listOf(1, 4), lines.map { it.page }.distinct())
        val text = active(lines)
        assertTrue(text.contains("משולם זוסיל"))
        assertTrue(text.contains("יהודא ליב הכהן"))
        assertFalse(lines.first().isCatchword)
        assertTrue(lines.first().centered)
        assertTrue(lines.any { it.columns.size == 2 })
    }
    @Test fun oldEditionOmissionsRemainAvailableAsMarkedCorrections() {
        val approval = active(VerifiedTanyaPrint.fullPagesForReference("Tanya, Part I; Likkutei Amarim, Approbation 3:1-10"))
        assertTrue(approval.contains("בנוסח המתוקן:"))
        assertTrue(approval.contains("תקע"))
        val section = active(VerifiedTanyaPrint.fullPagesForReference("Tanya, Part I; Likkutei Amarim 7:8"))
        assertTrue(section.contains("וכן החיות"))
        assertTrue(section.contains("בנוסח המתוקן:"))
        assertTrue(section.contains("אשתו טהורה"))
    }
    @Test fun sharedPagesKeepIdenticalRowsAndOnlySelectedWordsChange() {
        val a = VerifiedTanyaPrint.fullPagesForReference("Tanya, Part I; Likkutei Amarim 23:1-3")
        val b = VerifiedTanyaPrint.fullPagesForReference("Tanya, Part I; Likkutei Amarim 23:4")
        val shared = a.map { it.page }.intersect(b.map { it.page }.toSet())
        assertTrue(shared.isNotEmpty())
        for (page in shared) assertEquals(a.filter { it.page == page }.map { it.text }, b.filter { it.page == page }.map { it.text })
        assertFalse(active(b).contains("ומאחר"))
        assertTrue(b.any { !VerifiedTanyaPrint.hasDailyWords(it) })
    }
    @Test fun finalParagraphOnASharedSourceRowIsNeverDropped() {
        val text = active(VerifiedTanyaPrint.fullPagesForReference("Tanya, Part I; Likkutei Amarim 11:1-8"))
        assertTrue(text.endsWith("ולכן ארז\"ל כל בי עשרה שכינתא שריא:"))
    }
    @Test fun unpreparedDaysFailClosedAndPartNumbersNeverCollide() {
        assertTrue(VerifiedTanyaPrint.fullPagesForReference("Tanya, Part I; Likkutei Amarim 23:5-7").isEmpty())
        assertTrue(VerifiedTanyaPrint.fullPagesForReference("Tanya, Part I; Likkutei Amarim 24:1").isEmpty())
        assertTrue(VerifiedTanyaPrint.fullPagesForReference("Tanya, Part I; Likkutei Amarim 1:1-19").isEmpty())
        assertFalse(VerifiedTanyaPrint.fullPagesForReference("Tanya, Part IV; Iggeret HaKodesh 23:1-3").isEmpty())
    }
}
