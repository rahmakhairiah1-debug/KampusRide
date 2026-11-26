package com.example.kampusride.ui.ticket

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.kampusride.data.model.Booking
import com.example.kampusride.data.repository.AuthRepository
import com.example.kampusride.data.repository.BookingRepository
import com.example.kampusride.util.AppConfig
import com.example.kampusride.util.DateUtil
import com.example.kampusride.util.ErrorMapper
import com.example.kampusride.util.UiState
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/** Daftar tiket milik pengguna (READ) + pencarian. Dipakai bersama oleh Beranda dan Tiket. */
class TicketViewModel(application: Application) : AndroidViewModel(application) {

    private val bookingRepository = BookingRepository()
    private val authRepository = AuthRepository()

    private var all: List<Booking> = emptyList()
    private var hasLoaded = false

    private val _state = MutableLiveData<UiState<List<Booking>>>(UiState.Loading)
    val state: LiveData<UiState<List<Booking>>> = _state

    private val _activeTicket = MutableLiveData<Booking?>(null)
    val activeTicket: LiveData<Booking?> = _activeTicket

    var query: String = ""
        private set

    val hasData: Boolean get() = all.isNotEmpty()

    /** Muat ulang daftar tiket. Loading hanya ditampilkan jika belum ada data. */
    fun refresh() {
        if (!hasLoaded) _state.value = UiState.Loading
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId() ?: return@launch
                val list = withTimeout(AppConfig.TIMEOUT_MS) {
                    bookingRepository.getBookingsByUser(userId)
                }
                all = sortTickets(list)
                hasLoaded = true
                _activeTicket.value = all.firstOrNull { !DateUtil.isPast(it.travelDate) }
                applyFilter()
            } catch (e: Exception) {
                if (!hasLoaded) {
                    _state.value = UiState.Error(ErrorMapper.message(getApplication(), e))
                }
            }
        }
    }

    fun setQuery(newQuery: String) {
        if (newQuery == query) return
        query = newQuery
        if (hasLoaded) applyFilter()
    }

    // Tiket mendatang (terdekat dulu), lalu tiket yang sudah lewat (terbaru dulu)
    private fun sortTickets(list: List<Booking>): List<Booking> {
        val (upcoming, past) = list.partition { !DateUtil.isPast(it.travelDate) }
        return upcoming.sortedWith(compareBy({ it.travelDate }, { it.departureTime })) +
            past.sortedWith(compareByDescending<Booking> { it.travelDate }.thenByDescending { it.departureTime })
    }

    private fun applyFilter() {
        val q = query.trim().lowercase()
        val filtered = if (q.isEmpty()) all else all.filter {
            it.ticketCode.lowercase().contains(q) ||
                it.kodeRute.lowercase().contains(q) ||
                it.pickupPoint.lowercase().contains(q) ||
                it.pickupAddress.lowercase().contains(q) ||
                it.destination.lowercase().contains(q) ||
                it.seatNumber.lowercase() == q
        }
        _state.value = UiState.Success(filtered)
    }
}
