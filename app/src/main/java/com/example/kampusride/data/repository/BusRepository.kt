package com.example.kampusride.data.repository

import com.example.kampusride.data.model.Bus
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class BusRepository {

    private val db = FirebaseFirestore.getInstance()

    private val busCollection = db.collection("buses")

    // Mengambil semua data bus
    suspend fun getAllBuses(): List<Bus> {
        val snapshot = busCollection.get().await()

        return snapshot.documents.mapNotNull { document ->
            document.toObject(Bus::class.java)
                ?.copy(busId = document.id)
        }
    }

    // Mengambil bus berdasarkan ID
    suspend fun getBusById(busId: String): Bus? {
        val document = busCollection
            .document(busId)
            .get()
            .await()

        return document.toObject(Bus::class.java)
            ?.copy(busId = document.id)
    }

    // Menambahkan bus
    suspend fun addBus(bus: Bus): String {
        val document = busCollection.document()

        val newBus = bus.copy(
            busId = document.id
        )

        document.set(newBus).await()

        return document.id
    }

    // Mengubah data bus
    suspend fun updateBus(bus: Bus) {
        busCollection.document(bus.busId)
            .set(bus)
            .await()
    }

    // Menghapus bus
    suspend fun deleteBus(busId: String) {
        busCollection.document(busId)
            .delete()
            .await()
    }
}