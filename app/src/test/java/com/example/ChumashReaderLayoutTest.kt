package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.chumash.ChumashRepository
import com.example.data.local.UserPreferences
import com.example.ui.screens.ChumashReaderScreen
import com.example.ui.theme.DailyRambamTheme
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.runBlocking
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
class ChumashReaderLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test fun thursdayShowsChapterFourAndVerseNineteen() {
        val lesson = runBlocking { ChumashRepository(RuntimeEnvironment.getApplication())
            .getDailyChumashLesson(LocalDate.of(2026, 10, 8))!! }
        compose.setContent { DailyRambamTheme {
            ChumashReaderScreen(chumashLesson = lesson, selectedAliyaIndex = 5,
                preferences = UserPreferences(), isLoading = false, onSelectAliya = {},
                onToggleShowTeamim = {}, onChangeFontSize = {}, onToggleAliyaCompletion = { _, _ -> }, onBack = {})
        } }
        compose.onNodeWithText("בראשית · פרק ד").assertIsDisplayed()
        compose.onNodeWithTag("verse_item_4_19").assertIsDisplayed()
        compose.onNodeWithText("י״ט. ", substring = true).assertIsDisplayed()
        compose.onNodeWithText("חמישי · ד׳:י״ט", substring = true).assertIsDisplayed()
        compose.onRoot().captureRoboImage(filePath = "build/review/chumash-daily-127.png")
    }
}
