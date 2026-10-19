package dk.jagtfokus.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

/** Weather for one day. Display only, like the sun times. */
data class MetDay(
    val symbolCode: String?,
    val minTemperature: Double?,
    val maxTemperature: Double?,
    val maxWindSpeed: Double?,
    val maxWindDirection: Double?,
    val precipitation: Double?,
    val humidity: Double?,
    val temperature: Double?,
    val windSpeed: Double?,
    val windDirection: Double?,
    val pressure: Double?,
    val pressureTrend: Double?,
)

data class MetHour(
    val time: Instant,
    val temperature: Double?,
    val humidity: Double?,
    val pressure: Double?,
    val windSpeed: Double?,
    val windDirection: Double?,
    val symbolCode: String?,
    val precipitation: Double?,
    val hourPrecipitation: Double?,
)

data class MetForecast(val updatedAt: Instant?, val timeseries: List<MetHour>) {
    /**
     * The chosen day's weather, grouped by the clock in [zone]. Null when the day has no
     * forecast. [now] picks the reference hour: nearest the moment today, else midday.
     */
    fun day(on: LocalDate, zone: ZoneId, now: Instant? = null): MetDay? {
        val hours = timeseries.filter { it.time.atZone(zone).toLocalDate() == on }
        if (hours.isEmpty()) return null
        var minTemperature: Double? = null
        var maxTemperature: Double? = null
        var maxWind: Double? = null
        var maxWindDirection: Double? = null
        for (hour in hours) {
            val temperature = hour.temperature
            if (temperature != null) {
                minTemperature = minOf(minTemperature ?: temperature, temperature)
                maxTemperature = maxOf(maxTemperature ?: temperature, temperature)
            }
            val wind = hour.windSpeed
            if (wind != null && wind > (maxWind ?: -1.0)) {
                maxWind = wind
                maxWindDirection = hour.windDirection
            }
        }
        val reference = referenceHour(hours, on, zone, now)
        val trend = reference?.let { hour ->
            val ahead = timeseries.firstOrNull { it.time == hour.time.plusSeconds(3 * 3600) }
            val from = hour.pressure
            val to = ahead?.pressure
            if (from != null && to != null) to - from else null
        }
        val rain = rain(hours)
        val noon = on.atTime(12, 0).atZone(zone).toInstant()
        return MetDay(
            symbolCode = symbol(sixHourBlocks(hours), noon),
            minTemperature = minTemperature,
            maxTemperature = maxTemperature,
            maxWindSpeed = maxWind,
            maxWindDirection = maxWindDirection,
            precipitation = rain,
            humidity = reference?.humidity,
            temperature = reference?.temperature,
            windSpeed = reference?.windSpeed,
            windDirection = reference?.windDirection,
            pressure = reference?.pressure,
            pressureTrend = trend,
        )
    }

    private fun rain(hours: List<MetHour>): Double? {
        var total: Double? = null
        var counted = Instant.EPOCH
        for (hour in hours) {
            if (hour.time < counted) continue
            val hourly = hour.hourPrecipitation
            val block = hour.precipitation
            if (hourly != null) {
                total = (total ?: 0.0) + hourly
                counted = hour.time.plusSeconds(3600)
            } else if (block != null) {
                total = (total ?: 0.0) + block
                counted = hour.time.plusSeconds(6 * 3600)
            }
        }
        return total
    }

    private fun sixHourBlocks(hours: List<MetHour>): List<MetHour> {
        val blocks = mutableListOf<MetHour>()
        for (hour in hours) {
            if (hour.symbolCode == null) continue
            val last = blocks.lastOrNull()
            if (last != null && hour.time < last.time.plusSeconds(6 * 3600)) continue
            blocks += hour
        }
        return blocks
    }

    private fun referenceHour(hours: List<MetHour>, on: LocalDate, zone: ZoneId, now: Instant?): MetHour? {
        val moment = if (now != null && now.atZone(zone).toLocalDate() == on) {
            now
        } else {
            on.atTime(12, 0).atZone(zone).toInstant()
        }
        return hours.minByOrNull { abs(it.time.epochSecond - moment.epochSecond) }
    }

    private fun symbol(blocks: List<MetHour>, noon: Instant): String? {
        val wettest = blocks.maxByOrNull { it.precipitation ?: 0.0 }
        if (wettest != null && (wettest.precipitation ?: 0.0) > 0.05 && wettest.symbolCode != null) {
            return wettest.symbolCode
        }
        val overNoon = blocks.firstOrNull { it.time <= noon && noon < it.time.plusSeconds(6 * 3600) }
        return (overNoon ?: blocks.firstOrNull())?.symbolCode
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun decode(bytes: ByteArray): MetForecast {
            val response = json.decodeFromString<Response>(bytes.decodeToString())
            val hours = response.properties.timeseries.orEmpty().map { entry ->
                val details = entry.data?.instant?.details
                MetHour(
                    time = Instant.parse(entry.time),
                    temperature = details?.airTemperature,
                    humidity = details?.relativeHumidity,
                    pressure = details?.airPressureAtSeaLevel,
                    windSpeed = details?.windSpeed,
                    windDirection = details?.windFromDirection,
                    symbolCode = entry.data?.next6Hours?.summary?.symbolCode,
                    precipitation = entry.data?.next6Hours?.details?.precipitationAmount,
                    hourPrecipitation = entry.data?.next1Hours?.details?.precipitationAmount,
                )
            }
            val updated = response.properties.meta?.updatedAt?.let { runCatching { Instant.parse(it) }.getOrNull() }
            return MetForecast(updated, hours)
        }

        fun rounded(value: Double): Double = kotlin.math.round(value * 100) / 100.0

        fun requestUrl(latitude: Double, longitude: Double): String =
            "https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=${rounded(latitude)}&lon=${rounded(longitude)}"
    }
}

@Serializable
private data class Response(val properties: Properties) {
    @Serializable
    data class Properties(val meta: Meta? = null, val timeseries: List<Entry>? = null)

    @Serializable
    data class Meta(@SerialName("updated_at") val updatedAt: String? = null)

    @Serializable
    data class Entry(val time: String, val data: Payload? = null)

    @Serializable
    data class Payload(
        val instant: InstantBlock? = null,
        @SerialName("next_1_hours") val next1Hours: Block? = null,
        @SerialName("next_6_hours") val next6Hours: Block? = null,
    )

    @Serializable
    data class InstantBlock(val details: Details? = null)

    @Serializable
    data class Block(val summary: Summary? = null, val details: Precip? = null)

    @Serializable
    data class Summary(@SerialName("symbol_code") val symbolCode: String? = null)

    @Serializable
    data class Precip(@SerialName("precipitation_amount") val precipitationAmount: Double? = null)

    @Serializable
    data class Details(
        @SerialName("air_temperature") val airTemperature: Double? = null,
        @SerialName("relative_humidity") val relativeHumidity: Double? = null,
        @SerialName("air_pressure_at_sea_level") val airPressureAtSeaLevel: Double? = null,
        @SerialName("wind_speed") val windSpeed: Double? = null,
        @SerialName("wind_from_direction") val windFromDirection: Double? = null,
    )
}
