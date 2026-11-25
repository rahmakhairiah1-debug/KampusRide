package com.example.kampusride.data

import com.example.kampusride.data.model.Place

/**
 * Titik lokasi KampusRide (kampus + titik jemput per kecamatan).
 *
 * Cara kerja posisi di peta:
 *  1. Jika verified = true  -> pakai koordinat di bawah apa adanya.
 *  2. Jika verified = false -> aplikasi mencari posisi dari teks `query` lewat Geocoder Google
 *     (hasil yang jauh di luar wilayah Lhokseumawe/Aceh Utara diabaikan).
 *  3. Jika pencarian gagal  -> pakai koordinat cadangan (PERKIRAAN, bisa meleset).
 *
 * UNTUK HASIL PALING AKURAT: buka Google Maps, tekan lama pada titik yang benar, salin angka
 * koordinatnya (misal 5.2271, 97.0452), tempel ke lat/lng di bawah, lalu ubah verified menjadi true.
 */
object LocationData {

    const val BUKIT_INDAH = "Kampus Bukit Indah"
    const val REULEUT = "Kampus Reuleut"

    // Batas wilayah wajar (Lhokseumawe + Aceh Utara) untuk menolak hasil geocoding yang salah tempat
    const val MIN_LAT = 4.9
    const val MAX_LAT = 5.5
    const val MIN_LNG = 96.8
    const val MAX_LNG = 97.5

    private const val KOTA = "Kota Lhokseumawe, Aceh"
    private const val UTARA = "Kabupaten Aceh Utara, Aceh"

    private fun stop(name: String, wilayah: String, area: String, lat: Double, lng: Double) =
        Place(name, lat, lng, "$name, $wilayah, $area")

    val campuses: List<Place> = listOf(
        Place(
            BUKIT_INDAH, 5.2280, 97.0480,
            "Universitas Malikussaleh Kampus Bukit Indah, Blang Pulo, Muara Satu, $KOTA"
        ),
        Place(
            REULEUT, 5.2060, 97.0540,
            "Universitas Malikussaleh Kampus Reuleut, Reuleut Timur, Muara Batu, $UTARA"
        )
    )

    val pickupPoints: List<Place> = listOf(
        // Banda Sakti
        stop("Lapangan Hiraq", "Banda Sakti", KOTA, 5.1830, 97.1440),
        stop("Simpang Kutablang", "Banda Sakti", KOTA, 5.1860, 97.1330),
        stop("Keude Aceh", "Banda Sakti", KOTA, 5.1790, 97.1500),
        // Muara Dua
        stop("Simpang Cunda", "Muara Dua", KOTA, 5.2000, 97.1230),
        stop("Simpang Elak", "Muara Dua", KOTA, 5.1700, 97.1250),
        stop("Panggoi", "Muara Dua", KOTA, 5.1600, 97.1100),
        // Blang Mangat
        stop("Simpang Kandang", "Blang Mangat", KOTA, 5.1950, 97.1000),
        stop("Keude Puntet", "Blang Mangat", KOTA, 5.1900, 97.0900),
        // Muara Satu
        stop("Padang Sakti", "Muara Satu", KOTA, 5.2100, 97.0900),
        stop("Batuphat", "Muara Satu", KOTA, 5.2170, 97.0750),
        stop("Simpang Len Blang Pulo", "Muara Satu", KOTA, 5.2290, 97.0420),
        // Dewantara
        stop("Simpang Krueng Geukueh", "Dewantara", UTARA, 5.2400, 97.0200),
        stop("Tambon Tunong", "Dewantara", UTARA, 5.2500, 97.0100),
        // Muara Batu
        stop("Bungkah", "Muara Batu", UTARA, 5.1950, 97.0300),
        stop("Keude Mane", "Muara Batu", UTARA, 5.2100, 97.0200),
        stop("Simpang Cot Teungku Nie", "Muara Batu", UTARA, 5.2000, 97.0450)
    )

    val places: List<Place> = campuses + pickupPoints

    fun find(name: String): Place? = places.firstOrNull { it.name == name }
}
