package com.example.data.prayers.audio

import android.content.Context
import org.json.JSONObject
import java.time.DayOfWeek

/** Release assets contain no draft audio. Future approved HTTPS clips use this same schema. */
object PrayerAudioCatalog {
    fun load(context: Context, day: DayOfWeek, allowDraft: Boolean): Map<String, PrayerAudioClip> {
        val file = if (allowDraft) "prayer_audio_debug.json" else "prayer_audio.json"
        val text = runCatching { context.assets.open(file).bufferedReader().use { it.readText() } }.getOrNull()
            ?: return emptyMap()
        val items = JSONObject(text).getJSONArray("clips")
        val records = (0 until items.length()).map { index ->
            val item = items.getJSONObject(index)
            val weekday = item.optString("weekday").takeIf { it.isNotEmpty() }?.let { DayOfWeek.valueOf(it) }
            item.getString("stepId") to PrayerAudioClip(item.getString("id"), item.getString("uri"),
                item.optBoolean("approved", false), weekday)
        }.filter { (_, clip) -> PrayerAudioPlan.playable(clip, day, allowDraft) }
        require(records.map { it.first }.distinct().size == records.size) { "Duplicate prayer audio step" }
        return records.toMap()
    }
}
