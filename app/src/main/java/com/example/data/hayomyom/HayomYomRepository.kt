package com.example.data.hayomyom

import android.content.Context
import android.icu.util.Calendar
import android.icu.util.HebrewCalendar
import android.icu.util.TimeZone
import com.example.domain.tanya.DailyTanyaLesson
import com.example.domain.tanya.TanyaSection
import com.example.ui.util.HebrewTextNormalizer
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneOffset

data class HayomYomLesson(
    val date: LocalDate,
    val dateLabel: String,
    val paragraphs: List<String>,
    val sourceUrl: String,
    val revisionId: Long
) {
    // Adapter for the shared paragraph reader; no Tanya print-layout controls.
    fun asReaderLesson(completed: Boolean = false) = DailyTanyaLesson(
        date = date.toString(), hebrewDate = dateLabel, fullRef = "hayom_yom:$dateLabel",
        heRef = dateLabel, bookTitle = "היום יום", chapterTitle = dateLabel,
        sections = paragraphs.mapIndexed { index, text -> TanyaSection(
            sectionIndex = index + 1, sectionHebrew = "",
            textWithNikud = text, textPlain = HebrewTextNormalizer.stripMarks(text)) },
        isCompleted = completed
    )
}

object HayomYomCalendar {
    fun key(date: LocalDate): String {
        // The caller already applies the user's midnight/sunset day boundary.
        // Noon UTC avoids locale, DST and device-time-zone date shifts.
        val calendar = HebrewCalendar(TimeZone.getTimeZone("UTC"))
        calendar.timeInMillis = date.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        val year = calendar.get(Calendar.YEAR)
        val month = when (calendar.get(Calendar.MONTH)) {
            HebrewCalendar.TISHRI -> "tishrei"
            HebrewCalendar.HESHVAN -> "cheshvan"
            HebrewCalendar.KISLEV -> "kislev"
            HebrewCalendar.TEVET -> "tevet"
            HebrewCalendar.SHEVAT -> "shevat"
            HebrewCalendar.ADAR_1 -> "adar1"
            HebrewCalendar.ADAR -> if ((7 * year + 1) % 19 < 7) "adar2" else "adar"
            HebrewCalendar.NISAN -> "nisan"
            HebrewCalendar.IYAR -> "iyar"
            HebrewCalendar.SIVAN -> "sivan"
            HebrewCalendar.TAMUZ -> "tammuz"
            HebrewCalendar.AV -> "av"
            HebrewCalendar.ELUL -> "elul"
            else -> error("Unexpected Hebrew calendar month")
        }
        return "$month-${calendar.get(Calendar.DAY_OF_MONTH)}"
    }
}

class HayomYomRepository(context: Context) {
    private val entries by lazy {
        context.assets.open("hayom_yom.json").bufferedReader().use {
            JSONObject(it.readText()).getJSONObject("entries")
        }
    }

    fun lessonFor(date: LocalDate): HayomYomLesson? {
        val entry = entries.optJSONObject(HayomYomCalendar.key(date)) ?: return null
        val paragraphs = entry.getJSONArray("paragraphs")
        return HayomYomLesson(date, entry.getString("dateLabel"),
            (0 until paragraphs.length()).map { paragraphs.getString(it) },
            entry.getString("sourceUrl"), entry.getLong("revisionId"))
    }
}
