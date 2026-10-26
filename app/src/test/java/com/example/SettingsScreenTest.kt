package com.example

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.local.UserPreferences
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.DailyRambamTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h800dp")
class SettingsScreenTest {
    val compose = createComposeRule()
    @get:Rule val hostAndCompose = withComposeHost(compose)

    @Test fun zmanimHasAnInternalPageAndBackReturnsToGeneralPreferences() {
        var closed = false
        compose.setContent { DailyRambamTheme {
            SettingsScreen(UserPreferences(),onBack={closed=true},onOpenAttribution={},onDayBoundaryChange={},onCityChange={},
                onFontFamilyChange={},onToggleStudy={},onTanyaBookViewChange={})
        } }
        compose.onNodeWithTag("zmanim_enabled").assertDoesNotExist()
        compose.onNodeWithTag("settings_zmanim").performScrollTo().performClick()
        compose.onNodeWithTag("zmanim_settings_page").assertExists()
        compose.onNodeWithTag("zmanim_enabled").assertIsDisplayed()
        compose.onNodeWithTag("settings_study_tanya").assertDoesNotExist()
        compose.onRoot().captureRoboImage("build/review/zmanim-internal-settings.png")
        compose.onNodeWithTag("close_settings_button").performClick()
        compose.onNodeWithTag("settings_study_tanya").assertIsDisplayed()
        assertFalse(closed)
        compose.onNodeWithTag("close_settings_button").performClick()
        assertTrue(closed)
    }

    @Test fun ratingHasADirectStoreActionInTheNormalPreferencesScreen() {
        var opened=false
        compose.setContent { DailyRambamTheme {
            SettingsScreen(UserPreferences(),onBack={},onOpenAttribution={},onDayBoundaryChange={},onCityChange={},
                onFontFamilyChange={},onToggleStudy={},onTanyaBookViewChange={},onRateInStore={opened=true})
        } }
        compose.onNodeWithTag("settings_rate_in_store").performScrollTo().assertIsDisplayed().performClick()
        assertTrue(opened)
        compose.onRoot().captureRoboImage("build/review/settings-rate-in-store-139.png")
    }

    @Test fun readingControlsChangeTheSamePreferencesAndFontSamplesShareABaseline() {
        val preferences = mutableStateOf(UserPreferences())
        compose.setContent { DailyRambamTheme {
            SettingsScreen(preferences.value, onBack = {}, onOpenAttribution = {},
                onDayBoundaryChange = {}, onCityChange = {}, onFontFamilyChange = {},
                onToggleStudy = {}, onTanyaBookViewChange = {},
                onFontSizeChange = { preferences.value = preferences.value.copy(fontSizeSp = it) },
                onLineSpacingChange = { preferences.value = preferences.value.copy(lineSpacingMultiplier = it) },
                onLetterSpacingChange = { preferences.value = preferences.value.copy(letterSpacingEm = it) },
                onNikudToggle = { preferences.value = preferences.value.copy(showNikud = it) },
                onThemeChange = { preferences.value = preferences.value.copy(readerTheme = it) },
                onKeepScreenOnChange = { preferences.value = preferences.value.copy(keepScreenOn = it) })
        } }
        compose.onNodeWithTag("settings_font_bonanova").performScrollTo()
        val baselines = com.example.ui.theme.FontStyleOption.entries.map { option ->
            val node = compose.onNodeWithTag("settings_font_sample_${option.id}", useUnmergedTree = true)
            val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            node.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
            node.fetchSemanticsNode().boundsInRoot.top + layouts.single().firstBaseline
        }
        assertTrue("The Hebrew samples must share their baseline", baselines.max() - baselines.min() <= 1f)
        compose.onNodeWithTag("settings_font_size_increase").performScrollTo().performClick()
        compose.onNodeWithTag("settings_line_spacing_increase").performScrollTo().performClick()
        compose.onNodeWithTag("settings_letter_spacing").performScrollTo()
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) { it(.08f) }
        compose.onNodeWithTag("settings_theme_sepia").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithTag("settings_nikud").performScrollTo().performClick().assertIsOff()
        compose.onNodeWithTag("settings_keep_screen_on").performScrollTo().performClick().assertIsOff()
        compose.runOnIdle {
            assertEquals(21f, preferences.value.fontSizeSp)
            assertEquals(1.75f, preferences.value.lineSpacingMultiplier, .001f)
            assertEquals(.08f, preferences.value.letterSpacingEm, .001f)
            assertEquals("sepia", preferences.value.readerTheme)
            assertFalse(preferences.value.showNikud)
            assertFalse(preferences.value.keepScreenOn)
        }
        compose.onNodeWithTag("settings_font_size_increase").performScrollTo()
        compose.onRoot().captureRoboImage(filePath = "build/review/settings-typography-controls.png")
    }

    @Test fun choicesUpdateIndependentlyAndCityOnlyAppearsForSunset() {
        val preferences = mutableStateOf(UserPreferences())
        compose.setContent {
            DailyRambamTheme {
                SettingsScreen(
                    preferences.value, onBack = {}, onOpenAttribution = {},
                    onDayBoundaryChange = { preferences.value = preferences.value.copy(dayBoundary = it) },
                    onCityChange = { preferences.value = preferences.value.copy(selectedCity = it) },
                    onFontFamilyChange = { preferences.value = preferences.value.copy(fontFamily = it) },
                    onToggleStudy = {},
                    onTanyaBookViewChange = { preferences.value = preferences.value.copy(tanyaBookView = it) }
                )
            }
        }
        compose.onNodeWithTag("settings_city").assertDoesNotExist()
        compose.onNodeWithTag("settings_day_sunset").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithTag("settings_day_midnight").assertIsNotSelected()
        compose.onNodeWithTag("settings_city").performScrollTo().performClick()
        compose.onNodeWithText("תל אביב - יפו").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("תל אביב - יפו", preferences.value.selectedCity) }
        compose.onNodeWithTag("home_tanya_book_view").performScrollTo().performClick().assertIsOn()
        compose.onNodeWithTag("settings_font_libertinus").performScrollTo().performClick().assertIsSelected()
        compose.runOnIdle {
            assertEquals("libertinus", preferences.value.fontFamily)
            assertTrue(preferences.value.tanyaBookView)
            assertEquals("sunset", preferences.value.dayBoundary)
        }
        compose.onNodeWithTag("settings_day_midnight").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithTag("settings_city").assertDoesNotExist()
        compose.onRoot().captureRoboImage(filePath = "build/review/settings-reading.png")
    }

    @Test fun studyVisibilityAndBackDoNotChangeReadingPreferences() {
        val initial = UserPreferences(tanyaBookView = true)
        val preferences = mutableStateOf(initial)
        var wentBack = false
        compose.setContent {
            DailyRambamTheme {
                SettingsScreen(
                    preferences.value, onBack = { wentBack = true }, onOpenAttribution = {},
                    onDayBoundaryChange = {}, onCityChange = {}, onFontFamilyChange = {},
                    onTanyaBookViewChange = {}, onToggleStudy = { key ->
                        val visible = preferences.value.visibleStudies
                        preferences.value = preferences.value.copy(
                            visibleStudies = if (key in visible) visible - key else visible + key
                        )
                    }
                )
            }
        }
        compose.onNodeWithTag("settings_screen").assertIsDisplayed()
        compose.onNodeWithTag("settings_study_tanya").assertIsOn().performClick().assertIsOff()
        compose.runOnIdle {
            assertEquals(initial.copy(visibleStudies = initial.visibleStudies - "tanya"), preferences.value)
        }
        compose.onRoot().captureRoboImage(filePath = "build/review/settings-studies.png")
        compose.onNodeWithTag("close_settings_button").performClick()
        compose.runOnIdle { assertTrue(wentBack) }
    }
}
