package com.nextgencaller.domain.usecase

import com.nextgencaller.domain.model.CallState
import com.nextgencaller.domain.repository.CallRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManageCallUseCase @Inject constructor(
    private val callRepository: CallRepository
) {
    val callState: StateFlow<CallState> = callRepository.callState

    suspend fun startOutgoingCall(targetUserId: String, targetName: String, targetNumber: String, isVideo: Boolean) {
        callRepository.startOutgoingCall(targetUserId, targetName, targetNumber, isVideo)
    }

    suspend fun answerCall() {
        callRepository.answerCall()
    }

    suspend fun rejectCall() {
        callRepository.rejectCall()
    }

    suspend fun endCall() {
        callRepository.endCall()
    }

    fun toggleMute(mute: Boolean) {
        callRepository.toggleMute(mute)
    }

    fun toggleVideo(enable: Boolean) {
        callRepository.toggleVideo(enable)
    }

    fun toggleSpeaker(enable: Boolean) {
        callRepository.toggleSpeaker(enable)
    }

    fun switchCamera() {
        callRepository.switchCamera()
    }

    fun listenForSignalingEvents() {
        callRepository.listenForSignalingEvents()
    }

    fun clearCallState() {
        callRepository.clearCallState()
    }
}