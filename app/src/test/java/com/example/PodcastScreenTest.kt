package com.example

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.data.podcasts.*
import com.example.ui.HomeTab
import com.example.ui.screens.*
import com.example.ui.theme.DailyRambamTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w412dp-h892dp-mdpi")
class PodcastScreenTest {
    @get:Rule val rule = createComposeRule()

    @Test fun singleSearchFiltersSeriesAndOpensTheSelectedSeries() {
        var opened: PodcastChannel? = null
        rule.setContent { DailyRambamTheme {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                PodcastLibrary(onOpen = { opened = it }, cover = { _, m -> Box(m) })
            }
        } }
        rule.onAllNodesWithTag("podcast_search").assertCountEquals(1)
        rule.onNodeWithTag("podcast_search").performTextInput("נדב")
        rule.onNodeWithTag("podcast_series_nadav").assertExists().performClick()
        rule.runOnIdle { assertEquals("nadav", opened?.id) }
        rule.onNodeWithTag("podcast_series_ashkenazi").assertDoesNotExist()
        rule.onNodeWithTag("podcast_search").performTextClearance()
        rule.onNodeWithTag("podcast_search").performTextInput("איןסדרהכזו")
        rule.onNodeWithText("לא נמצאו סדרות מתאימות").assertExists()
    }

    @Test fun detailExposesEpisodePlaybackBackAndRetry() {
        var played = false; var back = false; var retry = false
        val episode = PodcastEpisode("מה עושים בגן עדן?", "https://example.org/episode", "https://example.org/audio.mp3", "")
        rule.setContent { DailyRambamTheme {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                PodcastSeriesDetail(PodcastCatalog.channels.first(), listOf(episode), false, "לא ניתן לטעון",
                    { back = true }, { retry = true }, {}, { played = true }, cover = { _, m -> Box(m) })
            }
        } }
        rule.onNodeWithContentDescription("הפעלת מה עושים בגן עדן?").performClick()
        rule.onNodeWithText("נסה שוב").performClick()
        rule.onNodeWithTag("podcast_back").performClick()
        rule.runOnIdle { assertTrue(played && back && retry) }
    }

    @Test fun exportActualScreensWithPublisherArtworkAndRealFeedEpisodes() {
        val destination = System.getenv("PODCAST_PREVIEW_DIR") ?: return
        val research = File("/Users/hyh/Documents/Codex/לימוד-יומי/מחקר")
        val covers = File("/Users/hyh/Documents/Codex/לימוד-יומי/עיצוב/podcast-official-covers")
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cache = File(context.cacheDir, "podcast-covers").apply { mkdirs() }
        PodcastCatalog.channels.forEach { channel ->
            val source = File(covers, channel.id + if (channel.id == "ashkenazi") ".png" else ".jpg")
            if (source.exists()) source.copyTo(File(cache, "${channel.id}.image"), overwrite = true)
            val xml = File(research, "podcast-${channel.id}-feed.xml").readText()
            assertTrue("${channel.id} should expose real audio", PodcastRepository.parse(xml).any { it.audio != null })
        }
        val detail = mutableStateOf(false)
        val episodes = PodcastRepository.parse(File(research, "podcast-ashkenazi-feed.xml").readText()).take(3)
        System.setProperty("roborazzi.test.record", "true")
        rule.setContent { DailyRambamTheme {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                key(detail.value) {
                Box(Modifier.fillMaxSize().background(Color(0xFFFFFCF6)).testTag("podcast_preview")) {
                    if (detail.value) PodcastSeriesDetail(PodcastCatalog.channels.first(), episodes, false, null,
                        {}, {}, {}, {}, modifier = Modifier.fillMaxSize().padding(bottom = 100.dp))
                    else PodcastLibrary({}, Modifier.fillMaxSize().padding(bottom = 100.dp), onSettings = {})
                    FloatingDarkNavBar(HomeTab.PODCASTS, {}, Modifier.align(androidx.compose.ui.Alignment.BottomCenter).padding(bottom = 24.dp))
                }
                }
            }
        } }
        rule.waitUntil(15_000) { rule.onAllNodesWithContentDescription("עטיפת לומדים תניא").fetchSemanticsNodes().isNotEmpty() }
        File(destination).mkdirs()
        rule.onNodeWithTag("podcast_preview").captureRoboImage(filePath = File(destination, "library.png").path)
        rule.runOnIdle { detail.value = true }
        rule.waitUntil(15_000) { rule.onAllNodesWithContentDescription("עטיפת לומדים תניא").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("פודקאסטים").assertIsDisplayed()
        rule.onNodeWithTag("podcast_preview").captureRoboImage(filePath = File(destination, "series.png").path)
        System.clearProperty("roborazzi.test.record")
    }
}
