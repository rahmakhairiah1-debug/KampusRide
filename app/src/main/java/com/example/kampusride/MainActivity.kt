package com.example.kampusride

import android.content.Intent
import android.os.Bundle
import com.example.kampusride.data.repository.AuthRepository
import com.example.kampusride.databinding.ActivityMainBinding
import com.example.kampusride.ui.BaseActivity
import com.example.kampusride.ui.auth.LoginActivity
import com.example.kampusride.util.applySystemBarsPadding

/** Layar sementara setelah login. Akan diganti dengan navigasi bawah pada tahap berikutnya. */
class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Pengaman: jika sesi login sudah tidak ada, kembali ke halaman login
        if (!AuthRepository().isLoggedIn()) {
            goToLogin()
            return
        }

        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding()

        binding.btnLogout.setOnClickListener {
            AuthRepository().logout()
            goToLogin()
        }
    }

    private fun goToLogin() {
        startActivity(
            Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }
}
