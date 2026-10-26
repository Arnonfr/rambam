package com.example.domain.tehillim

import android.icu.util.Calendar
import android.icu.util.HebrewCalendar
import android.icu.util.TimeZone
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

data class HebrewBirthday(val year: Int, val month: Int, val day: Int)

enum class AdarBirthday { FIRST, SECOND }
enum class MissingBirthdayDay { NEXT_MONTH, LAST_DAY }

data class PersonalTehillimEntry(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "",
    val fixedChapter: Int? = null,
    val birthday: HebrewBirthday? = null,
    val adar: AdarBirthday = AdarBirthday.SECOND,
    val missingDay: MissingBirthdayDay = MissingBirthdayDay.NEXT_MONTH
) {
    init {
        require((fixedChapter != null) != (birthday != null))
        require(fixedChapter == null || fixedChapter in 1..150)
        birthday?.let { PersonalPsalmCalendar.toCivil(it) }
    }

    fun chapter(date: LocalDate): Int? {
        fixedChapter?.let { return it }
        val birth = birthday ?: return null
        if (date < PersonalPsalmCalendar.toCivil(birth)) return null
        val year = PersonalPsalmCalendar.fromCivil(date).year
        val age = year - birth.year - if (date < PersonalPsalmCalendar.anniversary(this, year)) 1 else 0
        return (age + 1).takeIf { it in 1..150 }
    }

    fun nextBirthday(date: LocalDate): LocalDate? {
        birthday ?: return null
        val year = PersonalPsalmCalendar.fromCivil(date).year
        val thisYear = PersonalPsalmCalendar.anniversary(this, year)
        return if (thisYear > date) thisYear else PersonalPsalmCalendar.anniversary(this, year + 1)
    }
}

/** ICU month indices stay stable across leap years (Adar I = 5, Adar = 6). */
object PersonalPsalmCalendar {
    val monthNames = listOf("תשרי", "חשוון", "כסלו", "טבת", "שבט", "אדר א׳", "אדר / אדר ב׳",
        "ניסן", "אייר", "סיוון", "תמוז", "אב", "אלול")

    fun isLeap(year: Int) = ((7 * year + 1) % 19) < 7

    fun readingDate(selected: LocalDate, effectiveToday: LocalDate,
        city: com.example.domain.sunset.CityLocation, now: java.time.Instant): LocalDate {
        if (selected != effectiveToday) return selected
        val zone = java.time.ZoneId.of(city.defaultZoneId)
        val today = now.atZone(zone).toLocalDate()
        val sunset = com.example.domain.sunset.SolarSunsetCalculator.calculateSunset(today, city, zone)
        return today.plusDays(if (sunset != null && !now.isBefore(sunset)) 1 else 0)
    }

    private fun calendar() = HebrewCalendar(TimeZone.getTimeZone("UTC")).apply { clear(); isLenient = false }

    fun daysInMonth(year: Int, month: Int): Int {
        toCivil(HebrewBirthday(year, month, 1))
        return calendar().apply { set(year, month, 1) }.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun fromCivil(date: LocalDate, afterSunset: Boolean = false): HebrewBirthday {
        val c = calendar()
        c.timeInMillis = (if (afterSunset) date.plusDays(1) else date).atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        return HebrewBirthday(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
    }

    fun toCivil(date: HebrewBirthday): LocalDate {
        require(date.year in 5000..7000 && date.month in 0..12 && date.day in 1..30)
        require(date.month != 5 || isLeap(date.year)) { "אדר א׳ קיים רק בשנה מעוברת" }
        val c = calendar()
        c.set(date.year, date.month, date.day, 12, 0, 0)
        val result = java.time.Instant.ofEpochMilli(c.timeInMillis).atZone(ZoneOffset.UTC).toLocalDate()
        require(fromCivil(result) == date) { "התאריך אינו קיים בלוח העברי" }
        return result
    }

    fun anniversary(entry: PersonalTehillimEntry, year: Int): LocalDate {
        val birth = requireNotNull(entry.birthday)
        var month = birth.month
        if (month == 5 && !isLeap(year)) month = 6
        if (month == 6 && !isLeap(birth.year) && isLeap(year)) {
            month = if (entry.adar == AdarBirthday.FIRST) 5 else 6
        }
        val first = toCivil(HebrewBirthday(year, month, 1))
        val candidate = first.plusDays((birth.day - 1).toLong())
        return if (fromCivil(candidate).month == month || entry.missingDay == MissingBirthdayDay.NEXT_MONTH)
            candidate else candidate.minusDays(1)
    }

    fun label(date: HebrewBirthday) = "${date.day} ${monthNames[date.month]} ${date.year}"
}
