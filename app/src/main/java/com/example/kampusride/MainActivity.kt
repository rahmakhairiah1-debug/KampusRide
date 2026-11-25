package com.example.kampusride

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.kampusride.data.repository.AuthRepository
import com.example.kampusride.databinding.ActivityMainBinding
import com.example.kampusride.ui.BaseActivity
import com.example.kampusride.ui.auth.LoginActivity
import com.example.kampusride.ui.home.HomeFragment
import com.example.kampusride.ui.profile.ProfileFragment
import com.example.kampusride.util.applySystemBarsPadding

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Pengaman: jika sesi login sudah tidak ada, kembali ke halaman login
        if (!AuthRepository().isLoggedIn()) {
            startActivity(
                Intent(this, LoginActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Padding bawah ditangani sendiri oleh BottomNavigationView
        binding.root.applySystemBarsPadding(bottom = false)

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_profile -> ProfileFragment()
                else -> return@setOnItemSelectedListener false
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit()
            true
        }
        binding.bottomNavigation.setOnItemReselectedListener { }

        if (savedInstanceState == null) {
            binding.bottomNavigation.selectedItemId = R.id.nav_home
        }
    }

    fun navigateTo(menuId: Int) {
        binding.bottomNavigation.selectedItemId = menuId
    }
}
