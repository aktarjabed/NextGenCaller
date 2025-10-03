package com.nextgencaller.data.repository

import com.nextgencaller.data.local.dao.CallLogDao
import com.nextgencaller.data.local.entity.*
import com.nextgencaller.data.remote.CallQualityMonitor
import com.nextgencaller.data.remote.SignalingClient
import com.nextgencaller.data.remote.WebRTCClient
import com.nextgencaller.domain.model.*
import com.nextgencaller.domain.repository.CallRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallRepositoryImpl @Inject constructor(
    private val signalingClient: SignalingClient,
    private val webRTCClient: WebRTCClient,
    private val callLogDao: CallLogDao,
    private val appScope: CoroutineScope
) : CallRepository {

    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    override val callState: StateFlow<CallState> = _callState.asStateFlow()

    override val connectionQuality: StateFlow<ConnectionQuality> = webRTCClient.connectionQuality
    override val qualityMetrics: StateFlow<QualityMetrics> = webRTCClient.qualityMetrics

    private val _isMuted = MutableStateFlow(false)
    override val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isVideoEnabled = MutableStateFlow(true)
    override val isVideoEnabled: StateFlow<Boolean> = _isVideoEnabled.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(false)
    override val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    private var callDetails: CallDetails? = null
    private var durationJob: Job? = null
    private var callStartTime: Long = 0

    init {
        // This is a singleton, so it's created once.
        // We might want to start listening only when the app is active.
        // listenForSignalingEvents()
    }

    override fun listenForSignalingEvents() {
        appScope.launch {
            signalingClient.observeIncomingOffers().collect { offer ->
                handleIncomingOffer(offer)
            }
        }
        appScope.launch {
            signalingClient.observeAnswers().collect { answer ->
                handleAnswer(answer)
            }
        }
        appScope.launch {
            signalingClient.observeIceCandidates().collect { candidate ->
                handleIceCandidate(candidate)
            }
        }
        appScope.launch {
            signalingClient.observeCallEnded().collect { userId ->
                onRemoteCallEnded(userId)
            }
        }
    }

    override suspend fun startOutgoingCall(targetUserId: String, targetName: String, targetNumber: String, isVideo: Boolean) {
        if (_callState.value !is CallState.Idle) {
            Timber.w("Cannot start a new call, another call is already in progress.")
            return
        }

        val details = CallDetails(
            callId = UUID.randomUUID().toString(),
            peerId = targetUserId,
            peerName = targetName,
            peerNumber = targetNumber,
            isVideo = isVideo,
            isOutgoing = true
        )
        this.callDetails = details
        _isVideoEnabled.value = isVideo
        _callState.value = CallState.Dialing(details)

        try {
            initializeWebRTC(details.isVideo)
            val offerSdp = webRTCClient.createOffer()
            if (offerSdp != null) {
                signalingClient.sendOffer(targetUserId, "You", isVideo, offerSdp)
                _callState.value = CallState.Ringing(details)
            } else {
                throw Exception("Failed to create offer")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to start outgoing call")
            endCallInternal("Failed to start call: ${e.message}", CallStatus.FAILED)
        }
    }

    override suspend fun handleIncomingOffer(offer: OfferSignal) {
        if (_callState.value !is CallState.Idle) {
            Timber.w("Ignoring incoming offer, a call is already in progress.")
            // Optionally send a busy signal
            return
        }

        val details = CallDetails(
            callId = UUID.randomUUID().toString(),
            peerId = offer.fromUserId,
            peerName = offer.callerName,
            peerNumber = offer.fromUserId, // Assuming peerId is the number for now
            isVideo = offer.isVideo,
            isOutgoing = false
        )
        this.callDetails = details
        _isVideoEnabled.value = offer.isVideo
        webRTCClient.setRemoteDescription(SessionDescription(SessionDescription.Type.OFFER, offer.sdp))
        _callState.value = CallState.Incoming(details)
    }

    override suspend fun answerCall() {
        val currentDetails = callDetails ?: run {
            Timber.e("Cannot answer call, no call details available.")
            return
        }
        if (_callState.value !is CallState.Incoming) {
            Timber.w("Cannot answer call, not in incoming state.")
            return
        }

        try {
            initializeWebRTC(currentDetails.isVideo)
            val answerSdp = webRTCClient.createAnswer()
            if (answerSdp != null) {
                webRTCClient.setLocalDescription(answerSdp)
                signalingClient.sendAnswer(currentDetails.peerId, answerSdp)
                startOngoingState(currentDetails)
            } else {
                throw Exception("Failed to create answer")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to answer call")
            endCallInternal("Failed to answer call: ${e.message}", CallStatus.FAILED)
        }
    }

    private suspend fun handleAnswer(answer: AnswerSignal) {
        val currentDetails = callDetails ?: return
        if (currentDetails.peerId != answer.fromUserId) {
            Timber.w("Received answer from unexpected user: ${answer.fromUserId}")
            return
        }

        try {
            webRTCClient.setRemoteDescription(SessionDescription(SessionDescription.Type.ANSWER, answer.sdp))
            startOngoingState(currentDetails)
        } catch (e: Exception) {
            Timber.e(e, "Failed to handle answer")
            endCallInternal("Connection failed", CallStatus.FAILED)
        }
    }

    private fun handleIceCandidate(candidateSignal: IceCandidateSignal) {
        val iceCandidate = IceCandidate(
            candidateSignal.sdpMid,
            candidateSignal.sdpMLineIndex,
            candidateSignal.candidate
        )
        webRTCClient.addIceCandidate(iceCandidate)
    }

    override suspend fun rejectCall() {
        endCallInternal("Call rejected", CallStatus.REJECTED)
    }

    override suspend fun endCall() {
        endCallInternal("Call ended", CallStatus.ANSWERED)
    }

    override suspend fun onRemoteCallEnded(userId: String) {
        if (callDetails?.peerId == userId) {
            endCallInternal("Peer ended the call", CallStatus.ANSWERED)
        }
    }

    private suspend fun endCallInternal(reason: String, status: CallStatus) {
        val details = callDetails ?: return

        if (_callState.value is CallState.Idle || _callState.value is CallState.Ended) return

        _callState.value = CallState.Ended(reason)
        callDetails?.peerId?.let { signalingClient.endCall(it) }

        durationJob?.cancel()
        webRTCClient.close()

        val endTime = System.currentTimeMillis()
        val duration = if (callStartTime > 0) endTime - callStartTime else 0
        saveCallLog(details, status, duration)

        clearCallState()
    }

    private fun startOngoingState(details: CallDetails) {
        val ongoingState = OngoingCallState(
            details = details,
            localStream = webRTCClient.localStream.value,
            remoteStream = webRTCClient.remoteStream.value
        )
        _callState.value = CallState.Ongoing(ongoingState)
        callStartTime = System.currentTimeMillis()

        startDurationTimer(ongoingState)

        appScope.launch {
            webRTCClient.remoteStream.collect { stream ->
                (_callState.value as? CallState.Ongoing)?.let {
                    _callState.value = it.copy(state = it.state.copy(remoteStream = stream))
                }
            }
        }
    }

    private fun startDurationTimer(initialState: OngoingCallState) {
        durationJob?.cancel()
        durationJob = appScope.launch {
            var state = initialState
            while (isActive) {
                delay(1000)
                val newDuration = System.currentTimeMillis() - callStartTime
                state = state.copy(duration = newDuration)
                _callState.value = CallState.Ongoing(state)
            }
        }
    }

    private fun initializeWebRTC(isVideo: Boolean) {
        webRTCClient.initializePeerConnection(
            onIceCandidate = { candidate ->
                callDetails?.let {
                    signalingClient.sendIceCandidate(it.peerId, candidate)
                }
            },
            onAddStream = { stream ->
                (_callState.value as? CallState.Ongoing)?.let {
                    _callState.value = it.copy(state = it.state.copy(remoteStream = stream))
                }
            },
            onConnectionChange = { iceState ->
                if (iceState == org.webrtc.PeerConnection.IceConnectionState.FAILED ||
                    iceState == org.webrtc.PeerConnection.IceConnectionState.DISCONNECTED ||
                    iceState == org.webrtc.PeerConnection.IceConnectionState.CLOSED) {
                    appScope.launch { endCallInternal("Connection failed", CallStatus.FAILED) }
                }
            }
        )
        webRTCClient.startLocalStream(isVideo)
        _isMuted.value = false
        _isVideoEnabled.value = isVideo
        _isSpeakerOn.value = isVideo // Speaker is on by default for video calls
    }

    private suspend fun saveCallLog(details: CallDetails, status: CallStatus, duration: Long) {
        val log = CallLogEntity(
            callId = details.callId,
            peerName = details.peerName,
            peerNumber = details.peerNumber,
            callStartTime = callStartTime,
            callEndTime = System.currentTimeMillis(),
            callDuration = duration,
            callType = if (details.isVideo) CallType.VIDEO else CallType.AUDIO,
            callDirection = if (details.isOutgoing) CallDirection.OUTGOING else CallDirection.INCOMING,
            callStatus = status,
            callQuality = connectionQuality.value.name
        )
        withContext(Dispatchers.IO) {
            callLogDao.insertCallLog(log)
        }
    }

    override fun toggleMute(mute: Boolean) {
        webRTCClient.toggleAudio(!mute)
        _isMuted.value = mute
    }

    override fun toggleVideo(enable: Boolean) {
        webRTCClient.toggleVideo(enable)
        _isVideoEnabled.value = enable
    }

    override fun toggleSpeaker(enable: Boolean) {
        // This needs implementation with an audio manager
        _isSpeakerOn.value = enable
        Timber.d("Speaker toggled: $enable (needs implementation)")
    }

    override fun switchCamera() {
        webRTCClient.switchCamera()
    }

    override fun clearCallState() {
        callDetails = null
        callStartTime = 0
        _callState.value = CallState.Idle
    }
}