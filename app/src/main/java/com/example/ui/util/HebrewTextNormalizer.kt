package com.example.ui.util

import java.text.Normalizer

/**
 * Keeps Hebrew base letters and their combining marks in a stable canonical order.
 * CGJ is deliberately preserved because it can carry meaningful ordering information
 * in Biblical Hebrew.
 */
object HebrewTextNormalizer {
    private fun isHebrewMark(char: Char) = char in '\u0591'..'\u05C7' &&
        Character.getType(char) == Character.NON_SPACING_MARK.toInt()

    fun forDisplay(value: String): String {
        // Legacy Hebrew presentation glyphs can bypass font mark positioning.
        // Decompose only these glyphs, not general compatibility characters.
        val expanded = buildString {
            value.forEach { char -> append(if (char in '\uFB1D'..'\uFB4F')
                Normalizer.normalize(char.toString(), Normalizer.Form.NFKD) else char.toString()) }
        }
        return Normalizer.normalize(expanded, Normalizer.Form.NFC)
    }

    fun withoutBiblicalAnnotations(value: String): String = forDisplay(value)
        .filterNot { it in '\u0591'..'\u05AF' || it in listOf('\u05BD', '\u05BF', '\u05C0', '\u05C4', '\u05C5', '\u05C6') }
        .replace('\u05C7', '\u05B8')

    fun containsCantillationOrRareMarks(value: String): Boolean = value.any { char ->
        char in '\u0591'..'\u05AF' ||
            char == '\u05BD' || // meteg
            char == '\u05BF' || // rafe
            char == '\u05C4' ||
            char == '\u05C5' ||
            char == '\u05C7'
    }

    fun stripMarks(value: String): String = forDisplay(value).filterNot(::isHebrewMark)
}
