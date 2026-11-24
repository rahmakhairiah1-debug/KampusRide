package com.example.kampusride.data.repository

/** Kursi sudah dipesan pengguna lain. */
class SeatTakenException : Exception("Kursi sudah dipesan")

/** Kesalahan aplikasi dengan pesan yang sudah siap ditampilkan ke pengguna. */
class AppException(message: String) : Exception(message)
