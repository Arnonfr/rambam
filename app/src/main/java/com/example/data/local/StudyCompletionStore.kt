package com.example.data.local

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** A copy of the explicitly saved remainder, independent of the live reading bookmark. */
data class CompletionText(
    val key: String,
    val label: String,
    val textWithNikud: String,
    val textPlain: String,
    val rashi: List<String> = emptyList()
)

data class SavedStudyCompletion(
    val track: String,
    val studyDate: String,
    val dateLabel: String,
    val title: String,
    val boundaryLabel: String,
    val blocks: List<CompletionText>,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val id: String get() = "$track:$studyDate"
}

class StudyCompletionStore(context: Context) {
    private val prefs = context.getSharedPreferences("manual_study_completions_v1", Context.MODE_PRIVATE)

    fun all(): List<SavedStudyCompletion> = prefs.all.values.mapNotNull { raw ->
        runCatching {
            val o = JSONObject(raw as String)
            val blocks = o.getJSONArray("blocks")
            SavedStudyCompletion(o.getString("track"), o.getString("studyDate"),
                o.getString("dateLabel"), o.getString("title"), o.getString("boundaryLabel"),
                (0 until blocks.length()).map { i ->
                    val b = blocks.getJSONObject(i)
                    val r = b.getJSONArray("rashi")
                    CompletionText(b.getString("key"), b.getString("label"),
                        b.getString("textWithNikud"), b.getString("textPlain"),
                        (0 until r.length()).map(r::getString))
                }, o.getLong("updatedAt"))
        }.getOrNull()
    }.sortedWith(compareBy<SavedStudyCompletion> { it.studyDate }.thenBy { it.track })

    fun save(entry: SavedStudyCompletion): Boolean {
        require(entry.blocks.isNotEmpty())
        val blocks = JSONArray()
        entry.blocks.forEach { b -> blocks.put(JSONObject().put("key", b.key).put("label", b.label)
            .put("textWithNikud", b.textWithNikud).put("textPlain", b.textPlain).put("rashi", JSONArray(b.rashi))) }
        val json = JSONObject().put("track", entry.track).put("studyDate", entry.studyDate)
            .put("dateLabel", entry.dateLabel).put("title", entry.title).put("boundaryLabel", entry.boundaryLabel)
            .put("blocks", blocks).put("updatedAt", entry.updatedAt)
        return prefs.edit().putString(entry.id, json.toString()).commit()
    }

    fun remove(id: String): Boolean = prefs.edit().remove(id).commit()
}
