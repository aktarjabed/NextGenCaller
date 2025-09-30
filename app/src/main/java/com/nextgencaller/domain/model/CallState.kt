package com.nextgencaller.domain.model

import org.webrtc.MediaStream

data class CallDetails(
    val callId: String,
    val peerId: String,
    val peerName: String,
    val peerNumber: String,
    val isVideo: Boolean,
    val isOutgoing: Boolean
)

data class OngoingCallState(
    val details: CallDetails,
    val duration: Long = 0L,
    val localStream: MediaStream? = null,
    val remoteStream: MediaStream? = null
)

sealed class CallState {
    object Idle : CallState()
    data class Incoming(val details: CallDetails) : CallState()
    data class Dialing(val details: CallDetails) : CallState()
    data class Ringing(val details: CallDetails) : CallState()
    data class Ongoing(val state: OngoingCallState) : CallState()
    data class Ended(val reason: String) : CallState()
    data class Error(val message: String) : CallState()
}