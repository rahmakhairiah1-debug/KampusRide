package com.example.kampusride.data.model

/**
 * Satu titik lokasi.
 *  - [query]    : teks pencarian (nama tempat + wilayah) yang di-geocode Google saat aplikasi berjalan.
 *  - [latitude]/[longitude] : koordinat cadangan (PERKIRAAN) bila geocoding gagal.
 *  - [verified] : isi true HANYA jika koordinat sudah kamu salin dari Google Maps; maka koordinat
 *                 ini dipakai langsung dan geocoding dilewati.
 */
data class Place(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val query: String = name,
    val verified: Boolean = false
)
