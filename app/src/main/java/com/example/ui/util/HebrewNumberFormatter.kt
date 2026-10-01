package com.example.ui.util

object HebrewNumberFormatter {
    /**
     * Converts an integer to Hebrew numeral letters (e.g. 1 -> א, 28 -> כח).
     * If withGershayim = true: 1 -> א׳, 28 -> כ״ח.
     * With withGershayim = false: 1 -> א, 28 -> כח (as requested: "הלכה א מתוך כח").
     */
    fun toHebrewNumeral(number: Int, withGershayim: Boolean = false): String {
        if (number <= 0) return number.toString()
        if (number > 999) return number.toString()
        var remainder = number
        val raw = buildString {
            while (remainder >= 400) {
                append("ת")
                remainder -= 400
            }
            listOf(300 to "ש", 200 to "ר", 100 to "ק").forEach { (value, letter) ->
                if (remainder >= value) {
                    append(letter)
                    remainder -= value
                }
            }
            if (remainder == 15) {
                append("טו")
                remainder = 0
            } else if (remainder == 16) {
                append("טז")
                remainder = 0
            }
            listOf(
                90 to "צ", 80 to "פ", 70 to "ע", 60 to "ס", 50 to "נ",
                40 to "מ", 30 to "ל", 20 to "כ", 10 to "י", 9 to "ט",
                8 to "ח", 7 to "ז", 6 to "ו", 5 to "ה", 4 to "ד",
                3 to "ג", 2 to "ב", 1 to "א"
            ).forEach { (value, letter) ->
                if (remainder >= value) {
                    append(letter)
                    remainder -= value
                }
            }
        }

        if (!withGershayim) return raw
        return when (raw.length) {
            1 -> "$raw׳"
            2 -> "${raw[0]}״${raw[1]}"
            else -> raw
        }
    }
}
