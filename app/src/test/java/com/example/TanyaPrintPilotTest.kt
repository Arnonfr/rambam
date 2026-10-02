package com.example

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TanyaPrintPilotTest {
    @Test fun fixedPageHasCompleteOrderedLinesAndInBoundsWords() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val page = JSONObject(context.assets.open("tanya_print_pilot.json").bufferedReader().use { it.readText() })
        val lines = page.getJSONArray("lines")
        assertEquals(28, lines.length())
        val ids = mutableSetOf<String>()
        var previousBottom = 0.0
        for (i in 0 until lines.length()) {
            val line = lines.getJSONObject(i)
            assertTrue(ids.add(line.getString("id")))
            assertTrue(line.getDouble("top") >= previousBottom)
            previousBottom = line.getDouble("bottom")
            assertTrue(previousBottom <= page.getDouble("height"))
            val words = line.getJSONArray("words")
            val reconstructed = (0 until words.length()).joinToString(" ") { words.getJSONObject(it).getString("text") }
            assertEquals(line.getString("text"), reconstructed)
            assertFalse(Regex("[\u0591-\u05C7]").containsMatchIn(reconstructed))
            var previousLeft = page.getDouble("width")
            for (j in 0 until words.length()) {
                val word = words.getJSONObject(j)
                val left = word.getDouble("left")
                val right = word.getDouble("right")
                assertTrue(left >= 0 && right > left && right <= previousLeft)
                previousLeft = left
            }
        }
        // A viewport changes scale only: the line IDs/order and aspect ratio remain fixed.
        for (width in listOf(320.0, 360.0, 412.0, 600.0)) {
            val scale = width / page.getDouble("width")
            assertEquals(page.getDouble("height") / page.getDouble("width"),
                page.getDouble("height") * scale / width, 0.000001)
        }
    }
}
