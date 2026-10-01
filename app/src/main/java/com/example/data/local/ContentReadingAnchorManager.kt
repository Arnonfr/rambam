package com.example.data.local

import android.content.Context
import org.json.JSONObject

data class ContentReadingAnchor(
    val contentId: String,
    val assignmentDate: String,
    val sectionId: String,
    val blockId: String,
    val blockIndex: Int,
    val textOffset: Int = 0,
    val pageNumber: Int? = null,
    val normalizedYOffset: Float = 0f,
    val updatedAt: Long = System.currentTimeMillis()
)

/** Stores one independent, date-scoped anchor for every study/prayer content type. */
class ContentReadingAnchorManager(context: Context) {
    private val prefs = context.getSharedPreferences("content_reading_anchors_v2", Context.MODE_PRIVATE)

    fun save(anchor: ContentReadingAnchor) {
        prefs.edit().putString(key(anchor.contentId), anchor.toJson().toString()).commit()
    }

    fun get(contentId: String, assignmentDate: String): ContentReadingAnchor? {
        val raw = prefs.getString(key(contentId), null) ?: return null
        return runCatching { JSONObject(raw).toAnchor() }
            .getOrNull()
            ?.takeIf { it.assignmentDate == assignmentDate }
    }

    fun clear(contentId: String) {
        prefs.edit().remove(key(contentId)).commit()
    }

    fun clearAll() {
        prefs.edit().clear().commit()
    }

    private fun key(contentId: String) = "anchor_${contentId.replace(Regex("[^A-Za-z0-9_-]"), "_")}" 

    private fun ContentReadingAnchor.toJson() = JSONObject()
        .put("contentId", contentId)
        .put("assignmentDate", assignmentDate)
        .put("sectionId", sectionId)
        .put("blockId", blockId)
        .put("blockIndex", blockIndex)
        .put("textOffset", textOffset)
        .put("pageNumber", pageNumber)
        .put("normalizedYOffset", normalizedYOffset.toDouble())
        .put("updatedAt", updatedAt)

    private fun JSONObject.toAnchor() = ContentReadingAnchor(
        contentId = getString("contentId"),
        assignmentDate = getString("assignmentDate"),
        sectionId = getString("sectionId"),
        blockId = getString("blockId"),
        blockIndex = getInt("blockIndex"),
        textOffset = optInt("textOffset", 0),
        pageNumber = if (isNull("pageNumber")) null else getInt("pageNumber"),
        normalizedYOffset = optDouble("normalizedYOffset", 0.0).toFloat(),
        updatedAt = optLong("updatedAt", 0L)
    )
}
