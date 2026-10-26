package com.example

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.prayers.audio.PrayerAudioState
import com.example.ui.components.PrayerAudioControls
import com.example.ui.theme.DailyRambamTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PrayerAudioControlsTest {
    @get:Rule val rule = createComposeRule()
    @Test fun `reading is the default and only an explicit tap starts audio`() {
        var starts = 0
        rule.setContent { DailyRambamTheme {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                PrayerAudioControls(PrayerAudioState(), "light", { starts++ }, {}, {}, {}, {}, {})
            }
        } }
        rule.runOnIdle { assertEquals(0, starts) }
        rule.onNodeWithTag("prayer_audio_seek").assertDoesNotExist()
        rule.onNodeWithTag("prayer_audio_start").performClick()
        rule.runOnIdle { assertEquals(1, starts) }
    }
    @Test fun `active player exposes pause stop seek and explicit partial recording status`() {
        var toggled = false; var closed = false; var sections = false
        rule.setContent { DailyRambamTheme {
            PrayerAudioControls(PrayerAudioState(active = true, playing = true, title = "אשרי", draft = true,
                durationMs = 65000, positionMs = 1000), "light", {}, { toggled = true },
                { closed = true }, {}, {}, { sections = true })
        } }
        rule.onNodeWithText("אודיו ניסיוני · כיסוי חלקי").assertExists()
        rule.onNodeWithContentDescription("השהיית האודיו").performClick()
        rule.onNodeWithText("אשרי").performClick()
        rule.onNodeWithTag("prayer_audio_close").performClick()
        rule.onNodeWithTag("prayer_audio_seek").assertExists()
        rule.runOnIdle { assertTrue(toggled && closed && sections) }
    }
}
