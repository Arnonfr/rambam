package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.data.zmanim.*
import com.example.domain.zmanim.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class SingaviZmanimTest {
    private val date=LocalDate.parse("2026-10-23")
    private val location=ZmanimPreferences.defaultLocation
    private val fixture=javaClass.classLoader!!.getResourceAsStream("singavi-telaviv-2026-10-23.json")!!.bufferedReader().use { it.readText() }
    @org.junit.Before fun clearCaches() {
        val context=ApplicationProvider.getApplicationContext<android.content.Context>()
        listOf("singavi_published_v1", "singavi_city_matches_v1").forEach {
            context.getSharedPreferences(it,0).edit().clear().commit()
        }
    }
    private fun parse(raw:String=fixture, day:LocalDate=date, city:String="תל אביב", minutes:Int?=null)=SingaviPublishedDay.parse(raw,day,location,minutes,expectedCity=city)
    @Test fun `published Friday times match official table without changing visible versus true sunset`() {
        val day=parse()
        val expected=mapOf(ZmanId.DAWN to "04:45",ZmanId.SUNRISE to "06:44",ZmanId.SHEMA to "09:35",
            ZmanId.NOON to "12:25",ZmanId.MINCHA_GEDOLA to "12:53",ZmanId.MINCHA_KETANA to "15:44",
            ZmanId.PLAG to "16:56",ZmanId.SUNSET to "18:07",ZmanId.NIGHT to "18:27")
        expected.forEach { (id,value) -> assertEquals(value,day.times.single { it.id==id }.display(day.zone)) }
        assertEquals("17:45",day.candleLighting!!.result.display(day.zone))
        assertEquals(18,day.candleLighting!!.minutesBeforeSunset)
        assertEquals("זריחה אמיתית",ZmanId.SUNRISE.labelFor(day.method))
        assertEquals("שקיעה אמיתית",day.headerTimes(ZmanimPreferences(method=day.method)).last().second)
        assertNull(day.times.single { it.id==ZmanId.TALLIT }.rawInstant)
        assertNull(day.times.single { it.id==ZmanId.PRAYER }.rawInstant)
        assertNotEquals(day.times,ZmanimCalculator().calculate(date,location).times)
    }
    @Test fun `custom candle offset uses visible sunset rather than true sunset`() {
        val day=parse(minutes=22)
        assertEquals("17:41",day.candleLighting!!.result.display(day.zone))
    }
    @Test fun `different day city or clock season is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { parse(day=date.plusDays(1)) }
        assertThrows(IllegalArgumentException::class.java) { parse(city="ירושלים") }
        assertThrows(IllegalArgumentException::class.java) { parse(fixture.replace("קיץ","חורף")) }
    }
    @Test fun `method persists and old installations retain their original method`() {
        val context=ApplicationProvider.getApplicationContext<android.content.Context>()
        context.getSharedPreferences("zmanim_v1",0).edit().clear().commit()
        val store=ZmanimStore(context)
        assertEquals(ZmanimMethod.CHABAD_ORG,store.state.value.method)
        store.update(store.state.value.copy(method=ZmanimMethod.SINGAVI))
        assertEquals(ZmanimMethod.SINGAVI,ZmanimStore(context).state.value.method)
    }
    @Test fun `unsupported GPS historical date and city do not fall back to other calculations`() = runBlocking {
        val context=ApplicationProvider.getApplicationContext<android.content.Context>()
        context.getSharedPreferences("singavi_published_v1",0).edit().clear().commit()
        val repository=SingaviZmanimRepository(context)
        listOf(location.copy(gps=true),location.copy(name="Unknown town"),location).forEach { loc ->
            val day=repository.day(LocalDate.parse("2020-01-01"),loc,null,null)
            assertEquals(ZmanimMethod.SINGAVI,day.method)
            assertTrue(day.times.all { it.rawInstant==null }); assertNotNull(day.sourceMessage)
        }
    }

    @Test fun `live solar date stays civil today when study advances after sunset`() {
        val civil = LocalDate.parse("2026-10-23")
        assertEquals(civil, zmanimDisplayDate(civil.plusDays(1), civil.plusDays(1), civil))
        assertEquals(civil, zmanimDisplayDate(civil, civil, civil))
        assertEquals(civil.minusDays(2), zmanimDisplayDate(civil.minusDays(2), civil.plusDays(1), civil))
    }
    @Test fun `GPS uses only a uniquely matched published city and caches its actual date`() = runBlocking {
        val context=ApplicationProvider.getApplicationContext<android.content.Context>()
        context.getSharedPreferences("singavi_published_v1",0).edit().clear().commit()
        val urls=mutableListOf<String>()
        val repository=SingaviZmanimRepository(context, { url, body ->
            urls+=url
            if(body != null) """{"matches":[{"nameHe":"תל אביב"}]}""" else fixture
        }, { date })
        val day=repository.day(date,location.copy(gps=true,name="המיקום שלי"),null,null)
        assertEquals("06:44",day.times.single { it.id==ZmanId.SUNRISE }.display(day.zone))
        assertTrue(day.sourceMessage!!.contains("תל אביב"))
        assertTrue(urls.any { it.endsWith("/locate") })
        assertTrue(urls.any { it.contains("/zmanim/israel/") })
        val offline=SingaviZmanimRepository(context, { _, _ -> error("offline") }, { date.plusDays(1) })
        assertEquals(day.times, offline.day(date,location,null,null).times)
        assertEquals(day.times, offline.day(date,location.copy(gps=true,name="המיקום שלי"),null,null).times)
    }
    @Test fun `ambiguous GPS match and wrong server date never show another city's or day's times`() = runBlocking {
        val context=ApplicationProvider.getApplicationContext<android.content.Context>()
        context.getSharedPreferences("singavi_published_v1",0).edit().clear().commit()
        val ambiguous=SingaviZmanimRepository(context, { _, _ -> """{"matches":[{"nameHe":"תל אביב"},{"nameHe":"רמת גן"}]}""" }, { date })
        assertTrue(ambiguous.day(date,location.copy(gps=true),null,null).times.all { it.rawInstant == null })
        val stale=SingaviZmanimRepository(context, { _, _ -> fixture }, { date.plusDays(1) })
        assertTrue(stale.day(date.plusDays(1),location,null,null).times.all { it.rawInstant == null })
    }
    @Test fun `geocoder city names with country suffix match the fixed published city`() = runBlocking {
        val context=ApplicationProvider.getApplicationContext<android.content.Context>()
        context.getSharedPreferences("singavi_published_v1",0).edit().clear().commit()
        val repository=SingaviZmanimRepository(context, { _, _ -> fixture }, { date })
        val day=repository.day(date,location.copy(name="תל אביב–יפו, ישראל"),null,null)
        assertEquals("18:07",day.times.single { it.id==ZmanId.SUNSET }.display(day.zone))
    }
}
