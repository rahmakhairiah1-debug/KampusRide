package com.example.kampusride.ui.booking

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.kampusride.R
import com.example.kampusride.data.model.Booking
import com.example.kampusride.data.model.Schedule
import com.example.kampusride.data.repository.AppException
import com.example.kampusride.data.repository.AuthRepository
import com.example.kampusride.data.repository.BookingRepository
import com.example.kampusride.data.repository.ScheduleRepository
import com.example.kampusride.data.repository.SeatTakenException
import com.example.kampusride.util.AppConfig
import com.example.kampusride.util.ErrorMapper
import com.example.kampusride.util.UiState
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

data class BookingForm(
    val schedule: Schedule,
    val takenSeats: Set<String>,
    val booking: Booking?          // null = pemesanan baru, ada isi = mode ubah
)

class BookingViewModel(application: Application) : AndroidViewModel(application) {

    private val scheduleRepository = ScheduleRepository()
    private val bookingRepository = BookingRepository()
    private val authRepository = AuthRepository()

    private val _form = MutableLiveData<UiState<BookingForm>>(UiState.Loading)
    val form: LiveData<UiState<BookingForm>> = _form

    private val _submit = MutableLiveData<UiState<Booking>>(UiState.Idle)
    val submit: LiveData<UiState<Booking>> = _submit

    var selectedSeat: String? = null
    var selectedStopName: String? = null

    private var lastScheduleId: String? = null
    private var lastBookingId: String? = null
    private var loaded = false

    private fun str(res: Int) = getApplication<Application>().getString(res)

    fun load(scheduleId: String?, bookingId: String?, force: Boolean = false) {
        if (loaded && !force) return
        lastScheduleId = scheduleId
        lastBookingId = bookingId
        _form.value = UiState.Loading
        viewModelScope.launch {
            try {
                val data = withTimeout(AppConfig.TIMEOUT_MS) {
                    val booking = if (bookingId != null) {
                        bookingRepository.getBookingById(bookingId)
                            ?: throw AppException(str(R.string.error_booking_not_found))
                    } else null

                    val sid = booking?.scheduleId ?: scheduleId
                        ?: throw AppException(str(R.string.error_schedule_not_found))
                    val schedule = scheduleRepository.getScheduleById(sid)
                        ?: throw AppException(str(R.string.error_schedule_not_found))

                    var taken = bookingRepository.getTakenSeats(sid)
                    if (booking != null) taken = taken - booking.seatNumber
                    BookingForm(schedule, taken, booking)
                }
                if (selectedSeat == null && data.booking != null) {
                    selectedSeat = data.booking.seatNumber
                }
                if (selectedStopName == null && data.booking != null) {
                    selectedStopName =
                        if (data.schedule.outbound()) data.booking.pickupPoint else data.booking.destination
                }
                loaded = true
                _form.value = UiState.Success(data)
            } catch (e: Exception) {
                _form.value = UiState.Error(ErrorMapper.message(getApplication(), e))
            }
        }
    }

    fun selectSeat(seat: String) {
        selectedSeat = seat
    }

    fun submit(pickupAddress: String) {
        val form = (_form.value as? UiState.Success)?.data ?: return
        val seat = selectedSeat ?: return
        val schedule = form.schedule
        val stop = schedule.stops.firstOrNull { it.name == selectedStopName } ?: return
        // Pergi : titik jemput (pilihan) -> kampus.  Pulang : kampus -> titik turun (pilihan)
        val pickup = if (schedule.outbound()) stop.name else schedule.kampus
        val destination = if (schedule.outbound()) schedule.kampus else stop.name

        _submit.value = UiState.Loading
        viewModelScope.launch {
            try {
                val result = withTimeout(AppConfig.WRITE_TIMEOUT_MS) {
                    val old = form.booking
                    if (old == null) {
                        val uid = authRepository.getCurrentUserId()
                            ?: throw AppException(str(R.string.error_not_logged_in))
                        bookingRepository.createBooking(
                            Booking(
                                userId = uid,
                                scheduleId = schedule.scheduleId,
                                kodeRute = schedule.kodeRute,
                                koridor = schedule.koridor,
                                arah = schedule.arah,
                                kampus = schedule.kampus,
                                pickupPoint = pickup,
                                pickupAddress = pickupAddress,
                                destination = destination,
                                travelDate = schedule.tanggal,
                                departureTime = schedule.jamBerangkat,
                                stopTime = stop.jam,
                                busName = schedule.busName,
                                seatNumber = seat
                            )
                        )
                    } else {
                        bookingRepository.updateBooking(old, pickup, destination, stop.jam, pickupAddress, seat)
                        old.copy(
                            pickupPoint = pickup,
                            pickupAddress = pickupAddress,
                            destination = destination,
                            stopTime = stop.jam,
                            seatNumber = seat
                        )
                    }
                }
                _submit.value = UiState.Success(result)
            } catch (e: Exception) {
                _submit.value = UiState.Error(ErrorMapper.message(getApplication(), e))
                if (e is SeatTakenException) {
                    // Perbarui peta kursi supaya kursi yang baru diambil orang lain tampil terisi
                    selectedSeat = null
                    load(lastScheduleId, lastBookingId, force = true)
                }
            }
        }
    }
}
