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
        "lightrain" -> R.string.weather_light_rain
        "heavyrain" -> R.string.weather_heavy_rain
        "rainshowers" -> R.string.weather_showers
        "lightrainshowers" -> R.string.weather_light_showers
        "heavyrainshowers" -> R.string.weather_heavy_showers
        "sleet" -> R.string.weather_sleet
        "lightsleet" -> R.string.weather_light_sleet
        "heavysleet" -> R.string.weather_heavy_sleet
        "snow" -> R.string.weather_snow
        "lightsnow" -> R.string.weather_light_snow
        "heavysnow" -> R.string.weather_heavy_snow
        "snowshowers" -> R.string.weather_snow_showers
        else -> when {
            base.contains("thunder") -> R.string.weather_thunder
            base.contains("sleet") -> R.string.weather_sleet
            base.contains("snow") -> R.string.weather_snow
            base.contains("rain") -> R.string.weather_rain
            else -> null
        }
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
