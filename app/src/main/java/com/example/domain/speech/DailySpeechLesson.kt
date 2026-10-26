package com.example.domain.speech

import java.security.MessageDigest

data class SpeechPart(val chapterId: String, val chapterTitle: String, val text: String,
    val unit: Int = 0, val unitLabel: String = "", val audioUrl: String = "")
data class DailySpeechLesson(val id: String, val title: String, val parts: List<SpeechPart>, val source: String = "michael") {
    val recorded: Boolean get() = source != "michael"
    fun nextUnitStart(index: Int): Int? = parts.indices.firstOrNull { it > index && parts[it].unit != parts[index].unit }
    fun previousUnitStart(index: Int): Int? {
        val first = parts.indexOfFirst { it.unit == parts[index].unit }
        if(first <= 0) return null
        return parts.indexOfFirst { it.unit == parts[first-1].unit }
    }
}

/** Speech text is a separate copy. Never mutate the displayed / source text. */
object DailySpeechText {
    fun prepare(source: String): String = source
        .replace(Regex("<[^>]+>"), " ")
        .replace("&nbsp;", " ").replace("&quot;", "\"").replace("&amp;", "&")
        .replace(Regex("[\\u0591-\\u05AF\\u05BD\\u05BF\\u05C0\\u05C4\\u05C5]"), "")
        .replace('־', ' ').replace('׃', '.')
        .replace(Regex("\\s+"), " ").trim()

    /** What is read aloud: unpointed parentheses are source references such as (דברים ו-ד), printed for the eye only. */
    fun speak(source: String): String = prepare(source)
        .replace(Regex("\\s*\\([^()\\u05B0-\\u05BC\\u05C1\\u05C2\\u05C7]*\\)"), " ")
        .replace(Regex("[()\\[\\]]"), " ")
        .replace(Regex("\\s+"), " ").trim()

    /** Prefer sentence boundaries; a long sentence is split only at a word boundary. */
    fun split(source: String, limit: Int = 700): List<String> {
        require(limit >= 32)
        var rest = prepare(source)
        val result = mutableListOf<String>()
        while (rest.length > limit) {
            val window = rest.take(limit)
            val sentence = window.indexOfLast { it in ".;:!?" }.takeIf { it >= limit / 3 }
            val cut = sentence?.plus(1) ?: window.lastIndexOf(' ').takeIf { it > 0 } ?: limit
            result += rest.take(cut).trim()
            rest = rest.drop(cut).trimStart()
        }
        if (rest.isNotBlank()) result += rest
        return result
    }

    fun lesson(date: String, track: String, dateLabel: String,
        chapters: List<Triple<String, String, List<String>>>): DailySpeechLesson {
        require(chapters.isNotEmpty() && chapters.all { it.third.isNotEmpty() && it.third.all(String::isNotBlank) })
        var unit = 0
        val parts = chapters.flatMap { (id, title, paragraphs) ->
            paragraphs.flatMapIndexed { index, paragraph ->
                val number = unit++
                // The halacha number (א, יא…) is a printed label; from יא on it was read as a word.
                split((if(index==0) "$title. " else "") + paragraph.replaceFirst(HALACHA_NUMBER, "")).map {
                    SpeechPart(id, title, it, number, "הלכה ${index+1}")
                }
            }
        }
        val fingerprint = hash("units-v2:" + parts.joinToString("\n") { "${it.chapterId}:${it.unit}:${it.text}" }).take(16)
        return DailySpeechLesson("rambam-$date-$track-$fingerprint", "רמב״ם · $dateLabel", parts)
    }

    private val HALACHA_NUMBER = Regex("^\\s*[א-ת]{1,3}\\s+")

    fun hash(text: String): String = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
        .joinToString("") { "%02x".format(it) }
}
