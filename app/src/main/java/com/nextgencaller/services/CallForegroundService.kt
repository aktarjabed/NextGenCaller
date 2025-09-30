package com.nextgencaller.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.nextgencaller.domain.model.CallState
import com.nextgencaller.domain.usecase.ManageCallUseCase
import com.nextgencaller.utils.toCallDuration
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class CallForegroundService : Service() {

    @Inject
    lateinit var manageCallUseCase: ManageCallUseCase

    @Inject
    lateinit var notificationHelper: NotificationHelper

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        const val ACTION_START_CALL = "com.nextgencaller.START_CALL"
        const val ACTION_END_CALL = "com.nextgencaller.END_CALL"
        const val ACTION_REJECT_CALL = "com.nextgencaller.REJECT_CALL"
        const val ACTION_ANSWER_CALL = "com.nextgencaller.ANSWER_CALL"

        const val EXTRA_CALLER_NAME = "CALLER_NAME"
        const val EXTRA_IS_VIDEO = "IS_VIDEO"
        const val EXTRA_CALL_ID = "CALL_ID"

        fun startCall(context: Context, callerName: String, isVideo: Boolean) {
            val intent = Intent(context, CallForegroundService::class.java).apply {
                action = ACTION_START_CALL
                putExtra(EXTRA_CALLER_NAME, callerName)
                putExtra(EXTRA_IS_VIDEO, isVideo)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun endCall(context: Context) {
            val intent = Intent(context, CallForegroundService::class.java).apply {
                action = ACTION_END_CALL
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        observeCallState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_CALL -> {
                val callerName = intent.getStringExtra(EXTRA_CALLER_NAME) ?: "Unknown"
                val isVideo = intent.getBooleanExtra(EXTRA_IS_VIDEO, false)
                startCallForeground(callerName, isVideo)
            }
            ACTION_ANSWER_CALL -> {
                serviceScope.launch {
                    manageCallUseCase.answerCall()
                }
            }
            ACTION_REJECT_CALL -> {
                serviceScope.launch {
                    manageCallUseCase.rejectCall()
                    stopForegroundService()
                }
            }
            ACTION_END_CALL -> {
                serviceScope.launch {
                    manageCallUseCase.endCall()
                    stopForegroundService()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startCallForeground(callerName: String, isVideo: Boolean) {
        val notification = notificationHelper.buildOngoingCallNotification(
            callerName = callerName,
            isVideo = isVideo,
            duration = "00:00"
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                 ServiceCompat.startForeground(
                    this,
                    NotificationHelper.NOTIFICATION_ID_ONGOING,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                )
            } else {
                startForeground(NotificationHelper.NOTIFICATION_ID_ONGOING, notification)
            }
            Timber.d("✅ Call foreground service started")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to start foreground service")
            stopSelf()
        }
    }

    private fun observeCallState() {
        serviceScope.launch {
            manageCallUseCase.callState.collectLatest { state ->
                when (state) {
                    is CallState.Ongoing -> {
                        updateOngoingNotification(state)
                    }
                    is CallState.Ended, CallState.Idle -> {
                        stopForegroundService()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun updateOngoingNotification(state: CallState.Ongoing) {
        val duration = state.state.duration.toCallDuration()
        val notification = notificationHelper.buildOngoingCallNotification(
            callerName = state.state.details.peerName,
            isVideo = state.state.details.isVideo,
            duration = duration
        )
        notificationHelper.notificationManager.notify(
            NotificationHelper.NOTIFICATION_ID_ONGOING,
            notification
        )
    }

    private fun stopForegroundService() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        notificationHelper.cancelNotification(NotificationHelper.NOTIFICATION_ID_ONGOING)
        stopSelf()
        Timber.d("🛑 Call foreground service stopped")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}