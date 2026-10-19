package dk.jagtfokus.app.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

class LocationMissing : Exception()

data class LocationFix(val latitude: Double, val longitude: Double, val names: List<String>)

/**
 * Coarse fix plus the geocoder's place names. One geocode at a time.
 * Names go into [dk.jagtfokus.core.matchPlace]. The fix is not stored.
 */
class AndroidLocationSource(context: Context) {
    private val appContext = context.applicationContext
    private val mutex = Mutex()

    suspend fun fix(locale: Locale): LocationFix = mutex.withLock {
        val location = currentLocation() ?: throw LocationMissing()
        LocationFix(location.latitude, location.longitude, geocode(location, locale))
    }

    @SuppressLint("MissingPermission")
    private suspend fun currentLocation(): Location? {
        val manager = appContext.getSystemService(LocationManager::class.java) ?: return null
        val provider = when {
            Build.VERSION.SDK_INT >= 31 -> LocationManager.FUSED_PROVIDER
            else -> LocationManager.NETWORK_PROVIDER
        }
        if (provider != LocationManager.FUSED_PROVIDER && !manager.isProviderEnabled(provider)) {
            return null
        }
        return suspendCancellableCoroutine { cont ->
            if (Build.VERSION.SDK_INT >= 30) {
                val cancel = CancellationSignal()
                cont.invokeOnCancellation { cancel.cancel() }
                manager.getCurrentLocation(provider, cancel, appContext.mainExecutor) { location ->
                    if (cont.isActive) cont.resume(location)
                }
            } else {
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        manager.removeUpdates(this)
                        if (cont.isActive) cont.resume(location)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit

                    override fun onProviderDisabled(provider: String) {
                        manager.removeUpdates(this)
                        if (cont.isActive) cont.resume(null)
                    }
                }
                cont.invokeOnCancellation { manager.removeUpdates(listener) }
                @Suppress("DEPRECATION")
                manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
            }
        }
    }

    private suspend fun geocode(location: Location, locale: Locale): List<String> {
        if (!Geocoder.isPresent()) throw LocationMissing()
        val geocoder = Geocoder(appContext, locale)
        val addresses = withContext(Dispatchers.IO) {
            @Suppress("DEPRECATION")
            geocoder.getFromLocation(location.latitude, location.longitude, 1).orEmpty()
        }
        val names = addresses.flatMap { address ->
            listOfNotNull(
                address.featureName,
                address.locality,
                address.subLocality,
                address.subAdminArea,
                address.adminArea,
                address.countryName,
            )
        }
        if (names.isEmpty()) throw LocationMissing()
        return names
    }
}
