package com.example.kampusride.ui.ticket

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.kampusride.R
import com.example.kampusride.data.model.Booking
import com.example.kampusride.data.repository.AppException
import com.example.kampusride.data.repository.BookingRepository
import com.example.kampusride.util.AppConfig
import com.example.kampusride.util.ErrorMapper
import com.example.kampusride.util.UiState
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class TicketDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BookingRepository()

    private val _booking = MutableLiveData<UiState<Booking>>(UiState.Loading)
    val booking: LiveData<UiState<Booking>> = _booking

    private val _delete = MutableLiveData<UiState<Unit>>(UiState.Idle)
    val delete: LiveData<UiState<Unit>> = _delete

    fun load(bookingId: String) {
        if (_booking.value !is UiState.Success) _booking.value = UiState.Loading
        viewModelScope.launch {
            try {
                val result = withTimeout(AppConfig.TIMEOUT_MS) {
                    repository.getBookingById(bookingId)
                        ?: throw AppException(
                            getApplication<Application>().getString(R.string.error_booking_not_found)
                        )
                }
                _booking.value = UiState.Success(result)
            } catch (e: Exception) {
                if (_booking.value !is UiState.Success) {
                    _booking.value = UiState.Error(ErrorMapper.message(getApplication(), e))
                }
            }
        }
    }

    fun deleteBooking() {
        val current = (_booking.value as? UiState.Success)?.data ?: return
        _delete.value = UiState.Loading
        viewModelScope.launch {
            try {
                withTimeout(AppConfig.WRITE_TIMEOUT_MS) { repository.deleteBooking(current) }
                _delete.value = UiState.Success(Unit)
            } catch (e: Exception) {
                _delete.value = UiState.Error(ErrorMapper.message(getApplication(), e))
            }
        }
    }

    fun clearDeleteState() {
        _delete.value = UiState.Idle
    }
}
