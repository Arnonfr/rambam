package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.data.zmanim.OfflineTimezone
import com.example.data.zmanim.ZmanimStore
import com.example.domain.zmanim.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.*
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ZmanimTest {
    private val jerusalem = ZmanimLocation("ירושלים", 31.7683, 35.2137, "Asia/Jerusalem")
    private val newYork = ZmanimLocation("ניו יורק", 40.7128, -74.0060, "America/New_York")
    private val engine = ZmanimCalculator()
    @Test fun `all eleven events are chronological and deadlines never round late`() {
        val locations = listOf(jerusalem, newYork, ZmanimLocation("לונדון", 51.5074, -.1278, "Europe/London"),
            ZmanimLocation("פרדס חנה", 32.474, 34.977, "Asia/Jerusalem"))
        val dates = listOf("2026-03-08", "2026-03-27", "2026-03-29", "2026-06-21", "2026-10-25", "2026-11-01", "2026-12-21")
        locations.forEach { location -> dates.forEach { date ->
            val day = engine.calculate(LocalDate.parse(date), location)
            assertEquals(11, day.times.size)
            val available = day.times.mapNotNull { it.rawInstant }
            assertEquals(available.sorted(), available)
            day.times.forEach { event -> event.rawInstant?.let { raw ->
                if (event.id.latest) assertFalse(event.displayedInstant!! > raw)
                else if (event.id != ZmanId.NOON) assertFalse(event.displayedInstant!! < raw)
                assertTrue(Duration.between(raw, event.displayedInstant).abs().seconds < 60)
            } }
        } }
    }
    @Test fun `halachic anchors use degrees and mincha is at least thirty minutes after noon`() {
        listOf("2026-06-21", "2026-12-21").forEach { date ->
            val times = engine.calculate(LocalDate.parse(date), jerusalem).times.associate { it.id to it.rawInstant!! }
            assertTrue(Duration.between(times.getValue(ZmanId.NOON), times.getValue(ZmanId.MINCHA_GEDOLA)).seconds >= 1800)
            // True sunrise is earlier than the displayed sunrise, with a seasonal, non-fixed offset.
            val hour = Duration.between(times.getValue(ZmanId.SHEMA), times.getValue(ZmanId.PRAYER))
            val trueSunrise = times.getValue(ZmanId.SHEMA).minus(hour.multipliedBy(3))
            assertTrue(trueSunrise < times.getValue(ZmanId.SUNRISE))
            assertTrue(kotlin.math.abs(times.getValue(ZmanId.NOON).toEpochMilli() - trueSunrise.plus(hour.multipliedBy(6)).toEpochMilli()) <= 5)
        }
    }
    @Test fun `device timezone does not change selected foreign city or calendar day`() {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"))
            val first = engine.calculate(LocalDate.parse("2026-10-21"), newYork)
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Honolulu"))
            val second = ZmanimCalculator().calculate(first.date, newYork)
            assertEquals(first, second)
            assertEquals(first.date, first.times.first().rawInstant!!.atZone(first.zone).toLocalDate())
            assertNotEquals(first.times, engine.calculate(first.date.plusDays(1), newYork).times)
            assertNotEquals(first.times, engine.calculate(first.date, jerusalem).times)
        } finally { TimeZone.setDefault(original) }
    }
    @Test fun `polar events stay unavailable rather than inventing a substitute`() {
        val polar = ZmanimLocation("Tromso", 69.6492, 18.9553, "Europe/Oslo")
        val day = engine.calculate(LocalDate.parse("2026-06-21"), polar)
        assertTrue(day.times.all { it.rawInstant == null && it.display(day.zone) == "—" })
    }
    @Test fun `next event is among chosen events and only belongs to the selected local today`() {
        val day = engine.calculate(LocalDate.parse("2026-10-21"), jerusalem)
        val sunrise = day.times.first { it.id == ZmanId.SUNRISE }.rawInstant!!
        assertEquals(ZmanId.SUNRISE, day.nextAmong(ZmanId.defaults, sunrise.minusSeconds(60)))
        assertEquals(ZmanId.NOON, day.nextAmong(ZmanId.defaults, sunrise.plusSeconds(60)))
        assertNull(day.nextAmong(ZmanId.defaults, day.times.last().rawInstant!!.plusSeconds(60)))
        assertNull(day.nextAmong(ZmanId.defaults, sunrise.plusSeconds(86400)))
    }
    @Test fun `location selection and display order persist without reusing study city`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.getSharedPreferences("zmanim_v1", 0).edit().clear().commit()
        val store = ZmanimStore(context)
        assertEquals(ZmanimPreferences.defaultLocation,store.state.value.location)
        val sample = newYork.copy(gps = true, accuracyMeters = 6000f, capturedAt = 100L)
        val prefs = ZmanimPreferences(selected = listOf(ZmanId.NIGHT, ZmanId.SUNRISE), mode = "gps",
            fixedLocation = jerusalem, gpsLocation = sample)
        store.update(prefs)
        assertEquals(prefs, ZmanimStore(context).state.value)
        store.update(prefs.copy(enabled = false))
        assertEquals(prefs.selected, store.state.value.selected)
        assertEquals(jerusalem, store.state.value.fixedLocation)
        assertEquals("מיקום משוער", sample.status(Instant.ofEpochMilli(100)))
        assertEquals("מיקום אחרון", sample.status(Instant.ofEpochMilli(1_800_101)))
    }
    @Test fun `offline coordinate timezone lookup works across the world`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        assertEquals("Asia/Jerusalem", OfflineTimezone.at(context, 31.7683, 35.2137))
        assertEquals("America/New_York", OfflineTimezone.at(context, 40.7128, -74.006))
        assertEquals("Europe/London", OfflineTimezone.at(context, 51.5074, -.1278))
        assertEquals("Pacific/Auckland", OfflineTimezone.at(context, -36.8485, 174.7633))
    }
    @Test fun `display rounding preserves midnight and event civil date`() {
        val result = ZmanResult(ZmanId.NIGHT, Instant.parse("2026-10-21T23:59:59Z"))
        assertEquals("00:00", result.display(ZoneOffset.UTC))
        assertEquals(1L, result.displayedInstant!!.atZone(ZoneOffset.UTC).toLocalDate().toEpochDay() - LocalDate.parse("2026-10-21").toEpochDay())
    }
    @Test fun `fresh install defaults to Tel Aviv and migration preserves deliberate choices`() {
        val context=ApplicationProvider.getApplicationContext<android.content.Context>()
        val storage=context.getSharedPreferences("zmanim_v1",0)
        storage.edit().clear().commit()
        assertEquals(ZmanimPreferences.defaultLocation,ZmanimStore(context).state.value.location)
        assertFalse(ZmanimStore(context).state.value.showCivilDate)
        storage.edit().putString("preferences", """{"enabled":false,"selected":["SUNSET"],"mode":"fixed"}""").commit()
        val migrated=ZmanimStore(context)
        assertEquals(ZmanimPreferences.defaultLocation,migrated.state.value.location)
        assertFalse(migrated.state.value.enabled)
        assertEquals(listOf(ZmanId.SUNSET),migrated.state.value.selected)
        migrated.update(migrated.state.value.copy(fixedLocation=jerusalem,showCivilDate=true,scrollHintsShown=2,candleLightingMinutes=40,holidayInIsrael=false))
        val saved=ZmanimStore(context).state.value
        assertEquals(jerusalem,saved.location)
        assertTrue(saved.showCivilDate)
        assertEquals(2,saved.scrollHintsShown)
        assertEquals(40,saved.candleLightingMinutes)
        assertEquals(false,saved.holidayInIsrael)
    }
    @Test fun `Friday entry uses local custom and is prominent without removing selected times`() {
        val date=LocalDate.parse("2026-10-23")
        val telAviv=ZmanimPreferences.defaultLocation
        val day=engine.calculate(date,telAviv)
        val event=day.candleLighting!!
        assertEquals("כניסת שבת",event.label)
        assertFalse(event.afterNightfall)
        assertEquals(22,event.minutesBeforeSunset)
        val sunset=day.times.single{it.id==ZmanId.SUNSET}.rawInstant!!
        assertEquals(22*60L,Duration.between(event.result.rawInstant,sunset).seconds)
        assertTrue(event.result.displayedInstant!!<=event.result.rawInstant)
        assertEquals(ZmanId.CANDLES,day.headerTimes(ZmanimPreferences()).first().first.id)
        assertEquals(4,day.headerTimes(ZmanimPreferences()).size)
        assertEquals(3,day.headerTimes(ZmanimPreferences(showCandleLighting=false)).size)
        assertEquals(40,engine.calculate(date,jerusalem).candleLighting!!.minutesBeforeSunset)
        assertEquals(18,engine.calculate(date,newYork).candleLighting!!.minutesBeforeSunset)
        assertEquals(25,engine.calculate(date,telAviv,candleLightingMinutes=25).candleLighting!!.minutesBeforeSunset)
        assertNull(engine.calculate(LocalDate.parse("2026-10-21"),telAviv).candleLighting)
    }
    @Test fun `holiday candles distinguish Israel diaspora and nights following Shabbat or Yom Tov`() {
        val telAviv=ZmanimPreferences.defaultLocation
        val pesachEve=engine.calculate(LocalDate.parse("2026-04-01"),telAviv).candleLighting!!
        assertEquals("כניסת חג",pesachEve.label)
        assertFalse(pesachEve.afterNightfall)
        assertNull(engine.calculate(LocalDate.parse("2026-04-02"),telAviv).candleLighting)
        val secondNight=engine.calculate(LocalDate.parse("2026-04-02"),newYork).candleLighting!!
        assertTrue(secondNight.afterNightfall)
        assertEquals("נרות חג",secondNight.label)
        assertTrue(secondNight.result.displayedInstant!! >= secondNight.result.rawInstant)
        val afterShabbat=engine.calculate(LocalDate.parse("2026-09-12"),telAviv).candleLighting!!
        assertTrue(afterShabbat.afterNightfall)
        assertTrue(afterShabbat.result.rawInstant!! > engine.calculate(LocalDate.parse("2026-09-12"),telAviv).times.last().rawInstant)
        assertNull(engine.calculate(LocalDate.parse("2026-09-26"),telAviv).candleLighting)
        assertTrue(engine.calculate(LocalDate.parse("2026-09-26"),newYork).candleLighting!!.afterNightfall)
        assertTrue(engine.calculate(LocalDate.parse("2026-09-26"),telAviv,holidayInIsrael=false).candleLighting!!.afterNightfall)
        assertEquals("כניסת שבת",engine.calculate(LocalDate.parse("2026-05-22"),telAviv).candleLighting!!.label)
        assertNull(engine.calculate(LocalDate.parse("2026-12-06"),telAviv).candleLighting)
    }

}
