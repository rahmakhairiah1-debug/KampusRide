package com.example.kampusride.util

import android.content.Context
import android.location.Geocoder
import com.example.kampusride.data.LocationData
import com.example.kampusride.data.model.Place
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Menentukan posisi sebuah titik: koordinat terverifikasi > hasil Geocoder Google > koordinat cadangan.
 */
object GeoResolver {

    private val cache = HashMap<String, LatLng>()

    suspend fun resolve(context: Context, place: Place): LatLng {
        cache[place.name]?.let { return it }

        val fallback = LatLng(place.latitude, place.longitude)
        if (place.verified) return fallback

        val geocoded: LatLng? = withContext(Dispatchers.IO) {
            try {
                if (!Geocoder.isPresent()) {
                    null
                } else {
                    @Suppress("DEPRECATION")
                    val result = Geocoder(context.applicationContext, Locale.forLanguageTag("id-ID"))
                        .getFromLocationName(place.query, 1)
                        ?.firstOrNull()
                    if (result != null && isInRegion(result.latitude, result.longitude)) {
                        LatLng(result.latitude, result.longitude)
                    } else {
                        null
                    }
                }
            } catch (e: Exception) {
                null
            }
        }

        return if (geocoded != null) {
            cache[place.name] = geocoded
            geocoded
        } else {
            fallback
        }
    }

    private fun isInRegion(lat: Double, lng: Double): Boolean =
        lat in LocationData.MIN_LAT..LocationData.MAX_LAT &&
            lng in LocationData.MIN_LNG..LocationData.MAX_LNG
}
