package com.example.kampusride.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kampusride.MainActivity
import com.example.kampusride.R
import com.example.kampusride.databinding.FragmentHomeBinding
import com.example.kampusride.ui.profile.ProfileViewModel
import com.example.kampusride.ui.schedule.ScheduleAdapter
import com.example.kampusride.ui.schedule.ScheduleListItem
import com.example.kampusride.ui.schedule.ScheduleDetailActivity
import com.example.kampusride.ui.schedule.ScheduleViewModel
import com.example.kampusride.ui.ticket.TicketDetailActivity
import com.example.kampusride.ui.ticket.TicketViewModel
import com.example.kampusride.util.DateUtil
import com.example.kampusride.util.Extras
import com.example.kampusride.util.UiState
import com.example.kampusride.util.setVisible

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val profileViewModel: ProfileViewModel by activityViewModels()
    private val ticketViewModel: TicketViewModel by activityViewModels()
    private val scheduleViewModel: ScheduleViewModel by activityViewModels()

    private val adapter = ScheduleAdapter { schedule ->
        startActivity(
            Intent(requireContext(), ScheduleDetailActivity::class.java)
                .putExtra(Extras.SCHEDULE_ID, schedule.scheduleId)
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvNearest.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNearest.adapter = adapter
        binding.btnOrder.setOnClickListener {
            (requireActivity() as MainActivity).navigateTo(R.id.nav_schedule)
        }

        profileViewModel.state.observe(viewLifecycleOwner) { state ->
            if (state is UiState.Success && state.data.nama.isNotBlank()) {
                val firstName = state.data.nama.trim().split(" ").first()
                binding.tvGreeting.text = getString(R.string.home_greeting, firstName)
            }
        }

        ticketViewModel.activeTicket.observe(viewLifecycleOwner) { ticket ->
            if (ticket == null) {
                binding.tvActiveTitle.setText(R.string.home_no_ticket)
                binding.tvActiveDetail.setVisible(false)
                binding.cardActive.setOnClickListener(null)
                binding.cardActive.isClickable = false
            } else {
                binding.tvActiveTitle.text =
                    getString(R.string.route_format, ticket.pickupPoint, ticket.destination)
                binding.tvActiveDetail.text = getString(
                    R.string.date_time_format,
                    DateUtil.pretty(ticket.travelDate),
                    getString(R.string.ticket_seat_format, ticket.seatNumber)
                ) + " • " + ticket.departureTime
                binding.tvActiveDetail.setVisible(true)
                binding.cardActive.setOnClickListener {
                    startActivity(
                        Intent(requireContext(), TicketDetailActivity::class.java)
                            .putExtra(Extras.BOOKING_ID, ticket.bookingId)
                    )
                }
            }
        }

        scheduleViewModel.state.observe(viewLifecycleOwner) { state ->
            binding.progressNearest.setVisible(state is UiState.Loading)
            when (state) {
                is UiState.Error -> {
                    binding.tvNearestEmpty.text = state.message
                    binding.tvNearestEmpty.setVisible(true)
                    adapter.submitList(emptyList())
                }
                else -> binding.tvNearestEmpty.setVisible(false)
            }
        }

        scheduleViewModel.allSchedules.observe(viewLifecycleOwner) { list ->
            adapter.submitList(ScheduleListItem.rows(list.take(3)))
            if (list.isEmpty() && scheduleViewModel.state.value is UiState.Success) {
                binding.tvNearestEmpty.setText(R.string.schedule_empty)
                binding.tvNearestEmpty.setVisible(true)
            }
        }
    }

    override fun onDestroyView() {
        binding.rvNearest.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
