package com.nextgencaller.utils

object Constants {
    // Database
    const val DATABASE_NAME = "nextgen_caller_db"
    const val DATABASE_VERSION = 1

    // WebRTC Configuration
    const val STUN_SERVER_1 = "stun:stun.l.google.com:19302"
    const val STUN_SERVER_2 = "stun:stun1.l.google.com:19302"
    const val STUN_SERVER_3 = "stun:stun2.l.google.com:19302"

    // Quality Thresholds
    const val PACKET_LOSS_THRESHOLD_POOR = 5.0
    const val PACKET_LOSS_THRESHOLD_FAIR = 2.0
    const val PACKET_LOSS_THRESHOLD_GOOD = 1.0

    const val RTT_THRESHOLD_POOR = 400.0
    const val RTT_THRESHOLD_FAIR = 250.0
    const val RTT_THRESHOLD_GOOD = 150.0

    // Reconnection
    const val MAX_RECONNECTION_ATTEMPTS = 5
    const val INITIAL_RECONNECTION_DELAY_MS = 1000L
    const val MAX_RECONNECTION_DELAY_MS = 10_000L

    // Call Log Cleanup
    const val CALL_LOG_RETENTION_DAYS = 90L

    // Preferences
    const val PREF_NAME = "nextgen_caller_prefs"
    const val PREF_USER_ID = "user_id"
    const val PREF_FCM_TOKEN = "fcm_token"
}