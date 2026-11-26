package com.example.kampusride.ui.ticket

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
import com.example.kampusride.databinding.FragmentTicketBinding
import com.example.kampusride.util.Extras
import com.example.kampusride.util.UiState
import com.example.kampusride.util.setVisible

class TicketFragment : Fragment() {

    private var _binding: FragmentTicketBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TicketViewModel by activityViewModels()

    private val adapter = TicketAdapter { booking ->
        startActivity(
            Intent(requireContext(), TicketDetailActivity::class.java)
                .putExtra(Extras.BOOKING_ID, booking.bookingId)
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTicketBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvTicket.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTicket.adapter = adapter
        binding.btnRetry.setOnClickListener { viewModel.refresh() }

        binding.searchView.setQuery(viewModel.query, false)
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setQuery(newText.orEmpty())
                return true
            }
        })

        viewModel.state.observe(viewLifecycleOwner) { state ->
            binding.progressBar.setVisible(state is UiState.Loading)
            binding.layoutError.setVisible(state is UiState.Error)
            when (state) {
                is UiState.Success -> {
                    adapter.submitList(state.data)
                    val empty = state.data.isEmpty()
                    binding.tvEmpty.setVisible(empty)
                    binding.rvTicket.setVisible(!empty)
                    if (empty) {
                        binding.tvEmpty.setText(
                            if (viewModel.hasData) R.string.search_not_found else R.string.ticket_empty
                        )
                    }
                }
                is UiState.Error -> {
                    binding.tvError.text = state.message
                    binding.tvEmpty.setVisible(false)
                    binding.rvTicket.setVisible(false)
                }
                else -> {
                    binding.tvEmpty.setVisible(false)
                    binding.rvTicket.setVisible(false)
                }
            }
        }
    }

    override fun onDestroyView() {
        binding.rvTicket.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
