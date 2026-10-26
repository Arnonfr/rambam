package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.data.local.UserPreferences
import com.example.data.prayers.audio.StudyPlaybackStatus
import com.example.ui.DailyRambamUiState
import com.example.ui.screens.MainContentList
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="iw-rIL-w360dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReaderPolishTest {
    val compose = createComposeRule()
    @get:Rule val host = withComposeHost(compose)

    @Test fun audioOverlayWrapsItsContent() {
        compose.setContent { DailyRambamTheme {
            Box(Modifier.fillMaxSize()) { ReaderAudioOverlay({}) { Text("נגן קצר") } }
        } }
        val bounds = compose.onNodeWithTag("reader_audio_overlay").fetchSemanticsNode().boundsInRoot
        assertTrue("Short audio controls must not create a tall empty panel", bounds.height / compose.density.density < 150f)
    }

    @Test fun completionKeepsSquareGeometryFromPartialArcToFullCircle() {
        val state = EndOfLessonPullState(56f, {false}, {}, {})
        state.connection.onPostScroll(Offset.Zero, Offset(0f,-8f), NestedScrollSource.UserInput)
        compose.setContent { DailyRambamTheme { Surface { EndOfLessonPullIndicator(state, Color(0xFFFF6422)) } } }
        val before = compose.onNodeWithContentDescription("משכו לסיום").fetchSemanticsNode().boundsInRoot
        assertEquals(before.width, before.height, .01f)
        compose.onRoot().captureRoboImage("build/review/completion-clean-partial.png")
        compose.runOnIdle { state.connection.onPostScroll(Offset.Zero, Offset(0f,-80f), NestedScrollSource.UserInput) }
        val after = compose.onNodeWithContentDescription("שחררו לסיום").fetchSemanticsNode().boundsInRoot
        assertEquals(before, after)
        compose.onRoot().captureRoboImage("build/review/completion-clean-full.png")
    }

    @Test fun homeWaveformFollowsTheActualPlayingTrackAndStopsOnPause() {
        StudyPlaybackStatus.report("tanya", true)
        try {
            compose.setContent { DailyRambamTheme {
                MainContentList(uiState=DailyRambamUiState(preferences=UserPreferences(visibleStudies=setOf("tanya","rambam_one"))),
                    onOpenChapter={}, onOpenDailyLesson={}, onResumeReading={}, onOpenChumash={},
                    onOpenTehillim={}, onOpenTanya={}, onOpenMitzvah={}, onOpenSettings={})
            } }
            compose.onNodeWithTag("study_audio_tanya", useUnmergedTree=true).assertExists()
            compose.onNodeWithTag("study_audio_one", useUnmergedTree=true).assertDoesNotExist()
            compose.onRoot().captureRoboImage("build/review/home-playing-wave.png")
            compose.runOnIdle { StudyPlaybackStatus.report("tanya",false) }
            compose.onNodeWithTag("study_audio_tanya", useUnmergedTree=true).assertDoesNotExist()
            compose.runOnIdle { StudyPlaybackStatus.report("rambam_one",true) }
            compose.onNodeWithTag("study_audio_one", useUnmergedTree=true).assertExists()
            compose.onNodeWithTag("study_audio_tanya", useUnmergedTree=true).assertDoesNotExist()
        } finally { StudyPlaybackStatus.report(null,false) }
    }

    @Test fun fontNamesAreHebrewAndQuotesRenderInTheSelectedFace() {
        assertEquals(listOf("נוטו","וילנא","ליברטינוס","בונה נובה"), FontStyleOption.entries.map {it.title})
        compose.setContent { DailyRambamTheme {
            Column(Modifier.width(340.dp)) {
                FontStyleOption.entries.forEach { font ->
                    Text(font.title)
                    StudyTextBlock("כ״ד", "רמב״ם · י״ח יום · כ\"ג · \"ציטוט\" · ה׳", "", UserPreferences(fontFamily=font.id, fontSizeSp=30f))
                }
            }
        } }
        compose.onRoot().captureRoboImage("build/review/native-quotes-all-fonts.png")
    }
}
