package com.example.kampusride.data.repository

import com.example.kampusride.data.model.Booking
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * CRUD pemesanan.
 *
 * Struktur Firestore:
 *  - bookings/{bookingId}              -> data pemesanan (milik satu pengguna)
 *  - seats/{scheduleId}_{seatNumber}   -> "kunci kursi" agar dua orang tidak mendapat kursi yang sama
 */
class BookingRepository {

    private val db = FirebaseFirestore.getInstance()
    private val bookings = db.collection("bookings")
    private val seats = db.collection("seats")

    private fun seatRef(scheduleId: String, seatNumber: String) =
        seats.document("${scheduleId}_$seatNumber")

    private fun seatData(scheduleId: String, seatNumber: String, bookingId: String, userId: String) =
        mapOf<String, Any>(
            "scheduleId" to scheduleId,
            "seatNumber" to seatNumber,
            "bookingId" to bookingId,
            "userId" to userId
        )

    private fun generateTicketCode(): String =
        "KR-" + UUID.randomUUID().toString().replace("-", "").take(8).uppercase()

    // Kursi yang sudah terisi pada satu jadwal
    suspend fun getTakenSeats(scheduleId: String): Set<String> {
        val snapshot = seats.whereEqualTo("scheduleId", scheduleId).get().await()
        return snapshot.documents.mapNotNull { it.getString("seatNumber") }.toSet()
    }

    // CREATE
    suspend fun createBooking(booking: Booking): Booking {
        val bookingRef = bookings.document()
        val newBooking = booking.copy(
            bookingId = bookingRef.id,
            ticketCode = generateTicketCode(),
            status = "Dikonfirmasi",
            createdAt = System.currentTimeMillis()
        )
        val seat = seatRef(booking.scheduleId, booking.seatNumber)

        val success = db.runTransaction { transaction ->
            if (transaction.get(seat).exists()) {
                false
            } else {
                transaction.set(
                    seat,
                    seatData(booking.scheduleId, booking.seatNumber, bookingRef.id, booking.userId)
                )
                transaction.set(bookingRef, newBooking)
                true
            }
        }.await()

        if (!success) throw SeatTakenException()
        return newBooking
    }

    // READ (daftar)
    suspend fun getBookingsByUser(userId: String): List<Booking> {
        val snapshot = bookings.whereEqualTo("userId", userId).get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Booking::class.java)?.copy(bookingId = doc.id)
        }
    }

    // READ (detail)
    suspend fun getBookingById(bookingId: String): Booking? {
        val doc = bookings.document(bookingId).get().await()
        return doc.toObject(Booking::class.java)?.copy(bookingId = doc.id)
    }

    // UPDATE (titik jemput, tujuan, kursi)
    suspend fun updateBooking(
        old: Booking,
        pickupPoint: String,
        destination: String,
        stopTime: String,
        pickupAddress: String,
        seatNumber: String
    ) {
        val bookingRef = bookings.document(old.bookingId)
        val oldSeat = seatRef(old.scheduleId, old.seatNumber)
        val newSeat = seatRef(old.scheduleId, seatNumber)

        val success = db.runTransaction { transaction ->
            var ok = true
            if (seatNumber != old.seatNumber) {
                if (transaction.get(newSeat).exists()) {
                    ok = false
                } else {
                    transaction.delete(oldSeat)
                    transaction.set(
                        newSeat,
                        seatData(old.scheduleId, seatNumber, old.bookingId, old.userId)
                    )
                }
            }
            if (ok) {
                transaction.update(
                    bookingRef,
                    mapOf<String, Any>(
                        "pickupPoint" to pickupPoint,
                        "pickupAddress" to pickupAddress,
                        "stopTime" to stopTime,
                        "destination" to destination,
                        "seatNumber" to seatNumber
                    )
                )
            }
            ok
        }.await()

        if (!success) throw SeatTakenException()
    }

    // DELETE (hapus pemesanan + lepas kursi)
    suspend fun deleteBooking(booking: Booking) {
        val batch = db.batch()
        batch.delete(bookings.document(booking.bookingId))
        batch.delete(seatRef(booking.scheduleId, booking.seatNumber))
        batch.commit().await()
    }
}
