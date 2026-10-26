package com.example

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.local.UserPreferences
import com.example.data.tehillim.PersonalTehillimState
import com.example.data.tehillim.PersonalTehillimStore
import com.example.domain.tehillim.*
import com.example.ui.screens.PersonalTehillimScreen
import com.example.ui.theme.DailyRambamTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "iw-rIL-w360dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PersonalTehillimScreenTest {
    val compose = createComposeRule()
    @get:Rule val hostAndCompose = withComposeHost(compose)
    private val date = LocalDate.of(2026, 10, 15)
    private var back = false
    private fun show(state: MutableState<PersonalTehillimState>, manage: Boolean = false) {
        compose.setContent { DailyRambamTheme {
            PersonalTehillimScreen(state.value, date, UserPreferences(keepScreenOn = false), onBack = { back = true },
                startWithManagement = manage,
                onSave = { entry -> state.value = state.value.copy(entries = state.value.entries.filterNot { it.id == entry.id } + entry) },
                onDelete = {}, onMove = { _, _ -> }, onComplete = { entry, checked ->
                    val key = PersonalTehillimStore.completionKey(entry.id, date.toString(), entry.chapter(date)!!)
                    state.value = state.value.copy(completed = if (checked) state.value.completed + key else state.value.completed - key)
                }, onFontSizeChange = {}, onLineSpacingChange = {}, onFontFamilyChange = {},
                onNikudToggle = {}, onThemeChange = {}, onKeepScreenOnChange = {})
        } }
    }

    @Test fun homeOpensReadingDirectlyWithEditAndPullToComplete() {
        val entry = PersonalTehillimEntry(label = "שלי", fixedChapter = 1)
        val state = mutableStateOf(PersonalTehillimState(listOf(entry)))
        show(state)
        compose.onNodeWithTag("personal_psalm_reader").assertExists()
        compose.onNodeWithTag("personal_tehillim_screen").assertDoesNotExist()
        compose.onNodeWithTag("personal_reader_menu").assertDoesNotExist()
        compose.onNodeWithTag("personal_reader_edit").assertTextEquals("עריכה").performClick()
        compose.onNodeWithTag("personal_tehillim_screen").assertExists()
        compose.onNodeWithTag("personal_open_${entry.id}").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("personal_verse_${entry.id}_1").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("finish_personal_psalm").assertDoesNotExist()
        compose.onNodeWithTag("personal_psalm_text").performScrollToIndex(6)
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("personal_psalm_text").performTouchInput { swipeUp() }
        compose.mainClock.advanceTimeBy(2000)
        compose.mainClock.autoAdvance = true
        compose.waitUntil(5000) { back }
        compose.runOnIdle {
            assertTrue(back)
            assertTrue(PersonalTehillimStore.completionKey(entry.id, date.toString(), 1) in state.value.completed)
        }
    }

    @Test fun chaptersShareOneContinuousListAndOnlyTheEndOfTheSequenceExits() {
        val first = PersonalTehillimEntry(label = "שלי", fixedChapter = 1)
        val second = PersonalTehillimEntry(label = "למשפחה", fixedChapter = 2)
        val state = mutableStateOf(PersonalTehillimState(listOf(first, second)))
        show(state)
        compose.waitUntil(15000) { compose.onAllNodesWithTag("personal_verse_${first.id}_1").fetchSemanticsNodes().isNotEmpty() }
        val reader = compose.onNodeWithTag("personal_psalm_text")
        // Last verse of the first entry and the following heading coexist in the same viewport.
        reader.performScrollToIndex(6)
        compose.onNodeWithTag("personal_verse_${first.id}_6").assertIsDisplayed()
        compose.onNodeWithTag("personal_heading_${second.id}").assertIsDisplayed()
        compose.runOnIdle { assertFalse(back) }
        reader.performScrollToIndex(7)
        compose.onNodeWithTag("personal_verse_${second.id}_1").assertExists()
        compose.onNodeWithContentDescription("השיעור הושלם").assertDoesNotExist()
        compose.runOnIdle {
            assertFalse(back)
            assertTrue(PersonalTehillimStore.completionKey(first.id, date.toString(), 1) in state.value.completed)
            assertFalse(PersonalTehillimStore.completionKey(second.id, date.toString(), 2) in state.value.completed)
        }
        reader.performScrollToIndex(19)
        compose.mainClock.autoAdvance = false
        reader.performTouchInput { swipeUp() }
        compose.mainClock.advanceTimeBy(2000)
        compose.mainClock.autoAdvance = true
        compose.waitUntil(5000) { back }
        compose.runOnIdle {
            assertTrue(PersonalTehillimStore.completionKey(second.id, date.toString(), 2) in state.value.completed)
        }
    }

    @Test fun addingAChapterUsesTheBrandedEditorAndSavesTheOptionalName() {
        val state = mutableStateOf(PersonalTehillimState())
        show(state, manage = true)
        compose.onNodeWithTag("add_personal_psalm").performScrollTo().performClick()
        compose.onNodeWithTag("personal_psalm_editor").assertExists()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("personal_label").performTextInput("משפחה")
        compose.mainClock.advanceTimeBy(300)
        compose.onNodeWithTag("save_personal_psalm").performClick()
        compose.mainClock.advanceTimeBy(300)
        compose.mainClock.autoAdvance = true
        compose.runOnIdle {
            assertEquals("משפחה", state.value.entries.single().label)
            assertEquals(1, state.value.entries.single().fixedChapter)
        }
    }

    @Test fun hebrewBirthdayUsesLetterDaysAndHorizontalMonthAndYearSelectors() {
        val entry = PersonalTehillimEntry(label = "שלי", birthday = HebrewBirthday(5757, 1, 4))
        val state = mutableStateOf(PersonalTehillimState(listOf(entry)))
        show(state, manage = true)
        compose.onNodeWithText("עריכה").performClick()
        compose.onNodeWithTag("personal_day_4").assertTextEquals("ד")
        compose.onNodeWithTag("personal_month_1").assertTextEquals("חשוון")
        compose.onNodeWithTag("personal_year_picker").performScrollTo().performScrollToIndex(1)
        compose.onNodeWithTag("personal_year_5786").assertIsSelected()
        compose.onNodeWithTag("save_personal_psalm").performClick()
        compose.runOnIdle {
            assertEquals(HebrewBirthday(5786, 1, 4), state.value.entries.single().birthday)
            assertEquals(2, state.value.entries.single().chapter(date))
        }
    }
}
