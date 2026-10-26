package com.example

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.*
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.lifecycle.Lifecycle
import com.example.data.prayers.audio.*
import com.example.data.prayers.PrayerRepository
import com.example.data.prayers.PrayerServices
import com.example.data.local.UserPreferences
import com.example.ui.screens.PrayerReaderScreen
import com.example.ui.theme.DailyRambamTheme
import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PrayerAudioPlaybackTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun readerRemainsSilentUntilTheUserChoosesAnAudioSection() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val date = LocalDate.of(2026, 10, 5)
        val service = PrayerServices.group(PrayerRepository(context).load(date).sections).single { it.id == "shacharit" }
        rule.setContent { DailyRambamTheme {
            PrayerReaderScreen(service, UserPreferences(keepScreenOn = false), {}, null, null,
                {}, {}, {}, {}, {}, {}, date)
        } }
        rule.onNodeWithTag("prayer_audio_start").assertExists()
        rule.onNodeWithTag("prayer_audio_toggle").assertDoesNotExist()
        rule.onNodeWithTag("prayer_audio_sections").performClick()
        // 24 Tishrei: no Tachanun, so Lamnatzeach is not part of the day's sequence.
        rule.onNodeWithTag("prayer_audio_day").assertTextContains("ללא תחנון", substring = true)
        rule.onAllNodesWithTag("prayer_audio_play_closing_lamnatzeach").assertCountEquals(0)
        rule.onNodeWithTag("prayer_audio_picker").performScrollToNode(hasTestTag("prayer_audio_play_closing_ashrei"))
        rule.onNodeWithTag("prayer_audio_play_closing_ashrei").performClick()
        rule.waitUntil(20000) {
            rule.onAllNodesWithContentDescription("השהיית האודיו").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("אודיו ניסיוני · כיסוי חלקי").assertExists()
        val image = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        if (image != null) {
            File(context.getExternalFilesDir(null), "prayer-player-playing.png").outputStream().use {
                image.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            image.recycle()
        }
        rule.onNodeWithTag("prayer_audio_close").performClick()
        rule.onNodeWithTag("prayer_audio_start").assertExists()
        rule.onNodeWithTag("prayer_audio_seek").assertDoesNotExist()
    }

    @Test fun optionalAudioPlaysPausesResumesAdvancesAndStopsAtMissingSection() {
        lateinit var player: PrayerAudioPlayer
        fun step(id: String, filename: String?) = PrayerAudioStep(id, id, "closing", clip = filename?.let {
            PrayerAudioClip(filename, "asset:///prayer-audio/$filename.m4a", approved = false)
        })
        val steps = listOf(step("ashrei", "closing-ashrei-v2"), step("lamnatzeach", "closing-lamnatzeach-v2"),
            step("missing", null), step("after_gap", "closing-uvaletzion-v2"))
        try {
            rule.runOnIdle {
                player = PrayerAudioPlayer(ApplicationProvider.getApplicationContext(), "shacharit-test", LocalDate.of(2026, 10, 5), true)
                assertFalse(player.state.value.active)
                assertFalse(player.state.value.playing)
                player.start(steps, "ashrei")
            }
            rule.waitUntil(20000) { player.state.value.playing && player.state.value.durationMs > 0 }
            rule.runOnIdle { player.seek(12000); player.toggle() }
            rule.waitUntil(5000) { !player.state.value.playing }
            rule.runOnIdle { assertTrue(player.state.value.positionMs >= 11000); player.toggle() }
            rule.waitUntil(5000) { player.state.value.playing }
            // Closing the audio keeps its place; the next start resumes the same section and position.
            rule.runOnIdle { player.stop(); assertFalse(player.state.value.active); player.start(steps, "lamnatzeach", resume = true) }
            rule.waitUntil(20000) { player.state.value.playing && player.state.value.stepId == "ashrei" }
            rule.runOnIdle { assertTrue(player.state.value.positionMs >= 11000) }
            val beforeBackground = player.state.value.positionMs
            rule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
            rule.waitUntil(5000) { player.state.value.positionMs > beforeBackground + 500 }
            assertTrue(player.state.value.playing)
            rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            rule.runOnIdle { player.seek(player.state.value.durationMs - 500) }
            rule.waitUntil(10000) { player.state.value.stepId == "lamnatzeach" && player.state.value.playing }
            rule.runOnIdle { player.seek(player.state.value.durationMs - 500) }
            rule.waitUntil(10000) { player.state.value.error?.contains("missing") == true }
            rule.runOnIdle {
                assertFalse(player.state.value.playing)
                assertEquals("lamnatzeach", player.state.value.stepId)
                assertFalse(player.state.value.finished)
                assertEquals("after_gap", player.state.value.nextAvailableId)
                player.clearError()
            }
            // The stop message is not raised again by the position ticker after it was dismissed.
            Thread.sleep(1500)
            rule.runOnIdle {
                assertNull(player.state.value.error)
                assertFalse(player.state.value.finished)
                player.stop()
                assertFalse(player.state.value.active)
            }
        } finally {
            rule.runOnIdle { runCatching { player.close() } }
        }
    }
}
