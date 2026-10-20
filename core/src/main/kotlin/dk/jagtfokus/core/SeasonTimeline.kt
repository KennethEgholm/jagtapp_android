package dk.jagtfokus.core

/** Maps dates onto the hunting year, 1 April – 31 March, as fractions 0...1. */
class SeasonTimeline {
    val months: List<Int> = (0 until 12).map { (START_MONTH - 1 + it) % 12 + 1 }

    fun position(day: MonthDay): Double = ordinal(day).toDouble() / LENGTH

    fun positionOfMonth(month: Int): Double = position(MonthDay(month, 1))

    /** Open stretches, end day inclusive. Split when a period crosses 1 April. */
    fun segments(periods: List<SeasonPeriod>): List<ClosedFloatingPointRange<Double>> =
        periods.flatMap { period ->
            val start = ordinal(period.from).toDouble() / LENGTH
            val end = (ordinal(period.to) + 1).toDouble() / LENGTH
            if (start < end) listOf(start..end) else listOf(0.0..end, start..1.0)
        }.sortedBy { it.start }

    private fun ordinal(day: MonthDay): Int {
        val month = day.month.coerceIn(1, 12)
        val length = DAYS_IN_MONTH[month - 1]
        val dayInMonth = day.day.coerceIn(1, length)
        var offset = 0
        var current = START_MONTH
        while (current != month) {
            offset += DAYS_IN_MONTH[current - 1]
            current = current % 12 + 1
        }
        return offset + dayInMonth - 1
    }

    /** The month's initial for the bar's month row, as iOS: J F M A M J J A S O N D by month number. */
    fun initial(month: Int): String = INITIALS[month - 1]

    companion object {
        const val START_MONTH = 4
        private val INITIALS = listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
        private val DAYS_IN_MONTH = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        private const val LENGTH = 365.0
    }
}
