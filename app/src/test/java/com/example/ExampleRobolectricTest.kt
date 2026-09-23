package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.schedule.DailyScheduleEngine
import com.example.domain.sunset.CityLocation
import com.example.domain.sunset.SolarSunsetCalculator
import com.example.domain.sunset.SupportedCities
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

    @Test
    fun `read string from context matches app name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertTrue(appName.isNotBlank())
    }

    @Test
    fun `verify PRD acceptance test date 2026-09-13 is Ishut Chapter 17`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = DailyScheduleEngine(context)

        // 2026-09-13 noon Jerusalem time
        val instant = LocalDate.of(2026, 9, 13).atTime(12, 0).atZone(ZoneId.of("Asia/Jerusalem")).toInstant()
        val lesson = engine.getDailyLesson(
            instant = instant,
            zoneId = ZoneId.of("Asia/Jerusalem"),
            track = "one",
            dayBoundary = "midnight"
        )

        assertEquals("2026-09-13", lesson.civilDate)
        assertEquals("2026-09-13", lesson.studyDate)
        assertEquals(1, lesson.chapters.size)
        val chap = lesson.chapters[0]
        assertEquals("ishut", chap.sectionId)
        assertEquals(17, chap.chapterNumber)
        assertTrue(chap.isBundledInSeferNashim)
    }

    @Test
    fun `verify PRD acceptance test date 2026-09-22 is Gerushin Chapter 1`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = DailyScheduleEngine(context)

        val instant = LocalDate.of(2026, 9, 22).atTime(12, 0).atZone(ZoneId.of("Asia/Jerusalem")).toInstant()
        val lesson = engine.getDailyLesson(
            instant = instant,
            zoneId = ZoneId.of("Asia/Jerusalem"),
            track = "one",
            dayBoundary = "midnight"
        )

        assertEquals("2026-09-22", lesson.civilDate)
        assertEquals("2026-09-22", lesson.studyDate)
        assertEquals(1, lesson.chapters.size)
        val chap = lesson.chapters[0]
        assertEquals("gerushin", chap.sectionId)
        assertEquals(1, chap.chapterNumber)
        assertTrue(chap.isBundledInSeferNashim)
    }

    @Test
    fun `verify three chapters track for 2026-09-13 is Shear Avot HaTumah 18 to 20`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = DailyScheduleEngine(context)

        val instant = LocalDate.of(2026, 9, 13).atTime(12, 0).atZone(ZoneId.of("Asia/Jerusalem")).toInstant()
        val lesson = engine.getDailyLesson(
            instant = instant,
            zoneId = ZoneId.of("Asia/Jerusalem"),
            track = "three",
            dayBoundary = "midnight"
        )

        assertEquals(3, lesson.chapters.size)
        assertEquals("shear_avot_hatumah", lesson.chapters[0].sectionId)
        assertEquals(18, lesson.chapters[0].chapterNumber)
        assertEquals(19, lesson.chapters[1].chapterNumber)
        assertEquals(20, lesson.chapters[2].chapterNumber)
    }

    @Test
    fun `verify solar sunset calculation for Jerusalem`() {
        val jerusalem = SupportedCities.findCityByName("ירושלים")
        val testDate = LocalDate.of(2026, 9, 13)
        val sunset = SolarSunsetCalculator.calculateSunset(testDate, jerusalem, ZoneId.of("Asia/Jerusalem"))
        assertNotNull(sunset)
        val zdt = sunset!!.atZone(ZoneId.of("Asia/Jerusalem"))
        // Sunset in Jerusalem in mid-September is around 18:40 - 18:50
        assertEquals(18, zdt.hour)
        assertTrue(zdt.minute in 35..55)
    }

    @Test
    fun `verify 2026-09-20 Chumash is Vzot HaBracha Aliya Rishon`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = com.example.data.chumash.ChumashRepository(context)
        val lesson = repo.getDailyChumashLesson(LocalDate.of(2026, 9, 20))
        assertNotNull(lesson)
        assertEquals("וזאת הברכה", lesson!!.parashaName)
        assertEquals(1, lesson.currentAliyaIndex)
        assertEquals("ראשון", lesson.todayAliya?.aliyaName)
    }

    @Test
    fun `verify 2026-09-20 Tanya loads 9 Tishrei lesson`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = com.example.data.tanya.TanyaRepository(context)
        val lesson = repo.getDailyTanyaLesson(LocalDate.of(2026, 9, 20))
        assertNotNull(lesson)
        assertEquals("2026-09-20", lesson!!.date)
        assertTrue(lesson.sections.isNotEmpty())
    }
}
