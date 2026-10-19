package dk.jagtfokus.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.tan

/** Sunrise and sunset for one day and place. Null when the sun does not rise or set. Display only. */
data class SunTimes(val sunrise: Instant?, val sunset: Instant?)

/** NOAA solar position. Within about a minute in Denmark. The day is the calendar day, not the phone's zone. */
fun SeasonCalendar.sunTimes(on: LocalDate, latitude: Double, longitude: Double): SunTimes {
    val midnightJd = julianDayAtMidnightUtc(on.year, on.monthValue, on.dayOfMonth)
    val midnight = on.atStartOfDay(ZoneOffset.UTC).toInstant()
    fun at(minutes: Double?): Instant? {
        if (minutes == null) return null
        return midnight.plusMillis((minutes * 60_000).roundToLong())
    }
    return SunTimes(
        sunrise = at(solarEventMinutesUtc(rising = true, midnightJd = midnightJd, latitude = latitude, longitude = longitude)),
        sunset = at(solarEventMinutesUtc(rising = false, midnightJd = midnightJd, latitude = latitude, longitude = longitude)),
    )
}

/** "07.42" in Danish, "07:42" in English. Rounded to the nearest minute, in this calendar's zone. */
fun SeasonCalendar.clockTime(instant: Instant): String {
    val rounded = Instant.ofEpochSecond((instant.epochSecond / 60.0).roundToLong() * 60)
    val time = rounded.atZone(zone)
    val separator = if (locale.language == "da") "." else ":"
    return "%02d%s%02d".format(time.hour, separator, time.minute)
}

private const val SOLAR_ZENITH = 90.833

private fun julianDayAtMidnightUtc(year: Int, month: Int, day: Int): Double {
    val a = (14 - month) / 12
    val y = year + 4800 - a
    val m = month + 12 * a - 3
    val jdn = day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045
    return jdn.toDouble() - 0.5
}

private fun solarEventMinutesUtc(rising: Boolean, midnightJd: Double, latitude: Double, longitude: Double): Double? {
    var minutes = 720 - 4 * longitude
    repeat(2) {
        val next = solarEvent(rising, midnightJd + minutes / 1440, latitude, longitude) ?: return null
        minutes = next
    }
    return minutes
}

private fun solarEvent(rising: Boolean, jd: Double, latitude: Double, longitude: Double): Double? {
    val t = (jd - 2_451_545) / 36_525
    val (declination, equationOfTime) = solarPosition(t)
    val lat = Math.toRadians(latitude)
    val cosHa = cos(Math.toRadians(SOLAR_ZENITH)) / (cos(lat) * cos(declination)) - tan(lat) * tan(declination)
    if (cosHa !in -1.0..1.0) return null
    val hourAngle = Math.toDegrees(acos(cosHa))
    val noon = 720 - 4 * longitude - equationOfTime
    return if (rising) noon - 4 * hourAngle else noon + 4 * hourAngle
}

private fun solarPosition(t: Double): Pair<Double, Double> {
    val l0 = Math.toRadians((280.46646 + t * (36000.76983 + t * 0.0003032)).mod(360.0))
    val m = Math.toRadians(357.52911 + t * (35999.05029 - 0.0001537 * t))
    val e = 0.016708634 - t * (0.000042037 + 0.0000001267 * t)
    val center = sin(m) * (1.914602 - t * (0.004817 + 0.000014 * t)) +
        sin(2 * m) * (0.019993 - 0.000101 * t) +
        sin(3 * m) * 0.000289
    val omega = Math.toRadians(125.04 - 1934.136 * t)
    val lambda = Math.toRadians(Math.toDegrees(l0) + center - 0.00569 - 0.00478 * sin(omega))
    val meanObliquity = 23 + (26 + (21.448 - t * (46.815 + t * (0.00059 - t * 0.001813))) / 60) / 60
    val obliquity = Math.toRadians(meanObliquity + 0.00256 * cos(omega))
    val declination = asin(sin(obliquity) * sin(lambda))
    val y = tan(obliquity / 2).pow(2)
    val equationOfTime = 4 * Math.toDegrees(
        y * sin(2 * l0) - 2 * e * sin(m) + 4 * e * y * sin(m) * cos(2 * l0) -
            0.5 * y * y * sin(4 * l0) - 1.25 * e * e * sin(2 * m),
    )
    return declination to equationOfTime
}

