package com.example.kampusride.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtil {

    private const val ISO = "yyyy-MM-dd"
    private val localeId: Locale = Locale.forLanguageTag("id-ID")

    fun today(): String = SimpleDateFormat(ISO, Locale.US).format(Date())

    /** Daftar [count] tanggal ISO mulai hari ini. */
    fun nextDays(count: Int): List<String> {
        val format = SimpleDateFormat(ISO, Locale.US)
        val calendar = Calendar.getInstance()
        val result = ArrayList<String>()
        repeat(count) {
            result.add(format.format(calendar.time))
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        return result
    }

    private fun parse(iso: String): Date? = try {
        SimpleDateFormat(ISO, Locale.US).parse(iso)
    } catch (e: Exception) {
        null
    }

    /** Contoh: "Kamis, 12 Nov 2026" */
    fun pretty(iso: String): String {
        val date = parse(iso) ?: return iso
        return SimpleDateFormat("EEEE, d MMM yyyy", localeId).format(date)
    }

    /** Contoh: "Kam, 12 Nov" */
    fun short(iso: String): String {
        val date = parse(iso) ?: return iso
        return SimpleDateFormat("EEE, d MMM", localeId).format(date)
    }

    fun isPast(iso: String): Boolean = iso < today()
}
