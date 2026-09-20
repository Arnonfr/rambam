package com.example.domain.schedule

import android.content.Context
import com.example.domain.sunset.CityLocation
import com.example.domain.sunset.SolarSunsetCalculator
import com.example.domain.sunset.SupportedCities
import org.json.JSONObject
import java.io.InputStreamReader
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class ScheduledChapter(
    val workId: String = "mishneh-torah",
    val sectionId: String,
    val sectionNameHebrew: String,
    val chapterNumber: Int,
    val chapterHebrew: String,
    val isBundledInSeferNashim: Boolean
)

data class DailyLessonResult(
    val civilDate: String, // YYYY-MM-DD
    val studyDate: String, // Date of the lesson (may be tomorrow if after sunset)
    val hebrewDate: String,
    val track: String, // "one" or "three"
    val chapters: List<ScheduledChapter>,
    val dayBoundaryUsed: String,
    val sunsetTimeFormatted: String?,
    val isAvailableInBundle: Boolean, // True if at least one or all chapters have text bundled
    val bundleNotice: String?
)

class DailyScheduleEngine(private val context: Context) {

    private val scheduleByDate = mutableMapOf<String, ScheduleDayData>()
    private var isInitialized = false

    private data class ScheduleDayData(
        val civilDate: String,
        val hebrewDate: String,
        val one: List<ScheduledChapter>,
        val three: List<ScheduledChapter>
    )

    private val hebrewNumeralMap = listOf(
        "", "א׳", "ב׳", "ג׳", "ד׳", "ה׳", "ו׳", "ז׳", "ח׳", "ט׳", "י׳",
        "י״א", "י״ב", "י״ג", "י״ד", "ט״ו", "ט״ז", "י״ז", "י״ח", "י״ט", "כ׳",
        "כ״א", "כ״ב", "כ״ג", "כ״ד", "כ״ה", "כ״ו", "כ״ז", "כ״ח", "כ״ט", "ל׳"
    )

    private fun formatChapterHebrew(num: Int): String {
        return if (num in 1 until hebrewNumeralMap.size) {
            "פרק ${hebrewNumeralMap[num]}"
        } else {
            "פרק $num"
        }
    }

    private fun isSectionInSeferNashim(sectionId: String): Boolean {
        return true
    }

    @Synchronized
    fun initialize() {
        if (isInitialized) return
        try {
            context.assets.open("rambam_schedule.json").use { inputStream ->
                InputStreamReader(inputStream, Charsets.UTF_8).use { reader ->
                    val jsonStr = reader.readText()
                    val root = JSONObject(jsonStr)
                    val daysObj = root.optJSONObject("days") ?: JSONObject()

                    val keys = daysObj.keys()
                    while (keys.hasNext()) {
                        val dateKey = keys.next()
                        val dayObj = daysObj.getJSONObject(dateKey)
                        val civDate = dayObj.optString("civilDate", dateKey)
                        val hebDate = dayObj.optString("hebrewDate", "")

                        val oneArray = dayObj.optJSONArray("one")
                        val oneList = mutableListOf<ScheduledChapter>()
                        if (oneArray != null) {
                            for (i in 0 until oneArray.length()) {
                                val item = oneArray.getJSONObject(i)
                                val secId = item.optString("sectionId")
                                val chNum = item.optInt("chapter", 1)
                                val secName = item.optString("sectionNameHebrew", getHebrewNameForSection(secId))
                                oneList.add(
                                    ScheduledChapter(
                                        sectionId = secId,
                                        sectionNameHebrew = secName,
                                        chapterNumber = chNum,
                                        chapterHebrew = formatChapterHebrew(chNum),
                                        isBundledInSeferNashim = isSectionInSeferNashim(secId)
                                    )
                                )
                            }
                        }

                        val threeArray = dayObj.optJSONArray("three")
                        val threeList = mutableListOf<ScheduledChapter>()
                        if (threeArray != null) {
                            for (i in 0 until threeArray.length()) {
                                val item = threeArray.getJSONObject(i)
                                val secId = item.optString("sectionId")
                                val chNum = item.optInt("chapter", 1)
                                val secName = item.optString("sectionNameHebrew", getHebrewNameForSection(secId))
                                threeList.add(
                                    ScheduledChapter(
                                        sectionId = secId,
                                        sectionNameHebrew = secName,
                                        chapterNumber = chNum,
                                        chapterHebrew = formatChapterHebrew(chNum),
                                        isBundledInSeferNashim = isSectionInSeferNashim(secId)
                                    )
                                )
                            }
                        }

                        scheduleByDate[civDate] = ScheduleDayData(
                            civilDate = civDate,
                            hebrewDate = hebDate,
                            one = oneList,
                            three = threeList
                        )
                    }
                }
            }
            isInitialized = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getHebrewNameForSection(sectionId: String): String {
        return when (sectionId) {
            "ishut" -> "הלכות אישות"
            "gerushin" -> "הלכות גירושין"
            "yibum" -> "הלכות ייבום וחליצה"
            "naarah" -> "הלכות נערה בתולה"
            "sotah" -> "הלכות סוטה"
            "shear-avot-hatumah" -> "הלכות שאר אבות הטומאות"
            "tumat-ochlin" -> "הלכות טומאת אוכלין"
            "keilim" -> "הלכות כלים"
            else -> sectionId
        }
    }

    /**
     * Core lookup function matching PRD: getDailyLesson(instant, location, track)
     */
    fun getDailyLesson(
        instant: Instant = Instant.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
        track: String = "one", // "one" or "three"
        dayBoundary: String = "midnight", // "midnight" or "sunset"
        cityName: String = "ירושלים"
    ): DailyLessonResult {
        initialize()

        val zonedDateTime = instant.atZone(zoneId)
        val currentCivilDate = zonedDateTime.toLocalDate()
        val currentCivilDateStr = currentCivilDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

        var sunsetFormatted: String? = null
        var targetStudyDate = currentCivilDate

        if (dayBoundary == "sunset") {
            val city = SupportedCities.findCityByName(cityName)
            val sunsetInstant = SolarSunsetCalculator.calculateSunset(currentCivilDate, city, zoneId)
            if (sunsetInstant != null) {
                val sunsetZoned = sunsetInstant.atZone(zoneId)
                sunsetFormatted = String.format("%02d:%02d", sunsetZoned.hour, sunsetZoned.minute)
                if (instant >= sunsetInstant) {
                    // It is after sunset: move to tomorrow's civil date (the new Jewish date!)
                    targetStudyDate = currentCivilDate.plusDays(1)
                }
            }
        }

        val targetStudyDateStr = targetStudyDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

        // Lookup in schedule
        val dayData = scheduleByDate[targetStudyDateStr] ?: getFallbackSchedule(targetStudyDate)

        val scheduledChapters = if (track == "three") dayData.three else dayData.one

        return DailyLessonResult(
            civilDate = currentCivilDateStr,
            studyDate = targetStudyDateStr,
            hebrewDate = dayData.hebrewDate,
            track = track,
            chapters = scheduledChapters,
            dayBoundaryUsed = dayBoundary,
            sunsetTimeFormatted = sunsetFormatted,
            isAvailableInBundle = true,
            bundleNotice = null
        )
    }

    private fun getFallbackSchedule(date: LocalDate): ScheduleDayData {
        // Fallback for dates outside 2026 pre-computed table
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val defaultChapter = ScheduledChapter(
            sectionId = "ishut",
            sectionNameHebrew = "הלכות אישות",
            chapterNumber = 17,
            chapterHebrew = "פרק י״ז",
            isBundledInSeferNashim = true
        )
        return ScheduleDayData(
            civilDate = dateStr,
            hebrewDate = "לוח לימוד יומי",
            one = listOf(defaultChapter),
            three = listOf(
                ScheduledChapter("mishneh-torah", "shear-avot-hatumah", "הלכות שאר אבות הטומאות", 18, "פרק י״ח", false),
                ScheduledChapter("mishneh-torah", "shear-avot-hatumah", "הלכות שאר אבות הטומאות", 19, "פרק י״ט", false),
                ScheduledChapter("mishneh-torah", "shear-avot-hatumah", "הלכות שאר אבות הטומאות", 20, "פרק כ׳", false)
            )
        )
    }

    fun getHebrewDate(date: LocalDate): String {
        initialize()
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        return scheduleByDate[dateStr]?.hebrewDate ?: "ב׳ בתשרי תשפ״ז"
    }

    fun getSunsetInstant(date: LocalDate, cityName: String, zoneId: ZoneId = ZoneId.systemDefault()): Instant? {
        val city = SupportedCities.findCityByName(cityName)
        return SolarSunsetCalculator.calculateSunset(date, city, zoneId)
    }
}
