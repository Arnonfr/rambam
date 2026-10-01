package com.example.data.mitzvot

import android.content.Context
import com.example.domain.mitzvot.MitzvahAssignmentItem
import com.example.domain.mitzvot.MitzvahAssignmentType
import org.json.JSONArray
import org.json.JSONObject

data class MitzvahText(
    val id: String,
    val title: String,
    val paragraphs: List<String>,
    val sourceName: String,
    val sourceUrl: String,
    val license: String,
    val contentVersion: String
)

data class MitzvahLessonEntry(
    val item: MitzvahAssignmentItem,
    val paragraphs: List<String>
)

class SeferHamitzvotRepository(private val context: Context) {
    private val root: JSONObject by lazy {
        val raw = context.assets.open("sefer_hamitzvot_text.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        JSONObject(raw)
    }

    fun getText(item: MitzvahAssignmentItem): MitzvahText? {
        val number = item.number ?: return null
        val (arrayName, titlePrefix, idPrefix) = when (item.type) {
            MitzvahAssignmentType.POSITIVE -> Triple("positive", "מצוות עשה", "positive")
            MitzvahAssignmentType.NEGATIVE -> Triple("negative", "מצוות לא תעשה", "negative")
            MitzvahAssignmentType.SPECIAL -> return null
        }
        val commandment = root.getJSONArray(arrayName).optJSONArray(number - 1) ?: return null
        return MitzvahText(
            id = "${idPrefix}_$number",
            title = "$titlePrefix $number",
            paragraphs = commandment.toStringList(),
            sourceName = root.getString("versionTitle"),
            sourceUrl = root.getString("sourceUrl"),
            license = root.getString("license"),
            contentVersion = root.getString("version")
        )
    }

    fun getLessonEntries(
        assignment: com.example.domain.mitzvot.DailyMitzvahAssignment
    ): List<MitzvahLessonEntry> = assignment.items.map { item ->
        val paragraphs = when (item.type) {
            MitzvahAssignmentType.POSITIVE,
            MitzvahAssignmentType.NEGATIVE -> getText(item)?.paragraphs.orEmpty()
            MitzvahAssignmentType.SPECIAL -> specialParagraphs(assignment.lessonNumber)
        }
        MitzvahLessonEntry(item = item, paragraphs = paragraphs)
    }

    fun getRambamIntroduction(): List<String> =
        root.getJSONArray("rambamIntroduction").toStringList()

    fun getRoot(number: Int): List<String> {
        require(number in 1..14)
        return root.getJSONArray("roots").getJSONArray(number - 1).toStringList()
    }

    private fun specialParagraphs(lessonNumber: Int): List<String> = when (lessonNumber) {
        1 -> getRambamIntroduction()
        2 -> (1..3).flatMap(::getRoot)
        3 -> (4..6).flatMap(::getRoot)
        4 -> (7..9).flatMap(::getRoot)
        else -> emptyList()
    }

    private fun JSONArray.toStringList(): List<String> = buildList {
        for (index in 0 until length()) {
            when (val value = opt(index)) {
                is JSONArray -> addAll(value.toStringList())
                is String -> if (value.isNotBlank()) add(value)
            }
        }
    }
}
