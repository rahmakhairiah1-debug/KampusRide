package com.example.kampusride.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.activity.viewModels
import com.example.kampusride.MainActivity
import com.example.kampusride.R
import com.example.kampusride.databinding.ActivityLoginBinding
import com.example.kampusride.ui.BaseActivity
import com.example.kampusride.util.UiState
import com.example.kampusride.util.applySystemBarsPadding
import com.example.kampusride.util.setVisible

class LoginActivity : BaseActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding()

        binding.btnLogin.setOnClickListener { attemptLogin() }
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        binding.tvForgot.setOnClickListener { attemptReset() }

        viewModel.state.observe(this) { state ->
            val loading = state is UiState.Loading
            binding.progressBar.setVisible(loading)
            binding.btnLogin.isEnabled = !loading
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

        viewModel.resetState.observe(this) { state ->
            when (state) {
                is UiState.Success -> {
                    binding.tvInfo.setText(R.string.reset_email_sent)
                    binding.tvInfo.setVisible(true)
                }
                is UiState.Error -> {
                    binding.tvError.text = state.message
                    binding.tvError.setVisible(true)
                }
                else -> Unit
            }
        }
    }

    private fun attemptLogin() {
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tvInfo.setVisible(false)

        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        var valid = true
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
        if (valid) viewModel.login(email, password)
    }

    private fun attemptReset() {
        binding.tilEmail.error = null
        binding.tvInfo.setVisible(false)
        val email = binding.etEmail.text.toString().trim()
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.reset_need_email)
            return
        }
        viewModel.resetPassword(email)
    }
}
