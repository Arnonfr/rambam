package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.data.local.UserPreferences
import com.example.ui.components.*
import com.example.ui.theme.DailyRambamTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.nio.ByteBuffer

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h900dp")
class ReaderCorrectionsTest {
    val compose = createComposeRule()
    @get:Rule val hostAndCompose = withComposeHost(compose)

    @Test fun `nested editorial notes and citations shrink without altering the source or its emphasis`() {
        val source = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("א׳. ") }
            append("זֶה סֵפֶר*(בספרי תימן [נוסח אחר] סֵפֶר בסמ״ך גדולה) שֶׁנֶּאֱמַר (דברים כה-ו) 'וְלֹא יִמָּחֶה'")
        }
        val result = withSmallReadingNotes(source, 20.sp)
        assertEquals(source.text, result.text)
        assertTrue(result.spanStyles.any { it.item.fontWeight == FontWeight.Bold })
        val notes = result.spanStyles.filter { it.item.fontSize == 20.sp }
        assertEquals(2, notes.size)
        assertEquals("*(בספרי תימן [נוסח אחר] סֵפֶר בסמ״ך גדולה)", result.text.substring(notes[0].start, notes[0].end))
        assertEquals("(דברים כה-ו)", result.text.substring(notes[1].start, notes[1].end))
        assertTrue(notes.all { it.end < result.text.indexOf("'וְלֹא") })
    }

    @Test fun `note sizes follow reader size and nikud changes`() {
        var size by mutableStateOf(32f)
        var pointed by mutableStateOf(true)
        compose.setContent { DailyRambamTheme {
            StudyTextBlock("א׳", "שָׁלוֹם (מקור)", "שלום (מקור)",
                UserPreferences(fontFamily = "libertinus", fontSizeSp = size, showNikud = pointed))
        } }
        fun layout(): TextLayoutResult {
            val results = mutableListOf<TextLayoutResult>()
            compose.onNode(hasText("(מקור)", substring = true)).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
            return results.single()
        }
        assertEquals(20.8f, layout().layoutInput.text.spanStyles.single { it.item.fontSize.isSpecified }.item.fontSize.value, .001f)
        compose.runOnIdle { size = 40f; pointed = false }
        val result = layout()
        assertEquals("א׳. שלום (מקור)", result.layoutInput.text.text)
        assertEquals(26f, result.layoutInput.text.spanStyles.single { it.item.fontSize.isSpecified }.item.fontSize.value, .001f)
        assertTrue(result.layoutInput.text.spanStyles.any { it.item.fontWeight == FontWeight.Bold })
    }

    @Test fun `Libertinus resources contain Hebrew composition and positioning tables`() {
        val resources = RuntimeEnvironment.getApplication().resources
        listOf(com.example.R.font.libertinus_serif_regular,
            com.example.R.font.libertinus_serif_bold).forEach { id ->
            val bytes = resources.openRawResource(id).use { it.readBytes() }
            val buffer = ByteBuffer.wrap(bytes)
            val count = buffer.getShort(4).toInt() and 0xffff
            val tables = (0 until count).associate { i ->
                val at = 12 + i * 16
                String(bytes, at, 4, Charsets.US_ASCII) to buffer.getInt(at + 8)
            }
            assertTrue("GSUB is required for Hebrew niqqud composition", tables.containsKey("GSUB"))
            assertTrue(tables.containsKey("GPOS"))
            val gsub = tables.getValue("GSUB")
            val scripts = gsub + (buffer.getShort(gsub + 4).toInt() and 0xffff)
            val scriptCount = buffer.getShort(scripts).toInt() and 0xffff
            assertTrue((0 until scriptCount).any { String(bytes, scripts + 2 + it * 6, 4, Charsets.US_ASCII) == "hebr" })
            val features = gsub + (buffer.getShort(gsub + 6).toInt() and 0xffff)
            val featureCount = buffer.getShort(features).toInt() and 0xffff
            assertTrue((0 until featureCount).any { String(bytes, features + 2 + it * 6, 4, Charsets.US_ASCII) == "ccmp" })
        }
    }

    @Test fun `Romm Hebrew positioning works without a specific locale`() {
        val bytes = RuntimeEnvironment.getApplication().resources.openRawResource(R.font.derech_vilna_regular).use { it.readBytes() }
        val buffer = ByteBuffer.wrap(bytes)
        fun u16(at: Int) = buffer.getShort(at).toInt() and 0xffff
        val gpos = (0 until u16(4)).map { 12 + it * 16 }
            .first { String(bytes, it, 4, Charsets.US_ASCII) == "GPOS" }
            .let { buffer.getInt(it + 8) }
        val scripts = gpos + u16(gpos + 4)
        val hebrewRecord = (0 until u16(scripts)).map { scripts + 2 + it * 6 }
            .first { String(bytes, it, 4, Charsets.US_ASCII) == "hebr" }
        val hebrew = scripts + u16(hebrewRecord + 4)
        val defaultLanguage = u16(hebrew)
        assertTrue("Default Hebrew language must enable mark positioning", defaultLanguage > 0)
        assertTrue(u16(hebrew + defaultLanguage + 4) > 0)
    }

    @Test fun `Hebrew quotes inherit each selected font and preserve emphasis and offsets`() {
        val source = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("רמב\"ם כ״ד ‘מילה’ \"ציטוט\"") }
        }
        com.example.ui.theme.FontStyleOption.entries.forEach { font ->
            val result = withStableHebrewMarks(source, font.fontFamily)
            assertEquals(source.length, result.length)
            assertEquals("רמב״ם כ״ד ‘מילה’ \"ציטוט\"", result.text)
            assertEquals(source.spanStyles, result.spanStyles)
            assertFalse(result.spanStyles.any { it.item.fontFamily != null })
        }
    }

    @Test fun `reader screenshots show small notes and weekday date in Libertinus`() {
        val passages = listOf(
            "שֶׁהִיא עֶרְוָה. שֶׁנֶּאֱמַר (דברים כה-ו) 'וְלֹא יִמָּחֶה שְׁמוֹ מִיִּשְׂרָאֵל' פְּרָט לְסָרִיס חַמָּה וְאַנְדְּרוֹגִינוֹס שֶׁשְּׁמָם מָחוּי הוֹאִיל וְאֵינָן רְאוּיִין לֵילֵד מִתְּחִלַּת בְּרִיָּתָן. וַהֲרֵי הֵן כְּמִין בִּפְנֵי עַצְמוֹ. (דברים כה-ו) 'וְהָיָה הַבְּכוֹר אֲשֶׁר תֵּלֵד' פְּרָט לְאַיְלוֹנִית שֶׁאֵינָהּ רְאוּיָה לֵילֵד מִתְּחִלַּת בְּרִיָּתָהּ.",
            "זֶה סֵפֶר*(בספרי תימן סֵפֶר בסמ״ך גדולה) תּוֹלְדֹת אָדָם בְּיוֹם בְּרֹא אֱלֹהִים אָדָם בִּדְמוּת אֱלֹהִים עָשָׂה אֹתוֹ׃"
        )
        var page by mutableStateOf(0)
        compose.setContent { DailyRambamTheme {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Column(Modifier.fillMaxSize().background(Color(0xFFFAF9F5))) {
                    CompactReaderHeader(if (page == 0) "רמב״ם" else "חומש",
                        if (page == 0) "הלכות יבום וחליצה · פרק ו׳" else "פרשת בראשית · פרק ה׳",
                        Color(0xFFFF6C27), 0f, "light")
                    Column(Modifier.weight(1f).padding(20.dp)) {
                        StudyTextBlock(if (page == 0) "" else "א׳", passages[page], passages[page],
                            UserPreferences(fontFamily = "libertinus", fontSizeSp = 32f, showNikud = true), textColor = Color.Black)
                    }
                    FloatingReaderBar("", "כ״ט בתשרי תשפ״ז", 0f, {}, {}, {}, "light",
                        {}, {}, studyDate = "2026-10-10")
                }
            }
        } }
        compose.onNodeWithText("יום שבת כ״ט בתשרי").assertExists()
        compose.onNodeWithText("כ״ט בתשרי תשפ״ז").assertDoesNotExist()
        compose.onRoot().captureRoboImage(filePath = "build/review/reader-rambam-corrected.png")
        compose.runOnIdle { page = 1 }
        compose.onRoot().captureRoboImage(filePath = "build/review/reader-chumash-corrected.png")
    }
}
