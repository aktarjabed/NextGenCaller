package com.nextgencaller.presentation.call

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextgencaller.data.repository.CallRepository
import com.nextgencaller.domain.model.CallState
import com.nextgencaller.domain.model.ConnectionQuality
import com.nextgencaller.domain.model.QualityMetrics
import com.nextgencaller.domain.usecase.ManageCallUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject constructor(
    private val manageCallUseCase: ManageCallUseCase,
    private val callRepository: CallRepository
) : ViewModel() {

    val callState: StateFlow<CallState> = manageCallUseCase.callState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CallState.Idle)

    val connectionQuality: StateFlow<ConnectionQuality> = callRepository.connectionQuality
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConnectionQuality.EXCELLENT)

    val qualityMetrics: StateFlow<QualityMetrics> = callRepository.qualityMetrics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), QualityMetrics())

    val isMuted: StateFlow<Boolean> = callRepository.isMuted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isVideoEnabled: StateFlow<Boolean> = callRepository.isVideoEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isSpeakerOn: StateFlow<Boolean> = callRepository.isSpeakerOn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _uiEvent = MutableSharedFlow<CallUiEvent>()
    val uiEvent: SharedFlow<CallUiEvent> = _uiEvent.asSharedFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun answerCall() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Timber.d("📞 Answering call")
                manageCallUseCase.answerCall()
            } catch (e: Exception) {
                Timber.e(e, "❌ Failed to answer call")
                _uiEvent.emit(CallUiEvent.Error("Failed to answer call: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun rejectCall() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Timber.d("❌ Rejecting call")
                manageCallUseCase.rejectCall()
                _uiEvent.emit(CallUiEvent.CallEnded)
            } catch (e: Exception) {
                Timber.e(e, "❌ Failed to reject call")
                _uiEvent.emit(CallUiEvent.Error("Failed to reject call: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun endCall() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Timber.d("🛑 Ending call")
                manageCallUseCase.endCall()
                _uiEvent.emit(CallUiEvent.CallEnded)
            } catch (e: Exception) {
                Timber.e(e, "❌ Failed to end call")
                _uiEvent.emit(CallUiEvent.Error("Failed to end call: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleMute() {
        viewModelScope.launch {
            try {
                val newMuteState = !isMuted.value
                manageCallUseCase.toggleMute(newMuteState)
                Timber.d("🎤 Mute toggled: $newMuteState")
            } catch (e: Exception) {
                Timber.e(e, "❌ Failed to toggle mute")
                _uiEvent.emit(CallUiEvent.Error("Failed to toggle mute"))
            }
        }
    }

    fun toggleVideo() {
        viewModelScope.launch {
            try {
                val newVideoState = !isVideoEnabled.value
                manageCallUseCase.toggleVideo(newVideoState)
                Timber.d("📹 Video toggled: $newVideoState")
            } catch (e: Exception) {
                Timber.e(e, "❌ Failed to toggle video")
                _uiEvent.emit(CallUiEvent.Error("Failed to toggle video"))
            }
        }
    }

    fun toggleSpeaker() {
        viewModelScope.launch {
            try {
                val newSpeakerState = !isSpeakerOn.value
                manageCallUseCase.toggleSpeaker(newSpeakerState)
                Timber.d("🔊 Speaker toggled: $newSpeakerState")
            } catch (e: Exception) {
                Timber.e(e, "❌ Failed to toggle speaker")
                _uiEvent.emit(CallUiEvent.Error("Failed to toggle speaker"))
            }
        }
    }

    fun switchCamera() {
        viewModelScope.launch {
            try {
                manageCallUseCase.switchCamera()
                Timber.d("🔄 Camera switched")
            } catch (e: Exception) {
                Timber.e(e, "❌ Failed to switch camera")
                _uiEvent.emit(CallUiEvent.Error("Failed to switch camera"))
            }
        }
    }
}

sealed class CallUiEvent {
    object CallEnded : CallUiEvent()
    data class Error(val message: String) : CallUiEvent()
    data class Info(val message: String) : CallUiEvent()
}