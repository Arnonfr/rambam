package com.example.domain.sunset

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.*

data class CityLocation(
    val nameHebrew: String,
    val nameEnglish: String,
    val latitude: Double,
    val longitude: Double,
    val defaultZoneId: String
)

object SupportedCities {
    val CITIES = listOf(
        CityLocation("ירושלים", "Jerusalem", 31.7683, 35.2137, "Asia/Jerusalem"),
        CityLocation("בני ברק", "Bnei Brak", 32.0833, 34.8333, "Asia/Jerusalem"),
        CityLocation("תל אביב - יפו", "Tel Aviv", 32.0853, 34.7818, "Asia/Jerusalem"),
        CityLocation("חיפה", "Haifa", 32.7940, 34.9896, "Asia/Jerusalem"),
        CityLocation("באר שבע", "Beer Sheva", 31.2529, 34.7915, "Asia/Jerusalem"),
        CityLocation("צפת", "Safed", 32.9646, 35.4960, "Asia/Jerusalem"),
        CityLocation("אשדוד", "Ashdod", 31.8014, 34.6435, "Asia/Jerusalem"),
        CityLocation("מודיעין עילית", "Modiin Illit", 31.9328, 35.0442, "Asia/Jerusalem"),
        CityLocation("ביתר עילית", "Beitar Illit", 31.6989, 35.1147, "Asia/Jerusalem"),
        CityLocation("ניו יורק", "New York", 40.7128, -74.0060, "America/New_York"),
        CityLocation("לונדון", "London", 51.5074, -0.1278, "Europe/London"),
        CityLocation("פריז", "Paris", 48.8566, 2.3522, "Europe/Paris")
    )

    fun findCityByName(name: String): CityLocation {
        return CITIES.find { it.nameHebrew == name || it.nameEnglish.equals(name, ignoreCase = true) }
            ?: CITIES[0] // Default to Jerusalem
    }
}

object SolarSunsetCalculator {

    /**
     * Calculates the sunset instant for a given date and location using the standard NOAA algorithm.
     * Zenith is 90.833° (standard optical sunset taking refraction into account).
     */
    fun calculateSunset(date: LocalDate, city: CityLocation, zoneId: ZoneId): Instant? {
        val dayOfYear = date.dayOfYear
        val isLeapYear = date.isLeapYear
        val totalDays = if (isLeapYear) 366.0 else 365.0

        // Fractional year in radians
        val gamma = (2.0 * Math.PI / totalDays) * (dayOfYear - 1)

        // Equation of time in minutes
        val eqTime = 229.18 * (
            0.000075 +
            0.001868 * cos(gamma) - 0.032077 * sin(gamma) -
            0.014615 * cos(2.0 * gamma) - 0.040849 * sin(2.0 * gamma)
        )

        // Solar declination angle in radians
        val decl = (
            0.006918 -
            0.399912 * cos(gamma) + 0.070257 * sin(gamma) -
            0.006758 * cos(2.0 * gamma) + 0.000907 * sin(2.0 * gamma) -
            0.002697 * cos(3.0 * gamma) + 0.00148 * sin(3.0 * gamma)
        )

        val latRad = Math.toRadians(city.latitude)
        val zenithRad = Math.toRadians(90.833) // Standard sunrise/sunset zenith

        // Hour angle for sunset
        val cosHourAngle = (cos(zenithRad) / (cos(latRad) * cos(decl))) - (tan(latRad) * tan(decl))

        // Check for polar day / night
        if (cosHourAngle > 1.0 || cosHourAngle < -1.0) {
            return null
        }

        val hourAngleDeg = Math.toDegrees(acos(cosHourAngle))

        // Time of sunset in minutes from UTC midnight
        val timeUtcMinutes = 720.0 - (4.0 * city.longitude) - eqTime + (4.0 * hourAngleDeg)

        val hours = (timeUtcMinutes / 60.0).toInt()
        val minutes = (timeUtcMinutes % 60.0).toInt()
        val seconds = (((timeUtcMinutes % 60.0) - minutes) * 60.0).toInt()

        // Create UTC ZonedDateTime and convert to specified zoneId
        val utcDate = date.atTime(
            hours.coerceIn(0, 23),
            minutes.coerceIn(0, 59),
            seconds.coerceIn(0, 59)
        ).atZone(ZoneId.of("UTC"))

        return utcDate.toInstant()
    }
}
