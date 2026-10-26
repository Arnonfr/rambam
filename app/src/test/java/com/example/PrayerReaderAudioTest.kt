package com.example

import android.app.Application
import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.UserPreferences
import com.example.data.prayers.*
import com.example.ui.screens.PrayerReaderScreen
import com.example.ui.theme.DailyRambamTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w412dp-h892dp-mdpi")
class PrayerReaderAudioTest {
    @get:Rule val rule = createComposeRule()
    @Test fun readerOpensWithoutStartingAudioAndOffersExplicitSectionSelection() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val date = LocalDate.of(2026, 10, 5)
        val service = PrayerServices.group(PrayerRepository(context).load(date).sections).single { it.id == "shacharit" }
        rule.setContent { DailyRambamTheme {
            PrayerReaderScreen(service, UserPreferences(keepScreenOn = false), {}, null, null,
                {}, {}, {}, {}, {}, {}, date)
        } }
        rule.onNodeWithTag("prayer_audio_start").assertExists()
        rule.onNodeWithTag("prayer_audio_toggle").assertDoesNotExist()
        rule.runOnIdle {
            assertNull(Shadows.shadowOf(context as Application).nextStartedService)
        }
        val output = System.getenv("PRAYER_PREVIEW_DIR")
        if (output != null) {
            File(output).mkdirs()
            System.setProperty("roborazzi.test.record", "true")
            rule.onRoot().captureRoboImage(filePath = File(output, "prayer-reading.png").path)
        }
        rule.onNodeWithTag("prayer_audio_sections").performClick()
        rule.onNodeWithText("שחרית · מקטעי האודיו").assertExists()
        if (output != null) {
            rule.onRoot().captureRoboImage(filePath = File(output, "prayer-audio-sections.png").path)
            System.clearProperty("roborazzi.test.record")
        }
    }
}
