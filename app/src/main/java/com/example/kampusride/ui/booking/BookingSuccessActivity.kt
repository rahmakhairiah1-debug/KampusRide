package com.example.kampusride.ui.booking

import android.content.Intent
import android.os.Bundle
import com.example.kampusride.MainActivity
import com.example.kampusride.databinding.ActivityBookingSuccessBinding
import com.example.kampusride.ui.BaseActivity
import com.example.kampusride.util.Extras
import com.example.kampusride.util.applySystemBarsPadding

/** Layar "Pemesanan Berhasil" setelah tiket dibuat. */
class BookingSuccessActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityBookingSuccessBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding()

        binding.tvTicketCode.text = intent.getStringExtra(Extras.TICKET_CODE).orEmpty()

        // Halaman tiket tersedia pada tahap berikutnya
        binding.btnViewTicket.setOnClickListener { goHome() }
        binding.btnHome.setOnClickListener { goHome() }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        goHome()
    }

    private fun goHome() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }
}
