package com.example.kampusride.ui.schedule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.kampusride.R
import com.example.kampusride.data.model.Schedule
import com.example.kampusride.data.repository.AppException
import com.example.kampusride.data.repository.BookingRepository
import com.example.kampusride.data.repository.ScheduleRepository
import com.example.kampusride.util.AppConfig
import com.example.kampusride.util.ErrorMapper
import com.example.kampusride.util.UiState
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

data class ScheduleDetail(val schedule: Schedule, val takenCount: Int)

class ScheduleDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val scheduleRepository = ScheduleRepository()
    private val bookingRepository = BookingRepository()

    private val _state = MutableLiveData<UiState<ScheduleDetail>>(UiState.Loading)
    val state: LiveData<UiState<ScheduleDetail>> = _state

    fun load(scheduleId: String) {
        // Muat ulang diam-diam jika data sudah tampil (misal kembali dari layar pemesanan)
        if (_state.value !is UiState.Success) _state.value = UiState.Loading
        viewModelScope.launch {
            try {
                val detail = withTimeout(AppConfig.TIMEOUT_MS) {
                    val schedule = scheduleRepository.getScheduleById(scheduleId)
                        ?: throw AppException(getApplication<Application>().getString(R.string.error_schedule_not_found))
                    ScheduleDetail(schedule, bookingRepository.getTakenSeats(scheduleId).size)
                }
                _state.value = UiState.Success(detail)
            } catch (e: Exception) {
                if (_state.value !is UiState.Success) {
                    _state.value = UiState.Error(ErrorMapper.message(getApplication(), e))
                }
            }
        }
    }
}
