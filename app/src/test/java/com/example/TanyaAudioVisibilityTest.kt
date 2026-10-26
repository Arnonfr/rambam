package com.example

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.UserPreferences
import com.example.domain.tanya.DailyTanyaLesson
import com.example.domain.tanya.TanyaSection
import com.example.ui.screens.TanyaReaderScreen
import com.example.ui.theme.DailyRambamTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.junit.Assert.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "iw-rIL-w360dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TanyaAudioVisibilityTest {
    val compose = createComposeRule()
    @get:Rule val hostAndCompose = withComposeHost(compose)

    @Test fun headphonesShowAndHideAudioWithoutRemovingTheStudyText() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Robolectric does not host the native MediaSession service. Model it as unavailable;
        // this check exercises reader visibility independently of service/network playback.
        org.robolectric.Shadows.shadowOf(context as android.app.Application).declareComponentUnbindable(
            android.content.ComponentName(context, com.example.data.prayers.audio.PrayerPlaybackService::class.java))
        // Use the external-source option so this UI check does not depend on a live network.
        context.getSharedPreferences("tanya_audio_preferences", Context.MODE_PRIVATE).edit().putString("teacher", "wilhelm").commit()
        val lesson = DailyTanyaLesson("2026-10-15", "ד חשוון", "Tanya, Part IV; Iggeret HaKodesh 26:1-10",
            "", "תניא", "אגרת הקדש", listOf(TanyaSection(1, "א", "טקסט הלימוד", "טקסט הלימוד")))
        compose.setContent { DailyRambamTheme {
            TanyaReaderScreen(lesson = lesson, isLoading = false, onBack = {},
                preferences = UserPreferences(keepScreenOn = false, tanyaBookView = false), audioStudyDate = lesson.date)
        } }
        compose.onNodeWithTag("tanya_audio_controls").assertDoesNotExist()
        compose.onNodeWithContentDescription("הצגת השיעור המוקלט").performClick()
        compose.onNodeWithTag("tanya_audio_controls").assertExists()
        compose.onNodeWithText("הרב שניאור אשכנזי").assertExists()
        compose.onNodeWithContentDescription("הסתרת השיעור המוקלט").performClick()
        compose.onNodeWithTag("tanya_audio_controls").assertDoesNotExist()
        compose.onNodeWithTag("tanya_section_1").assertExists()
    }

    @Test fun missingDailyChapterOffersScrollableLessonsAndVideoRemainsExternal() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        org.robolectric.Shadows.shadowOf(context as android.app.Application).declareComponentUnbindable(
            android.content.ComponentName(context, com.example.data.prayers.audio.PrayerPlaybackService::class.java))
        context.getSharedPreferences("tanya_audio_preferences", Context.MODE_PRIVATE).edit().putString("teacher", "teib").commit()
        val lesson = DailyTanyaLesson("2026-10-16", "ה חשוון", "Tanya, Part IV; Iggeret HaKodesh 26:1-10",
            "", "תניא", "אגרת הקדש", listOf(TanyaSection(1, "א", "טקסט הלימוד", "טקסט הלימוד")))
        compose.setContent { DailyRambamTheme {
            TanyaReaderScreen(lesson = lesson, isLoading = false, onBack = {},
                preferences = UserPreferences(keepScreenOn = false, tanyaBookView = false), audioStudyDate = lesson.date)
        } }
        compose.onNodeWithContentDescription("הצגת השיעור המוקלט").performClick()
        compose.onNodeWithText("עדיין אין הקלטה לפרק הזה").assertExists()
        compose.onNodeWithText("השיעורים הזמינים").performClick()
        compose.onNodeWithTag("tanya_available_lessons").assertExists()
        val targetIndex = com.example.data.tanya.TanyaAvailableLessons.get(context).cached("teib")
            .sortedBy { if (it.kind == "audio") 0 else 1 }.indexOfFirst { it.id == "youtube:hHwKffBOVeg" }
        assertTrue(targetIndex >= 0)
        compose.onNodeWithTag("tanya_available_lessons").performScrollToIndex(targetIndex)
        compose.onNodeWithTag("available_youtube:hHwKffBOVeg").performClick()
        compose.onNodeWithTag("tanya_youtube_player").assertDoesNotExist()
        compose.onNodeWithText("סגירת רשימת השיעורים").performClick()
        compose.onNodeWithText("פתיחת נגן הווידאו").assertDoesNotExist()
        compose.onNodeWithText("פתיחת השיעור באתר המקור").assertExists()
        compose.onNodeWithText("הצפייה בשיעור נפתחת באתר המקור, מחוץ לאפליקציה.").assertExists()
        compose.onNodeWithContentDescription("הסתרת השיעור המוקלט").performClick()
        compose.onNodeWithContentDescription("הצגת השיעור המוקלט").performClick()
        compose.onNodeWithText("פתיחת השיעור באתר המקור").assertExists()
        compose.onNodeWithContentDescription("הסתרת השיעור המוקלט").performClick()
        compose.onNodeWithTag("tanya_audio_controls").assertDoesNotExist()
    }
}
