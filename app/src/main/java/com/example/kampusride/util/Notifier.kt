package com.example.kampusride.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.kampusride.R
import com.example.kampusride.data.model.Booking
import com.example.kampusride.ui.ticket.TicketDetailActivity

/** Notifikasi lokal ketika pemesanan berhasil (fitur tambahan / bonus). */
object Notifier {

    private const val CHANNEL_ID = "kampusride_booking"

    @SuppressLint("MissingPermission")
    fun showBookingCreated(context: Context, booking: Booking) {
        // Izin notifikasi diminta saat runtime (Android 13+). Jika ditolak, aplikasi tetap berjalan normal.
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val intent = Intent(context, TicketDetailActivity::class.java)
            .putExtra(Extras.BOOKING_ID, booking.bookingId)
        val pending = PendingIntent.getActivity(
            context,
            booking.bookingId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bus)
            .setContentTitle(context.getString(R.string.notif_title))
            .setContentText(
                context.getString(
                    R.string.notif_text_format,
                    booking.ticketCode,
                    booking.seatNumber,
                    DateUtil.short(booking.travelDate)
                )
            )
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(booking.bookingId.hashCode(), notification)
    }
}
