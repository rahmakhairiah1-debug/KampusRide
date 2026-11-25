package com.example.kampusride.data.repository

import com.example.kampusride.data.model.Bus
import com.example.kampusride.data.model.Schedule
import com.example.kampusride.data.model.Stop
import com.example.kampusride.util.DateUtil
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ScheduleRepository {

    private companion object {
        const val SEED_PREFIX = "sch3_"
    }

    private val db = FirebaseFirestore.getInstance()
    private val schedules = db.collection("schedules")
    private val buses = db.collection("buses")

    private data class Route(
        val code: String,
        val koridor: Int,
        val kampus: String,
        val stops: List<Stop>,   // jam = jam jemput pada perjalanan pagi
        val tiba: String         // estimasi tiba di kampus (pagi)
    )

    private val seedBuses = listOf(
        Bus("bus_01", "Bus Kampus 01", "BK-01", 20, "Aktif"),
        Bus("bus_02", "Bus Kampus 02", "BK-02", 20, "Aktif"),
        Bus("bus_03", "Bus Kampus 03", "BK-03", 20, "Aktif")
    )

    // Koridor 1 (Kota): Banda Sakti, Muara Dua, Blang Mangat
    // Koridor 2 (Tengah): Muara Satu
    // Koridor 3 (Barat): Dewantara, Muara Batu
    private val seedRoutes = listOf(
        Route(
            "K1-RLT", 1, "Kampus Reuleut",
            listOf(
                Stop("Lapangan Hiraq", "Banda Sakti", "06:40"),
                Stop("Simpang Kutablang", "Banda Sakti", "06:45"),
                Stop("Keude Aceh", "Banda Sakti", "06:50"),
                Stop("Simpang Cunda", "Muara Dua", "06:55"),
                Stop("Simpang Elak", "Muara Dua", "07:00"),
                Stop("Panggoi", "Muara Dua", "07:05")
            ),
            "07:30"
        ),
        Route(
            "K1-BI", 1, "Kampus Bukit Indah",
            listOf(
                Stop("Simpang Kandang", "Blang Mangat", "07:00"),
                Stop("Keude Puntet", "Blang Mangat", "07:05")
            ),
            "07:25"
        ),
        Route(
            "K2-RLT", 2, "Kampus Reuleut",
            listOf(
                Stop("Padang Sakti", "Muara Satu", "07:05"),
                Stop("Batuphat", "Muara Satu", "07:10"),
                Stop("Simpang Len Blang Pulo", "Muara Satu", "07:12")
            ),
            "07:30"
        ),
        Route(
            "K2-BI", 2, "Kampus Bukit Indah",
            listOf(
                Stop("Padang Sakti", "Muara Satu", "07:15"),
                Stop("Batuphat", "Muara Satu", "07:18"),
                Stop("Simpang Len Blang Pulo", "Muara Satu", "07:22")
            ),
            "07:35"
        ),
        Route(
            "K3-BI", 3, "Kampus Bukit Indah",
            listOf(
                Stop("Simpang Krueng Geukueh", "Dewantara", "07:00"),
                Stop("Tambon Tunong", "Dewantara", "07:08")
            ),
            "07:20"
        ),
        Route(
            "K3-RLT", 3, "Kampus Reuleut",
            listOf(
                Stop("Bungkah", "Muara Batu", "07:05"),
                Stop("Keude Mane", "Muara Batu", "07:10"),
                Stop("Simpang Cot Teungku Nie", "Muara Batu", "07:15")
            ),
            "07:25"
        )
    )

    // Jam bus pulang dari kampus
    private val returnTimes = listOf("12:30", "16:30", "18:00")

    private fun addMinutes(time: String, minutes: Int): String {
        val (h, m) = time.split(":").map { it.toInt() }
        val total = h * 60 + m + minutes
        return "%02d:%02d".format(total / 60, total % 60)
    }

    private fun scheduleId(date: String, code: String) = "$SEED_PREFIX${date}_$code"

    private fun buildTrips(date: String): List<Pair<String, Schedule>> {
        val trips = ArrayList<Pair<String, Schedule>>()
        var busIndex = 0
        fun nextBus(): Bus = seedBuses[busIndex++ % seedBuses.size]

        for (route in seedRoutes) {
            // Pergi (pagi): titik jemput -> kampus
            val bus = nextBus()
            val outCode = "${route.code}-PAGI"
            trips.add(
                outCode to Schedule(
                    scheduleId = scheduleId(date, outCode), kodeRute = outCode,
                    koridor = route.koridor, arah = "Pergi", kampus = route.kampus,
                    stops = route.stops, busId = bus.busId, busName = bus.namaBus,
                    asal = route.stops.first().name, tujuan = route.kampus,
                    tanggal = date, jamBerangkat = route.stops.first().jam,
                    jamTiba = route.tiba, kapasitas = bus.kapasitas
                )
            )
            // Pulang: kampus -> titik turun (urutan dibalik)
            for (time in returnTimes) {
                val retBus = nextBus()
                val code = "${route.code}-${time.replace(":", "")}"
                val dropStops = route.stops.reversed().mapIndexed { i, stop ->
                    Stop(stop.name, stop.wilayah, addMinutes(time, 20 + i * 5))
                }
                trips.add(
                    code to Schedule(
                        scheduleId = scheduleId(date, code), kodeRute = code,
                        koridor = route.koridor, arah = "Pulang", kampus = route.kampus,
                        stops = dropStops, busId = retBus.busId, busName = retBus.namaBus,
                        asal = route.kampus, tujuan = dropStops.last().name,
                        tanggal = date, jamBerangkat = time,
                        jamTiba = dropStops.last().jam, kapasitas = retBus.kapasitas
                    )
                )
            }
        }
        return trips
    }

    /**
     * Mengisi data awal jadwal 7 hari ke depan. Dicek lewat satu dokumen penanda (hari ke-7),
     * sehingga hanya 1 pembacaan per pembukaan aplikasi. ID dokumen deterministik -> tidak ada duplikat.
     * Jadwal mendatang dari versi lama ikut dihapus.
     */
    suspend fun ensureSeedData() {
        val dates = DateUtil.nextDays(7)
        val marker = scheduleId(dates.last(), "${seedRoutes.first().code}-PAGI")
        if (schedules.document(marker).get().await().exists()) return

        val batch = db.batch()
        seedBuses.forEach { bus -> batch.set(buses.document(bus.busId), bus) }

        val old = schedules.whereGreaterThanOrEqualTo("tanggal", DateUtil.today()).get().await()
        old.documents.filter { !it.id.startsWith(SEED_PREFIX) }.forEach { batch.delete(it.reference) }

        for (date in dates) {
            for ((_, schedule) in buildTrips(date)) {
                batch.set(schedules.document(schedule.scheduleId), schedule)
            }
        }
        batch.commit().await()
    }

    suspend fun getUpcomingSchedules(): List<Schedule> {
        val snapshot = schedules
            .whereGreaterThanOrEqualTo("tanggal", DateUtil.today())
            .get()
            .await()

        return snapshot.documents
            .mapNotNull { doc -> doc.toObject(Schedule::class.java)?.copy(scheduleId = doc.id) }
            .sortedWith(compareBy({ it.tanggal }, { it.jamBerangkat }))
    }

    suspend fun getScheduleById(scheduleId: String): Schedule? {
        val doc = schedules.document(scheduleId).get().await()
        return doc.toObject(Schedule::class.java)?.copy(scheduleId = doc.id)
    }
}
