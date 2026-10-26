package com.example.data.prayers

import android.icu.util.Calendar
import android.icu.util.HebrewCalendar
import android.icu.util.TimeZone
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset

data class HebrewDate(val year: Int, val month: Int, val day: Int) {
    val leap: Boolean get() = (7 * year + 1) % 19 < 7
}

enum class Hallel { NONE, PARTIAL, FULL }

/**
 * Weekday Shacharit conditions (Chabad custom) for one civil date. Shabbat and Yom Tov are outside
 * the supported range and are reported, never mapped to a weekday order. Local reasons to omit
 * Tachanun (a groom, a brit) cannot be derived from the date: [PrayerDayRules.of] takes an override.
 */
data class PrayerDay(
    val date: LocalDate, val hebrew: HebrewDate, val outOfScope: String?,
    val tachanun: Boolean, val tachanunReason: String?,
    val roshChodesh: Boolean, val cholHamoed: Boolean, val chanukah: Boolean,
    val elulLedavid: Boolean, val tenDaysOfRepentance: Boolean, val avinuMalkeinu: Boolean,
    val mizmorLetoda: Boolean, val hallel: Hallel, val musaf: Boolean,
    val fast: String?, val purim: Boolean
) {
    val weekday: DayOfWeek get() = date.dayOfWeek
    val mondayOrThursday: Boolean get() = weekday == DayOfWeek.MONDAY || weekday == DayOfWeek.THURSDAY
    val torahReading: Boolean get() = mondayOrThursday || roshChodesh || cholHamoed || chanukah || fast != null || purim

    /** Condition names used by assets/prayer_audio_order.json (and the review page). */
    fun flags(minyan: Boolean): Set<String> = buildSet {
        if (tachanun) add("tachanun")
        if (mondayOrThursday) add("mondayOrThursday")
        if (roshChodesh) add("roshChodesh")
        if (cholHamoed) add("cholHamoed")
        if (chanukah) add("chanukah")
        if (elulLedavid) add("elulLedavid")
        if (tenDaysOfRepentance) add("tenDaysOfRepentance")
        if (avinuMalkeinu) add("avinuMalkeinu")
        if (mizmorLetoda) add("mizmorLetoda")
        if (hallel != Hallel.NONE) add("hallel")
        if (hallel == Hallel.FULL) add("hallelFull")
        if (musaf) add("musaf")
        if (fast != null) add("fast")
        if (purim) add("purim")
        if (torahReading) add("torahReading")
        if (minyan) add("minyan")
    }
}

object PrayerDayRules {
    // android.icu month numbering: Tishrei 0 … Adar I 5, Adar (II) 6, Nisan 7 … Elul 12.
    const val TISHREI = 0; const val CHESHVAN = 1; const val KISLEV = 2; const val TEVET = 3
    const val SHEVAT = 4; const val ADAR_I = 5; const val ADAR = 6; const val NISAN = 7
    const val IYAR = 8; const val SIVAN = 9; const val TAMMUZ = 10; const val AV = 11; const val ELUL = 12

    private fun calendar() = HebrewCalendar(TimeZone.GMT_ZONE)

    fun hebrew(date: LocalDate): HebrewDate {
        val c = calendar()
        c.timeInMillis = date.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        return HebrewDate(c.get(Calendar.EXTENDED_YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
    }

    fun civil(year: Int, month: Int, day: Int): LocalDate {
        val c = calendar()
        c.clear()
        c.set(Calendar.EXTENDED_YEAR, year); c.set(Calendar.MONTH, month); c.set(Calendar.DAY_OF_MONTH, day)
        c.set(Calendar.HOUR_OF_DAY, 12)
        return java.time.Instant.ofEpochMilli(c.timeInMillis).atZone(ZoneOffset.UTC).toLocalDate()
    }

    /** The fast actually observed (postponed from Shabbat; Ta'anit Esther moved back to Thursday). */
    private fun fast(h: HebrewDate, date: LocalDate): String? {
        fun observed(month: Int, day: Int, forwardFromShabbat: Boolean): LocalDate {
            val nominal = civil(h.year, month, day)
            return if (nominal.dayOfWeek != DayOfWeek.SATURDAY) nominal
            else if (forwardFromShabbat) nominal.plusDays(1) else nominal.minusDays(2)
        }
        return when (date) {
            observed(TISHREI, 3, true) -> "צום גדליה"
            civil(h.year, TEVET, 10) -> "עשרה בטבת"
            observed(ADAR, 13, false) -> "תענית אסתר"
            observed(TAMMUZ, 17, true) -> "שבעה עשר בתמוז"
            observed(AV, 9, true) -> "תשעה באב"
            else -> null
        }
    }

    fun of(date: LocalDate, inIsrael: Boolean = true, tachanunOverride: Boolean? = null): PrayerDay {
        val h = hebrew(date)
        val (m, d) = h.month to h.day
        val yomTov = when {
            m == TISHREI && d in 1..2 -> "ראש השנה"
            m == TISHREI && d == 10 -> "יום הכיפורים"
            m == TISHREI && (d == 15 || (!inIsrael && d == 16)) -> "סוכות"
            m == TISHREI && (d == 22 || (!inIsrael && d == 23)) -> "שמיני עצרת ושמחת תורה"
            m == NISAN && (d == 15 || (!inIsrael && d == 16)) -> "פסח"
            m == NISAN && (d == 21 || (!inIsrael && d == 22)) -> "שביעי של פסח"
            m == SIVAN && (d == 6 || (!inIsrael && d == 7)) -> "שבועות"
            else -> null
        }
        val fast = fast(h, date)
        val outOfScope = when {
            date.dayOfWeek == DayOfWeek.SATURDAY -> "שבת"
            yomTov != null -> yomTov
            fast == "תשעה באב" -> "תשעה באב — סדר נפרד (קינות, תפילין במנחה) שאינו ממופה"
            else -> null
        }
        val roshChodesh = (d == 1 && m != TISHREI) || d == 30
        val cholHamoedSukkot = m == TISHREI && d in (if (inIsrael) 16 else 17)..21
        val cholHamoedPesach = m == NISAN && d in (if (inIsrael) 16 else 17)..20
        val cholHamoed = cholHamoedSukkot || cholHamoedPesach
        val chanukahStart = civil(h.year, KISLEV, 25)
        val chanukahDay = java.time.temporal.ChronoUnit.DAYS.between(chanukahStart, date)
        val chanukah = chanukahDay in 0..7
        val purimMonths = if (h.leap) setOf(ADAR_I, ADAR) else setOf(ADAR)
        val noTachanun: String? = when {
            roshChodesh -> "ראש חודש"
            chanukah -> "חנוכה"
            m == NISAN -> "חודש ניסן"
            m == TISHREI && d >= 9 -> "מערב יום הכיפורים עד סוף תשרי"
            m == ELUL && d == 29 -> "ערב ראש השנה"
            m == IYAR && d == 14 -> "פסח שני"
            m == IYAR && d == 18 -> "ל״ג בעומר"
            m == SIVAN && d <= 12 -> "ימי התשלומין של שבועות"
            m == AV && d == 15 -> "ט״ו באב"
            fast == "תשעה באב" -> "תשעה באב"
            m == SHEVAT && d == 15 -> "ט״ו בשבט"
            m in purimMonths && d in 14..15 -> if (m == ADAR_I) "פורים קטן" else "פורים"
            m == KISLEV && d in setOf(10, 19, 20) -> "י׳ / י״ט–כ׳ כסלו (מנהג חב״ד)"
            m == TAMMUZ && d in 12..13 -> "י״ב–י״ג תמוז (מנהג חב״ד)"
            else -> null
        }
        val tachanun = tachanunOverride ?: (noTachanun == null)
        val tenDays = m == TISHREI && d in 3..9
        return PrayerDay(
            date = date, hebrew = h, outOfScope = outOfScope,
            tachanun = tachanun, tachanunReason = if (tachanunOverride == false) "דילוג ידני" else noTachanun,
            roshChodesh = roshChodesh, cholHamoed = cholHamoed, chanukah = chanukah,
            elulLedavid = (m == AV && d == 30) || m == ELUL || (m == TISHREI && d <= 21),
            tenDaysOfRepentance = tenDays,
            avinuMalkeinu = (tenDays && d != 9) || (fast != null && fast != "תשעה באב"),
            mizmorLetoda = !cholHamoedPesach && !(m == NISAN && d == 14) && !(m == TISHREI && d == 9),
            hallel = when {
                chanukah || cholHamoedSukkot -> Hallel.FULL
                roshChodesh || cholHamoedPesach -> Hallel.PARTIAL
                else -> Hallel.NONE
            },
            musaf = roshChodesh || cholHamoed,
            fast = fast, purim = m == ADAR && d in 14..15
        )
    }

    private val monthNames = mapOf(TISHREI to "תשרי", CHESHVAN to "חשוון", KISLEV to "כסלו", TEVET to "טבת",
        SHEVAT to "שבט", NISAN to "ניסן", IYAR to "אייר", SIVAN to "סיוון", TAMMUZ to "תמוז", AV to "אב", ELUL to "אלול")

    fun monthName(h: HebrewDate): String = when (h.month) {
        ADAR_I -> "אדר א׳"
        ADAR -> if (h.leap) "אדר ב׳" else "אדר"
        else -> monthNames.getValue(h.month)
    }

    private fun numeral(n: Int): String {
        val ones = listOf("", "א", "ב", "ג", "ד", "ה", "ו", "ז", "ח", "ט")
        val body = when (n) { 15 -> "טו"; 16 -> "טז"; else -> listOf("", "י", "כ", "ל")[n / 10] + ones[n % 10] }
        return if (body.length == 1) "$body׳" else body.dropLast(1) + "״" + body.last()
    }

    /** One line for the audio section list, e.g. "שני · כ״ד תשרי · ללא תחנון (…)". */
    fun describe(day: PrayerDay): String = buildList {
        add(com.example.ui.util.HebrewDateHelper.getHebrewDayOfWeekName(day.weekday))
        add("${numeral(day.hebrew.day)} ${monthName(day.hebrew)}")
        day.outOfScope?.let { add("$it — מחוץ לתחום האודיו") } ?: run {
            add(if (day.tachanun) (if (day.mondayOrThursday) "תחנון מורחב" else "תחנון") else "ללא תחנון (${day.tachanunReason})")
            if (day.roshChodesh) add("ראש חודש")
            if (day.cholHamoed) add("חול המועד")
            if (day.chanukah) add("חנוכה")
            day.fast?.let { add(it) }
            if (day.elulLedavid) add("לדוד ה׳ אורי")
            if (day.tenDaysOfRepentance) add("עשרת ימי תשובה")
        }
    }.joinToString(" · ")
}
