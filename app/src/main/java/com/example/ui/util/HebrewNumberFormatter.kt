package com.example.ui.util

object HebrewNumberFormatter {
    /**
     * Converts an integer to Hebrew numeral letters (e.g. 1 -> א, 28 -> כח).
     * If withGershayim = true: 1 -> א׳, 28 -> כ״ח.
     * With withGershayim = false: 1 -> א, 28 -> כח (as requested: "הלכה א מתוך כח").
     */
    fun toHebrewNumeral(number: Int, withGershayim: Boolean = false): String {
        if (number <= 0) return number.toString()
        val tens = listOf("", "י", "כ", "ל", "מ", "נ", "ס", "ע", "פ", "צ")
        val units = listOf("", "א", "ב", "ג", "ד", "ה", "ו", "ז", "ח", "ט")

        val raw = when {
            number == 15 -> "טו"
            number == 16 -> "טז"
            number < 10 -> units[number]
            number in 10..99 -> {
                val t = number / 10
                val u = number % 10
                if (t == 1 && u == 5) "טו"
                else if (t == 1 && u == 6) "טז"
                else tens[t] + units[u]
            }
            number == 100 -> "ק"
            number in 101..199 -> "ק" + toHebrewNumeral(number - 100, false)
            else -> number.toString()
        }

        if (!withGershayim) return raw
        return when (raw.length) {
            1 -> "$raw׳"
            2 -> "${raw[0]}״${raw[1]}"
            else -> raw
        }
    }
}
