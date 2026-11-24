package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.*
import com.example.ui.PlaybackBarState
import com.example.ui.components.GlobalPlaybackBar
import com.example.ui.components.FloatingPlaybackOverlay
import com.example.ui.components.FloatingReaderBar
import com.example.ui.HomeTab
import com.example.ui.screens.FloatingDarkNavBar
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

    @Test fun `mini player stays above home tabs and reader navigation without moving them`() {
        var state by mutableStateOf(PlaybackBarState())
        var home by mutableStateOf(true)
        compose.setContent {
            DailyRambamTheme {
                Box(Modifier.fillMaxSize().navigationBarsPadding()) {
                    if (home) {
                        FloatingDarkNavBar(HomeTab.STUDY, {},
                            Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp))
                    } else {
                        FloatingReaderBar("", "י״ג בכסלו", 0f, {}, {}, {}, "light", {}, {},
                            modifier = Modifier.align(Alignment.BottomCenter), studyDate = "2026-11-23")
                    }
                    FloatingPlaybackOverlay(state, { state = state.copy(playing = false) }, { state = PlaybackBarState() })
                }
            }
        }
        for (navigationTag in listOf("home_bottom_menu", "reader_bottom_date_picker_button")) {
            val navigation = compose.onNodeWithTag(navigationTag)
            val original = navigation.getUnclippedBoundsInRoot()
            compose.runOnIdle { state = PlaybackBarState(true, "תניא יומי", "הרב נדב כהן", playing = true) }
            val player = compose.onNodeWithTag("global_playback_bar")
            assertEquals(original, navigation.getUnclippedBoundsInRoot())
            assertTrue(player.getUnclippedBoundsInRoot().bottom < original.top)
            compose.onNodeWithTag("global_playback_toggle").performClick()
            assertEquals(original, navigation.getUnclippedBoundsInRoot())
            compose.onNodeWithTag("global_playback_stop").performClick()
            player.assertDoesNotExist()
            assertEquals(original, navigation.getUnclippedBoundsInRoot())
            compose.runOnIdle { home = false }
        }
    }
}
