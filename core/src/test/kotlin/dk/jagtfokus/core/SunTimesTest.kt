package dk.jagtfokus.core

import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SunTimesTest {
    private val calendar = danishCalendar()

    /** Almanacs and the iOS tests allow about a minute. The row rounds to the nearest minute. */
    private fun expect(on: java.time.LocalDate, latitude: Double, longitude: Double, rise: Pair<Int, Int>, set: Pair<Int, Int>) {
        val times = calendar.sunTimes(on, latitude, longitude)
        kotlin.test.assertTrue(kotlin.math.abs(minutes(times.sunrise) - (rise.first * 60 + rise.second)) <= 2)
        kotlin.test.assertTrue(kotlin.math.abs(minutes(times.sunset) - (set.first * 60 + set.second)) <= 2)
    }

    private fun minutes(instant: java.time.Instant?): Int {
        val time = instant!!.atZone(SeasonCalendar.ZONE)
        return time.hour * 60 + time.minute
    }

    @Test
    fun aarhusInOctoberMatchesTheAlmanac() {
        expect(day(2026, 10, 11), 56.1629, 10.2039, 7 to 42, 18 to 29)
    }

    @Test
    fun copenhagenAtMidsummer() {
        expect(day(2026, 6, 21), 55.6761, 12.5683, 4 to 25, 21 to 58)
    }

    @Test
    fun skagenAtMidwinter() {
        expect(day(2026, 12, 21), 57.7209, 10.5839, 9 to 0, 15 to 32)
    }

    @Test
    fun theClockChangeDoesNotShiftTheDay() {
        expect(day(2026, 10, 24), 56.1629, 10.2039, 8 to 9, 17 to 56)
        expect(day(2026, 10, 25), 56.1629, 10.2039, 7 to 12, 16 to 54)
    }

    @Test
    fun polarNightHasNoRiseOrSet() {
        val times = calendar.sunTimes(day(2026, 12, 21), 78.22, 15.65)
        assertNull(times.sunrise)
        assertNull(times.sunset)
    }

    @Test
    fun clockTimeUsesTheCalendarZoneAndLocale() {
        val instant = day(2026, 10, 11).atTime(7, 42).atZone(SeasonCalendar.ZONE).toInstant()
        assertEquals("07.42", calendar.clockTime(instant))
        assertEquals("07:42", englishCalendar().clockTime(instant))
        val auckland = SeasonCalendar(ZoneId.of("Pacific/Auckland"), java.util.Locale.forLanguageTag("da-DK"))
        val morning = day(2026, 10, 11).atTime(7, 42).atZone(ZoneId.of("Pacific/Auckland")).toInstant()
        assertEquals("07.42", auckland.clockTime(morning))
    }
}
