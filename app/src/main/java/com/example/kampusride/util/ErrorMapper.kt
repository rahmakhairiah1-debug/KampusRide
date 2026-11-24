package com.example.kampusride.util

import android.content.Context
import com.example.kampusride.R
import com.example.kampusride.data.repository.AppException
import com.example.kampusride.data.repository.SeatTakenException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.TimeoutCancellationException
import java.net.UnknownHostException

/** Mengubah exception teknis menjadi pesan Bahasa Indonesia yang jelas untuk pengguna. */
object ErrorMapper {

    fun message(context: Context, error: Throwable): String {
        if (error is AppException) {
            return error.message ?: context.getString(R.string.error_unknown)
        }
        val res = when (error) {
            is TimeoutCancellationException -> R.string.error_timeout
            is FirebaseNetworkException, is UnknownHostException -> R.string.error_network
            is FirebaseAuthWeakPasswordException -> R.string.error_weak_password
            is FirebaseAuthUserCollisionException -> R.string.error_email_exists
            is FirebaseAuthInvalidUserException -> R.string.error_user_not_found
            is FirebaseAuthInvalidCredentialsException -> R.string.error_invalid_credentials
            is FirebaseTooManyRequestsException -> R.string.error_too_many_requests
            is SeatTakenException -> R.string.error_seat_taken
            is FirebaseFirestoreException -> when (error.code) {
                FirebaseFirestoreException.Code.UNAVAILABLE -> R.string.error_network
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> R.string.error_timeout
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> R.string.error_permission_denied
                else -> R.string.error_unknown
            }
            else -> R.string.error_unknown
        }
        return context.getString(res)
    }
}
