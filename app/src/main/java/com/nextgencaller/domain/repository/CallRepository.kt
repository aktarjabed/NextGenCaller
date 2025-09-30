package com.nextgencaller.domain.repository

import com.nextgencaller.domain.model.*
import kotlinx.coroutines.flow.StateFlow

interface CallRepository {
    val callState: StateFlow<CallState>
    val connectionQuality: StateFlow<ConnectionQuality>
    val qualityMetrics: StateFlow<QualityMetrics>
    val isMuted: StateFlow<Boolean>
    val isVideoEnabled: StateFlow<Boolean>
    val isSpeakerOn: StateFlow<Boolean>

    suspend fun startOutgoingCall(targetUserId: String, targetName: String, targetNumber: String, isVideo: Boolean)
    suspend fun handleIncomingOffer(offer: OfferSignal)
    suspend fun answerCall()
    suspend fun rejectCall()
    suspend fun endCall()
    suspend fun onRemoteCallEnded(userId: String)

    fun toggleMute(mute: Boolean)
    fun toggleVideo(enable: Boolean)
    fun toggleSpeaker(enable: Boolean)
    fun switchCamera()

    fun listenForSignalingEvents()
    fun clearCallState()
}