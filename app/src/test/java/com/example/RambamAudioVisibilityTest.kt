package com.example

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.local.*
import com.example.domain.schedule.DailyLessonResult
import com.example.ui.screens.ReaderScreen
import com.example.ui.theme.DailyRambamTheme
import org.junit.Assert.*
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="iw-rIL-w360dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RambamAudioVisibilityTest {
    val compose=createComposeRule()
    @get:Rule val host=withComposeHost(compose)
    @Test fun `reader uses Tanya visibility rules and hiding controls does not stop the audio`() {
        var date by mutableStateOf("2026-10-22")
        var playing by mutableStateOf(false)
        val chapter=ChapterEntity("ishut_1","ishut",1,"פרק א׳",1,"מקור","רישיון")
        val row=HalachaEntity("ishut_1_1","ishut_1","ishut",1,1,"הלכה א׳","א׳","טקסט לבדיקה","טקסט לבדיקה","")
        compose.setContent { DailyRambamTheme {
            ReaderScreen(audioPlaying=playing, audioControls={
                com.example.ui.components.DailySpeechControls(
                    if(playing) com.example.data.speech.DailySpeechState("test","רמב״ם · י״א חשוון","אישות · הלכה א׳",active=true,playing=true,canNext=true)
                    else com.example.data.speech.DailySpeechState(),"י״א חשוון","one",{playing=true},{})
            },chapter=chapter,section=null,halachot=(1..40).map { row.copy(id="ishut_1_$it", halachaNumber=it, textPlain="שיעור $it ".repeat(20), textWithNikud="שיעור $it ".repeat(20)) },savedPosition=null,isCompleted=false,
                preferences=UserPreferences(keepScreenOn=false),dailyLesson=DailyLessonResult(date,date,"י״א חשוון","one",emptyList(),"midnight",null,true,null),
                onBack={},onScrollChanged={_,_,_->},onMarkCompleted={},onNextChapter={},onPreviousChapter={},
                onJumpToToday={},onOpenDatePicker={},onFontSizeChange={},onLineSpacingChange={},onFontFamilyChange={},
                onNikudToggle={},onThemeChange={},onKeepScreenOnChange={})
        } }
        compose.onNodeWithTag("daily_speech_controls").assertDoesNotExist()
        compose.onNode(hasScrollAction()).performScrollToIndex(20)
        val before = compose.onNodeWithTag("halacha_item_20").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("rambam_audio_toggle").assertIsDisplayed().performClick()
        compose.onNodeWithTag("reader_audio_overlay").assertIsDisplayed()
        assertEquals(before, compose.onNodeWithTag("halacha_item_20").fetchSemanticsNode().boundsInRoot)
        compose.onNodeWithTag("play_tanya_audio").assertIsDisplayed().performClick()
        assertTrue(playing)
        compose.onNodeWithTag("rambam_audio_toggle_playing", useUnmergedTree=true).assertExists()
        compose.onNodeWithTag("rambam_audio_toggle").performClick()
        compose.onNodeWithTag("daily_speech_controls").assertDoesNotExist()
        assertEquals(before, compose.onNodeWithTag("halacha_item_20").fetchSemanticsNode().boundsInRoot)
        assertTrue("Hiding the panel only changes visibility",playing)
        compose.onNodeWithTag("rambam_audio_toggle_playing", useUnmergedTree=true).assertExists()
        compose.onNodeWithTag("rambam_audio_toggle").performClick()
        compose.onNodeWithTag("play_tanya_audio").assertIsDisplayed()
        val panel=compose.onNodeWithTag("daily_speech_controls").fetchSemanticsNode().boundsInRoot
        val toggle=compose.onNodeWithTag("rambam_audio_toggle").fetchSemanticsNode().boundsInRoot
        assertTrue("The player sits below the reader header",panel.top>=toggle.bottom)
        compose.onRoot().captureRoboImage("build/review/rambam-audio-placement-138.png")
        compose.runOnIdle {date="2026-10-23"}
        compose.onNodeWithTag("daily_speech_controls").assertDoesNotExist()
    }
}
