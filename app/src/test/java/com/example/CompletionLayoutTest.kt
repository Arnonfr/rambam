package com.example

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.example.data.local.*
import com.example.ui.screens.StudyCompletionReaderScreen
import com.example.ui.theme.DailyRambamTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp")
class CompletionLayoutTest {
    @get:Rule val compose = createComposeRule()
    private var saved: SavedStudyCompletion? = null
    private val entry = SavedStudyCompletion("one", "2026-10-06", "כ״ה תשרי", "רמב״ם", "אחרי הלכה א",
        (1..20).map { CompletionText("$it", "הלכה $it", "טקסט ללימוד ".repeat(18), "טקסט ללימוד ".repeat(18)) })

    private fun show() {
        compose.setContent {
            DailyRambamTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    StudyCompletionReaderScreen(entry, UserPreferences(), {}, { saved = it }, {})
                }
            }
        }
    }

    @Test fun footerRemainsAtBottomAndSaveWaitsForConfirmation() {
        show()
        compose.onNodeWithTag("manual_completion_menu").assertWidthIsEqualTo(52.dp)
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val back = compose.onNodeWithTag("back_saved_completion").fetchSemanticsNode().boundsInRoot
        assertTrue("Back button must stay at the bottom", root.bottom - back.bottom < 40)
        compose.onNodeWithTag("manual_completion_menu").performClick()
        compose.onNodeWithTag("save_for_later").performClick()
        compose.onNodeWithTag("completion_boundary_line").assertExists()
        compose.onNodeWithTag("completion_boundary_preview").assertIsDisplayed()
        assertNull(saved)
        val confirm = compose.onNodeWithTag("confirm_save_completion").fetchSemanticsNode().boundsInRoot
        val cancel = compose.onNodeWithText("ביטול").fetchSemanticsNode().boundsInRoot
        assertTrue("Primary action should be on the right in Hebrew", confirm.left > cancel.right)
        compose.onNodeWithTag("confirm_save_completion").performClick()
        compose.runOnIdle {
            assertNotNull(saved)
            assertTrue(saved!!.blocks.size < entry.blocks.size)
            assertEquals(entry.id, saved!!.id)
        }
    }

    @Test fun completionStripHasUsableExpandRemoveAndContinueButtons() {
        var opened = false
        compose.setContent { DailyRambamTheme {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                com.example.ui.screens.SavedCompletionsStrip(androidx.compose.ui.graphics.Color(0xFFFF873F), listOf(entry), { opened = true }, {})
            }
        } }
        compose.onNodeWithContentDescription("פתח שיעורים להשלים").assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(48.dp).performClick()
        compose.onNodeWithContentDescription("הסר את ההשלמה מ${entry.dateLabel}").assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(48.dp)
        compose.onRoot().captureRoboImage(filePath = "build/review/completions-controls.png")
        compose.onNodeWithText("המשך השלמה").performClick()
        compose.runOnIdle { assertTrue(opened) }
    }

    @Test fun cancellingPreviewDoesNotChangeTheSavedLesson() {
        show()
        compose.onNodeWithTag("manual_completion_menu").performClick()
        compose.onNodeWithTag("save_for_later").performClick()
        compose.onNodeWithText("ביטול").performClick()
        compose.runOnIdle { assertNull(saved) }
        compose.onNodeWithTag("finish_saved_completion").assertIsDisplayed()
    }
}
