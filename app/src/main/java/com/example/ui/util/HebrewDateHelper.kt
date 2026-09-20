package com.example.ui.util

import java.time.DayOfWeek
import java.time.LocalDate

object HebrewDateHelper {

    fun getHebrewDayOfWeekShort(dayOfWeek: DayOfWeek): String {
        return when (dayOfWeek) {
            DayOfWeek.SUNDAY -> "א׳"
            DayOfWeek.MONDAY -> "ב׳"
            DayOfWeek.TUESDAY -> "ג׳"
            DayOfWeek.WEDNESDAY -> "ד׳"
            DayOfWeek.THURSDAY -> "ה׳"
            DayOfWeek.FRIDAY -> "ו׳"
            DayOfWeek.SATURDAY -> "ש׳"
        }
    }

    fun getHebrewDayOfWeekName(dayOfWeek: DayOfWeek): String {
        return when (dayOfWeek) {
            DayOfWeek.SUNDAY -> "ראשון"
            DayOfWeek.MONDAY -> "שני"
            DayOfWeek.TUESDAY -> "שלישי"
            DayOfWeek.WEDNESDAY -> "רביעי"
            DayOfWeek.THURSDAY -> "חמישי"
            DayOfWeek.FRIDAY -> "שישי"
            DayOfWeek.SATURDAY -> "שבת קודש"
        }
    }

    fun getCivilMonthShortHebrew(monthValue: Int): String {
        return when (monthValue) {
            1 -> "ינו׳"
            2 -> "פבר׳"
            3 -> "מרץ"
            4 -> "אפר׳"
            5 -> "מאי"
            6 -> "יוני"
            7 -> "יולי"
            8 -> "אוג׳"
            9 -> "ספט׳"
            10 -> "אוק׳"
            11 -> "נוב׳"
            12 -> "דצמ׳"
            else -> ""
        }
    }

    fun getCivilMonthFullHebrew(monthValue: Int): String {
        return when (monthValue) {
            1 -> "בינואר"
            2 -> "בפברואר"
            3 -> "במרץ"
            4 -> "באפריל"
            5 -> "במאי"
            6 -> "ביוני"
            7 -> "ביולי"
            8 -> "באוגוסט"
            9 -> "בספטמבר"
            10 -> "באוקטובר"
            11 -> "בנובמבר"
            12 -> "בדצמבר"
            else -> ""
        }
    }

    fun formatCivilDateHebrew(date: LocalDate): String {
        return "${date.dayOfMonth} ${getCivilMonthFullHebrew(date.monthValue)}"
    }

    /**
     * Extracts Hebrew month and year from a full Hebrew date string like "ב׳ בתשרי תשפ״ז" -> "תשרי תשפ״ז"
     */
    fun extractHebrewMonthYear(fullHebrewDate: String): String {
        val parts = fullHebrewDate.trim().split(" ")
        return if (parts.size >= 3) {
            val month = parts[1].removePrefix("ב")
            "$month ${parts[2]}"
        } else {
            fullHebrewDate
        }
    }

    /**
     * Extracts Hebrew day portion from "ב׳ בתשרי תשפ״ז" -> "ב׳ בתשרי"
     */
    fun extractHebrewDay(fullHebrewDate: String): String {
        val parts = fullHebrewDate.trim().split(" ")
        return if (parts.size >= 2) {
            "${parts[0]} ${parts[1]}"
        } else {
            fullHebrewDate
        }
    }

    /**
     * Extracts only Hebrew day numeral from "ב׳ בתשרי תשפ״ז" -> "ב׳"
     */
    fun extractHebrewDayNumeral(fullHebrewDate: String): String {
        val parts = fullHebrewDate.trim().split(" ")
        return if (parts.isNotEmpty()) parts[0] else ""
    }

    /**
     * Extracts Hebrew month name without prefix: "ב׳ בתשרי תשפ״ז" -> "תשרי"
     */
    fun extractHebrewMonth(fullHebrewDate: String): String {
        val parts = fullHebrewDate.trim().split(" ")
        return if (parts.size >= 2) parts[1].removePrefix("ב") else ""
    }

    fun toHebrewNumeral(day: Int): String {
        return when (day) {
            1 -> "א׳"
            2 -> "ב׳"
            3 -> "ג׳"
            4 -> "ד׳"
            5 -> "ה׳"
            6 -> "ו׳"
            7 -> "ז׳"
            8 -> "ח׳"
            9 -> "ט׳"
            10 -> "י׳"
            11 -> "י״א"
            12 -> "י״ב"
            13 -> "י״ג"
            14 -> "י״ד"
            15 -> "ט״ו"
            16 -> "ט״ז"
            17 -> "י״ז"
            18 -> "י״ח"
            19 -> "י״ט"
            20 -> "כ׳"
            21 -> "כ״א"
            22 -> "כ״ב"
            23 -> "כ״ג"
            24 -> "כ״ד"
            25 -> "כ״ה"
            26 -> "כ״ו"
            27 -> "כ״ז"
            28 -> "כ״ח"
            29 -> "כ״ט"
            30 -> "ל׳"
            else -> "$day"
        }
    }
}
