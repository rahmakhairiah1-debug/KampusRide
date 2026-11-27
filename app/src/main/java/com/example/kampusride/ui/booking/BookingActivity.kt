package com.example.kampusride.ui.booking

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.example.kampusride.R
import com.example.kampusride.data.model.Booking
import com.example.kampusride.data.model.Stop
import com.example.kampusride.databinding.ActivityBookingBinding
import com.example.kampusride.ui.BaseActivity
import com.example.kampusride.util.DateUtil
import com.example.kampusride.util.Extras
import com.example.kampusride.util.Notifier
import com.example.kampusride.util.UiState
import com.example.kampusride.util.applySystemBarsPadding
import com.example.kampusride.util.setVisible

/**
 * Form pemesanan. Dipakai untuk CREATE (extra SCHEDULE_ID) dan UPDATE (extra BOOKING_ID).
 * Mahasiswa memilih titik jemput (rute pergi) atau titik turun (rute pulang), lalu kursi.
 */
class BookingActivity : BaseActivity() {

    private lateinit var binding: ActivityBookingBinding
    private val viewModel: BookingViewModel by viewModels()

    private var scheduleId: String? = null
    private var bookingId: String? = null
    private var currentStops: List<Stop> = emptyList()

    private val seatAdapter = SeatAdapter { seat ->
        viewModel.selectSeat(seat)
        showSelectedSeat(seat)
        binding.tvFormError.setVisible(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding()

        scheduleId = intent.getStringExtra(Extras.SCHEDULE_ID)
        bookingId = intent.getStringExtra(Extras.BOOKING_ID)
        val editMode = bookingId != null

        binding.toolbar.setTitle(if (editMode) R.string.booking_title_edit else R.string.booking_title_new)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnSubmit.setText(if (editMode) R.string.btn_save_changes else R.string.btn_confirm_booking)

        binding.rvSeats.layoutManager = GridLayoutManager(this, 4)
        binding.rvSeats.adapter = seatAdapter

        binding.btnRetry.setOnClickListener {
            viewModel.load(scheduleId, bookingId, force = true)
        }
        binding.btnSubmit.setOnClickListener { attemptSubmit() }
        binding.actStop.setOnItemClickListener { _, _, position, _ ->
            currentStops.getOrNull(position)?.let { viewModel.selectedStopName = it.name }
            binding.tilStop.error = null
        }

        viewModel.load(scheduleId, bookingId)

        viewModel.form.observe(this) { state ->
            binding.progressBar.setVisible(state is UiState.Loading)
            binding.layoutError.setVisible(state is UiState.Error)
            binding.layoutContent.setVisible(state is UiState.Success)
            when (state) {
                is UiState.Error -> binding.tvError.text = state.message
                is UiState.Success -> renderForm(state.data)
                else -> Unit
            }
        }

        viewModel.submit.observe(this) { state ->
            val loading = state is UiState.Loading
            binding.progressSubmit.setVisible(loading)
            binding.btnSubmit.isEnabled = !loading
            when (state) {
                is UiState.Error -> {
                    binding.tvFormError.text = state.message
                    binding.tvFormError.setVisible(true)
                }
                is UiState.Success -> onSuccess(state.data, editMode)
                else -> binding.tvFormError.setVisible(false)
            }
        }
    }

    private fun renderForm(form: BookingForm) {
        val schedule = form.schedule
        val outbound = schedule.outbound()
        currentStops = schedule.stops

        binding.tvSummaryCode.text =
            getString(R.string.meta_format, schedule.kodeRute, schedule.corridorLabel())
        binding.tvSummaryRoute.text = if (outbound) {
            getString(R.string.route_format, schedule.wilayahText(), schedule.kampus)
        } else {
            getString(R.string.route_format, schedule.kampus, schedule.wilayahText())
        }
        binding.tvSummaryDateTime.text = getString(
            R.string.date_time_format,
            DateUtil.pretty(schedule.tanggal),
            getString(R.string.stop_time_format, schedule.jamBerangkat)
        )
        binding.tvSummaryBus.text = schedule.busName

        binding.tilStop.hint =
            getString(if (outbound) R.string.hint_stop_pergi else R.string.hint_stop_pulang)
        binding.tilCampus.hint =
            getString(if (outbound) R.string.hint_campus_pergi else R.string.hint_campus_pulang)
        binding.etCampus.setText(schedule.kampus)

        val labels = schedule.stops.map {
            getString(R.string.stop_option_format, it.label(), it.jam)
        }
        binding.actStop.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, labels)
        )
        val selected = schedule.stops.indexOfFirst { it.name == viewModel.selectedStopName }
        if (selected >= 0) binding.actStop.setText(labels[selected], false)

        if (binding.etAddress.text.isNullOrEmpty() && form.booking != null) {
            binding.etAddress.setText(form.booking.pickupAddress)
        }

        // Kursi diberi label baris-kolom: 1A 1B 1C 1D / 2A 2B ...
        val seats = (0 until schedule.kapasitas).map { "${it / 4 + 1}${'A' + it % 4}" }
        seatAdapter.submit(seats, form.takenSeats, viewModel.selectedSeat)
        showSelectedSeat(viewModel.selectedSeat)
    }

    private fun showSelectedSeat(seat: String?) {
        binding.tvSelectedSeat.text =
            if (seat == null) getString(R.string.no_seat_selected)
            else getString(R.string.selected_seat_format, seat)
    }

    private fun attemptSubmit() {
        binding.tilStop.error = null
        binding.tvFormError.setVisible(false)

        var valid = true
        if (viewModel.selectedStopName == null) {
            binding.tilStop.error = getString(R.string.err_stop_required)
            valid = false
        }
        if (viewModel.selectedSeat == null) {
            binding.tvFormError.setText(R.string.err_seat_required)
            binding.tvFormError.setVisible(true)
            valid = false
        }
        if (valid) viewModel.submit(binding.etAddress.text.toString().trim())
    }

    private fun onSuccess(booking: Booking, editMode: Boolean) {
        if (editMode) {
            Toast.makeText(this, R.string.booking_updated, Toast.LENGTH_SHORT).show()
        } else {
            Notifier.showBookingCreated(this, booking)
            startActivity(
                Intent(this, BookingSuccessActivity::class.java)
                    .putExtra(Extras.BOOKING_ID, booking.bookingId)
                    .putExtra(Extras.TICKET_CODE, booking.ticketCode)
            )
        }
        finish()
    }
}
