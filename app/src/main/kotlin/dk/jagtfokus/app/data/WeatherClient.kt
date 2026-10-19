package dk.jagtfokus.app.data

import dk.jagtfokus.core.MetDay
import dk.jagtfokus.core.MetForecast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class WeatherClient {
    private val cache = mutableMapOf<String, Entry>()

    suspend fun load(latitude: Double, longitude: Double, on: LocalDate, zone: ZoneId, now: Instant): Result<MetDay?> {
        val key = "${MetForecast.rounded(latitude)},${MetForecast.rounded(longitude)},$on"
        val cached = cache[key]
        if (cached != null && now.epochSecond - cached.fetched.epochSecond < 600) {
            return Result.success(cached.forecast.day(on, zone, now))
        }
        return withContext(Dispatchers.IO) {
            try {
                val connection = (URL(MetForecast.requestUrl(latitude, longitude)).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 15_000
                    setRequestProperty("User-Agent", CatalogRepository.USER_AGENT)
                }
                if (connection.responseCode != 200) return@withContext Result.failure(WeatherException())
                val forecast = connection.inputStream.use { MetForecast.decode(it.readBytes()) }
                cache[key] = Entry(now, forecast)
                Result.success(forecast.day(on, zone, now))
            } catch (_: Exception) {
                Result.failure(WeatherException())
            }
        }
    }

    private data class Entry(val fetched: Instant, val forecast: MetForecast)
}

class WeatherException : Exception()
