package com.example.kampusride.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.kampusride.R
import com.example.kampusride.databinding.FragmentHomeBinding
import com.example.kampusride.ui.profile.ProfileViewModel
import com.example.kampusride.util.UiState
import com.example.kampusride.util.setVisible

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val profileViewModel: ProfileViewModel by activityViewModels()

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

        // Tombol pesan dan daftar jadwal terdekat diaktifkan pada tahap berikutnya
        binding.btnOrder.setVisible(false)
        binding.progressNearest.setVisible(false)
        binding.tvNearestEmpty.setText(R.string.schedule_empty)
        binding.tvNearestEmpty.setVisible(true)

        profileViewModel.state.observe(viewLifecycleOwner) { state ->
            if (state is UiState.Success && state.data.nama.isNotBlank()) {
                val firstName = state.data.nama.trim().split(" ").first()
                binding.tvGreeting.text = getString(R.string.home_greeting, firstName)
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
