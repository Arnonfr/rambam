package com.example
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import com.example.ui.components.WalkingLoadingScreen
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp")
class WalkingLoadingTest {
    @get:Rule val compose = createComposeRule()
    @Test fun loadingBackgroundIsWhiteAndWalkingChangesBetweenFrames() {
        val context = android.view.ContextThemeWrapper(androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>(), R.style.Theme_MyApplication)
        val nativeSplash = context.obtainStyledAttributes(intArrayOf(android.R.attr.windowSplashScreenBackground))
        assertEquals(android.graphics.Color.WHITE, nativeSplash.getColor(0, android.graphics.Color.BLACK))
        nativeSplash.recycle()
        compose.mainClock.autoAdvance = false
        compose.setContent { WalkingLoadingScreen() }
        compose.onRoot().captureRoboImage(filePath = "build/review/loading-walk-start.png")
        val first = javax.imageio.ImageIO.read(java.io.File("build/review/loading-walk-start.png"))
        assertEquals(android.graphics.Color.WHITE, first.getRGB(0, 0))
        compose.mainClock.advanceTimeBy(240)
        compose.onRoot().captureRoboImage(filePath = "build/review/loading-walk-next.png")
        val second = javax.imageio.ImageIO.read(java.io.File("build/review/loading-walk-next.png"))
        assertEquals(android.graphics.Color.WHITE, second.getRGB(second.width - 1, second.height - 1))
        var changed = 0
        for (y in 0 until first.height) for (x in 0 until first.width) if (first.getRGB(x, y) != second.getRGB(x, y)) changed++
        assertTrue("Walking must animate the artwork", changed > 10)
    }
}
