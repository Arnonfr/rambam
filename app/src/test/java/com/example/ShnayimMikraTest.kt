package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.chumash.ChumashRepository
import com.example.data.local.UserPreferences
import com.example.ui.components.ReaderTypographySheet
import com.example.ui.components.TorahTextModeSelector
import com.example.ui.screens.ChumashReaderScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.DailyRambamTheme
import com.example.ui.theme.FrankRuhlFontFamily
import com.example.ui.util.HebrewTextNormalizer
import com.example.ui.util.TorahTextMode
import androidx.compose.ui.semantics.SemanticsProperties
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h900dp")
class ShnayimMikraTest {
    @get:Rule val compose = createComposeRule()

    private fun lesson(date: LocalDate) = runBlocking {
        ChumashRepository(RuntimeEnvironment.getApplication()).getDailyChumashLesson(date)!!
    }

    private fun verseText(tag: String) = compose.onNodeWithTag(tag).onChildren().fetchSemanticsNodes()
        .flatMap { it.config.getOrElseNullable(SemanticsProperties.Text) { null }.orEmpty() }.joinToString { it.text }

    @Test fun everyBundledVerseHasOnkelos() {
        val repository = ChumashRepository(RuntimeEnvironment.getApplication())
        val first = runBlocking { repository.getParashaByName("בראשית")!! }
        assertEquals("בקדמין ברא יי ית שמיא וית ארעא:", HebrewTextNormalizer.stripMarks(first.aliyot[0].verses[0].targum))
        val last = runBlocking { repository.getParashaByName("וזאת הברכה")!! }
        assertTrue(last.aliyot.last().verses.last().targum.startsWith("וּלְכֹל"))
    }

    @Test fun textModesKeepOrDropMarks() {
        val verse = "בְּרֵאשִׁ֖ית בָּרָ֣א אֱלֹהִ֑ים"
        assertEquals(verse, TorahTextMode.TEAMIM.verse(verse))
        assertEquals("בְּרֵאשִׁית בָּרָא אֱלֹהִים", TorahTextMode.NIKUD.verse(verse))
        assertEquals("בראשית ברא אלהים", TorahTextMode.PLAIN.verse(verse))
        assertEquals("<b>בראשית.</b> אמר", TorahTextMode.PLAIN.commentary("<b>בראשית.</b> אָמַר"))
        assertEquals("אָמַר", TorahTextMode.NIKUD.commentary("אָמַר"))
        // An unknown or missing choice falls back to the old default: vowels without cantillation.
        assertEquals(TorahTextMode.NIKUD, TorahTextMode.fromId(null))
    }

    @Test fun readerShowsEachVerseTwiceThenTargum() {
        val lesson = lesson(LocalDate.of(2026, 10, 8))
        compose.setContent { DailyRambamTheme {
            ChumashReaderScreen(chumashLesson = lesson, selectedAliyaIndex = 5,
                preferences = UserPreferences(), isLoading = false, onSelectAliya = {},
                onChangeFontSize = {}, onToggleAliyaCompletion = { _, _ -> }, onBack = {}, shnayimMikra = true)
        } }
        compose.onNodeWithTag("verse_item_4_19").assertIsDisplayed()
        compose.onNodeWithTag("shnayim_mikra_second_4_19").assertIsDisplayed()
        compose.onNodeWithTag("targum_4_19").assertIsDisplayed()
        compose.onNodeWithText("שניים מקרא").assertIsDisplayed()
        compose.onRoot().captureRoboImage(filePath = "build/review/shnayim-mikra-reader.png")
    }

    @Test fun parashaEndRepeatsTheLastVerse() {
        val lesson = lesson(LocalDate.of(2026, 10, 10))
        compose.setContent { DailyRambamTheme {
            ChumashReaderScreen(chumashLesson = lesson, selectedAliyaIndex = 7,
                preferences = UserPreferences(), isLoading = false, onSelectAliya = {},
                onChangeFontSize = {}, onToggleAliyaCompletion = { _, _ -> }, onBack = {}, shnayimMikra = true)
        } }
        val ending = hasText("בסיום הפרשה חוזרים על הפסוק האחרון")
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(ending)
        compose.onNode(ending).assertIsDisplayed()
        compose.onRoot().captureRoboImage(filePath = "build/review/shnayim-mikra-parasha-end.png")
    }

    @Test fun chumashFollowsTheChosenTextMode() {
        val lesson = lesson(LocalDate.of(2026, 10, 8))
        var preferences by mutableStateOf(UserPreferences(torahTextMode = "plain"))
        compose.setContent { DailyRambamTheme {
            ChumashReaderScreen(chumashLesson = lesson, selectedAliyaIndex = 5,
                preferences = preferences, isLoading = false, onSelectAliya = {},
                onChangeFontSize = {}, onToggleAliyaCompletion = { _, _ -> }, onBack = {})
        } }
        val text = verseText("verse_item_4_19")
        assertFalse(text.any { it in '֑'..'ׇ' && it != '־' && it != '׃' })
        compose.onRoot().captureRoboImage(filePath = "build/review/chumash-plain.png")
        preferences = UserPreferences(torahTextMode = "teamim")
        compose.waitForIdle()
        val withTeamim = verseText("verse_item_4_19")
        assertTrue(withTeamim.any { it in '֑'..'֯' })
    }

    @Test fun typographySheetOffersTheThreeModes() {
        var chosen: TorahTextMode? = null
        compose.setContent { DailyRambamTheme {
            ReaderTypographySheet(preferences = UserPreferences(), onDismiss = {}, onFontSizeChange = {},
                onLineSpacingChange = {}, onFontFamilyChange = {}, onNikudToggle = {}, onThemeChange = {},
                onKeepScreenOnChange = {}, showNikudSwitch = false,
                extraControls = { TorahTextModeSelector(TorahTextMode.NIKUD, FrankRuhlFontFamily) { chosen = it } })
        } }
        compose.onNodeWithText("הצג ניקוד וטעמים").assertDoesNotExist()
        compose.onNodeWithTag("torah_text_mode_nikud").assertIsSelected()
        compose.onRoot().captureRoboImage(filePath = "build/review/torah-text-mode-sheet.png")
        compose.onNodeWithTag("torah_text_mode_plain").performClick()
        assertEquals(TorahTextMode.PLAIN, chosen)
    }

    @Test fun settingsListsShnayimMikra() {
        compose.setContent { DailyRambamTheme {
            SettingsScreen(UserPreferences(visibleStudies = setOf("chumash", "shnayim_mikra")), onBack = {},
                onOpenAttribution = {}, onDayBoundaryChange = {}, onCityChange = {},
                onFontFamilyChange = {}, onToggleStudy = {}, onTanyaBookViewChange = {})
        } }
        compose.onNodeWithTag("settings_study_shnayim_mikra").assertIsOn()
        compose.onNodeWithText("שניים מקרא ואחד תרגום").assertIsDisplayed()
        compose.onRoot().captureRoboImage(filePath = "build/review/settings-shnayim-mikra.png")
    }

    @Test fun wholeParashaOpensAtTheFirstUnmarkedAliyaAndCanReturnToToday() {
        // Thursday of Bereishit, with ראשון and שני already marked.
        val base = lesson(LocalDate.of(2026, 10, 8))
        val lesson = base.copy(allAliyot = base.allAliyot.map { it.copy(isCompleted = it.aliyaIndex <= 2) })
        var scope: String? = null
        compose.setContent { DailyRambamTheme {
            ChumashReaderScreen(chumashLesson = lesson, selectedAliyaIndex = 0,
                preferences = UserPreferences(shnayimMikraScope = "week"), isLoading = false, onSelectAliya = {},
                onChangeFontSize = {}, onToggleAliyaCompletion = { _, _ -> }, onBack = {},
                onShnayimMikraScopeChange = { scope = it }, shnayimMikra = true)
        } }
        compose.onNodeWithText("עליה שלישי").assertIsDisplayed()
        compose.onNodeWithText("עליה 3 מתוך 7 בפרשה").assertIsDisplayed()
        compose.onNodeWithTag("shnayim_mikra_toggle_week").assertIsSelected()
        compose.onRoot().captureRoboImage(filePath = "build/review/shnayim-mikra-week.png")
        compose.onNodeWithTag("shnayim_mikra_toggle_day").performClick()
        assertEquals("day", scope)
    }
}
