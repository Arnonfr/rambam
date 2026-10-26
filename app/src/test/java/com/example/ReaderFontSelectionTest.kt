package com.example

import androidx.compose.runtime.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.withStyle
import com.github.takahirom.roborazzi.captureRoboImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import com.example.ui.components.printedPageZoom
import com.example.ui.components.printedPageZoomLayer
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
    val compose = createComposeRule()
    @get:Rule val hostAndCompose = withComposeHost(compose)

    @Test fun `two fingers zoom printed text but never change its layout width or rows`() {
        val zoom = com.example.ui.components.PrintedPageZoomState()
        compose.setContent { DailyRambamTheme {
            Box(Modifier.size(300.dp).clipToBounds().printedPageZoom(zoom).testTag("zoom_viewport")) {
                Box(Modifier.width(300.dp).printedPageZoomLayer(zoom)) {
                    androidx.compose.material3.Text("צורת הדף נשארת קבועה", Modifier.testTag("zoom_text"))
                }
            }
        } }
        fun layout(): TextLayoutResult {
            val results = mutableListOf<TextLayoutResult>()
            compose.onNodeWithTag("zoom_text").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
            return results.single()
        }
        val before = layout()
        compose.onNodeWithTag("zoom_viewport").performTouchInput {
            down(0, Offset(center.x - 30f, center.y))
            down(1, Offset(center.x + 30f, center.y))
            advanceEventTime(16)
            moveTo(0, Offset(center.x - 60f, center.y))
            moveTo(1, Offset(center.x + 60f, center.y))
            up(0); up(1)
        }
        compose.runOnIdle { assertTrue(zoom.scale > 1.5f) }
        val after = layout()
        assertEquals(before.size, after.size)
        assertEquals(before.lineCount, after.lineCount)
        assertEquals(before.layoutInput.style.fontSize, after.layoutInput.style.fontSize)
    }

    @Test fun `former torani option selects actual Tanya print font`() {
        assertEquals("וילנא", FontStyleOption.SERIF.title)
        assertEquals(com.example.ui.theme.TanyaPrintFontFamily, FontStyleOption.SERIF.fontFamily)
    }

    @Test fun `prayer section title opens section picker`() {
        var opened = false
        compose.setContent { DailyRambamTheme {
            com.example.ui.components.FloatingReaderBar("", "קרבנות", 0f, {}, {}, {}, "light",
                {}, {}, prayerNavigation = true, onOpenPrayerSections = { opened = true })
        } }
        compose.onNodeWithText("קרבנות").performClick()
        compose.runOnIdle { assertTrue(opened) }
        compose.onNodeWithContentDescription("מעבר ליום אחר").assertDoesNotExist()
    }

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
        compose.onNodeWithTag("header").assertHeightIsEqualTo(54.dp)
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

    @Test fun `fallback keeps Hebrew vowels with their base letters and preserves bold Rashi`() {
        val source = androidx.compose.ui.text.buildAnnotatedString {
            withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)) {
                append("שָׁלוֹם ")
            }
            append("מֶֽלֶךְ טקסט")
        }
        val result = com.example.ui.components.withStableHebrewMarks(source, com.example.ui.theme.TanyaPrintFontFamily)
        assertEquals(source.text, result.text)
        assertTrue(result.spanStyles.any { it.item.fontWeight == androidx.compose.ui.text.font.FontWeight.Bold })
        assertFalse("Pointed words must keep the chosen font", result.spanStyles.any { it.item.fontFamily != null })
    }

    @Test fun `pointed Chumash words render with complete glyph clusters in every font`() {
        var fontId by mutableStateOf("sans")
        val verse = "וַיֹּאמֶר לוֹ יְהוָה לָכֵן כָּל־הֹרֵג קַיִן שִׁבְעָתַיִם יֻקָּם וַיָּשֶׂם יְהוָה לְקַיִן אוֹת לְבִלְתִּי הַכּוֹת־אֹתוֹ כָּל־מֹצְאוֹ׃"
        compose.setContent { DailyRambamTheme {
            CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
                Box(Modifier.width(340.dp).background(androidx.compose.ui.graphics.Color.White)) {
                    StudyTextBlock("ט״ו", verse, verse, UserPreferences(fontFamily = fontId, showNikud = true), textColor = androidx.compose.ui.graphics.Color.Black)
                }
            }
        } }
        FontStyleOption.entries.forEach { font ->
            compose.runOnIdle { fontId = font.id }
            compose.onRoot().captureRoboImage(filePath = "build/review/chumash-${font.id}.png")
        }
    }

    @Test fun `verse number is bold and letter spacing follows global preference`() {
        compose.setContent { DailyRambamTheme {
            StudyTextBlock("א׳", "שָׁלוֹם", "שלום", UserPreferences(letterSpacingEm = .08f))
        } }
        val results = mutableListOf<TextLayoutResult>()
        compose.onNode(hasText("א׳. שָׁלוֹם")).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        val layout = results.single()
        assertEquals(androidx.compose.ui.unit.TextUnitType.Em, layout.layoutInput.style.letterSpacing.type)
        assertEquals(.08f, layout.layoutInput.style.letterSpacing.value, .0001f)
        assertTrue(layout.layoutInput.text.spanStyles.any { it.start == 0 && it.item.fontWeight == androidx.compose.ui.text.font.FontWeight.Bold })
    }

    @Test fun `print mode hides controls which cannot affect its fixed layout`() {
        compose.setContent { DailyRambamTheme {
            ReaderTypographySheet(UserPreferences(), onDismiss = {}, onFontSizeChange = {},
                onLineSpacingChange = {}, onFontFamilyChange = {}, onNikudToggle = {},
                onThemeChange = {}, onKeepScreenOnChange = {}, fixedPrintLayout = true)
        } }
        compose.onNodeWithText("סוג גופן").assertDoesNotExist()
        compose.onNodeWithText("ריווח תווים").assertDoesNotExist()
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
        val baselines = line.text.split(' ').distinct().map { word ->
            val node = compose.onNodeWithText(word)
            val layouts = mutableListOf<TextLayoutResult>()
            node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertEquals(1, layouts.single().lineCount)
            node.fetchSemanticsNode().boundsInRoot.top + layouts.single().firstBaseline
        }
        assertTrue("All printed words share the baseline", baselines.max() - baselines.min() <= 1f)
    }

    @Test fun `printed pages fill available width and preserve source rows at phone widths`() {
        val references = listOf("23:1-3", "23:4-7", "23:8-9", "23:10-14", "24:1-9",
            "25:1-40", "26:1-36", "27:1-19", "28:1-5")
        val samples = references.flatMap {
            com.example.domain.tanya.VerifiedTanyaPrint.forReference("Tanya, Part IV; Iggeret HaKodesh $it")
        }.filter { it.largeOpeningWords == 0 && it.wordFontScales.isEmpty() && !it.centered && it.rightIndentFraction == 0f }
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
                val baselines = words.flatMap { word ->
                    val nodes = compose.onAllNodesWithText(word)
                    nodes.fetchSemanticsNodes().indices.map { index ->
                        val layouts = mutableListOf<TextLayoutResult>()
                        nodes[index].performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                        nodes[index].fetchSemanticsNode().boundsInRoot.top + layouts.single().firstBaseline
                    }
                }
                assertTrue("All original words must share one baseline", baselines.max() - baselines.min() <= 1f)
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
