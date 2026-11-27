package com.example.kampusride.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.kampusride.R
import com.example.kampusride.data.LocationData
import com.example.kampusride.data.model.Place
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

object MapHelper {

    /** Tampilkan rute dua titik (asal hijau, tujuan merah). */
    fun showRoute(scope: CoroutineScope, context: Context, map: GoogleMap, from: String, to: String): Job =
        showPath(scope, context, map, listOf(from, to))

    /**
     * Tampilkan beberapa titik berurutan: titik pertama hijau, terakhir merah, sisanya biru,
     * dihubungkan garis lurus. Posisi tiap titik dicari lewat [GeoResolver] (tidak memblokir layar).
     * Rute jalan sebenarnya dapat dilihat lewat tombol "Buka di Google Maps".
     */
    fun showPath(scope: CoroutineScope, context: Context, map: GoogleMap, names: List<String>): Job =
        scope.launch {
            val points = names.mapNotNull { LocationData.find(it) }
                .map { place -> place to GeoResolver.resolve(context, place) }
            map.clear()
            if (points.isEmpty()) return@launch

            points.forEachIndexed { index, (place, latLng) ->
                val hue = when (index) {
                    0 -> BitmapDescriptorFactory.HUE_GREEN
                    points.lastIndex -> BitmapDescriptorFactory.HUE_RED
                    else -> BitmapDescriptorFactory.HUE_AZURE
                }
                map.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title(place.name)
                        .icon(BitmapDescriptorFactory.defaultMarker(hue))
                )
            }
            if (points.size > 1) {
                map.addPolyline(
                    PolylineOptions()
                        .addAll(points.map { it.second })
                        .width(8f)
                        .color(0xFF0F7A3A.toInt())
                )
            }

            val center = LatLng(
                points.map { it.second.latitude }.average(),
                points.map { it.second.longitude }.average()
            )
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 12f))
        }

    /**
     * Buka aplikasi Google Maps untuk rute jalan sebenarnya. Titik yang belum diverifikasi dikirim
     * sebagai NAMA tempat, sehingga Google yang menentukan lokasinya (lebih akurat dari koordinat tebakan).
     */
    fun openDirections(context: Context, from: String, to: String) {
        val a = LocationData.find(from)
        val b = LocationData.find(to)
        if (a == null || b == null) {
            Toast.makeText(context, R.string.error_no_maps_app, Toast.LENGTH_SHORT).show()
            return
        }
        fun ref(p: Place) = if (p.verified) "${p.latitude},${p.longitude}" else Uri.encode(p.query)
        val uri = "https://www.google.com/maps/dir/?api=1&origin=${ref(a)}&destination=${ref(b)}"
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, R.string.error_no_maps_app, Toast.LENGTH_SHORT).show()
        }
    }
}

/**
 * Mengelola izin lokasi (runtime permission) untuk menampilkan titik lokasi pengguna di peta.
 * Jika izin ditolak, aplikasi tetap berjalan normal (peta hanya menampilkan rute).
 * Harus dibuat sebagai properti Activity (sebelum Activity STARTED).
 */
class MyLocationController(private val activity: AppCompatActivity) {

    private var map: GoogleMap? = null

    private val launcher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) {
            enableMyLocation()
        } else {
            Toast.makeText(activity, R.string.location_denied, Toast.LENGTH_LONG).show()
        }
    }

    fun attach(googleMap: GoogleMap) {
        map = googleMap
        if (hasPermission()) enableMyLocation()
    }

    fun request() {
        if (hasPermission()) {
            enableMyLocation()
        } else {
            launcher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        val googleMap = map ?: return
        if (!hasPermission()) return
        googleMap.isMyLocationEnabled = true
        googleMap.uiSettings.isMyLocationButtonEnabled = true
    }
}
