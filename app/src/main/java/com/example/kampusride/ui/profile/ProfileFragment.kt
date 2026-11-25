package com.example.kampusride.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.kampusride.R
import com.example.kampusride.databinding.FragmentProfileBinding
import com.example.kampusride.ui.auth.LoginActivity
import com.example.kampusride.util.UiState
import com.example.kampusride.util.setVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnRetry.setOnClickListener { viewModel.load() }
        binding.btnLogout.setOnClickListener { confirmLogout() }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            binding.progressBar.setVisible(state is UiState.Loading)
            binding.layoutError.setVisible(state is UiState.Error)
            binding.layoutContent.setVisible(state is UiState.Success)
            when (state) {
                is UiState.Error -> binding.tvError.text = state.message
                is UiState.Success -> {
                    val user = state.data
                    binding.tvAvatar.text = user.nama.take(1).uppercase()
                    binding.tvName.text = user.nama
                    binding.tvNim.text = user.nim
                    binding.tvJurusan.text = user.jurusan
                    binding.tvEmail.text = user.email
                }
                else -> Unit
            }
        }
    }

    private fun confirmLogout() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.logout_title)
            .setMessage(R.string.logout_message)
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.btn_yes_logout) { _, _ ->
                viewModel.logout()
                startActivity(
                    Intent(requireContext(), LoginActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
                requireActivity().finish()
            }
            .show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
