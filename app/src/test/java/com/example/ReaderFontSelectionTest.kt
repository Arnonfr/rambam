package com.example

import androidx.compose.runtime.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.local.UserPreferences
import com.example.ui.components.ReaderTypographySheet
import com.example.ui.components.StudyTextBlock
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
@Config(sdk = [35], qualifiers = "w400dp-h900dp")
class ReaderFontSelectionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun `catchword sits at physical left in RTL reader`() {
        val line = com.example.domain.tanya.VerifiedTanyaPrint.fullPagesForReference(
            "Tanya, Part IV; Iggeret HaKodesh 28:1-5").last()
        assertTrue(line.isCatchword)
        compose.setContent { DailyRambamTheme {
            CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
                Box(Modifier.width(280.dp).testTag("page")) {
                    com.example.ui.components.TanyaPrintedLine(line, androidx.compose.ui.graphics.Color.Black)
                }
            }
        } }
        val page = compose.onNodeWithTag("page").fetchSemanticsNode().boundsInRoot
        val word = compose.onNodeWithText(line.text).fetchSemanticsNode().boundsInRoot
        assertTrue(word.left >= page.left)
        assertTrue(word.left - page.left < page.width * .03f)
        assertTrue(word.right < page.center.x)
    }

    @Test fun `sticky reader header is compact without second status bar inset`() {
        compose.setContent { DailyRambamTheme {
            com.example.ui.components.CompactReaderHeader("תניא", "אגרת הקדש", androidx.compose.ui.graphics.Color.Magenta,
                0f, "light", Modifier.testTag("header"))
        } }
        compose.onNodeWithTag("header").assertHeightIsEqualTo(34.dp)
    }

    @Test fun `nikud and cantillation do not silently override chosen font`() {
        var id by mutableStateOf("sans")
        val text = "בְּרֵאשִׁ֖ית בָּרָא אֱלֹהִים"
        compose.setContent { DailyRambamTheme {
            StudyTextBlock("", text, text, UserPreferences(fontFamily = id, showNikud = true))
        } }
        FontStyleOption.entries.forEach { font ->
            compose.runOnIdle { id = font.id }
            val results = mutableListOf<TextLayoutResult>()
            compose.onNode(hasText(com.example.ui.util.HebrewTextNormalizer.forDisplay(text))).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
            assertEquals(font.fontFamily, results.single().layoutInput.style.fontFamily)
        }
    }

    @Test fun `print mode hides controls which cannot affect its fixed layout`() {
        compose.setContent { DailyRambamTheme {
            ReaderTypographySheet(UserPreferences(), onDismiss = {}, onFontSizeChange = {},
                onLineSpacingChange = {}, onFontFamilyChange = {}, onNikudToggle = {},
                onThemeChange = {}, onKeepScreenOnChange = {}, fixedPrintLayout = true)
        } }
        compose.onNodeWithText("סוג גופן").assertDoesNotExist()
        compose.onNodeWithText("גודל ומרווח").assertDoesNotExist()
        compose.onNodeWithText("הצג ניקוד וטעמים").assertDoesNotExist()
        compose.onNodeWithText("השאר מסך דולק").assertExists()
    }

    @Test fun `prayer navigation has prayer arrows and no calendar`() {
        var next = false
        var previous = false
        compose.setContent { DailyRambamTheme {
            com.example.ui.components.FloatingReaderBar("", "ברכות השחר", 0f, {}, {}, {}, "light",
                { next = true }, { previous = true }, prayerNavigation = true)
        } }
        compose.onNodeWithContentDescription("מעבר ליום אחר").assertDoesNotExist()
        compose.onNodeWithContentDescription("תפילה הבאה").performClick()
        compose.onNodeWithContentDescription("תפילה קודמת").performClick()
        compose.runOnIdle { assertTrue(next); assertTrue(previous) }
        compose.onNodeWithContentDescription("התאמת טקסט").assertExists()
    }
    @Test fun `printed page has paper edges instead of page heading`() {
        compose.setContent { DailyRambamTheme {
            com.example.ui.components.TanyaPageLine(
                com.example.domain.tanya.TanyaSourceLine(271, "טקסט חי", 1),
                androidx.compose.ui.graphics.Color.Black, androidx.compose.ui.graphics.Color.White,
                first = true, last = true, showAnchor = false)
        } }
        compose.onNodeWithTag("tanya_page_start_271").assertExists()
        compose.onNodeWithTag("tanya_page_end_271").assertExists()
        compose.onNodeWithText("עמוד 271").assertDoesNotExist()
    }

    @Test fun `printed source line never reflows even with large accessibility font scale`() {
        val line = com.example.domain.tanya.VerifiedTanyaPrint.forReference("Tanya, Part IV; Iggeret HaKodesh 23:1-3")[8]
        compose.setContent { DailyRambamTheme {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                Box(Modifier.width(280.dp)) {
                    com.example.ui.components.TanyaPrintedLine(line, androidx.compose.ui.graphics.Color.Black)
                }
            }
        } }
        val wordLayouts = mutableListOf<TextLayoutResult>()
        line.text.split(' ').distinct().forEach { word ->
            compose.onNodeWithText(word).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(wordLayouts) }
        }
        assertTrue(wordLayouts.all { it.lineCount == 1 })
        assertEquals(1, wordLayouts.map { it.firstBaseline }.distinct().size)
    }

    @Test fun `printed pages fill available width and preserve source rows at phone widths`() {
        val references = listOf("23:1-3", "23:4-7", "23:8-9", "23:10-14", "24:1-9",
            "25:1-40", "26:1-36", "27:1-19", "28:1-5")
        val samples = references.flatMap {
            com.example.domain.tanya.VerifiedTanyaPrint.forReference("Tanya, Part IV; Iggeret HaKodesh $it")
        }.filter { it.largeOpeningWords == 0 && it.wordFontScales.isEmpty() && !it.centered }
            .groupBy { it.page }.values.map { page -> page.maxBy { it.text.length } }
        var source by mutableStateOf(samples.first())
        var pageWidth by mutableStateOf(240.dp)
        compose.setContent { DailyRambamTheme {
            Box(Modifier.width(pageWidth).testTag("page_width")) {
                com.example.ui.components.TanyaPageLine(source,
                    androidx.compose.ui.graphics.Color.Black, androidx.compose.ui.graphics.Color.White,
                    first = false, last = false, showAnchor = false)
            }
        } }
        listOf(240.dp, 320.dp, 400.dp).forEach { width ->
            samples.forEach { line ->
                compose.runOnIdle { source = line; pageWidth = width }
                val pageBounds = compose.onNodeWithTag("page_width").fetchSemanticsNode().boundsInRoot
                val words = line.text.split(' ').distinct()
                val bounds = words.flatMap { compose.onAllNodesWithText(it).fetchSemanticsNodes() }.map { it.boundsInRoot }
                assertTrue("All original words must share a single row", bounds.all { kotlin.math.abs(it.top - bounds.first().top) < 1f })
                // Only the font's tiny ink-overhang guard is allowed, not page padding.
                val tolerance = pageBounds.width * .009f + 1f
                val pageInset = 4f * pageBounds.width / width.value
                assertEquals("Only a small right margin", pageBounds.right - pageInset, bounds.maxOf { it.right }, tolerance)
                if (line.justified) assertEquals("Only a small left margin", pageBounds.left + pageInset, bounds.minOf { it.left }, tolerance)
                assertTrue(bounds.all { it.left >= pageBounds.left && it.right <= pageBounds.right })
                words.forEach { word ->
                    val layouts = mutableListOf<TextLayoutResult>()
                    val nodes = compose.onAllNodesWithText(word)
                    nodes.fetchSemanticsNodes().indices.forEach { index ->
                        nodes[index].performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                    }
                    assertTrue(layouts.all { it.lineCount == 1 })
                }
            }
        }
    }

    @Test fun `chapter opening words use larger type without rewrapping source line`() {
        val source = com.example.domain.tanya.VerifiedTanyaPrint.fullPagesForReference(
            "Tanya, Part IV; Iggeret HaKodesh 23:1-3").first { it.largeOpeningWords > 0 }
        compose.setContent { DailyRambamTheme {
            Box(Modifier.width(320.dp)) { com.example.ui.components.TanyaPrintedLine(source, androidx.compose.ui.graphics.Color.Black) }
        } }
        fun result(word: String): TextLayoutResult {
            val results = mutableListOf<TextLayoutResult>()
            compose.onNodeWithText(word).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
            return results.single()
        }
        assertTrue(result("בגזירת").layoutInput.style.fontSize > result("עירין").layoutInput.style.fontSize)
        assertEquals(1, result("בגזירת").lineCount)
        assertEquals(1, result("עירין").lineCount)
    }
}
