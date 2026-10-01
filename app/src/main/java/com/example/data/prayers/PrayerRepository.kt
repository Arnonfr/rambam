package com.example.data.prayers

import android.content.Context
import org.json.JSONObject

data class PrayerSection(val id: String, val title: String, val source: String, val paragraphs: List<String>)
data class PrayerBook(val edition: String, val sections: List<PrayerSection>)

class PrayerRepository(private val context: Context) {
    fun load(): PrayerBook {
        val json = JSONObject(context.assets.open("prayers_torah_or.json").bufferedReader().use { it.readText() })
        val sections = json.getJSONArray("sections")
        return PrayerBook(json.getString("edition"), (0 until sections.length()).map { index ->
            val section = sections.getJSONObject(index)
            val paragraphs = section.getJSONArray("paragraphs")
            PrayerSection(section.getString("id"), section.getString("title"), section.getString("source"),
                (0 until paragraphs.length()).map { paragraphs.getString(it) })
        })
    }
}
