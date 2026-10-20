package dk.jagtfokus.app.ui

import androidx.annotation.StringRes
import dk.jagtfokus.app.R
import dk.jagtfokus.core.MoonPhase
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

fun decimal(value: Double, locale: Locale, minDigits: Int = 1, maxDigits: Int = 1): String {
    val format = NumberFormat.getNumberInstance(locale)
    format.minimumFractionDigits = minDigits
    format.maximumFractionDigits = maxDigits
    format.isGroupingUsed = false
    return format.format(value)
}

fun percent(fraction: Double, locale: Locale): String {
    val format = NumberFormat.getPercentInstance(locale)
    format.maximumFractionDigits = 0
    return format.format(fraction)
}

fun windFrom(degrees: Double): Int = when (((degrees + 22.5) / 45).toInt() % 8) {
    0 -> R.string.wind_n
    1 -> R.string.wind_ne
    2 -> R.string.wind_e
    3 -> R.string.wind_se
    4 -> R.string.wind_s
    5 -> R.string.wind_sw
    6 -> R.string.wind_w
    else -> R.string.wind_nw
}

/** The iOS word table (WeatherSymbols.text), verbatim: every code MET lists, null for others. */
@StringRes
fun weatherWords(code: String?): Int? {
    val base = code?.substringBefore('_') ?: return null
    return when (base) {
        "clearsky" -> R.string.weather_clear
        "fair" -> R.string.weather_fair
        "partlycloudy" -> R.string.weather_partly
        "cloudy" -> R.string.weather_cloudy
        "fog" -> R.string.weather_fog
        "rain" -> R.string.weather_rain
        "rainandthunder" -> R.string.weather_rain_thunder
        "rainshowers" -> R.string.weather_rain_showers
        "rainshowersandthunder" -> R.string.weather_rain_showers_thunder
        "lightrain" -> R.string.weather_light_rain
        "lightrainandthunder" -> R.string.weather_light_rain_thunder
        "lightrainshowers" -> R.string.weather_light_rain_showers
        "lightrainshowersandthunder" -> R.string.weather_light_rain_showers_thunder
        "heavyrain" -> R.string.weather_heavy_rain
        "heavyrainandthunder" -> R.string.weather_heavy_rain_thunder
        "heavyrainshowers" -> R.string.weather_heavy_rain_showers
        "heavyrainshowersandthunder" -> R.string.weather_heavy_rain_showers_thunder
        "sleet" -> R.string.weather_sleet
        "sleetandthunder" -> R.string.weather_sleet_thunder
        "sleetshowers" -> R.string.weather_sleet_showers
        "sleetshowersandthunder" -> R.string.weather_sleet_showers_thunder
        "lightsleet" -> R.string.weather_light_sleet
        "lightsleetandthunder" -> R.string.weather_light_sleet_thunder
        "lightsleetshowers" -> R.string.weather_light_sleet_showers
        "lightsleetshowersandthunder" -> R.string.weather_light_sleet_showers_thunder
        "heavysleet" -> R.string.weather_heavy_sleet
        "heavysleetandthunder" -> R.string.weather_heavy_sleet_thunder
        "heavysleetshowers" -> R.string.weather_heavy_sleet_showers
        "heavysleetshowersandthunder" -> R.string.weather_heavy_sleet_showers_thunder
        "snow" -> R.string.weather_snow
        "snowandthunder" -> R.string.weather_snow_thunder
        "snowshowers" -> R.string.weather_snow_showers
        "snowshowersandthunder" -> R.string.weather_snow_showers_thunder
        "lightsnow" -> R.string.weather_light_snow
        "lightsnowandthunder" -> R.string.weather_light_snow_thunder
        "lightsnowshowers" -> R.string.weather_light_snow_showers
        "lightsnowshowersandthunder" -> R.string.weather_light_snow_showers_thunder
        "heavysnow" -> R.string.weather_heavy_snow
        "heavysnowandthunder" -> R.string.weather_heavy_snow_thunder
        "heavysnowshowers" -> R.string.weather_heavy_snow_showers
        "heavysnowshowersandthunder" -> R.string.weather_heavy_snow_showers_thunder
        else -> null
    }
}

@StringRes
fun moonWords(name: MoonPhase.Name): Int = when (name) {
    MoonPhase.Name.New -> R.string.moon_new
    MoonPhase.Name.WaxingCrescent -> R.string.moon_waxing_crescent
    MoonPhase.Name.FirstQuarter -> R.string.moon_first
    MoonPhase.Name.WaxingGibbous -> R.string.moon_waxing
    MoonPhase.Name.Full -> R.string.moon_full
    MoonPhase.Name.WaningGibbous -> R.string.moon_waning
    MoonPhase.Name.LastQuarter -> R.string.moon_last
    MoonPhase.Name.WaningCrescent -> R.string.moon_waning_crescent
}

fun litPercent(illumination: Double): Int = (illumination * 100).roundToInt()
