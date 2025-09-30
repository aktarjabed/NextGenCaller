package com.nextgencaller.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.nextgencaller.R
import com.nextgencaller.presentation.MainActivity
import com.nextgencaller.presentation.call.IncomingCallActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_INCOMING_CALLS = "incoming_calls"
        const val CHANNEL_ONGOING_CALLS = "ongoing_calls"
        const val CHANNEL_MISSED_CALLS = "missed_calls"

        const val NOTIFICATION_ID_INCOMING = 1001
        const val NOTIFICATION_ID_ONGOING = 1002
        const val NOTIFICATION_ID_MISSED = 1003
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_INCOMING_CALLS,
                    "Incoming Calls",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for incoming calls"
                    setSound(null, null)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_ONGOING_CALLS,
                    "Ongoing Calls",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Shows ongoing call status"
                    setSound(null, null)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                },
                NotificationChannel(
                    CHANNEL_MISSED_CALLS,
                    "Missed Calls",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications for missed calls"
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
            )
            channels.forEach { notificationManager.createNotificationChannel(it) }
        }
    }

    fun buildIncomingCallNotification(callId: String, callerName: String, isVideo: Boolean): Notification {
        val answerIntent = Intent(context, IncomingCallActivity::class.java).apply {
            action = "ACTION_ANSWER_CALL"
            putExtra("CALL_ID", callId)
            putExtra("CALLER_NAME", callerName)
            putExtra("IS_VIDEO", isVideo)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val answerPendingIntent = PendingIntent.getActivity(
            context, 0, answerIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val rejectIntent = Intent(context, CallForegroundService::class.java).apply {
            action = CallForegroundService.ACTION_REJECT_CALL
            putExtra("CALL_ID", callId)
        }
        val rejectPendingIntent = PendingIntent.getService(
            context, 1, rejectIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, CHANNEL_INCOMING_CALLS)
            .setContentTitle("Incoming ${if (isVideo) "Video" else "Audio"} Call")
            .setContentText(callerName)
            .setSmallIcon(R.drawable.ic_call)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(answerPendingIntent, true)
            .addAction(R.drawable.ic_call, "Answer", answerPendingIntent)
            .addAction(R.drawable.ic_call_end, "Decline", rejectPendingIntent)
            .build()
    }

    fun buildOngoingCallNotification(callerName: String, isVideo: Boolean, duration: String): Notification {
        val openCallIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openCallPendingIntent = PendingIntent.getActivity(
            context, 0, openCallIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val endCallIntent = Intent(context, CallForegroundService::class.java).apply {
            action = CallForegroundService.ACTION_END_CALL
        }
        val endCallPendingIntent = PendingIntent.getService(
            context, 0, endCallIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, CHANNEL_ONGOING_CALLS)
            .setContentTitle("Ongoing ${if (isVideo) "Video" else "Audio"} Call")
            .setContentText("$callerName • $duration")
            .setSmallIcon(R.drawable.ic_call)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setContentIntent(openCallPendingIntent)
            .addAction(R.drawable.ic_call_end, "End Call", endCallPendingIntent)
            .build()
    }

    fun showMissedCallNotification(callerName: String) {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_MISSED_CALLS)
            .setContentTitle("Missed Call")
            .setContentText(callerName)
            .setSmallIcon(R.drawable.ic_call_missed)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID_MISSED, notification)
    }

    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }
}