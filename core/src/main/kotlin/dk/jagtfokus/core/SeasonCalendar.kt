package dk.jagtfokus.core

import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

data class SeasonStatus(
    val isOpen: Boolean,
    /** Open, and the last day is less than [soonWithinDays] away. */
    val isClosingSoon: Boolean,
    val endDate: LocalDate?,
    /** 0 on the last day. */
    val daysLeft: Int?,
    val nextOpening: LocalDate?,
    val daysUntilOpening: Int?,
)

/**
 * Season days are counted in [zone], never in the phone's zone.
 * A phone west of Denmark must not show a season opening 1 October as 30 September.
 */
class SeasonCalendar(
    val zone: ZoneId = ZONE,
    val locale: Locale = Locale.getDefault(),
) {
    fun localDate(instant: Instant): LocalDate = instant.atZone(zone).toLocalDate()

    fun monthDay(date: LocalDate): MonthDay = MonthDay(date.monthValue, date.dayOfMonth)

    fun isOpen(periods: List<SeasonPeriod>, on: LocalDate): Boolean {
        val today = monthDay(on)
        return periods.any { contains(today, it) }
    }

    fun contains(day: MonthDay, period: SeasonPeriod): Boolean =
        if (period.from <= period.to) {
            day >= period.from && day <= period.to
        } else {
            day >= period.from || day <= period.to
        }

    fun nextOpening(periods: List<SeasonPeriod>, after: LocalDate): LocalDate? {
        if (isOpen(periods, after)) return null
        var soonest: LocalDate? = null
        for (period in periods) {
            val candidate = nextStart(period.from, after) ?: continue
            if (soonest == null || candidate < soonest) soonest = candidate
        }
        return soonest
    }

    /** Last day of the open period that contains [on], or null when closed. */
    fun currentPeriodEnd(periods: List<SeasonPeriod>, on: LocalDate): LocalDate? {
        val day = monthDay(on)
        var latest: LocalDate? = null
        for (period in periods) {
            if (!contains(day, period)) continue
            val wraps = period.from > period.to && day >= period.from
            val end = date(if (wraps) on.year + 1 else on.year, period.to) ?: continue
            if (latest == null || end > latest) latest = end
        }
        return latest
    }

    /** Open/closed state on a day, with the dates the UI shows next to it. */
    fun status(periods: List<SeasonPeriod>, on: LocalDate, soonWithinDays: Int = 7): SeasonStatus {
        val end = currentPeriodEnd(periods, on)
        if (end != null) {
            val left = days(on, end)
            return SeasonStatus(
                isOpen = true,
                isClosingSoon = left < soonWithinDays,
                endDate = end,
                daysLeft = left,
                nextOpening = null,
                daysUntilOpening = null,
            )
        }
        val next = nextOpening(periods, on)
        return SeasonStatus(
            isOpen = false,
            isClosingSoon = false,
            endDate = null,
            daysLeft = null,
            nextOpening = next,
            daysUntilOpening = next?.let { days(on, it) },
        )
    }

    /** Whole calendar days between two dates in this calendar's zone. */
    fun days(from: LocalDate, to: LocalDate): Int = ChronoUnit.DAYS.between(from, to).toInt()

    /** Season text with short months: "16. maj – 15. jul. og 1. okt. – 31. jan.". */
    fun periodText(periods: List<SeasonPeriod>): String {
        if (periods.isEmpty()) return noSeason()
        return join(periods.map { "${short(it.from)} – ${short(it.to)}" })
    }

    /** Full month names, so TalkBack reads "16. maj til 15. juli og 1. oktober til 31. januar". */
    fun spokenPeriodText(periods: List<SeasonPeriod>): String {
        if (periods.isEmpty()) return noSeason()
        val to = if (isDanish) "til" else "to"
        return join(periods.map { "${full(it.from)} $to ${full(it.to)}" })
    }

    /** Short day and month: "11. okt." in Danish, "11 Oct" in English. */
    fun short(day: MonthDay): String =
        if (isDanish) "${day.day}. ${shortMonth(day.month)}"
        else "${day.day} ${shortMonth(day.month)}"

    /** Day and month: "1. oktober" in Danish, "1 October" in English. */
    fun full(day: MonthDay): String {
        val month = displayDate(day).month.getDisplayName(TextStyle.FULL, locale)
        return if (isDanish) "${day.day}. $month" else "${day.day} $month"
    }

    fun shortWithYear(instant: Instant): String = shortWithYear(localDate(instant))

    fun shortWithYear(date: LocalDate): String = "${short(monthDay(date))} ${date.year}"

    fun fullWithYear(instant: Instant): String = fullWithYear(localDate(instant))

    fun fullWithYear(date: LocalDate): String = "${full(monthDay(date))} ${date.year}"

    /** "søndag 11. oktober" in Danish, "Sunday 11 October" in English. */
    fun longDate(instant: Instant): String = longDate(localDate(instant))

    fun longDate(date: LocalDate): String {
        val weekday = date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
        val month = date.month.getDisplayName(TextStyle.FULL, locale)
        return if (isDanish) {
            "${weekday.lowercase(locale)} ${date.dayOfMonth}. $month"
        } else {
            "$weekday ${date.dayOfMonth} $month"
        }
    }

    /** "oktober 2026", from a day key. */
    fun monthYear(dayKey: String): String? {
        val date = runCatching { LocalDate.parse(dayKey) }.getOrNull() ?: return null
        val month = date.month.getDisplayName(TextStyle.FULL, locale)
        return "$month ${date.year}"
    }

    private val isDanish: Boolean get() = locale.language == "da"

    private fun shortMonth(month: Int): String {
        if (isDanish) return DANISH_SHORT[month - 1]
        return displayDate(MonthDay(month, 1)).month
            .getDisplayName(TextStyle.SHORT, locale)
            .removeSuffix(".")
    }

    /** Month and day on their own have no year. A leap year, so 29 February survives. */
    private fun displayDate(day: MonthDay): LocalDate = LocalDate.of(DISPLAY_YEAR, day.month, day.day)

    private fun nextStart(from: MonthDay, onOrAfter: LocalDate): LocalDate? {
        val thisYear = date(onOrAfter.year, from)
        if (thisYear != null && thisYear >= onOrAfter) return thisYear
        return date(onOrAfter.year + 1, from)
    }

    private fun date(year: Int, monthDay: MonthDay): LocalDate? {
        var day = monthDay.day
        if (monthDay.month == 2 && day == 29 && !isLeap(year)) day = 28
        return try {
            LocalDate.of(year, monthDay.month, day)
        } catch (_: DateTimeException) {
            null
        }
    }

    private fun isLeap(year: Int): Boolean =
        (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

    private fun noSeason(): String = if (isDanish) "Ingen jagttid" else "No season"

    /** Joins with the locale's own list wording: "og" in Danish, "and" in English. */
    private fun join(parts: List<String>): String {
        if (parts.isEmpty()) return ""
        if (parts.size == 1) return parts[0]
        val and = if (isDanish) "og" else "and"
        if (parts.size == 2) return "${parts[0]} $and ${parts[1]}"
        return parts.dropLast(1).joinToString(", ") + " $and " + parts.last()
    }

    companion object {
        val ZONE: ZoneId = ZoneId.of("Europe/Copenhagen")

        private const val DISPLAY_YEAR = 2004

        private val DANISH_SHORT = listOf(
            "jan.", "feb.", "mar.", "apr.", "maj", "jun.",
            "jul.", "aug.", "sep.", "okt.", "nov.", "dec.",
        )
    }
}
