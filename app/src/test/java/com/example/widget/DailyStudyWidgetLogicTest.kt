package com.example.widget

import com.example.data.local.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyStudyWidgetLogicTest {
    private val shortText = listOf(WidgetBlock("פתגם קצר של היום."))
    private val longText = listOf(WidgetBlock("מילה ".repeat(400).trim()))

    @Test
    fun shortTextFitsDefaultWidget() {
        val plan = WidgetTextFit.plan(shortText, bodyWidthDp = 264f, bodyHeightDp = 70f, fontSizeSp = 15f)
        assertEquals(WidgetTextFit.Mode.FULL, plan.mode)
    }

    @Test
    fun longTextIsTruncatedInSmallWidget() {
        val plan = WidgetTextFit.plan(longText, bodyWidthDp = 264f, bodyHeightDp = 70f, fontSizeSp = 15f)
        assertEquals(WidgetTextFit.Mode.TRUNCATED, plan.mode)
        assertTrue(plan.blocks.single().text.endsWith("..."))
        assertTrue(plan.lastBlockMaxLines in 1..3)
    }

    @Test
    fun enlargingTheWidgetShowsMoreThenAllOfTheText() {
        val small = WidgetTextFit.plan(longText, bodyWidthDp = 264f, bodyHeightDp = 70f, fontSizeSp = 15f)
        val large = WidgetTextFit.plan(longText, bodyWidthDp = 264f, bodyHeightDp = 260f, fontSizeSp = 15f)
        assertTrue(large.blocks.single().text.length > 3 * small.blocks.single().text.length)
        val huge = WidgetTextFit.plan(longText, bodyWidthDp = 264f, bodyHeightDp = 900f, fontSizeSp = 15f)
        assertEquals(WidgetTextFit.Mode.FULL, huge.mode)
        assertEquals(longText, huge.blocks)
    }

    @Test
    fun largerFontScaleShowsLessText() {
        val normal = WidgetTextFit.plan(longText, 264f, 120f, 15f, fontScale = 1f)
        val large = WidgetTextFit.plan(longText, 264f, 120f, 15f, fontScale = 1.3f)
        assertTrue(large.blocks.single().text.length < normal.blocks.single().text.length)
    }

    @Test
    fun truncateStopsAtWordBoundary() {
        assertEquals("אחת שתיים...", WidgetTextFit.truncate("אחת שתיים שלוש ארבע", 14))
    }

    @Test
    fun buttonsKeepOrderAndReplaceMainLesson() {
        assertEquals(
            listOf("חומש", "תהילים", "תניא", "רמב״ם"),
            WidgetContentLoader.buttonsFor(WidgetStudy.HAYOM_YOM, WidgetStudy.RAMBAM_ONE).map { it.label }
        )
        assertEquals(
            listOf(WidgetStudy.CHUMASH, WidgetStudy.TEHILLIM, WidgetStudy.HAYOM_YOM, WidgetStudy.RAMBAM_THREE),
            WidgetContentLoader.buttonsFor(WidgetStudy.TANYA, WidgetStudy.RAMBAM_THREE).map { it.study }
        )
    }

    @Test
    fun rambamButtonFollowsUserTrack() {
        fun target(track: String, visible: Set<String>) =
            WidgetContentLoader.rambamTarget(UserPreferences(selectedTrack = track, visibleStudies = visible))
        assertEquals(WidgetStudy.RAMBAM_THREE, target("three", setOf("rambam_three", "chumash")))
        assertEquals(WidgetStudy.RAMBAM_ONE, target("three", setOf("rambam_one")))
        assertEquals(WidgetStudy.MITZVOT, target("one", setOf("mitzvot", "tanya")))
    }

    @Test
    fun unknownStoredValuesFallBackToDefaults() {
        assertEquals(WidgetMainLesson.HAYOM_YOM, WidgetMainLesson.fromKey("removed_option"))
        assertEquals(WidgetTheme.DARK, WidgetTheme.fromKey(null))
    }

    @Test
    fun hayomYomKeepsHebrewAndTranslationOnly() {
        val original = listOf(
            "מברכים ברכת הקשת, ולא כיש מפקפקים בזה.",
            "דער בעש\"ט האט געהייסען די תלמידים געבען איינע די אנדערע די הענט.",
            "יום הכיפורים קומט אויס צו פאסטין זעקס און צוואנציג שעה.\n----\nתרגום ללשון הקודש: ביום הכיפורים יוצא לצום עשרים ושש שעות.",
            "הבעל שם טוב צוה לתלמידיו שיתנו אחד לשני את ידיהם."
        )
        assertEquals(
            listOf(
                "מברכים ברכת הקשת, ולא כיש מפקפקים בזה.",
                "ביום הכיפורים יוצא לצום עשרים ושש שעות.",
                "הבעל שם טוב צוה לתלמידיו שיתנו אחד לשני את ידיהם."
            ),
            HayomYomHebrew.paragraphs(original)
        )
    }

    @Test
    fun hayomYomWithoutTranslationFallsBackToOriginal() {
        val yiddishOnly = listOf("דער רבי האט געזאגט אז מען דארף זיין א חסיד.")
        assertEquals(yiddishOnly, HayomYomHebrew.paragraphs(yiddishOnly))
    }
}
