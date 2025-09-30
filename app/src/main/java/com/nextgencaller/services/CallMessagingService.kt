package com.nextgencaller.services

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.nextgencaller.data.remote.SignalingClient
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class CallMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var signalingClient: SignalingClient

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Timber.d("📨 FCM message received: ${message.data}")

        when (message.data["type"]) {
            "incoming_call" -> handleIncomingCall(message.data)
            "call_ended" -> handleCallEnded(message.data)
            "call_cancelled" -> handleCallCancelled(message.data)
            else -> Timber.w("⚠️ Unknown message type: ${message.data["type"]}")
        }
    }

    private fun handleIncomingCall(data: Map<String, String>) {
        try {
            val callId = data["call_id"] ?: return
            val callerName = data["caller_name"] ?: "Unknown"
            val callerUserId = data["caller_user_id"] ?: return
            val isVideo = data["is_video"]?.toBoolean() ?: false

            val notification = notificationHelper.buildIncomingCallNotification(
                callId = callId,
                callerName = callerName,
                isVideo = isVideo
            )

            notificationHelper.notificationManager.notify(
                NotificationHelper.NOTIFICATION_ID_INCOMING,
                notification
            )

            if (!signalingClient.isConnected()) {
                signalingClient.connect(getUserId())
            }

            Timber.d("✅ Incoming call notification shown for $callerName")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to handle incoming call")
        }
    }

    private fun handleCallEnded(data: Map<String, String>) {
        notificationHelper.cancelNotification(NotificationHelper.NOTIFICATION_ID_INCOMING)
        notificationHelper.cancelNotification(NotificationHelper.NOTIFICATION_ID_ONGOING)
        Timber.d("🛑 Call ended notification cleared")
    }

    private fun handleCallCancelled(data: Map<String, String>) {
        val callerName = data["caller_name"] ?: "Unknown"
        notificationHelper.cancelNotification(NotificationHelper.NOTIFICATION_ID_INCOMING)
        notificationHelper.showMissedCallNotification(callerName)
        Timber.d("📞 Missed call notification shown for $callerName")
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("🔑 New FCM token: $token")
    }

    private fun getUserId(): String {
        // This should be replaced with a proper user ID management strategy
        return "user_${System.currentTimeMillis()}"
    }
}