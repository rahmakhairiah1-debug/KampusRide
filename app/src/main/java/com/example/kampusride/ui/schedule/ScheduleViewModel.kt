package com.example.kampusride.ui.schedule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.kampusride.data.model.Schedule
import com.example.kampusride.data.repository.ScheduleRepository
import com.example.kampusride.util.AppConfig
import com.example.kampusride.util.ErrorMapper
import com.example.kampusride.util.UiState
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/** Daftar jadwal + pencarian (SearchView) + filter tanggal (Chip). Dipakai bersama oleh Beranda dan Jadwal. */
class ScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ScheduleRepository()

    private var all: List<Schedule> = emptyList()

    private val _state = MutableLiveData<UiState<List<Schedule>>>(UiState.Loading)
    val state: LiveData<UiState<List<Schedule>>> = _state

    private val _allSchedules = MutableLiveData<List<Schedule>>(emptyList())
    val allSchedules: LiveData<List<Schedule>> = _allSchedules

    private val _dates = MutableLiveData<List<String>>(emptyList())
    val dates: LiveData<List<String>> = _dates

    var query: String = ""
        private set
    var selectedDate: String? = null
        private set
    var corridor: String? = null   // null = semua, "TIMUR" atau "BARAT"
        private set

    val hasData: Boolean get() = all.isNotEmpty()

    init {
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            try {
                all = withTimeout(AppConfig.TIMEOUT_MS) {
                    try {
                        repository.ensureSeedData()
                    } catch (e: Exception) {
                        // Gagal mengisi data awal tidak boleh menghalangi pembacaan jadwal
                    }
                    repository.getUpcomingSchedules()
                }
                _allSchedules.value = all
                _dates.value = all.map { it.tanggal }.distinct().sorted()
                if (selectedDate != null && selectedDate !in all.map { it.tanggal }) {
                    selectedDate = null
                }
                applyFilter()
            } catch (e: Exception) {
                _state.value = UiState.Error(ErrorMapper.message(getApplication(), e))
            }
        }
    }

    fun setQuery(newQuery: String) {
        if (newQuery == query) return
        query = newQuery
        if (_state.value is UiState.Success) applyFilter()
    }

    fun setCorridor(newCorridor: String?) {
        if (newCorridor == corridor) return
        corridor = newCorridor
        if (_state.value is UiState.Success) applyFilter()
    }

    fun setDate(date: String?) {
        if (date == selectedDate) return
        selectedDate = date
        if (_state.value is UiState.Success) applyFilter()
    }

    private fun applyFilter() {
        val q = query.trim().lowercase()
        val filtered = all.filter { s ->
            (corridor == null || s.regionKey() == corridor) &&
                (selectedDate == null || s.tanggal == selectedDate) &&
                (q.isEmpty() ||
                    s.kampus.lowercase().contains(q) ||
                    s.kodeRute.lowercase().contains(q) ||
                    s.busName.lowercase().contains(q) ||
                    s.stops.any { it.name.lowercase().contains(q) || it.wilayah.lowercase().contains(q) })
        }
        _state.value = UiState.Success(filtered)
    }
}
