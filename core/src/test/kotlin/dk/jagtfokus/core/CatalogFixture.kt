package dk.jagtfokus.core

import java.io.File
import java.time.LocalDate
import java.util.Locale

fun bundledCatalog(): HuntingCatalog {
    val file = listOf(
        File("data/jagttider.json"),
        File("../data/jagttider.json"),
    ).firstOrNull { it.isFile } ?: error("Missing data/jagttider.json")
    return HuntingCatalog.decode(file.readBytes())
}

fun danishCalendar() = SeasonCalendar(locale = Locale.forLanguageTag("da-DK"))

fun englishCalendar() = SeasonCalendar(locale = Locale.forLanguageTag("en-GB"))

fun day(year: Int, month: Int, day: Int): LocalDate = LocalDate.of(year, month, day)

fun period(from: String, to: String): SeasonPeriod =
    SeasonPeriod(MonthDay.parse(from)!!, MonthDay.parse(to)!!)
