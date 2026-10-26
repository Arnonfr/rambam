package com.example

import com.example.data.chumash.ChumashRepository
import com.example.ui.screens.ChumashUiRow
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ChumashDailyTest {
    private val context get() = RuntimeEnvironment.getApplication()

    @Test fun todayUsesWorkbookBoundariesAndActualVerseNumbers() = runTest {
        val lesson = ChumashRepository(context).getDailyChumashLesson(LocalDate.of(2026, 10, 8))!!
        assertEquals("בראשית", lesson.parashaName)
        assertEquals(5, lesson.currentAliyaIndex)
        assertEquals(listOf(19, 20, 21, 22), lesson.todayAliya!!.verses.map { it.verseNumber })
        assertTrue(lesson.todayAliya!!.verses.all { it.chapterNumber == 4 })
        assertTrue(lesson.todayAliya!!.verses.first().rashi.first().contains("ויקח"))
    }

    @Test fun aChapterChangeKeepsVerseKeysUniqueAndRashiAttached() = runTest {
        val aliya = ChumashRepository(context).getParashaByName("בראשית")!!.aliyot[0]
        assertEquals(34, aliya.verses.size)
        assertEquals(1, aliya.verses.first().verseNumber)
        assertEquals(31, aliya.verses[30].verseNumber)
        assertEquals(2, aliya.verses[31].chapterNumber)
        assertEquals(1, aliya.verses[31].verseNumber)
        assertTrue(aliya.verses[31].rashi.isEmpty())
        assertTrue(aliya.verses[32].rashi.first().contains("ויכל אלהים"))
        val rowKeys = aliya.verses.mapIndexed { i, v -> ChumashUiRow.VerseRow(aliya, v, i, 34, 1, 1).key }
        assertEquals(34, rowKeys.distinct().size)
    }

    @Test fun combinedParashaUsesItsOwnSevenRanges() = runTest {
        val repository = ChumashRepository(context)
        val joined = repository.getParashaByName("ויקהל־פקודי")!!
        assertEquals("Exodus 38:1-39:1", joined.aliyot[3].ref)
        val lesson = repository.getDailyChumashLesson(LocalDate.of(2026, 3, 11))!!
        assertEquals("ויקהל-פקודי", lesson.parashaName)
        assertEquals(joined.aliyot[3].ref, lesson.todayAliya!!.ref)
        assertEquals(32, lesson.todayAliya!!.verses.size)
    }

    @Test fun israelAndDiasporaSelectDifferentParashotWhenRequired() = runTest {
        val repository = ChumashRepository(context)
        val date = LocalDate.of(2026, 6, 21)
        assertEquals("בלק", repository.getDailyChumashLesson(date, inIsrael = true)!!.parashaName)
        assertEquals("חוקת-בלק", repository.getDailyChumashLesson(date, inIsrael = false)!!.parashaName)
    }

    @Test fun festivalStudyRepeatsCycleInsteadOfPublicHolidayReading() = runTest {
        val repository = ChumashRepository(context)
        for (date in listOf(LocalDate.of(2027, 4, 18), LocalDate.of(2027, 4, 25))) {
            val lesson = repository.getDailyChumashLesson(date)!!
            assertEquals("אחרי מות", lesson.parashaName)
            assertEquals("Leviticus 16:1-16:17", lesson.todayAliya!!.ref)
        }
        assertEquals("וזאת הברכה", repository.getDailyChumashLesson(LocalDate.of(2026, 9, 27))!!.parashaName)
        assertEquals("בראשית", repository.getDailyChumashLesson(LocalDate.of(2026, 10, 4))!!.parashaName)
    }

    @Test fun simchatTorahFinishesVezotThenCatchesUpBereishitInOrder() = runTest {
        val lesson = ChumashRepository(context).getDailyChumashLesson(LocalDate.of(2025, 10, 14))!!
        assertEquals("וזאת הברכה ובראשית", lesson.parashaName)
        val verses = lesson.todayAliya!!.verses
        assertEquals("דברים", verses.first().bookName)
        assertEquals(33, verses.first().chapterNumber)
        assertEquals(13, verses.first().verseNumber)
        val firstGenesis = verses.indexOfFirst { it.bookName == "בראשית" }
        assertEquals(12, verses[firstGenesis - 1].verseNumber)
        assertEquals(34, verses[firstGenesis - 1].chapterNumber)
        assertEquals(1, verses[firstGenesis].verseNumber)
        assertEquals(3, verses.last().chapterNumber)
        assertEquals(21, verses.last().verseNumber)
        assertEquals(verses.size, verses.map { it.positionId }.distinct().size)
    }

    @Test fun everyBundledSegmentMatchesItsCatalogAndCoversEveryVerse() {
        val catalog = context.assets.open("chumash_daily_catalog.json").bufferedReader().use { JSONObject(it.readText()) }
        val content = context.assets.open("chumash_cache.json").bufferedReader().use { JSONObject(it.readText()) }
        val lengths = catalog.getJSONObject("chapterLengths")
        val rows = catalog.getJSONArray("segments")
        val allSingleKeys = mutableListOf<String>()
        assertEquals(427, rows.length())
        assertEquals(61, content.length())
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            val book = row.getString("book_id")
            val aliya = content.getJSONObject(row.getString("parasha_he"))
                .getJSONArray("aliyot").getJSONObject(row.getInt("day_index") - 1)
            val expected = mutableListOf<String>()
            for (c in row.getInt("start_chapter")..row.getInt("end_chapter")) {
                val start = if (c == row.getInt("start_chapter")) row.getInt("start_verse") else 1
                val end = if (c == row.getInt("end_chapter")) row.getInt("end_verse") else lengths.getJSONArray(book).getInt(c - 1)
                for (v in start..end) expected.add("$book:$c:$v")
            }
            val verses = aliya.getJSONArray("verses")
            val actual = (0 until verses.length()).map { j ->
                val v = verses.getJSONObject(j)
                assertTrue(v.getString("textWithTeamim").isNotBlank())
                "$book:${v.getInt("chapterNumber")}:${v.getInt("verseNumber")}"
            }
            assertEquals(row.getString("segment_id"), expected, actual)
            if (!row.getString("parasha_id").contains("_")) allSingleKeys.addAll(actual)
        }
        assertEquals(5846, allSingleKeys.size)
        assertEquals(5846, allSingleKeys.distinct().size)
    }
}
