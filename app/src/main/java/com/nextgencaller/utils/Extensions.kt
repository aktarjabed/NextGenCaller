package com.nextgencaller.utils

import android.content.Context
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*

fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun Long.toFormattedDate(): String {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return dateFormat.format(Date(this))
}

fun Long.toFormattedTime(): String {
    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    return dateFormat.format(Date(this))
}

fun Long.toCallDuration(): String {
    val seconds = (this / 1000) % 60
    val minutes = (this / (1000 * 60)) % 60
    val hours = (this / (1000 * 60 * 60))

    return when {
        hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, seconds)
        minutes > 0 -> String.format("%02d:%02d", minutes, seconds)
        else -> String.format("00:%02d", seconds)
    }
}

fun String.isValidPhoneNumber(): Boolean {
    val phoneRegex = Regex("^[+]?[0-9]{10,13}$")
    return phoneRegex.matches(this)
}