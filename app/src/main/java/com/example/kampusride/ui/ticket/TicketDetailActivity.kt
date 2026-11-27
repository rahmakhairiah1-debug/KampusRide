package com.example.kampusride.ui.ticket

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.kampusride.R
import com.example.kampusride.data.model.Booking
import com.example.kampusride.databinding.ActivityTicketDetailBinding
import com.example.kampusride.ui.BaseActivity
import com.example.kampusride.ui.booking.BookingActivity
import com.example.kampusride.util.DateUtil
import com.example.kampusride.util.Extras
import com.example.kampusride.util.MapHelper
import com.example.kampusride.util.MyLocationController
import com.example.kampusride.util.QrUtil
import com.example.kampusride.util.UiState
import com.example.kampusride.util.applySystemBarsPadding
import com.example.kampusride.util.setVisible
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Job

class TicketDetailActivity : BaseActivity() {

    private lateinit var binding: ActivityTicketDetailBinding
    private val viewModel: TicketDetailViewModel by viewModels()
    private val locationController = MyLocationController(this)

    private var bookingId: String = ""
    private var googleMap: GoogleMap? = null
    private var currentBooking: Booking? = null
    private var routeJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTicketDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding()

        bookingId = intent.getStringExtra(Extras.BOOKING_ID).orEmpty()

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnRetry.setOnClickListener { viewModel.load(bookingId) }
        binding.btnMyLocation.setOnClickListener { locationController.request() }
        binding.btnOpenMaps.setOnClickListener {
            currentBooking?.let { MapHelper.openDirections(this, it.pickupPoint, it.destination) }
        }
        binding.btnEdit.setOnClickListener {
            startActivity(
                Intent(this, BookingActivity::class.java).putExtra(Extras.BOOKING_ID, bookingId)
            )
        }
        binding.btnDelete.setOnClickListener { confirmDelete() }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync { map ->
            googleMap = map
            locationController.attach(map)
            currentBooking?.let { drawRoute(it) }
        }

        viewModel.booking.observe(this) { state ->
            binding.progressBar.setVisible(state is UiState.Loading)
            binding.layoutError.setVisible(state is UiState.Error)
            binding.layoutContent.setVisible(state is UiState.Success)
            when (state) {
                is UiState.Error -> binding.tvError.text = state.message
                is UiState.Success -> render(state.data)
                else -> Unit
            }
        }

        viewModel.delete.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.btnDelete.isEnabled = false
                    binding.btnEdit.isEnabled = false
                }
                is UiState.Success -> {
                    Toast.makeText(this, R.string.booking_deleted, Toast.LENGTH_SHORT).show()
                    finish()
                }
                is UiState.Error -> {
                    binding.btnDelete.isEnabled = true
                    binding.btnEdit.isEnabled = true
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                    viewModel.clearDeleteState()
                }
                else -> Unit
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Muat ulang agar perubahan dari layar "Ubah Pemesanan" langsung tampil
        viewModel.load(bookingId)
    }

    private fun render(booking: Booking) {
        currentBooking = booking
        val done = DateUtil.isPast(booking.travelDate)

        // QR Code berisi data tiket agar bisa dipindai petugas di titik jemput
        val qrContent = listOf(
            "KAMPUSRIDE", booking.ticketCode, booking.bookingId,
            booking.seatNumber, booking.pickupPoint, booking.travelDate
        ).joinToString("|")
        binding.imgQr.setImageBitmap(QrUtil.generate(qrContent))

        binding.tvTicketCode.text = booking.ticketCode
        binding.tvStatus.text = if (done) getString(R.string.status_done) else booking.status
        binding.tvStatus.setBackgroundResource(if (done) R.drawable.bg_badge_gray else R.drawable.bg_badge_green)
        binding.tvStatus.setTextColor(getColor(if (done) R.color.text_gray else R.color.green))
        binding.tvRoute.text = getString(R.string.route_format, booking.pickupPoint, booking.destination)
        binding.tvRuteCode.text = booking.kodeRute.ifBlank { "-" }
        binding.tvPickup.text = booking.pickupPoint
        binding.tvDestination.text = booking.destination
        binding.tvTime.text = getString(
            R.string.stop_time_format,
            booking.stopTime.ifBlank { booking.departureTime }
        )
        binding.tvDate.text = DateUtil.pretty(booking.travelDate)
        binding.tvBus.text = booking.busName
        binding.tvSeat.text = booking.seatNumber
        binding.rowAddress.setVisible(booking.pickupAddress.isNotBlank())
        binding.tvAddress.text = booking.pickupAddress

        // Tiket yang perjalanannya sudah lewat tidak dapat diubah
        binding.btnEdit.isEnabled = !done
        drawRoute(booking)
    }

    private fun drawRoute(booking: Booking) {
        val map = googleMap ?: return
        routeJob?.cancel()
        routeJob = MapHelper.showRoute(lifecycleScope, this, map, booking.pickupPoint, booking.destination)
    }

    private fun confirmDelete() {
        val booking = currentBooking ?: return
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_title)
            .setMessage(getString(R.string.delete_message, booking.ticketCode))
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.btn_yes_delete) { _, _ -> viewModel.deleteBooking() }
            .show()
    }
}
