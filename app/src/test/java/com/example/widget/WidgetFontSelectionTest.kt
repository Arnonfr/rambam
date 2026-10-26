package com.example.widget

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import com.example.ui.theme.DailyRambamTheme
import com.example.ui.theme.FontStyleOption
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "iw-rIL-w412dp-h892dp-xxhdpi")
class WidgetFontSelectionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun selectedFontIsSavedAlongsideExistingWidgetSettings() {
        var saved: WidgetConfig? = null
        val original = WidgetConfig(theme = WidgetTheme.LIGHT, mainLesson = WidgetMainLesson.CHUMASH)
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                DailyRambamTheme {
                    WidgetConfigScreen(original, onCancel = {}, onSave = { saved = it })
                }
            }
        }
        compose.onNodeWithText("Libertinus").performScrollTo().performClick()
        compose.onNode(hasText("Libertinus") and isSelected()).assertExists()
        compose.onNodeWithText("שמירה").performClick()
        compose.runOnIdle {
            assertEquals(original.copy(font = FontStyleOption.LIBERTINUS), saved)
        }
    }
}
