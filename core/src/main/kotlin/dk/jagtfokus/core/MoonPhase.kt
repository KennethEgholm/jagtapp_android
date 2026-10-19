package dk.jagtfokus.core

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.cos

/** The moon at one instant. Display only, like the sun times. */
data class MoonPhase(val name: Name, val illumination: Double) {
    enum class Name {
        New,
        WaxingCrescent,
        FirstQuarter,
        WaxingGibbous,
        Full,
        WaningGibbous,
        LastQuarter,
        WaningCrescent,
    }

    companion object {
        const val SYNODIC_MONTH = 29.530588853
        private val referenceNewMoon: Instant = Instant.parse("2000-01-06T18:14:00Z")

        fun at(instant: Instant): MoonPhase {
            val cycles = (instant.epochSecond - referenceNewMoon.epochSecond) / (SYNODIC_MONTH * 86_400)
            var fraction = cycles % 1.0
            if (fraction < 0) fraction += 1
            val illumination = (1 - cos(2 * Math.PI * fraction)) / 2
            val day = 1 / SYNODIC_MONTH
            val name = when {
                fraction < day || fraction >= 1 - day -> Name.New
                fraction >= 0.25 - day && fraction < 0.25 + day -> Name.FirstQuarter
                fraction >= 0.5 - day && fraction < 0.5 + day -> Name.Full
                fraction >= 0.75 - day && fraction < 0.75 + day -> Name.LastQuarter
                fraction < 0.25 - day -> Name.WaxingCrescent
                fraction < 0.5 - day -> Name.WaxingGibbous
                fraction < 0.75 - day -> Name.WaningGibbous
                else -> Name.WaningCrescent
            }
            return MoonPhase(name, illumination)
        }
    }
}

/** The moon at local noon on the chosen day, in this calendar's zone. */
fun SeasonCalendar.moonPhase(on: LocalDate): MoonPhase {
    val noon = on.atTime(LocalTime.NOON).atZone(zone).toInstant()
    return MoonPhase.at(noon)
}
