package com.example.kampusride.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.example.kampusride.MainActivity
import com.example.kampusride.data.repository.AuthRepository
import com.example.kampusride.databinding.ActivitySplashBinding
import com.example.kampusride.ui.auth.LoginActivity
import com.example.kampusride.util.applySystemBarsPadding

class SplashActivity : BaseActivity() {

    private val handler = Handler(Looper.getMainLooper())

    private val goNext = Runnable {
        // Sesi login tersimpan oleh Firebase Auth -> langsung ke beranda jika sudah login
        val target = if (AuthRepository().isLoggedIn()) {
            MainActivity::class.java
        } else {
            LoginActivity::class.java
        }
        startActivity(Intent(this, target))
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding()
        handler.postDelayed(goNext, 1200)
    }

    override fun onDestroy() {
        handler.removeCallbacks(goNext)
        super.onDestroy()
    }
}
