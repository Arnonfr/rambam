package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.ui.components.*
import com.example.ui.theme.DailyRambamTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.*
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.Velocity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "iw-rIL-w360dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReaderFeedbackTest {
    val compose = createComposeRule()
    @get:Rule val host = withComposeHost(compose)

    @Test fun playingRemainsVisibleWithHiddenControlsAndStopsOnPause() {
        var shown by mutableStateOf(true)
        var playing by mutableStateOf(true)
        compose.setContent { DailyRambamTheme {
            ReaderAudioButton(shown, playing, accent = Color(0xFFFFA477), readerTheme = "light",
                tag = "audio", showLabel = "הצגת ההקראה", hideLabel = "הסתרת ההקראה", onClick = { shown = !shown })
        } }
        compose.onNodeWithTag("audio").performClick()
        assertFalse(shown)
        assertTrue(playing)
        compose.onNodeWithTag("audio_playing", useUnmergedTree = true).assertExists()
        compose.runOnIdle { playing = false }
        compose.onNodeWithTag("audio_playing", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test fun ringTracksFingerAndReversesWithoutCompleting() {
        var completed = 0
        val state = EndOfLessonPullState(56f, { false }, { completed++ }, {})
        compose.setContent { DailyRambamTheme { Surface { EndOfLessonPullIndicator(state, Color.Yellow) } } }
        compose.runOnIdle { state.connection.onPostScroll(Offset.Zero, Offset(0f, -28f), NestedScrollSource.UserInput) }
        compose.onNodeWithContentDescription("משכו לסיום")
            .assertRangeInfoEquals(androidx.compose.ui.semantics.ProgressBarRangeInfo(.375f, 0f..1f))
        compose.runOnIdle { state.connection.onPreScroll(Offset(0f, 10.5f), NestedScrollSource.UserInput) }
        compose.onNodeWithContentDescription("משכו לסיום")
            .assertRangeInfoEquals(androidx.compose.ui.semantics.ProgressBarRangeInfo(.1875f, 0f..1f))
        compose.onNodeWithTag("lesson_completion_feedback").captureRoboImage("build/review/completion-pull-partial.png")
        assertEquals(0, completed)
    }

    @Test fun lightFeedback() = feedback("light")
    @Test fun darkFeedback() = feedback("oled")

    @Test fun circularConfirmationIsVisibleAfterRelease() {
        val state=EndOfLessonPullState(56f,{false},{},{})
        state.connection.onPostScroll(Offset.Zero,Offset(0f,-80f),NestedScrollSource.UserInput)
        lateinit var scope:kotlinx.coroutines.CoroutineScope
        compose.setContent { DailyRambamTheme {
            scope=rememberCoroutineScope()
            Surface { EndOfLessonPullIndicator(state,Color.Yellow) }
        } }
        compose.onNodeWithContentDescription("שחררו לסיום").assertIsDisplayed()
        compose.mainClock.autoAdvance=false
        compose.runOnIdle { scope.launch { state.connection.onPreFling(Velocity.Zero) } }
        compose.mainClock.advanceTimeBy(160)
        compose.onNodeWithContentDescription("השיעור הושלם").assertIsDisplayed()
        compose.onNodeWithTag("lesson_completion_feedback").captureRoboImage("build/review/completion-pull-confirmed.png")
    }

    private fun feedback(theme: String) {
        val state = EndOfLessonPullState(56f, { false }, {}, {})
        state.connection.onPostScroll(Offset.Zero, Offset(0f, -80f), NestedScrollSource.UserInput)
        compose.setContent { DailyRambamTheme(readerTheme = theme) {
            Surface(color = MaterialTheme.colorScheme.background) {
                Column(Modifier.width(360.dp).testTag("feedback_preview")) {
                    CompactReaderHeader("תניא", "אגרת הקדש כ״ז", Color(0xFFEA78D5), 1f, theme,
                        trailingAction = { ReaderAudioButton(false, true, accent = Color(0xFFEA78D5),
                            readerTheme = theme, tag = "audio", showLabel = "הצגת השיעור", hideLabel = "הסתרת השיעור", onClick = {}) })
                    EndOfLessonPullIndicator(state, Color(0xFFEA78D5))
                }
            }
        } }
        compose.onNodeWithContentDescription("שחררו לסיום").assertIsDisplayed()
        val glow = compose.onNodeWithTag("lesson_completion_feedback").fetchSemanticsNode().boundsInRoot
        assertTrue("Feedback should span the reading area", glow.width > glow.height)
        compose.onNodeWithTag("feedback_preview").captureRoboImage("build/review/reader-feedback-$theme.png")
    }
}
