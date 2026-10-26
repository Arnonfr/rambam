package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.data.speech.*
import com.example.ui.components.DailySpeechControls
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
@Config(sdk=[35], qualifiers="iw-rIL-w360dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DailySpeechControlsTest {
    val compose=createComposeRule()
    @get:Rule val host=withComposeHost(compose)
    @Test fun `one action starts the entire daily assignment and transport remains accessible at 320dp`() {
        var state by mutableStateOf(DailySpeechState())
        var started=false
        val actions=mutableListOf<String>()
        compose.setContent { DailyRambamTheme {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Box(Modifier.width(320.dp)) {
                    DailySpeechControls(state,"י״א חשוון","three",{started=true},{actions+=it})
                }
            }
        } }
        compose.onNodeWithTag("play_tanya_audio").assertIsDisplayed().performClick()
        assertTrue(started)
        compose.onNodeWithText("י״א חשוון · שלושה פרקים ברצף").assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/review/daily-speech-start-139.png")
        compose.runOnIdle {state=DailySpeechState("daily","רמב״ם · י״א חשוון","הלכות אישות, פרק 3",playing=true,active=true,canPrevious=true,canNext=true,position=45000,duration=300000)}
        compose.onNodeWithTag("tanya_playback").assertIsDisplayed()
        compose.onNodeWithTag("tanya_audio_timeline").assertIsDisplayed()
        assertTrue(compose.onNodeWithTag("daily_speech_previous").fetchSemanticsNode().boundsInRoot.center.x <
            compose.onNodeWithTag("daily_speech_next").fetchSemanticsNode().boundsInRoot.center.x)
        compose.onNodeWithTag("play_tanya_audio").performClick()
        compose.onNodeWithTag("daily_speech_previous").performClick()
        compose.onNodeWithTag("daily_speech_next").performClick()
        compose.onNodeWithTag("tanya_audio_speed").performClick()
        assertEquals(listOf(DailySpeechStatus.TOGGLE,DailySpeechStatus.PREVIOUS,DailySpeechStatus.NEXT,DailySpeechStatus.SPEED),actions)
        compose.onRoot().captureRoboImage("build/review/daily-speech-playing-139.png")
        compose.runOnIdle {state=state.copy(playing=false,paused=true)}
        compose.onRoot().captureRoboImage("build/review/daily-speech-paused-rtl-139.png")
        compose.runOnIdle {state=state.copy(error="לא נמצא קול עברי שהורד לטלפון.")}
        compose.onNodeWithTag("daily_speech_error").assertIsDisplayed()
        compose.onNodeWithText("הגדרות הקראה בטלפון").assertDoesNotExist()
    }
    @Test fun `recording source is selectable and never offers fictitious halacha boundaries`() {
        var source by mutableStateOf("recording")
        var starts=0
        compose.setContent { DailyRambamTheme(readerTheme="oled") {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Box(Modifier.width(320.dp)) {
                    DailySpeechControls(DailySpeechState(active=true,playing=true,recorded=true,
                        voiceStatus="ר׳ אליהו פרץ · חב״ד FM",chapter="מכירה א–ג",duration=300000),"י״ב חשוון","three",
                        {starts++},{},source=source,onSourceChange={source=it})
                }
            }
        } }
        compose.onNodeWithTag("rambam_source_recording").assertIsSelected()
        compose.onNodeWithTag("daily_speech_next").assertDoesNotExist()
        compose.onNodeWithTag("tanya_audio_timeline").assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/review/rambam-recording-dark-142.png")
        compose.onNodeWithTag("rambam_source_michael").performClick()
        assertEquals("michael",source);assertEquals(0,starts)
    }

}
