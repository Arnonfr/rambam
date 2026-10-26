package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.ui.components.TanyaAudioEmptyState
import com.example.ui.theme.DailyRambamTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "iw-rIL-w360dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TanyaAudioEmptyStateTest {
    val compose = createComposeRule()
    @get:Rule val hostAndCompose = withComposeHost(compose)

    @Test fun compactStateHasSeparateSecondaryActionsInRtlAndBothActionsWork() {
        var retried = 0
        var source = 0
        compose.setContent { DailyRambamTheme {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Surface(color = Color.White) {
                    Box(Modifier.fillMaxWidth().padding(12.dp)) {
                        TanyaAudioEmptyState(Color(0xFF171714), Color.White, Color(0xFFF0A0DD),
                            { retried++ }, { source++ })
                    }
                }
            }
        } }
        val root = compose.onNodeWithTag("tanya_audio_empty").fetchSemanticsNode().boundsInRoot
        val primary = compose.onNodeWithTag("tanya_audio_primary").fetchSemanticsNode().boundsInRoot
        val secondary = compose.onNodeWithTag("tanya_audio_source").fetchSemanticsNode().boundsInRoot
        val group = compose.onNodeWithTag("tanya_audio_actions").fetchSemanticsNode().boundsInRoot
        assertTrue("Actions need a visible gap, rather than a segmented toggle", primary.left > secondary.right)
        assertEquals(primary.height, secondary.height, 1f)
        assertTrue(primary.center.x > secondary.center.x)
        assertEquals(root.center.x, group.center.x, 1f)
        compose.onNodeWithTag("tanya_audio_primary").assert(SemanticsMatcher.keyNotDefined(androidx.compose.ui.semantics.SemanticsProperties.Selected))
        val density = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>().resources.displayMetrics.density
        assertTrue("Empty state should leave space for reading", root.height / density < 185f)
        assertTrue(primary.height / density >= 48f)
        compose.onNodeWithText("ניסיון נוסף").performClick()
        compose.onNodeWithText("למקור השיעור").performClick()
        assertEquals(1, retried)
        assertEquals(1, source)
        compose.onNodeWithTag("tanya_audio_empty").captureRoboImage("/tmp/daily-learning-check/tanya-empty-compact.png")
    }

    @Test fun chapterSelectionActionIsVisibleWithLargeTextAndDarkBackground() {
        var browsed = false
        compose.setContent { DailyRambamTheme {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides Density(density.density, 1.5f)) {
                Surface(color = Color(0xFF1E222A)) {
                    Box(Modifier.width(320.dp).padding(12.dp)) {
                        TanyaAudioEmptyState(Color(0xFFF4F5F5), Color(0xFF1E222A), Color(0xFFF0A0DD),
                            { browsed = true }, {}, primaryLabel = "בחירת פרק אחר")
                    }
                }
            }
        } }
        compose.onNodeWithText("בחירת פרק אחר").assertIsDisplayed().performClick()
        compose.onNodeWithText("למקור השיעור").assertIsDisplayed()
        assertEquals(compose.onNodeWithTag("tanya_audio_primary").fetchSemanticsNode().boundsInRoot.height,
            compose.onNodeWithTag("tanya_audio_source").fetchSemanticsNode().boundsInRoot.height, 1f)
        assertTrue(browsed)
        compose.onNodeWithTag("tanya_audio_empty").captureRoboImage("/tmp/daily-learning-check/tanya-empty-compact-dark-large.png")
    }
}
