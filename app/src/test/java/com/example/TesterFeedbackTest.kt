package com.example

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import com.example.ui.components.FeedbackScreen
import com.example.ui.theme.DailyRambamTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "iw-rIL-w360dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TesterFeedbackTest {
    private val compose = createComposeRule()
    @get:Rule val host = withComposeHost(compose)

    @Test fun feedbackNeedsContentAndOpensEditableEmailWithEncodedHebrewAndDeviceDetails() {
        compose.setContent { DailyRambamTheme {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { FeedbackScreen {} }
        } }
        compose.onNodeWithTag("feedback_email").assertIsNotEnabled()
        compose.onNodeWithTag("feedback_description").performTextInput("השמע נעצר & לא ממשיך? #בדיקה")
        compose.onNodeWithTag("feedback_email").performClick()
        val app = ApplicationProvider.getApplicationContext<Context>()
        val intent = shadowOf(app as android.app.Application).nextStartedActivity
        assertEquals(Intent.ACTION_SENDTO, intent.action)
        assertEquals("mailto", intent.data!!.scheme)
        val decoded = android.net.Uri.decode(intent.dataString)
        assertTrue(decoded.contains("frielabsdev@gmail.com"))
        assertTrue(decoded.contains("השמע נעצר & לא ממשיך? #בדיקה"))
        assertTrue(decoded.contains("(${BuildConfig.VERSION_CODE})"))
        assertTrue(decoded.contains("Android:"))
        compose.onNodeWithTag("feedback_description").assertTextContains("השמע נעצר", substring = true)
    }

    @Test fun rtlFeedbackFitsSmallScreenAndKeepsComposerActionVisible() {
        compose.setContent { DailyRambamTheme {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { FeedbackScreen {} }
        } }
        compose.onNodeWithTag("feedback_email").assertIsDisplayed()
        compose.onNodeWithTag("feedback_description").assertIsDisplayed()
        compose.onNodeWithTag("feedback_dialog").captureRoboImage("build/review/tester-feedback.png")
    }
}
