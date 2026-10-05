package com.example

import com.example.data.chumash.ChumashRepository
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ChumashRashiTest {
    @Test fun spanningChaptersPreserveEmptyVerseGroups() {
        val json = JSONObject("""{"sections":[2,4],"toSections":[3,2],
            "he":[[["<b>אלה.</b> ראשון"],[]],[["שלישי"],["רביעי","עוד"]]]}""")
        assertEquals(listOf(listOf("<b>אלה.</b> ראשון"), emptyList(),
            listOf("שלישי"), listOf("רביעי", "עוד")), ChumashRepository.parseRashiResponse(json))
    }

    @Test fun singleChapterPreservesEmptyVerseGroups() {
        val json = JSONObject("""{"sections":[4,19],"toSections":[4,21],
            "he":[["ראשון"],[],["שלישי"]]}""")
        assertEquals(listOf(listOf("ראשון"), emptyList(), listOf("שלישי")),
            ChumashRepository.parseRashiResponse(json))
    }

    @Test fun missingTrailingCommentDoesNotShiftNextChapter() {
        val json = JSONObject("""{"sections":[7,17],"toSections":[8,2],
            "he":[[["פרק ז"]],[["פרק ח"],[]]]}""")
        assertEquals(listOf(listOf("פרק ז"), emptyList(), listOf("פרק ח"), emptyList()),
            ChumashRepository.parseRashiResponse(json, listOf(2, 2)))
    }

    @Test fun htmlEntitiesAreDecodedAndRashiBoldIsPreserved() {
        assertEquals("א | ב & ג", ChumashRepository.cleanHtml("א&thinsp;|&thinsp;ב &amp; ג"))
        assertEquals("<b>אלה.</b> התורה", ChumashRepository.cleanRashiHtml("<b>אלה.</b>&nbsp;התורה"))
        assertEquals("א ב", ChumashRepository.cleanHtml("א&#32;ב"))
    }

    @Test fun bundledBereishitIncludesRashiInPreviouslyMissingAliyot() {
        val root = RuntimeEnvironment.getApplication().assets.open("chumash_cache.json")
            .bufferedReader().use { JSONObject(it.readText()) }
        val aliyot = root.getJSONObject("בראשית").getJSONArray("aliyot")
        for (index in listOf(0, 1, 2, 5)) {
            val verses = aliyot.getJSONObject(index).getJSONArray("verses")
            assertTrue("Missing Rashi in aliya ${index + 1}",
                (0 until verses.length()).sumOf { verses.getJSONObject(it).getJSONArray("rashi").length() } > 0)
            for (v in 0 until verses.length()) {
                assertFalse(verses.getJSONObject(v).getString("textWithTeamim").contains("&thinsp"))
            }
        }
        val firstOfSecond = aliyot.getJSONObject(1).getJSONArray("verses")
            .getJSONObject(0).getJSONArray("rashi").getString(0)
        assertTrue(firstOfSecond.startsWith("<b>אלה.</b>"))
    }
}
