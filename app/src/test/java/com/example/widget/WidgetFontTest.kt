package com.example.widget

import android.content.Context
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.FontStyleOption
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "iw-rIL-w412dp-h892dp-xxhdpi")
class WidgetFontTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun existingWidgetsKeepTheirFontAndLesson() {
        assertEquals(FontStyleOption.SERIF, WidgetConfigStore.read(emptyPreferences()).font)
        val existing = preferencesOf(stringPreferencesKey("widget_theme") to "light",
            stringPreferencesKey("widget_main_lesson") to "chumash")
        val config = WidgetConfigStore.read(existing)
        assertEquals(FontStyleOption.SERIF, config.font)
        assertEquals(WidgetTheme.LIGHT, config.theme)
        assertEquals(WidgetMainLesson.CHUMASH, config.mainLesson)
        FontStyleOption.entries.forEach { font ->
            assertEquals(font, WidgetConfigStore.read(preferencesOf(stringPreferencesKey("widget_font") to font.id)).font)
        }
    }

    @Test fun plainQuotesAndPunctuationFallbackRemainScopedToWidget() {
        val source = "רמב״ם אמר: “למעלה” ה׳"
        val displayed = VilnaBitmapText.layout(context, source, 250f, 24f).text
        assertEquals("רמב\"ם אמר: \"למעלה\" ה'", displayed.toString())
        assertEquals("רמב״ם אמר: “למעלה” ה׳", source)
        val spans = (displayed as android.text.Spanned).getSpans(0, displayed.length,
            android.text.style.MetricAffectingSpan::class.java)
        assertEquals(4, spans.size)
    }

    @Test fun selectedFontsProduceDifferentHebrewGlyphs() {
        val hashes = FontStyleOption.entries.map { font ->
            val bitmap = VilnaBitmapText.render(context, "אָב בָּרוּךְ שלום", 250f, 24f,
                android.graphics.Color.BLACK, 2, false, false, font)
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            assertTrue(pixels.any { android.graphics.Color.alpha(it) > 0 })
            pixels.contentHashCode()
        }
        assertEquals(4, hashes.toSet().size)
    }

    @Test fun textBudgetUsesTheSelectedFontsActualLineHeight() {
        val blocks = listOf(WidgetBlock("בָּרוּךְ אַתָּה שלום עולם ".repeat(60)), WidgetBlock("סוף השיעור"))
        FontStyleOption.entries.forEach { font ->
            for (width in listOf(224f, 360f)) {
                val height = 95f
                val plan = VilnaBitmapText.plan(context, blocks, width, height, font)
                assertEquals(WidgetTextFit.Mode.TRUNCATED, plan.mode)
                val density = context.resources.displayMetrics.density
                val used = plan.blocks.mapIndexed { index, block ->
                    VilnaBitmapText.layout(context, block.text, width, 16f,
                        maxLines = if (index == plan.blocks.lastIndex) plan.lastBlockMaxLines else Int.MAX_VALUE,
                        font = font).height + if (index > 0) 6f * density else 0f
                }.sum() + VilnaBitmapText.layout(context, "הצג עוד", width, 14f, bold = true, font = font).height + 2f * density
                assertTrue("${font.id} overflows at $width: $used", used <= height * density)
            }
        }
    }
}
