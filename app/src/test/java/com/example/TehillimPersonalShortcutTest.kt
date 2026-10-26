package com.example

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import com.example.data.local.UserPreferences
import com.example.domain.tehillim.DailyTehillimLesson
import com.example.domain.tehillim.TehillimChapter
import com.example.domain.tehillim.TehillimVerse
import com.example.ui.screens.TehillimReaderScreen
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
@Config(sdk = [35], qualifiers = "iw-rIL-w360dp-h800dp")
class TehillimPersonalShortcutTest {
    val compose = createComposeRule()
    @get:Rule val hostAndCompose = withComposeHost(compose)
    private val lesson = DailyTehillimLesson("2026-10-16", 5, "ה׳",
        listOf(TehillimChapter(29, "פרק כט", listOf(TehillimVerse(1, "א׳", "מזמור לדוד", "מזמור לדוד")))))

    @Test fun personalEntryIsReadableAndOpensEvenBeforeAddingChaptersInThisApp() {
        var theme by mutableStateOf("light")
        var opened = 0
        compose.setContent { DailyRambamTheme(readerTheme = theme) {
            TehillimReaderScreen(lesson = lesson, isLoading = false, onBack = {},
                preferences = UserPreferences(readerTheme = theme, keepScreenOn = false),
                onOpenPersonalPsalms = { opened++ })
        } }
        listOf("light", "dark", "sepia").forEach { selected ->
            compose.runOnIdle { theme = selected }
            val button = compose.onNodeWithTag("tehillim_personal_psalms")
            button.assertIsDisplayed()
            val text = mutableListOf<TextLayoutResult>()
            compose.onNodeWithText("הפרקים שלי").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(text) }
            val foreground = text.single().layoutInput.style.color.luminance()
            assertTrue("Text must be light on the dark theme and dark on light/sepia",
                if (selected == "dark") foreground > .65f else foreground < .1f)
            button.captureRoboImage("/tmp/daily-learning-check/tehillim-personal-$selected.png")
            button.performClick()
        }
        compose.runOnIdle { assertEquals(3, opened) }
        compose.runOnIdle { theme = "light" }
        compose.onNodeWithTag("tehillim_personal_psalms").captureRoboImage("/tmp/daily-learning-check/tehillim-personal-shortcut.png")
    }
}
