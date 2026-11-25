package com.example.kampusride.data.model

/** Satu titik naik (pergi) atau titik turun (pulang) pada sebuah rute bus. */
data class Stop(
    val name: String = "",     // contoh: Simpang Kutablang
    val wilayah: String = "",  // kecamatan, contoh: Banda Sakti
    val jam: String = ""       // jam bus tiba di titik ini (HH:mm)
) {
    fun label(): String = "$name ($wilayah)"
}

/**
 * Satu perjalanan bus pada tanggal tertentu.
 * Arah "Pergi"  : titik jemput (stops) -> kampus.
 * Arah "Pulang" : kampus -> titik turun (stops).
 *
 * Catatan: fungsi turunan sengaja berupa fungsi biasa (bukan properti) agar tidak ikut
 * tersimpan sebagai field di Firestore.
 */
data class Schedule(
    val scheduleId: String = "",
    val kodeRute: String = "",
    val koridor: Int = 1,
    val arah: String = "Pergi",
    val kampus: String = "",
    val stops: List<Stop> = emptyList(),
    val busId: String = "",
    val busName: String = "",
    val asal: String = "",
    val tujuan: String = "",
    val tanggal: String = "",      // ISO: yyyy-MM-dd
    val jamBerangkat: String = "", // jam bus berangkat dari titik pertama
    val jamTiba: String = "",      // estimasi tiba di titik terakhir
    val kapasitas: Int = 0
) {
    fun outbound(): Boolean = arah == "Pergi"

    fun wilayahText(): String = stops.map { it.wilayah }.distinct().joinToString(" – ")

    fun corridorLabel(): String = when (koridor) {
        1 -> "Koridor 1 - Kota"
        2 -> "Koridor 2 - Tengah"
        else -> "Koridor 3 - Barat"
    }

    /** TIMUR = koridor 1 & 2 (dari pusat kota), BARAT = koridor 3 (Aceh Utara). */
    fun regionKey(): String = if (koridor <= 2) "TIMUR" else "BARAT"
}
