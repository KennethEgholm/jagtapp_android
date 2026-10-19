package dk.jagtfokus.app.data

import android.content.Context
import dk.jagtfokus.core.CatalogRefused
import dk.jagtfokus.core.HuntingCatalog
import dk.jagtfokus.core.decodeChecked
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

enum class RemoteOutcome {
    Updated,
    BuiltInNewer,
    Broken,
    Offline,
    HttpFailed,
}

class CatalogRepository(context: Context) {
    private val app = context.applicationContext
    private val cache = File(app.filesDir, "jagttider.json")

    fun load(): HuntingCatalog {
        val bundled = loadCatalog(app)
        val cached = runCatching { decodeChecked(cache.readBytes()) }.getOrNull()
        return if (cached != null && cached.version >= bundled.version) cached else bundled
    }

    suspend fun refresh(current: HuntingCatalog): Pair<RemoteOutcome, HuntingCatalog> = withContext(Dispatchers.IO) {
        try {
            val connection = (URL(REMOTE).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 15_000
                setRequestProperty("User-Agent", USER_AGENT)
            }
            if (connection.responseCode != 200) return@withContext RemoteOutcome.HttpFailed to current
            val bytes = connection.inputStream.use { it.readBytes() }
            val remote = decodeChecked(bytes)
            if (remote.version >= current.version) {
                cache.writeBytes(bytes)
                RemoteOutcome.Updated to remote
            } else {
                RemoteOutcome.BuiltInNewer to current
            }
        } catch (_: CatalogRefused) {
            RemoteOutcome.Broken to current
        } catch (_: Exception) {
            RemoteOutcome.Offline to current
        }
    }

    companion object {
        const val REMOTE = "https://raw.githubusercontent.com/KennethEgholm/jagtapp/main/data/jagttider.json"
        const val USER_AGENT = "JagtFokus/1.0 +https://github.com/KennethEgholm/jagtapp"
    }
}
