package com.example.kampusride.ui.schedule

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.kampusride.R
import com.example.kampusride.data.model.Schedule
import com.example.kampusride.databinding.ActivityScheduleDetailBinding
import com.example.kampusride.databinding.ItemStopBinding
import com.example.kampusride.ui.BaseActivity
import com.example.kampusride.ui.booking.BookingActivity
import com.example.kampusride.util.DateUtil
import com.example.kampusride.util.Extras
import com.example.kampusride.util.MapHelper
import com.example.kampusride.util.MyLocationController
import com.example.kampusride.util.UiState
import com.example.kampusride.util.applySystemBarsPadding
import com.example.kampusride.util.setVisible
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import kotlinx.coroutines.Job

class ScheduleDetailActivity : BaseActivity() {

    private lateinit var binding: ActivityScheduleDetailBinding
    private val viewModel: ScheduleDetailViewModel by viewModels()
    private val locationController = MyLocationController(this)

    private var scheduleId: String = ""
    private var googleMap: GoogleMap? = null
    private var currentSchedule: Schedule? = null
    private var routeJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScheduleDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding()

        scheduleId = intent.getStringExtra(Extras.SCHEDULE_ID).orEmpty()

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnRetry.setOnClickListener { viewModel.load(scheduleId) }
        binding.btnMyLocation.setOnClickListener { locationController.request() }
        binding.btnOpenMaps.setOnClickListener {
            currentSchedule?.let {
                val names = pathNames(it)
                MapHelper.openDirections(this, names.first(), names.last())
            }
        }
        binding.btnBook.setOnClickListener {
            startActivity(
                Intent(this, BookingActivity::class.java)
                    .putExtra(Extras.SCHEDULE_ID, scheduleId)
            )
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync { map ->
            googleMap = map
            locationController.attach(map)
            currentSchedule?.let { drawRoute(it) }
        }

        viewModel.state.observe(this) { state ->
            binding.progressBar.setVisible(state is UiState.Loading)
            binding.layoutError.setVisible(state is UiState.Error)
            binding.layoutContent.setVisible(state is UiState.Success)
            when (state) {
                is UiState.Error -> binding.tvError.text = state.message
                is UiState.Success -> render(state.data)
                else -> Unit
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Jumlah kursi tersedia diperbarui setiap kembali dari layar pemesanan
        viewModel.load(scheduleId)
    }

    private fun render(detail: ScheduleDetail) {
        val schedule = detail.schedule
        currentSchedule = schedule
        val left = (schedule.kapasitas - detail.takenCount).coerceAtLeast(0)
        val outbound = schedule.outbound()

        binding.tvCorridor.text = schedule.corridorLabel()
        binding.tvTitle.text = if (outbound) schedule.wilayahText() else schedule.kampus
        binding.tvRoute.text = getString(
            R.string.subtitle_to,
            if (outbound) schedule.kampus else schedule.wilayahText()
        )
        binding.tvDate.text = DateUtil.pretty(schedule.tanggal)
        binding.tvDepartLabel.setText(
            if (outbound) R.string.label_depart_pergi else R.string.label_depart_pulang
        )
        binding.tvDepart.text = getString(R.string.stop_time_format, schedule.jamBerangkat)
        binding.tvArrive.text = getString(R.string.stop_time_format, schedule.jamTiba)
        binding.tvBus.text = schedule.busName
        binding.tvSeats.text = getString(R.string.seats_left_format, left, schedule.kapasitas)
        binding.tvStopsLabel.setText(
            if (outbound) R.string.label_stops_pergi else R.string.label_stops_pulang
        )
        renderStops(schedule)

        val past = DateUtil.isPast(schedule.tanggal)
        val full = left == 0
        binding.btnBook.isEnabled = !past && !full
        binding.tvBookInfo.setVisible(past || full)
        binding.tvBookInfo.setText(if (past) R.string.schedule_past else R.string.schedule_full)

        drawRoute(schedule)
    }

    private fun drawRoute(schedule: Schedule) {
        val map = googleMap ?: return
        routeJob?.cancel()
        routeJob = MapHelper.showPath(lifecycleScope, this, map, pathNames(schedule))
    }

    private fun pathNames(schedule: Schedule): List<String> =
        if (schedule.outbound()) schedule.stops.map { it.name } + schedule.kampus
        else listOf(schedule.kampus) + schedule.stops.map { it.name }

    /** Daftar titik jemput/turun berurutan (timeline) beserta jamnya. */
    private fun renderStops(schedule: Schedule) {
        binding.layoutStops.removeAllViews()
        data class Entry(val text: String, val time: String, val isCampus: Boolean)

        val stops = schedule.stops.map { Entry(it.label(), it.jam, false) }
        val campusEntry = Entry(
            schedule.kampus,
            if (schedule.outbound()) schedule.jamTiba else schedule.jamBerangkat,
            true
        )
        val entries = if (schedule.outbound()) stops + campusEntry else listOf(campusEntry) + stops

        for (entry in entries) {
            val item = ItemStopBinding.inflate(layoutInflater, binding.layoutStops, false)
            item.tvStopName.text = entry.text
            item.tvStopTime.text = getString(R.string.stop_time_format, entry.time)
            if (entry.isCampus) item.viewDot.setBackgroundResource(R.drawable.bg_dot_end)
            binding.layoutStops.addView(item.root)
        }
    }
}
