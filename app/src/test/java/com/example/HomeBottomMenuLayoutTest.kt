package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.HomeTab
import com.example.ui.components.FloatingReaderBar
import com.example.ui.screens.FloatingDarkNavBar
import com.example.ui.theme.DailyRambamTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import androidx.compose.ui.unit.dp

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp")
class HomeBottomMenuLayoutTest {
    @get:Rule val compose = createComposeRule()
    @Test fun lessonsAreOnTheRightAndTheHomeMenuMatchesTheReaderMenuHeight() {
        var reader by mutableStateOf(false)
        compose.setContent { DailyRambamTheme {
            Box(Modifier.fillMaxSize().consumeWindowInsets(WindowInsets.navigationBars)) {
                if (reader) FloatingReaderBar("", "כ״ו תשרי", 0f, {}, {}, {}, "light", {}, {},
                    modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
                else FloatingDarkNavBar(HomeTab.STUDY, {},
                    modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 10.dp))
            }
        } }
        val study = compose.onNodeWithTag("home_tab_STUDY").fetchSemanticsNode().boundsInRoot
        val prayers = compose.onNodeWithTag("home_tab_PRAYERS").fetchSemanticsNode().boundsInRoot
        val podcasts = compose.onNodeWithTag("home_tab_PODCASTS").fetchSemanticsNode().boundsInRoot
        assertTrue(study.left >= prayers.right)
        assertTrue(prayers.left >= podcasts.right)
        compose.onNodeWithTag("home_bottom_menu").assertHeightIsEqualTo(58.dp)
        compose.onRoot().captureRoboImage(filePath = "build/review/home-bottom-menu-2.png")
        compose.runOnIdle { reader = true }
        val typography = compose.onNodeWithTag("reader_typography_button").fetchSemanticsNode().boundsInRoot
        assertEquals("Both menus must share the same vertical centre", study.center.y, typography.center.y, 1f)
    }
}
