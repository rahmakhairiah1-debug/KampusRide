package com.example.kampusride.util

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/** Tambahkan padding agar konten tidak tertutup status bar / navigation bar (edge-to-edge). */
fun View.applySystemBarsPadding(top: Boolean = true, bottom: Boolean = true) {
    val left = paddingLeft
    val topPad = paddingTop
    val right = paddingRight
    val bottomPad = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        v.setPadding(
            left + bars.left,
            if (top) topPad + bars.top else topPad,
            right + bars.right,
            if (bottom) bottomPad + bars.bottom else bottomPad
        )
        insets
    }
}

fun View.setVisible(visible: Boolean) {
    visibility = if (visible) View.VISIBLE else View.GONE
}
