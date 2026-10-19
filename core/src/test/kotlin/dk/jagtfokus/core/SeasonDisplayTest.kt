package dk.jagtfokus.core

import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SeasonDisplayTest {
    private val calendar = danishCalendar()
    private val english = englishCalendar()

    @Test
    fun periodEndCrossesNewYear() {
        assertEquals(day(2027, 1, 31), calendar.currentPeriodEnd(listOf(period("10-01", "01-31")), day(2026, 10, 11)))
    }

    @Test
    fun periodEndInJanuaryStaysInSameYear() {
        assertEquals(day(2027, 1, 31), calendar.currentPeriodEnd(listOf(period("10-01", "01-31")), day(2027, 1, 10)))
    }

    @Test
    fun periodEndPicksTheOpenPeriod() {
        val buck = listOf(period("05-16", "07-15"), period("10-01", "01-31"))
        assertEquals(day(2026, 7, 15), calendar.currentPeriodEnd(buck, day(2026, 6, 1)))
        assertNull(calendar.currentPeriodEnd(buck, day(2026, 8, 1)))
    }

    @Test
    fun periodEndClampsLeapDay() {
        assertEquals(day(2027, 2, 28), calendar.currentPeriodEnd(listOf(period("09-01", "02-29")), day(2026, 12, 1)))
    }

    @Test
    fun daysCountsCalendarDays() {
        assertEquals(0, calendar.days(day(2026, 10, 11), day(2026, 10, 11)))
        assertEquals(21, calendar.days(day(2026, 10, 11), day(2026, 11, 1)))
        assertEquals(3, calendar.days(day(2026, 12, 30), day(2027, 1, 2)))
    }

    @Test
    fun longDateHasWeekdayAndFullMonth() {
        assertEquals("søndag 11. oktober", calendar.longDate(day(2026, 10, 11)))
        assertEquals("Sunday 11 October", english.longDate(day(2026, 10, 11)))
    }

    @Test
    fun shortAndFullDateFollowTheLocale() {
        val date = MonthDay(10, 1)
        assertEquals("1. okt.", calendar.short(date))
        assertEquals("1. oktober", calendar.full(date))
        assertEquals("1 Oct", english.short(date))
        assertEquals("1 October", english.full(date))
    }

    /**
     * The calendar's zone decides the day, not the device's. Auckland morning is still
     * the previous evening in UTC, so a formatter that used the machine zone would fail in CI.
     */
    @Test
    fun datesFollowTheCalendarsTimeZoneNotTheDevice() {
        val auckland = SeasonCalendar(
            zone = ZoneId.of("Pacific/Auckland"),
            locale = java.util.Locale.forLanguageTag("da-DK"),
        )
        val morning = ZonedDateTime.of(2026, 10, 11, 8, 0, 0, 0, ZoneId.of("Pacific/Auckland")).toInstant()
        assertEquals("1. okt.", auckland.short(MonthDay(10, 1)))
        assertEquals("1. oktober", auckland.full(MonthDay(10, 1)))
        assertEquals("11. okt. 2026", auckland.shortWithYear(morning))
        assertEquals("11. oktober 2026", auckland.fullWithYear(morning))
        assertEquals("søndag 11. oktober", auckland.longDate(morning))
    }

    @Test
    fun copenhagenMidnightIsNotThePreviousUtcDay() {
        val instant = ZonedDateTime.of(2026, 10, 1, 0, 30, 0, 0, SeasonCalendar.ZONE).toInstant()
        assertEquals("1. okt. 2026", calendar.shortWithYear(instant))
    }

    @Test
    fun monthDayOnItsOwnKeepsTheDayOfMonth() {
        assertEquals("29. feb.", calendar.short(MonthDay(2, 29)))
    }

    @Test
    fun shortWithYearAndMonthYearFollowTheLocale() {
        assertEquals("11. okt. 2026", calendar.shortWithYear(day(2026, 10, 11)))
        assertEquals("oktober 2026", calendar.monthYear("2026-10-11"))
        assertEquals("October 2026", english.monthYear("2026-10-11"))
    }

    @Test
    fun statusOpenFarFromEnd() {
        val status = calendar.status(listOf(period("10-01", "01-31")), day(2026, 10, 11))
        assertTrue(status.isOpen)
        assertFalse(status.isClosingSoon)
        assertEquals(112, status.daysLeft)
        assertEquals(day(2027, 1, 31), status.endDate)
        assertNull(status.nextOpening)
    }

    @Test
    fun statusClosingSoonWithinAWeek() {
        val status = calendar.status(listOf(period("09-16", "10-15")), day(2026, 10, 11))
        assertTrue(status.isClosingSoon)
        assertEquals(4, status.daysLeft)
        assertEquals(0, calendar.status(listOf(period("09-16", "10-15")), day(2026, 10, 15)).daysLeft)
    }

    @Test
    fun statusClosedKnowsNextOpening() {
        val status = calendar.status(listOf(period("11-01", "01-31")), day(2026, 10, 11))
        assertFalse(status.isOpen)
        assertEquals(21, status.daysUntilOpening)
        assertEquals(day(2026, 11, 1), status.nextOpening)
    }

    @Test
    fun statusWithoutPeriodsNeverOpens() {
        val status = calendar.status(emptyList(), day(2026, 10, 11))
        assertFalse(status.isOpen)
        assertNull(status.nextOpening)
        assertNull(status.daysUntilOpening)
    }

    @Test
    fun spokenPeriodTextUsesFullMonths() {
        val buck = listOf(period("05-16", "07-15"), period("10-01", "01-31"))
        assertEquals(
            "16. maj til 15. juli og 1. oktober til 31. januar",
            calendar.spokenPeriodText(buck),
        )
        assertEquals("Ingen jagttid", calendar.spokenPeriodText(emptyList()))
    }

    @Test
    fun periodTextJoinsWithTheLocaleListWording() {
        val buck = listOf(period("05-16", "07-15"), period("10-01", "01-31"))
        assertEquals("16. maj – 15. jul. og 1. okt. – 31. jan.", calendar.periodText(buck))
        assertEquals("16 May – 15 Jul and 1 Oct – 31 Jan", english.periodText(buck))
    }
}
