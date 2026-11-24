package com.example.kampusride.data.repository

import com.example.kampusride.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Registrasi mahasiswa + simpan profil di koleksi "users"
    suspend fun register(
        nama: String,
        nim: String,
        jurusan: String,
        email: String,
        password: String
    ): String {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val firebaseUser = result.user
            ?: throw AppException("Akun gagal dibuat. Silakan coba lagi.")

        val profileUpdate = UserProfileChangeRequest.Builder()
            .setDisplayName(nama)
            .build()
        firebaseUser.updateProfile(profileUpdate).await()

        val user = User(
            uid = firebaseUser.uid,
            nama = nama,
            nim = nim,
            email = email,
            jurusan = jurusan
        )
        db.collection("users").document(firebaseUser.uid).set(user).await()
        return firebaseUser.uid
    }

    // Login (sesi disimpan otomatis oleh Firebase Authentication)
    suspend fun login(email: String, password: String): String {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        return result.user?.uid ?: throw AppException("Login gagal. Silakan coba lagi.")
    }

    fun logout() {
        auth.signOut()
    }

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    fun isLoggedIn(): Boolean = auth.currentUser != null

    // Profil pengguna. Jika dokumen Firestore tidak ada, pakai data dari akun Auth.
    suspend fun getUserProfile(): User {
        val current = auth.currentUser
            ?: throw AppException("Sesi login berakhir. Silakan masuk kembali.")

        val fallback = User(
            uid = current.uid,
            nama = current.displayName
                ?: current.email?.substringBefore("@")
                ?: "",
            nim = "-",
            email = current.email ?: "",
            jurusan = "-"
        )

        return try {
            val snapshot = db.collection("users").document(current.uid).get().await()
            snapshot.toObject(User::class.java)?.copy(uid = current.uid) ?: fallback
        } catch (e: Exception) {
            fallback
        }
    }

    suspend fun resetPassword(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }
}
