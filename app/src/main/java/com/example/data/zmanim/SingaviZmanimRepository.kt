package com.example.data.zmanim

import android.content.Context
import com.example.domain.zmanim.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.*
import java.time.format.DateTimeFormatter

/** Uses the Rabbi's published table, not an approximation presented as his calculations. */
class SingaviZmanimRepository(context: Context) {
    private val cache = context.getSharedPreferences("singavi_published_v1", Context.MODE_PRIVATE)
    private val cities = JSONArray(context.assets.open("zmanim/singavi-cities.json").bufferedReader().use { it.readText() })
    private fun norm(s: String) = s.replace(Regex("[׳״'\"–־-]"), "").replace(Regex("\\s+"), "").lowercase()
    private fun city(location: ZmanimLocation): JSONObject? {
        val aliases = mapOf("תלאביביפו" to "תלאביב", "ירושליים" to "ירושלים", "צפת" to "צפת")
        val name = aliases[norm(location.name)] ?: norm(location.name)
        return (0 until cities.length()).map { cities.getJSONObject(it) }.filter {
            norm(it.getString("he")) == name || norm(it.getString("en")) == name ||
                (it.getString("he").contains(',') && norm(it.getString("he").substringAfterLast(',')) == name)
        }.singleOrNull()
    }
    suspend fun day(date: LocalDate, location: ZmanimLocation, minutes: Int?, israel: Boolean?): ZmanimDay = withContext(Dispatchers.IO) {
        fun unavailable(message: String) = ZmanimDay(date, location,
            ZmanId.entries.filter { it != ZmanId.CANDLES }.map { ZmanResult(it, null) },
            candleLightingMinutesOverride=minutes, holidayInIsraelOverride=israel,
            method=ZmanimMethod.SINGAVI, sourceMessage=message)
        if(location.gps) return@withContext unavailable("לוח הרב סינגאווי מתפרסם לפי ערים. בחרו עיר קבועה בהעדפות זמני היום.")
        val city = city(location) ?: return@withContext unavailable("לא נמצא לוח הרב סינגאווי לעיר זו. אפשר לבחור עיר נתמכת בהעדפות או לפתוח את אתר הרב.")
        val key = "${city.getString("region")}:${city.getString("he")}:$date"
        val stored = cache.getString(key, null)
        fun parse(raw: String) = SingaviPublishedDay.parse(raw, date, location, minutes, israel, city.getString("he"))
        if(stored != null && System.currentTimeMillis()-cache.getLong("$key:at",0) < 15*60_000)
            runCatching { parse(stored) }.getOrNull()?.let { return@withContext it }
        if(date != LocalDate.now(ZoneId.of(location.timezoneId))) {
            return@withContext stored?.let { runCatching { parse(it) }.getOrNull() }
                ?: unavailable("לוח הרב סינגאווי לתאריך הזה אינו שמור במכשיר. האתר מספק את היום המתפרסם כעת בלבד.")
        }
        try {
            val url = "https://zmanei-halacha-backend.onrender.com/api/zmanim/${city.getString("region")}/${URLEncoder.encode(city.getString("he"),"UTF-8").replace("+", "%20") }"
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout=15_000; connection.readTimeout=20_000
            val raw = try {
                check(connection.responseCode == 200)
                connection.inputStream.bufferedReader().use { it.readText().also { text -> check(text.length < 100_000) } }
            } finally { connection.disconnect() }
            val result = parse(raw)
            if(cache.all.size > 120) cache.edit().clear().apply()
            cache.edit().putString(key,raw).putLong("$key:at",System.currentTimeMillis()).apply()
            result
        } catch(e: CancellationException) { throw e }
        catch(_: Exception) {
            stored?.let { runCatching { parse(it).copy(sourceMessage="לוח שמור לתאריך ולעיר המוצגים; העדכון מהאתר לא הצליח.") }.getOrNull() }
                ?: unavailable("לוח הרב סינגאווי טרם זמין לתאריך ולעיר האלה, או שאין חיבור. לא הוצגו זמנים משיטה אחרת. נסו לפתוח שוב בהמשך.")
        }
    }
}

object SingaviPublishedDay {
    fun parse(raw: String, date: LocalDate, location: ZmanimLocation, minutes: Int? = null, israel: Boolean? = null,
        expectedCity: String): ZmanimDay {
        val root = JSONObject(raw)
        require(root.getBoolean("success"))
        require(LocalDate.parse(root.getJSONObject("dateInfo").getString("gregorianDate"), DateTimeFormatter.ofPattern("d/M/uuuu")) == date)
        val data = root.getJSONObject("data")
        require(data.getString("cityNameHe") == expectedCity)
        val zone = ZoneId.of(location.timezoneId)
        val summer = zone.rules.isDaylightSavings(date.atTime(12,0).atZone(zone).toInstant())
        require(when(data.optString("clockType")) { "קיץ" -> summer; "חורף" -> !summer; else -> true })
        val times = data.getJSONObject("zmanim")
        fun time(key: String): Instant? {
            val text = times.optString(key)
            if(!text.matches(Regex("\\d{1,2}:\\d{2}"))) return null
            val parts = text.split(':').map(String::toInt)
            return date.atTime(parts[0], parts[1]).atZone(zone).toInstant()
        }
        val keys = mapOf(ZmanId.DAWN to "alotHashachar", ZmanId.SUNRISE to "zricha", ZmanId.SHEMA to "sofZmanKriatShma",
            ZmanId.NOON to "chatzot", ZmanId.MINCHA_GEDOLA to "minchaGedola", ZmanId.MINCHA_KETANA to "minchaKetana",
            ZmanId.PLAG to "plagHaMincha", ZmanId.SUNSET to "shkiaAmitit", ZmanId.NIGHT to "tzeitHaKochavim")
        val results = ZmanId.entries.filter { it != ZmanId.CANDLES }.map { ZmanResult(it, keys[it]?.let(::time)) }
        val calendar = com.kosherjava.zmanim.hebrewcalendar.JewishCalendar().apply {
            setGregorianDate(date.year,date.monthValue-1,date.dayOfMonth); inIsrael=israel ?: (location.timezoneId == "Asia/Jerusalem")
        }
        val candles = if(calendar.hasCandleLighting()) {
            val after = date.dayOfWeek != DayOfWeek.FRIDAY && calendar.isAssurBemelacha
            val count = minutes ?: 18
            // The weekly tzeitShabbat column can belong to tomorrow: never use it as today's time.
            val instant = if(after) time("tzeitHaKochavim")?.plusSeconds(240)
                else if(minutes != null || date.dayOfWeek != DayOfWeek.FRIDAY) time("shkiaNireit")?.minusSeconds(count*60L)
                else time("hadlakatNerot")
            CandleLightingEvent(ZmanResult(ZmanId.CANDLES,instant), if(date.dayOfWeek == DayOfWeek.FRIDAY) "כניסת שבת" else "נרות חג",after,count,
                if(after) "ארבע דקות אחרי צאת הכוכבים של היום, לפי סדר הכנסת שבת בלוח הרב. הדלקה מאש קיימת בלבד."
                else "$count דקות לפני השקיעה הנראית לפי לוח הרב סינגאווי.")
        } else null
        return ZmanimDay(date,location,results,candles,minutes,israel,ZmanimMethod.SINGAVI,
            "לוח הרב סינגאווי · הזריחה והשקיעה המוצגות הן האמיתיות. ציצית ותפילין וסוף תפילה אינם מסופקים בלוח.")
    }
}
