package com.example

import com.example.data.prayers.Hallel
import com.example.data.prayers.PrayerDayRules
import java.time.LocalDate
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The day types of the planning workbook (sheet ״מטריצת ימים״), each on a real date. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PrayerDayRulesTest {
    @Test fun `every workbook scenario produces exactly its listed conditions`() {
        val json = JSONObject(javaClass.classLoader!!.getResource("prayer_day_scenarios.json").readText())
        val scenarios = json.getJSONArray("scenarios")
        val failures = mutableListOf<String>()
        for (i in 0 until scenarios.length()) {
            val s = scenarios.getJSONObject(i)
            val day = PrayerDayRules.of(LocalDate.parse(s.getString("date")))
            val flags = day.flags(minyan = false) + if (day.outOfScope != null) setOf("outOfScope") else emptySet()
            val expected = s.getJSONArray("flags").let { f -> (0 until f.length()).map { f.getString(it) }.toSet() }
            if (flags != expected) failures += "${s.getString("name")} ${s.getString("date")}: " +
                "missing ${expected - flags}, unexpected ${flags - expected} (${PrayerDayRules.describe(day)})"
        }
        assertEquals(emptyList<String>(), failures)
    }

    @Test fun `hebrew dates match known anchors`() {
        fun h(date: String) = PrayerDayRules.hebrew(LocalDate.parse(date)).let { Triple(it.year, it.month, it.day) }
        assertEquals(Triple(5787, PrayerDayRules.TISHREI, 1), h("2026-09-12"))
        assertEquals(Triple(5787, PrayerDayRules.TISHREI, 24), h("2026-10-05"))
        assertEquals(Triple(5787, PrayerDayRules.KISLEV, 25), h("2026-12-05"))
        assertEquals(Triple(5787, PrayerDayRules.ADAR_I, 14), h("2027-02-21"))
        assertEquals(LocalDate.parse("2027-03-23"), PrayerDayRules.civil(5787, PrayerDayRules.ADAR, 14))
    }

    @Test fun `postponed fast and manual tachanun override`() {
        // 9 Av 5789 falls on Shabbat (2029-07-21); the fast and its rules move to Sunday.
        val sunday = PrayerDayRules.of(LocalDate.parse("2029-07-22"))
        assertEquals("תשעה באב", sunday.fast)
        assertEquals(false, PrayerDayRules.of(LocalDate.parse("2026-10-21"), tachanunOverride = false).tachanun)
        assertEquals(Hallel.NONE, PrayerDayRules.of(LocalDate.parse("2026-10-21")).hallel)
    }
}
