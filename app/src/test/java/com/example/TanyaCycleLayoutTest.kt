package com.example
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.domain.tanya.VerifiedTanyaPrint
import com.example.ui.components.TanyaPageLine
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
@Config(sdk = [35], qualifiers = "iw-rIL-w400dp-h1400dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TanyaCycleLayoutTest {
    val compose = createComposeRule()
    @get:Rule val hostAndCompose = withComposeHost(compose)
    @Test fun sideNotesStayPhysicallyLeftAndChapterTextRight() {
        val ref = mutableStateOf("Tanya, Part I; Likkutei Amarim 1:1-12")
        val page = mutableStateOf(10)
        compose.setContent { DailyRambamTheme {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                val lines = VerifiedTanyaPrint.fullPagesForReference(ref.value).filter { it.page == page.value }
                Column(Modifier.width(300.dp).background(Color.White).testTag("cycle_page")) {
                    lines.forEachIndexed { i, line -> TanyaPageLine(line, Color.Black, Color.White,
                        first = i == 0, last = i == lines.lastIndex, showAnchor = false) }
                }
            }
        } }
        val left = compose.onNodeWithText("הגהה").fetchSemanticsNode().boundsInRoot
        val right = compose.onNodeWithText("והא").fetchSemanticsNode().boundsInRoot
        assertTrue(left.center.x < right.center.x)
        assertTrue(left.height < right.height)
        compose.onNodeWithTag("cycle_page").captureRoboImage("/tmp/daily-learning-check/tanya-cycle-page-10.png")
        compose.runOnIdle {
            ref.value = "Tanya, Part I; Likkutei Amarim, Title Page 1-5; Approbation 1:1-9; Approbation 2:1-6"
            page.value = 4
        }
        compose.onNodeWithTag("cycle_page").captureRoboImage("/tmp/daily-learning-check/tanya-cycle-page-4.png")
    }
}
