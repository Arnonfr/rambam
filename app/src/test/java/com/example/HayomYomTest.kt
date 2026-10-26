package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.data.hayomyom.HayomYomCalendar
import com.example.data.hayomyom.HayomYomRepository
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HayomYomTest {
    @Test fun reportedCheshvan13TranslationIncludesTheWholeConclusion() {
        val lesson = HayomYomRepository(ApplicationProvider.getApplicationContext())
            .lessonFor(LocalDate.of(2026,10,24))!!
        val text = lesson.paragraphs.joinToString("\n")
        assertTrue(text.contains("את הממציא, את המספר ואת השומע"))
        assertTrue(text.endsWith("שזה קשה יותר מרציחה בגשמיות."))
        assertTrue(lesson.asReaderLesson().sections.last().textWithNikud.endsWith("מרציחה בגשמיות."))
    }

    @Test fun allBundledAphorismsHaveReviewedEndings() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val entries = context.assets.open("hayom_yom.json").bufferedReader().use { org.json.JSONObject(it.readText()).getJSONObject("entries") }
        entries.keys().forEach { key ->
            val paragraphs = entries.getJSONObject(key).getJSONArray("paragraphs")
            val text = paragraphs.getString(paragraphs.length()-1).trimEnd()
            assertFalse("Possible cut at $key: ${text.takeLast(100)}", text.isBlank() || text.last() in 'א'..'ת' || text.last().isDigit())
        }
    }

    @Test fun shevat10TranslationPreservesTheMeaningAboutEating() {
        val lesson = HayomYomRepository(ApplicationProvider.getApplicationContext())
            .lessonFor(LocalDate.of(2027,1,18))!!
        val text = lesson.paragraphs.joinToString("\n")
        assertFalse(text.contains("להיות כפויה"))
        assertTrue(text.contains("אינך צריכה להישאר בלי אוכל."))
        assertTrue(text.contains("מאמר זה אמר אבי אדמו\"ר לאחד על יחידות"))
    }

    @Test fun datesDoNotDependOnRambamScheduleOrDeviceTimeZone() {
        assertEquals("tishrei-22", HayomYomCalendar.key(LocalDate.of(2026,10,3)))
        assertEquals("tishrei-1", HayomYomCalendar.key(LocalDate.of(2026,9,12)))
        assertEquals("adar1-1", HayomYomCalendar.key(LocalDate.of(2024,2,10)))
        assertEquals("adar2-1", HayomYomCalendar.key(LocalDate.of(2024,3,11)))
        assertEquals("adar-1", HayomYomCalendar.key(LocalDate.of(2025,3,1)))
    }

    @Test fun everyDayInCommonAndLeapYearsHasItsOwnOfflineText() {
        val repository = HayomYomRepository(ApplicationProvider.getApplicationContext())
        val start = LocalDate.of(2023,9,16)
        repeat(1120) { offset ->
            val date = start.plusDays(offset.toLong())
            val lesson = repository.lessonFor(date)
            assertNotNull("Missing ${HayomYomCalendar.key(date)} on $date", lesson)
            assertEquals(date, lesson!!.date)
            assertTrue(lesson.paragraphs.all { it.isNotBlank() && !it.contains("{{") })
            assertTrue(lesson.revisionId > 0)
        }
    }

    @Test fun readerAdapterKeepsTextAndIndependentContentIdentity() {
        val lesson = HayomYomRepository(ApplicationProvider.getApplicationContext())
            .lessonFor(LocalDate.of(2026,10,1))!!
        val reader = lesson.asReaderLesson()
        assertEquals("היום יום", reader.bookTitle)
        assertTrue(reader.fullRef.startsWith("hayom_yom:"))
        assertEquals(lesson.paragraphs, reader.sections.map { it.textWithNikud })
    }

    @Test fun originalStudiesKeepTheOriginalWeekdayAndAssignmentsAcrossYears() {
        val repository = HayomYomRepository(ApplicationProvider.getApplicationContext())
        val today = repository.lessonFor(LocalDate.of(2026, 10, 6))!!.asReaderLesson().originalStudies!!
        val previousYear = repository.lessonFor(LocalDate.of(2025, 10, 17))!!.originalStudies!!
        assertEquals(previousYear, today)
        assertTrue(today.dateText.contains("יום ראשון"))
        assertTrue(today.dateText.contains("תש\"ד"))
        assertTrue(today.chumash.startsWith("נח"))
        assertTrue(today.tehillim.startsWith("קיט"))
        assertTrue(today.tanya.contains("להבין"))
    }

    @Test fun originalStudyHeadingsCoverCommonAndLeapYearsOffline() {
        val repository = HayomYomRepository(ApplicationProvider.getApplicationContext())
        val start = LocalDate.of(2023, 9, 16)
        repeat(1120) { offset ->
            val date = start.plusDays(offset.toLong())
            val heading = repository.lessonFor(date)!!.originalStudies
            assertNotNull("Missing original studies on $date", heading)
            assertTrue(heading!!.chumash.isNotBlank())
            assertTrue(heading.tehillim.isNotBlank())
            assertTrue(heading.tanya.isNotBlank())
        }
    }

    @Test fun commonAdarKeepsBothOriginalAdarsWithTheCombinedAphorisms() {
        val lesson = HayomYomRepository(ApplicationProvider.getApplicationContext())
            .lessonFor(LocalDate.of(2025, 3, 1))!!.asReaderLesson()
        assertTrue(lesson.originalStudies!!.dateText.contains("אדר א"))
        assertTrue(lesson.additionalOriginalStudies.single().dateText.contains("אדר שני"))
    }
    @Test fun verifiedCorrectionsStayInBundledTextAndAllAdarVariants() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val corpus = context.assets.open("hayom_yom.json").bufferedReader().use { org.json.JSONObject(it.readText()).getJSONObject("entries") }
        val corrections = context.assets.open("hayom_yom_corrections.json").bufferedReader().use { org.json.JSONObject(it.readText()).getJSONObject("entries") }
        corrections.keys().forEach { key ->
            val entry = corpus.getJSONObject(key)
            val paragraphs = entry.getJSONArray("paragraphs")
            val text = (0 until paragraphs.length()).joinToString("\n") { paragraphs.getString(it) }
            val record = corrections.getJSONObject(key)
            assertEquals(record.getString("sourceUrl"), entry.getString("correctionsSourceUrl"))
            val replacements = record.getJSONArray("replacements")
            for (i in 0 until replacements.length()) {
                val pair = replacements.getJSONArray(i)
                if (!pair.getString(1).contains(pair.getString(0)))
                    assertFalse("Uncorrected $key: ${pair.getString(0)}", text.contains(pair.getString(0)))
                assertTrue("Missing $key: ${pair.getString(1)}", text.contains(pair.getString(1)))
            }
        }
    }

}
