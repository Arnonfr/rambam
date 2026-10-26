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
}
