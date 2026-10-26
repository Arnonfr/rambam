package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.data.hayomyom.HayomYomRepository
import com.example.data.local.UserPreferences
import com.example.ui.screens.TanyaReaderScreen
import com.example.ui.theme.DailyRambamTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp")
class HayomYomReaderLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test fun reportedCheshvan13ConclusionCanBeReadAtLargeFont() {
        val lesson = HayomYomRepository(ApplicationProvider.getApplicationContext())
            .lessonFor(LocalDate.of(2026,10,24))!!.asReaderLesson()
        compose.setContent { DailyRambamTheme {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                TanyaReaderScreen(lesson=lesson,isLoading=false,onBack={},preferences=UserPreferences(fontSizeSp=32f),
                    readerTitle="היום יום",contentId="hayom_yom",allowPrintLayout=false)
            }
        } }
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(lesson.sections.size)
        compose.onNode(hasScrollToIndexAction()).performSemanticsAction(SemanticsActions.ScrollBy) { it(0f,100000f) }
        compose.waitForIdle()
        val ending = compose.onNodeWithText("שזה קשה יותר מרציחה בגשמיות.",substring=true,useUnmergedTree=true)
        ending.assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        ending.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val layout = layouts.single()
        val lastLineTop = ending.fetchSemanticsNode().positionInRoot.y + layout.getLineTop(layout.lineCount-1)
        val lastLineBottom = ending.fetchSemanticsNode().positionInRoot.y + layout.getLineBottom(layout.lineCount-1)
        org.junit.Assert.assertTrue("Last line must be below the header",lastLineTop>=52*compose.density.density)
        org.junit.Assert.assertTrue("Last line must be above the floating bar",lastLineBottom<compose.onRoot().fetchSemanticsNode().boundsInRoot.bottom-100*compose.density.density)
        compose.onRoot().captureRoboImage("build/review/hayom-cheshvan-13-complete-145.png")
    }

    @Test fun largeFontKeepsStudyNamesAndColonsTogether() {
        val lesson = HayomYomRepository(ApplicationProvider.getApplicationContext())
            .lessonFor(LocalDate.of(2026, 10, 6))!!.asReaderLesson()
        compose.setContent { DailyRambamTheme {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                TanyaReaderScreen(lesson = lesson, isLoading = false, onBack = {},
                    preferences = UserPreferences(fontSizeSp = 32f, fontFamily = "serif"),
                    readerTitle = "היום יום", contentId = "hayom_yom", allowPrintLayout = false)
            }
        } }
        listOf("חומש", "תהלים", "תניא").forEach { label ->
            val results = mutableListOf<TextLayoutResult>()
            compose.onNodeWithTag("hayom_original_$label").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
            val layout = results.single()
            assertEquals(layout.getLineForOffset(0), layout.getLineForOffset(label.length))
        }
        compose.onNodeWithText("שיעורי שנת המקור", substring = true).assertDoesNotExist()
        compose.onRoot().captureRoboImage(filePath = "build/review/hayom-original-large-font.png")
    }

    @Test fun originalHeadingIsReadableAboveTheAphorism() {
        val lesson = HayomYomRepository(ApplicationProvider.getApplicationContext())
            .lessonFor(LocalDate.of(2026, 10, 6))!!.asReaderLesson()
        compose.setContent {
            DailyRambamTheme {
                TanyaReaderScreen(lesson = lesson, isLoading = false, onBack = {}, preferences = UserPreferences(),
                    readerTitle = "היום יום", contentId = "hayom_yom", allowPrintLayout = false)
            }
        }
        compose.onNodeWithTag("hayom_original_studies").assertIsDisplayed()
        compose.onNodeWithTag("hayom_original_חומש").assertTextContains("נח", substring = true)
        compose.onNodeWithTag("hayom_original_תניא").assertTextContains("להבין", substring = true)
        compose.onRoot().captureRoboImage(filePath = "build/review/hayom-original-studies.png")
    }
}
