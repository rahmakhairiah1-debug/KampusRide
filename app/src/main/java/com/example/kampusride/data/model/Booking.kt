package com.example.kampusride.data.model

/**
 * Entitas utama aplikasi (CRUD). Atribut selain ID: lebih dari 4.
 */
data class Booking(
    val bookingId: String = "",
    val userId: String = "",
    val scheduleId: String = "",
    val kodeRute: String = "",
    val koridor: Int = 1,
    val arah: String = "Pergi",
    val kampus: String = "",
    val pickupPoint: String = "",
    val pickupAddress: String = "",  // nama kost / alamat rumah (opsional)
    val destination: String = "",
    val travelDate: String = "",     // yyyy-MM-dd
    val departureTime: String = "",  // jam bus berangkat (HH:mm)
    val stopTime: String = "",       // jam bus di titik jemput / titik turun (HH:mm)
    val busName: String = "",
    val seatNumber: String = "",
    val ticketCode: String = "",
    val status: String = "Dikonfirmasi",
    val createdAt: Long = 0L
)
