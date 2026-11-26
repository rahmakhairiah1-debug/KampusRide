package com.example.kampusride.ui.schedule

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kampusride.R
import com.example.kampusride.databinding.FragmentScheduleBinding
import com.example.kampusride.util.DateUtil
import com.example.kampusride.util.UiState
import com.example.kampusride.util.Extras
import com.example.kampusride.util.setVisible
import com.google.android.material.chip.Chip

class ScheduleFragment : Fragment() {

    private var _binding: FragmentScheduleBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ScheduleViewModel by activityViewModels()

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
        _binding = FragmentScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvSchedule.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSchedule.adapter = adapter
        binding.btnRetry.setOnClickListener { viewModel.load() }

        binding.searchView.setQuery(viewModel.query, false)
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setQuery(newText.orEmpty())
                return true
            }
        })

        binding.chipGroupCorridor.check(
            when (viewModel.corridor) {
                "TIMUR" -> R.id.chipTimur
                "BARAT" -> R.id.chipBarat
                else -> R.id.chipAll
            }
        )
        binding.chipGroupCorridor.setOnCheckedStateChangeListener { _, ids ->
            viewModel.setCorridor(
                when (ids.firstOrNull()) {
                    R.id.chipTimur -> "TIMUR"
                    R.id.chipBarat -> "BARAT"
                    else -> null
                }
            )
        }

        viewModel.dates.observe(viewLifecycleOwner) { dates -> renderChips(dates) }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            binding.progressBar.setVisible(state is UiState.Loading)
            binding.layoutError.setVisible(state is UiState.Error)
            when (state) {
                is UiState.Success -> {
                    adapter.submitList(ScheduleListItem.group(state.data))
                    val empty = state.data.isEmpty()
                    binding.tvEmpty.setVisible(empty)
                    binding.rvSchedule.setVisible(!empty)
                    if (empty) {
                        binding.tvEmpty.setText(
                            if (viewModel.hasData) R.string.search_not_found else R.string.schedule_empty
                        )
                    }
                }
                is UiState.Error -> {
                    binding.tvError.text = state.message
                    binding.tvEmpty.setVisible(false)
                    binding.rvSchedule.setVisible(false)
                }
                else -> {
                    binding.tvEmpty.setVisible(false)
                    binding.rvSchedule.setVisible(false)
                }
            }
        }
    }

    private fun renderChips(dates: List<String>) {
        val group = binding.chipGroupDates
        group.removeAllViews()
        if (dates.isEmpty()) return

        addChip(getString(R.string.chip_all), null)
        dates.forEach { addChip(DateUtil.short(it), it) }
    }

    private fun addChip(label: String, date: String?) {
        val chip = layoutInflater.inflate(R.layout.item_chip_filter, binding.chipGroupDates, false) as Chip
        chip.text = label
        chip.isChecked = viewModel.selectedDate == date
        chip.setOnClickListener { viewModel.setDate(date) }
        binding.chipGroupDates.addView(chip)
    }

    override fun onDestroyView() {
        binding.rvSchedule.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
