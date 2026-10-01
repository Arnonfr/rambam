package com.example.ui.util

import java.text.Normalizer

/**
 * Keeps Hebrew base letters and their combining marks in a stable canonical order.
 * CGJ is deliberately preserved because it can carry meaningful ordering information
 * in Biblical Hebrew.
 */
object HebrewTextNormalizer {
    private val hebrewMarkRange = '\u0591'..'\u05C7'

    fun forDisplay(value: String): String =
        if (Normalizer.isNormalized(value, Normalizer.Form.NFC)) value
        else Normalizer.normalize(value, Normalizer.Form.NFC)

    fun containsCantillationOrRareMarks(value: String): Boolean = value.any { char ->
        char in '\u0591'..'\u05AF' ||
            char == '\u05BD' || // meteg
            char == '\u05BF' || // rafe
            char == '\u05C4' ||
            char == '\u05C5' ||
            char == '\u05C7'
    }

    fun stripMarks(value: String): String = value.filterNot { it in hebrewMarkRange }
}
