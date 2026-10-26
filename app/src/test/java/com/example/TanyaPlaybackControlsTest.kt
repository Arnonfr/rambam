package com.example

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.tanya.TanyaPlaybackState
import com.example.ui.components.TanyaPlaybackControls
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
@Config(sdk = [35], qualifiers = "iw-rIL-w360dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TanyaPlaybackControlsTest {
    val compose = createComposeRule()
    @get:Rule val hostAndCompose = withComposeHost(compose)

    @Test fun playerKeepsTheUniversalMediaDirectionInHebrew() {
        val state = mutableStateOf(TanyaPlaybackState())
        var played = 0
        val seeks = mutableListOf<Long>()
        compose.setContent { DailyRambamTheme {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                TanyaPlaybackControls(state.value, { played++ }, { seeks += it }, {}, Color.Black, Color.White, Color(0xFFF0A0DD))
            }
        } }
        compose.onNodeWithText("המשך הקריאה זמין בזמן ההאזנה").assertDoesNotExist()
        compose.onNodeWithContentDescription("אחורה 10 שניות").assertIsNotEnabled()
        val initial = compose.onNodeWithTag("play_tanya_audio").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("play_tanya_audio").performClick()
        assertEquals(1, played)
        compose.runOnIdle { state.value = TanyaPlaybackState(active = true, playing = true, position = 44_000, duration = 656_000) }
        val playing = compose.onNodeWithTag("play_tanya_audio").fetchSemanticsNode().boundsInRoot
        assertEquals(initial, playing)
        compose.onNodeWithTag("tanya_audio_elapsed").assertTextEquals("0:44")
        compose.onNodeWithTag("tanya_audio_duration").assertTextEquals("10:56")
        assertTrue(compose.onNodeWithTag("tanya_audio_elapsed").fetchSemanticsNode().boundsInRoot.center.x <
            compose.onNodeWithTag("tanya_audio_duration").fetchSemanticsNode().boundsInRoot.center.x)
        assertTrue(compose.onNodeWithContentDescription("אחורה 10 שניות").fetchSemanticsNode().boundsInRoot.center.x < playing.center.x)
        assertTrue(compose.onNodeWithContentDescription("קדימה 10 שניות").fetchSemanticsNode().boundsInRoot.center.x > playing.center.x)
        compose.onNodeWithContentDescription("אחורה 10 שניות").performClick()
        compose.onNodeWithContentDescription("קדימה 10 שניות").performClick()
        assertEquals(listOf(34_000L, 54_000L), seeks)
        seeks.clear()
        compose.onNodeWithTag("tanya_audio_timeline").performTouchInput {
            swipe(start = androidx.compose.ui.geometry.Offset(width * .9f, center.y),
                end = androidx.compose.ui.geometry.Offset(width * .25f, center.y), durationMillis = 300)
        }
        compose.runOnIdle {
            assertEquals(1, seeks.size)
            assertTrue(seeks.single() in 120_000L..210_000L)
        }
        compose.onNodeWithTag("tanya_playback").captureRoboImage("/tmp/daily-learning-check/tanya-player-refined.png")
    }
}
