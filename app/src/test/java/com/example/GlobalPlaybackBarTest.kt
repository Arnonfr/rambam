package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.*
import com.example.ui.PlaybackBarState
import com.example.ui.components.GlobalPlaybackBar
import com.example.ui.theme.DailyRambamTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35], qualifiers="iw-rIL-w360dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GlobalPlaybackBarTest {
    val compose=createComposeRule()
    @get:Rule val host=withComposeHost(compose)
    @Test fun `long Hebrew titles retain accessible pause resume and stop controls at narrow width`() {
        var state by mutableStateOf(PlaybackBarState(true,"שיעור תניא ארוך מאוד עם הרב נדב כהן","הרב נדב כהן",playing=true))
        var stops=0
        compose.setContent { DailyRambamTheme {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Box(Modifier.width(320.dp)) {
                    GlobalPlaybackBar(state, { state=state.copy(playing=!state.playing) }, { stops++;state=PlaybackBarState() })
                }
            }
        } }
        compose.onNodeWithContentDescription("השהיה").assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("המשך ניגון").assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/review/global-playback-151.png")
        compose.onNodeWithContentDescription("המשך ניגון").performClick()
        compose.onNodeWithContentDescription("השהיה").assertIsDisplayed()
        compose.onNodeWithContentDescription("עצירת השיעור").assertIsDisplayed().performClick()
        assertEquals(1,stops)
        compose.onNodeWithTag("global_playback_bar").assertDoesNotExist()
    }
}
