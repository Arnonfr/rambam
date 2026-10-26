package com.example.widget

import android.content.Context
import com.example.data.chumash.ChumashRepository
import com.example.data.hayomyom.HayomYomRepository
import com.example.data.local.RambamDatabase
import com.example.data.local.UserPreferences
import com.example.data.local.UserPreferencesRepository
import com.example.data.mitzvot.SeferHamitzvotRepository
import com.example.data.tanya.TanyaRepository
import com.example.data.tehillim.TehillimRepository
import com.example.domain.mitzvot.MitzvahAssignmentType
import com.example.domain.mitzvot.SeferHamitzvotSchedule
import com.example.domain.schedule.DailyScheduleEngine
import com.example.ui.util.HebrewDateHelper
import com.example.ui.util.HebrewNumberFormatter
import com.example.ui.util.HebrewTextNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** The study day exactly as DailyRambamViewModel computes it (Israel time, midnight or sunset). */
object WidgetStudyDay {
    val zone: ZoneId = ZoneId.of("Asia/Jerusalem")

    fun today(engine: DailyScheduleEngine, prefs: UserPreferences, now: Instant = Instant.now()): LocalDate {
        val civil = now.atZone(zone).toLocalDate()
        if (prefs.dayBoundary != "sunset") return civil
        val sunset = engine.getSunsetInstant(civil, prefs.selectedCity, zone)
        return if (sunset != null && now.isAfter(sunset)) civil.plusDays(1) else civil
    }

    /** When the widget must switch to the next study day. */
    fun nextBoundary(engine: DailyScheduleEngine, prefs: UserPreferences, now: Instant = Instant.now()): Instant {
        val civil = now.atZone(zone).toLocalDate()
        val midnight = civil.plusDays(1).atStartOfDay(zone).toInstant()
        if (prefs.dayBoundary != "sunset") return midnight
        val todaySunset = engine.getSunsetInstant(civil, prefs.selectedCity, zone)
        if (todaySunset != null && now.isBefore(todaySunset)) return todaySunset
        return engine.getSunsetInstant(civil.plusDays(1), prefs.selectedCity, zone) ?: midnight
    }
}

class WidgetContentLoader(context: Context) {
    private val appContext = context.applicationContext

    suspend fun load(config: WidgetConfig): WidgetContent = withContext(Dispatchers.IO) {
        val prefs = UserPreferencesRepository(appContext).userPreferencesFlow.first()
        val engine = engine(appContext)
        val date = WidgetStudyDay.today(engine, prefs)
        val rambam = rambamTarget(prefs)
        val main = when (config.mainLesson) {
            WidgetMainLesson.HAYOM_YOM -> WidgetStudy.HAYOM_YOM
            WidgetMainLesson.TANYA -> WidgetStudy.TANYA
            WidgetMainLesson.CHUMASH -> WidgetStudy.CHUMASH
            WidgetMainLesson.TEHILLIM -> WidgetStudy.TEHILLIM
            WidgetMainLesson.RAMBAM -> rambam.takeUnless { it == WidgetStudy.MITZVOT } ?: WidgetStudy.RAMBAM_ONE
            WidgetMainLesson.MITZVOT -> WidgetStudy.MITZVOT
        }
        val text = runCatching { loadText(main, date, engine, prefs) }.getOrNull()
        val clean: (String) -> String = if (prefs.showNikud) HebrewTextNormalizer::forDisplay else HebrewTextNormalizer::stripMarks
        WidgetContent(
            main = main,
            title = if (main == WidgetStudy.MITZVOT) "ספר המצוות" else main.label,
            subtitle = text?.subtitle ?: shortHebrewDate(engine, date),
            blocks = text?.blocks.orEmpty().map { it.copy(text = forVilna(clean(it.text)).trim()) }.filter { it.text.isNotEmpty() },
            buttons = buttonsFor(main, rambam),
            unavailableMessage = if (text?.blocks.isNullOrEmpty()) unavailableMessage(main) else null
        )
    }

    private data class MainText(val subtitle: String, val blocks: List<WidgetBlock>)

    private suspend fun loadText(
        main: WidgetStudy,
        date: LocalDate,
        engine: DailyScheduleEngine,
        prefs: UserPreferences
    ): MainText? = when (main) {
        WidgetStudy.HAYOM_YOM -> HayomYomRepository(appContext).lessonFor(date)?.let { lesson ->
            MainText(lesson.dateLabel, HayomYomHebrew.paragraphs(lesson.paragraphs).map { WidgetBlock(it) })
        }
        WidgetStudy.TANYA -> withTimeoutOrNull(NETWORK_TIMEOUT_MS) {
            TanyaRepository(appContext).getDailyTanyaLesson(date)
        }?.let { lesson ->
            MainText(lesson.chapterTitle.ifBlank { lesson.heRef }, lesson.sections.map { WidgetBlock(it.textWithNikud) })
        }
        WidgetStudy.CHUMASH -> ChumashRepository(appContext).getDailyChumashLesson(date, prefs.completedAliyot)
            ?.let { lesson ->
                val aliya = lesson.todayAliya ?: return@let null
                MainText(
                    "פרשת ${lesson.parashaName} · ${aliya.aliyaName}",
                    aliya.verses.map { WidgetBlock("(${it.verseHebrew}) ${it.textPlainNikud}") }
                )
            }
        WidgetStudy.TEHILLIM -> {
            val hebrewDate = engine.getHebrewDate(date)
            val dayNumeral = HebrewDateHelper.extractHebrewDayNumeral(hebrewDate)
            val day = HebrewDateHelper.parseHebrewNumeral(dayNumeral)?.takeIf { it in 1..30 } ?: 1
            val lesson = TehillimRepository(appContext)
                .getDailyTehillimLesson(day, HebrewDateHelper.extractHebrewMonth(hebrewDate), dayNumeral)
            MainText(
                "יום ${lesson.dayOfMonthHebrew} לחודש",
                (lesson.chapters + lesson.elulChapters).flatMap { chapter ->
                    listOf(
                        WidgetBlock(chapter.chapterHebrew, isHeading = true),
                        WidgetBlock(chapter.verses.joinToString(" ") { it.textPlainNikud })
                    )
                }
            )
        }
        WidgetStudy.RAMBAM_ONE, WidgetStudy.RAMBAM_THREE -> {
            val lesson = engine.getDailyLesson(
                instant = date.atTime(12, 0).atZone(WidgetStudyDay.zone).toInstant(),
                zoneId = WidgetStudyDay.zone,
                track = main.key,
                dayBoundary = "midnight",
                cityName = prefs.selectedCity
            )
            val dao = RambamDatabase.getInstance(appContext).rambamDao()
            val blocks = lesson.chapters.flatMap { chapter ->
                val halachot = dao.getHalachotForChapterList("${chapter.sectionId}_${chapter.chapterNumber}")
                if (halachot.isEmpty()) emptyList()
                else listOf(WidgetBlock("${chapter.sectionNameHebrew} ${chapter.chapterHebrew}", isHeading = true)) +
                    halachot.map { WidgetBlock(it.textWithNikud) }
            }
            MainText(lesson.chapters.joinToString(" · ") { "${it.sectionNameHebrew} ${it.chapterHebrew}" }, blocks)
        }
        WidgetStudy.MITZVOT -> SeferHamitzvotSchedule.assignmentFor(date)?.let { assignment ->
            val entries = SeferHamitzvotRepository(appContext).getLessonEntries(assignment)
            MainText(
                "שיעור ${assignment.lessonNumber}",
                entries.flatMap { entry ->
                    val heading = when (entry.item.type) {
                        MitzvahAssignmentType.POSITIVE ->
                            "מצוות עשה ${HebrewNumberFormatter.toHebrewNumeral(entry.item.number ?: 0, true)}"
                        MitzvahAssignmentType.NEGATIVE ->
                            "מצוות לא תעשה ${HebrewNumberFormatter.toHebrewNumeral(entry.item.number ?: 0, true)}"
                        MitzvahAssignmentType.SPECIAL -> entry.item.title.orEmpty()
                    }
                    listOf(WidgetBlock(heading, isHeading = true)) + entry.paragraphs.map { WidgetBlock(it) }
                }
            )
        }
    }

    /** Romm Vilna's en and em dash glyphs are empty; a hyphen keeps the pause visible. */
    private fun forVilna(text: String) = text.replace('\u2013', '-').replace('\u2014', '-')

    private fun shortHebrewDate(engine: DailyScheduleEngine, date: LocalDate): String {
        val full = engine.getHebrewDate(date)
        return "${HebrewDateHelper.extractHebrewDayNumeral(full)} ${HebrewDateHelper.extractHebrewMonth(full)}".trim()
    }

    private fun unavailableMessage(main: WidgetStudy) = when (main) {
        WidgetStudy.RAMBAM_ONE, WidgetStudy.RAMBAM_THREE -> "פתחו את האפליקציה פעם אחת כדי להכין את טקסט הרמב״ם"
        WidgetStudy.TANYA -> "שיעור התניא של היום עדיין לא נטען. לחצו לפתיחה"
        else -> "השיעור אינו זמין כרגע. לחצו לפתיחת האפליקציה"
    }

    companion object {
        private const val NETWORK_TIMEOUT_MS = 10_000L

        @Volatile private var sharedEngine: DailyScheduleEngine? = null

        /** The schedule engine parses a 1 MB asset; keep one per process. */
        fun engine(context: Context): DailyScheduleEngine =
            sharedEngine ?: synchronized(this) {
                sharedEngine ?: DailyScheduleEngine(context.applicationContext).also { sharedEngine = it }
            }

        /** The Rambam lesson the user follows: the visible track, else Sefer HaMitzvot. */
        fun rambamTarget(prefs: UserPreferences): WidgetStudy {
            val visible = prefs.visibleStudies
            fun track(key: String) = if (key == "three") WidgetStudy.RAMBAM_THREE else WidgetStudy.RAMBAM_ONE
            return when {
                "rambam_${prefs.selectedTrack}" in visible -> track(prefs.selectedTrack)
                "rambam_one" in visible -> WidgetStudy.RAMBAM_ONE
                "rambam_three" in visible -> WidgetStudy.RAMBAM_THREE
                "mitzvot" in visible -> WidgetStudy.MITZVOT
                else -> track(prefs.selectedTrack)
            }
        }

        /** חומש | תהילים | תניא | רמב״ם, with the main lesson replaced by היום יום. */
        fun buttonsFor(main: WidgetStudy, rambam: WidgetStudy): List<WidgetButton> {
            return listOf(WidgetStudy.CHUMASH, WidgetStudy.TEHILLIM, WidgetStudy.TANYA, rambam).map { study ->
                val shown = if (study == main) WidgetStudy.HAYOM_YOM else study
                WidgetButton(shown, shown.label)
            }
        }
    }
}
