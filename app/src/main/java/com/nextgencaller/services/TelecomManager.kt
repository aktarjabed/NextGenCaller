package com.nextgencaller.services

import android.content.Context
import android.net.Uri
import androidx.core.telecom.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TelecomManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    private val callsManager = CallsManager(context)

    private val _currentCall = MutableStateFlow<CallControlScope?>(null)
    val currentCall: StateFlow<CallControlScope?> = _currentCall

    init {
        registerWithTelecom()
    }

    private fun registerWithTelecom() {
        coroutineScope.launch {
            try {
                val capabilities = CallsManager.CAPABILITY_BASELINE or
                        CallsManager.CAPABILITY_SUPPORTS_VIDEO_CALLING or
                        CallsManager.CAPABILITY_SUPPORTS_CALL_STREAMING

                callsManager.registerAppWithTelecom(capabilities)
                Timber.d("✅ Successfully registered with Telecom")
            } catch (e: Exception) {
                Timber.e(e, "❌ Failed to register with Telecom")
            }
        }
    }

    suspend fun showIncomingCall(
        callerName: String,
        callerNumber: String,
        isVideo: Boolean,
        onAnswer: () -> Unit,
        onReject: () -> Unit
    ) {
        val attributes = CallAttributesCompat(
            displayName = callerName,
            address = Uri.parse("tel:$callerNumber"),
            direction = CallAttributesCompat.DIRECTION_INCOMING,
            callType = if (isVideo) {
                CallAttributesCompat.CALL_TYPE_VIDEO_CALL
            } else {
                CallAttributesCompat.CALL_TYPE_AUDIO_CALL
            },
            callCapabilities = CallAttributesCompat.CallCapability(
                supportsSetInactive = CallAttributesCompat.SUPPORTS_SET_INACTIVE,
                supportsStream = if (isVideo) CallAttributesCompat.SUPPORTS_STREAM else 0
            )
        )

        try {
            callsManager.addCall(
                callAttributes = attributes,
                onAnswer = { callType ->
                    Timber.d("☎️ Call answered with type: $callType")
                    onAnswer()
                },
                onDisconnect = { disconnectCause ->
                    Timber.d("🛑 Call disconnected: ${disconnectCause.reason}")
                    onReject()
                    _currentCall.value = null
                },
                onSetActive = {
                    Timber.d("✅ Call set active")
                },
                onSetInactive = {
                    Timber.d("⏸️ Call set inactive (hold)")
                }
            ) {
                _currentCall.value = this

                coroutineScope.launch {
                    currentCallEndpoint.collect { endpoint ->
                        Timber.d("🔊 Audio endpoint: ${endpoint?.name}")
                    }
                }

                coroutineScope.launch {
                    isMuted.collect { muted ->
                        Timber.d("🎤 Mute state: $muted")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to show incoming call")
        }
    }

    suspend fun endCall() {
        _currentCall.value?.disconnect(DisconnectCause(DisconnectCause.LOCAL))
        _currentCall.value = null
    }
}