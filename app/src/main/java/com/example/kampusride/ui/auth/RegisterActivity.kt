package com.example.kampusride.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.activity.viewModels
import com.example.kampusride.MainActivity
import com.example.kampusride.R
import com.example.kampusride.databinding.ActivityRegisterBinding
import com.example.kampusride.ui.BaseActivity
import com.example.kampusride.util.UiState
import com.example.kampusride.util.applySystemBarsPadding
import com.example.kampusride.util.setVisible

class RegisterActivity : BaseActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding()

        binding.btnRegister.setOnClickListener { attemptRegister() }
        binding.tvLogin.setOnClickListener { finish() }

        viewModel.state.observe(this) { state ->
            val loading = state is UiState.Loading
            binding.progressBar.setVisible(loading)
            binding.btnRegister.isEnabled = !loading
            when (state) {
                is UiState.Error -> {
                    binding.tvError.text = state.message
                    binding.tvError.setVisible(true)
                }
                is UiState.Success -> {
                    startActivity(
                        Intent(this, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    )
                    finish()
                }
                else -> binding.tvError.setVisible(false)
            }
        }
    }

    private fun attemptRegister() {
        listOf(
            binding.tilNama, binding.tilNim, binding.tilJurusan,
            binding.tilEmail, binding.tilPassword, binding.tilConfirm
        ).forEach { it.error = null }

        val nama = binding.etNama.text.toString().trim()
        val nim = binding.etNim.text.toString().trim()
        val jurusan = binding.etJurusan.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirm = binding.etConfirm.text.toString()

        var valid = true
        if (nama.isEmpty()) {
            binding.tilNama.error = getString(R.string.err_required)
            valid = false
        }
        if (nim.isEmpty()) {
            binding.tilNim.error = getString(R.string.err_required)
            valid = false
        } else if (!nim.matches(Regex("^[0-9]{8,15}$"))) {
            binding.tilNim.error = getString(R.string.err_nim_invalid)
            valid = false
        }
        if (jurusan.isEmpty()) {
            binding.tilJurusan.error = getString(R.string.err_required)
            valid = false
        }
        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.err_required)
            valid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.err_email_invalid)
            valid = false
        }
        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.err_required)
            valid = false
        } else if (password.length < 6) {
            binding.tilPassword.error = getString(R.string.err_password_short)
            valid = false
        }
        if (confirm != password) {
            binding.tilConfirm.error = getString(R.string.err_password_mismatch)
            valid = false
        }
        if (valid) viewModel.register(nama, nim, jurusan, email, password)
    }
}
