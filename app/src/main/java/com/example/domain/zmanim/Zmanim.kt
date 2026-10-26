package com.example.domain.zmanim

import com.kosherjava.zmanim.AstronomicalCalendar
import com.kosherjava.zmanim.util.GeoLocation
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.TimeZone

const val ZMANIM_SOURCE = "https://www.chabad.org/library/article_cdo/aid/3209349/jewish/About-Our-Zmanim-Calculations.htm"
const val ZMANIM_PROFILE = "חב״ד — לפי Chabad.org"
const val ZMANIM_VERSION = "chabad-org-2/rounding-1/kosherjava-2.5.0"

enum class ZmanimMethod(val label: String, val source: String) {
    CHABAD_ORG(ZMANIM_PROFILE, ZMANIM_SOURCE),
    SINGAVI("אדמו״ר הזקן — הרב סינגאווי", "https://zmanim.org.il/");
    companion object { fun from(value: String?) = entries.find { it.name == value } ?: CHABAD_ORG }
}

fun ZmanId.labelFor(method: ZmanimMethod): String = if(method == ZmanimMethod.SINGAVI) when(this) {
    ZmanId.SUNRISE -> "זריחה אמיתית"
    ZmanId.SUNSET -> "שקיעה אמיתית"
    else -> label
} else label

fun ZmanId.explanationFor(method: ZmanimMethod): String = if(method == ZmanimMethod.SINGAVI) when(this) {
    ZmanId.DAWN -> "עלות השחר לפי לוח הרב סינגאווי; שיטת 120 דקות בימים השווים, עם שינוי לפי המקום והעונה."
    ZmanId.SUNRISE -> "הזריחה האמיתית בלוח הרב: ארבע דקות לפני הזריחה הנראית, המתחשבת בגובה העיר."
    ZmanId.SUNSET -> "השקיעה האמיתית בלוח הרב: ארבע דקות אחרי השקיעה הנראית. לתוספת שבת יש להפסיק מלאכה כבר בשקיעה הנראית."
    ZmanId.SHEMA -> "רבע היום מהזריחה האמיתית עד השקיעה האמיתית."
    ZmanId.NOON -> "אמצע היום מהזריחה האמיתית עד השקיעה האמיתית."
    ZmanId.MINCHA_GEDOLA -> "חצי שעה זמנית אחרי חצות."
    ZmanId.PLAG -> "שעה ורבע זמניות לפני השקיעה האמיתית."
    ZmanId.NIGHT -> "שלושה כוכבים בינוניים לפי סדר הכנסת שבת, כפי שמפורסם בלוח הרב; אינו זמן צאת שבת."
    ZmanId.TALLIT, ZmanId.PRAYER -> "זמן זה אינו מסופק בממשק הלוח. לא הופעלה שיטת תחליף."
    else -> "כפי שמפורסם בלוח הרב סינגאווי."
} else this.method

enum class ZmanId(val label: String, val shortLabel: String, val method: String, val latest: Boolean = false) {
    DAWN("עלות השחר", "עלות", "מרכז השמש 16.9° מתחת לאופק בבוקר"),
    TALLIT("ציצית ותפילין", "ציצית ותפילין", "מרכז השמש 10.2° מתחת לאופק בבוקר"),
    SUNRISE("זריחה", "זריחה", "מרכז השמש 0.833° מתחת לאופק, בגובה פני הים"),
    SHEMA("סוף זמן קריאת שמע", "סוף ק״ש", "נץ אמיתי (1.583°) ועוד 3 שעות זמניות", true),
    PRAYER("סוף זמן תפילה", "סוף תפילה", "נץ אמיתי (1.583°) ועוד 4 שעות זמניות", true),
    NOON("חצות היום", "חצות", "אמצע פרק הזמן בין נץ אמיתי לשקיעה אמיתית"),
    MINCHA_GEDOLA("מנחה גדולה", "מנחה גדולה", "המאוחר מבין 6.5 שעות זמניות לבין חצות ועוד 30 דקות"),
    MINCHA_KETANA("מנחה קטנה", "מנחה קטנה", "נץ אמיתי ועוד 9.5 שעות זמניות"),
    PLAG("פלג המנחה", "פלג", "שעה ורבע זמניות לפני השקיעה האמיתית"),
    SUNSET("שקיעה", "שקיעה", "מרכז השמש 0.833° מתחת לאופק, בגובה פני הים", true),
    NIGHT("צאת הכוכבים", "צאת הכוכבים", "מרכז השמש 6° מתחת לאופק בערב"),
    CANDLES("כניסת שבת וחג", "כניסת שבת", "הדלקת נרות לפי מנהג המקום", true);

    companion object { val defaults = listOf(SUNRISE, NOON, SUNSET) }
}

data class ZmanimLocation(
    val name: String, val latitude: Double, val longitude: Double, val timezoneId: String,
    val gps: Boolean = false, val accuracyMeters: Float? = null, val capturedAt: Long? = null
) {
    init {
        require(latitude.isFinite() && latitude in -90.0..90.0)
        require(longitude.isFinite() && longitude in -180.0..180.0)
        ZoneId.of(timezoneId)
    }
    fun status(now: Instant): String = when {
        !gps -> "לפי היישוב $name"
        capturedAt == null || now.toEpochMilli() - capturedAt > 30 * 60_000 -> "מיקום אחרון"
        accuracyMeters == null || accuracyMeters > 5000 -> "מיקום משוער"
        else -> "המיקום שלי"
    }
}

data class ZmanimPreferences(
    val enabled: Boolean = true,
    val selected: List<ZmanId> = ZmanId.defaults,
    val highlightNext: Boolean = true,
    val mode: String = "fixed",
    val fixedLocation: ZmanimLocation? = defaultLocation,
    val gpsLocation: ZmanimLocation? = null,
    val showCivilDate: Boolean = false,
    val showCandleLighting: Boolean = true,
    val candleLightingMinutes: Int? = null,
    val holidayInIsrael: Boolean? = null,
    val scrollHintsShown: Int = 0,
    val method: ZmanimMethod = ZmanimMethod.CHABAD_ORG
) {
    val location: ZmanimLocation? get() = if (mode == "gps") gpsLocation else fixedLocation
    companion object {
        val defaultLocation = ZmanimLocation("תל אביב–יפו", 32.0853, 34.7818, "Asia/Jerusalem")
    }
}

data class ZmanResult(val id: ZmanId, val rawInstant: Instant?, val roundUp: Boolean = !id.latest) {
    // Deadlines round down, earliest times round up. No hidden safety offset.
    val displayedInstant: Instant? get() = rawInstant?.let { raw ->
        val seconds = Math.floorDiv(raw.epochSecond, 60) * 60
        Instant.ofEpochSecond(when {
            id == ZmanId.NOON -> if (raw.epochSecond - seconds >= 30) seconds + 60 else seconds
            !roundUp || (raw.epochSecond == seconds && raw.nano == 0) -> seconds
            else -> seconds + 60
        })
    }
    fun display(zone: ZoneId): String = displayedInstant?.atZone(zone)?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "—"
    fun dayOffset(date: LocalDate, zone: ZoneId): Long? = displayedInstant?.atZone(zone)?.toLocalDate()?.toEpochDay()?.minus(date.toEpochDay())
    fun isUpcoming(now: Instant): Boolean = displayedInstant?.let { now < it } == true
}

data class CandleLightingEvent(val result: ZmanResult, val label: String, val afterNightfall: Boolean,
    val minutesBeforeSunset: Int, val sourceExplanation: String? = null) {
    val explanation: String get() = sourceExplanation ?: if (afterNightfall)
        "הדלקת נרות חג אחרי צאת השבת או החג (8.5°), מאש קיימת בלבד."
        else "$minutesBeforeSunset דקות לפני השקיעה, לפי הגדרת מנהג המקום."
}

object CandleLightingRules {
    fun defaultMinutes(location: ZmanimLocation): Int {
        // Match named locality reference points; never claim a universal Israeli offset.
        fun near(lat: Double, lon: Double) = kotlin.math.abs(location.latitude-lat) < .045 && kotlin.math.abs(location.longitude-lon) < .045
        return when {
            location.timezoneId != "Asia/Jerusalem" -> 18
            near(31.7683,35.2137) -> 40
            near(32.7940,34.9896) -> 30
            near(32.0853,34.7818) -> 22
            else -> 18
        }
    }
}

data class ZmanimDay(val date: LocalDate, val location: ZmanimLocation, val times: List<ZmanResult>,
    val candleLighting: CandleLightingEvent? = null,
    val candleLightingMinutesOverride: Int? = null, val holidayInIsraelOverride: Boolean? = null,
    val method: ZmanimMethod = ZmanimMethod.CHABAD_ORG, val sourceMessage: String? = null) {
    val zone: ZoneId get() = ZoneId.of(location.timezoneId)
    fun next(now: Instant): ZmanId? = if (date == now.atZone(zone).toLocalDate())
        times.filter { it.isUpcoming(now) }.minByOrNull { it.rawInstant!! }?.id else null
    fun nextAmong(selected: List<ZmanId>, now: Instant): ZmanId? = if (date == now.atZone(zone).toLocalDate())
        times.filter { it.id in selected && it.isUpcoming(now) }.minByOrNull { it.rawInstant!! }?.id else null
}

class ZmanimCalculator {
    // Cache identity includes coordinates, local date, zone and the formula/rounding version.
    private data class Key(val date: LocalDate, val lat: Double, val lon: Double, val zone: String, val profile: String)
    private val cache = object : LinkedHashMap<Key, List<ZmanResult>>(32, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Key, List<ZmanResult>>?) = size > 32
    }
    @Synchronized fun calculate(date: LocalDate, location: ZmanimLocation,
        candleLightingMinutes: Int? = null, holidayInIsrael: Boolean? = null): ZmanimDay {
        val key = Key(date, location.latitude, location.longitude, location.timezoneId, ZMANIM_VERSION)
        val results = cache.getOrPut(key) {
            val tz = TimeZone.getTimeZone(location.timezoneId)
            val calendar = AstronomicalCalendar(GeoLocation(location.name, location.latitude, location.longitude, 0.0, tz))
            calendar.calendar = Calendar.getInstance(tz).apply {
                clear(); set(date.year, date.monthValue - 1, date.dayOfMonth, 12, 0, 0)
            }
            fun rise(degrees: Double) = calendar.getSunriseOffsetByDegrees(90.0 + degrees)?.toInstant()
            fun set(degrees: Double) = calendar.getSunsetOffsetByDegrees(90.0 + degrees)?.toInstant()
            val trueRise = rise(1.583)
            val trueSet = set(1.583)
            fun hour(h: Double): Instant? = if (trueRise != null && trueSet != null)
                trueRise.plusMillis((Duration.between(trueRise, trueSet).toMillis() * h / 12).toLong()) else null
            val noon = hour(6.0)
            val mincha = hour(6.5)?.let { maxOf(it, noon!!.plusSeconds(1800)) }
            val values = listOf(rise(16.9), rise(10.2), rise(.833), hour(3.0), hour(4.0), noon,
                mincha, hour(9.5), hour(10.75), set(.833), set(6.0))
            ZmanId.entries.filter { it != ZmanId.CANDLES }.zip(values).map { (id, instant) -> ZmanResult(id, instant) }
        }
        val jewishDate = com.kosherjava.zmanim.hebrewcalendar.JewishCalendar().apply {
            setGregorianDate(date.year, date.monthValue - 1, date.dayOfMonth)
            inIsrael = holidayInIsrael ?: (location.timezoneId == "Asia/Jerusalem")
        }
        val candles = if (jewishDate.hasCandleLighting()) {
            val afterNightfall = date.dayOfWeek != DayOfWeek.FRIDAY && jewishDate.isAssurBemelacha
            val minutes = (candleLightingMinutes ?: CandleLightingRules.defaultMinutes(location)).coerceIn(5, 60)
            val raw = if (afterNightfall) {
                AstronomicalCalendar(GeoLocation(location.name, location.latitude, location.longitude, 0.0,
                    TimeZone.getTimeZone(location.timezoneId))).apply {
                    calendar = Calendar.getInstance(TimeZone.getTimeZone(location.timezoneId)).apply {
                        clear(); set(date.year,date.monthValue-1,date.dayOfMonth,12,0,0)
                    }
                }.getSunsetOffsetByDegrees(98.5)?.toInstant()
            } else results.first { it.id == ZmanId.SUNSET }.rawInstant?.minusSeconds(minutes * 60L)
            CandleLightingEvent(ZmanResult(ZmanId.CANDLES, raw, roundUp = afterNightfall), when {
                date.dayOfWeek == DayOfWeek.FRIDAY -> "כניסת שבת"
                afterNightfall -> "נרות חג"
                else -> "כניסת חג"
            }, afterNightfall, minutes)
        } else null
        return ZmanimDay(date, location, results, candles, candleLightingMinutes, holidayInIsrael)
    }
}

/** Selected ordinary times retain their order; the relevant entry time is visible first. */
fun ZmanimDay.headerTimes(preferences: ZmanimPreferences): List<Pair<ZmanResult, String>> = buildList {
    if (preferences.showCandleLighting) candleLighting?.let { add(it.result to it.label) }
    preferences.selected.filter { it != ZmanId.CANDLES }.forEach { id ->
        times.find { it.id == id }?.let { add(it to if(preferences.method == ZmanimMethod.SINGAVI && id in listOf(ZmanId.SUNRISE, ZmanId.SUNSET)) id.labelFor(preferences.method) else id.shortLabel) }
    }
}
