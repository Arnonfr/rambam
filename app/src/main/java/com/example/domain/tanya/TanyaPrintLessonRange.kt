package com.example.domain.tanya

import java.text.Normalizer

/** Inclusive/exclusive offsets into the immutable, logical RTL word sequence of an edition. */
data class TanyaPrintLessonRange(val startWord: Int, val endWordExclusive: Int) {
    init {
        require(startWord >= 0 && endWordExclusive > startWord)
    }
    fun contains(word: Int): Boolean = word >= startWord && word < endWordExclusive
}

/**
 * Fail closed: only a complete, unique text match is accepted as a daily lesson boundary.
 * No fuzzy matching: a wrong boundary silently greying study text is worse than no boundary.
 * Printed abbreviations that differ from the daily text need a separately verified source map.
 */
object TanyaPrintLessonMatcher {
    private fun normalize(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFKD)
        .replace(Regex("[\u0591-\u05BD\u05BF-\u05C2\u05C4-\u05C5\u05C7]"), "")
        .replace(Regex("[^\u05D0-\u05EA]"), "")

    fun findUnique(printedWords: List<String>, lessonWords: List<String>): TanyaPrintLessonRange? {
        val print = printedWords.mapIndexedNotNull { index, word ->
            normalize(word).takeIf { it.isNotEmpty() }?.let { index to it }
        }
        val lesson = lessonWords.map(::normalize).filter { it.isNotEmpty() }
        if (lesson.isEmpty() || lesson.size > print.size) return null
        var found: TanyaPrintLessonRange? = null
        for (start in 0..print.size - lesson.size) {
            if (lesson.indices.all { print[start + it].second == lesson[it] }) {
                if (found != null) return null
                found = TanyaPrintLessonRange(print[start].first, print[start + lesson.lastIndex].first + 1)
            }
        }
        return found
    }
}
