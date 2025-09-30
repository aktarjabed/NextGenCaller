package com.nextgencaller.domain.model

data class OfferSignal(
    val fromUserId: String,
    val callerName: String,
    val isVideo: Boolean,
    val sdp: String,
    val type: String,
    val roomId: String
)

data class AnswerSignal(
    val fromUserId: String,
    val sdp: String,
    val type: String
)

data class IceCandidateSignal(
    val fromUserId: String,
    val candidate: String,
    val sdpMid: String,
    val sdpMLineIndex: Int
)

enum class ConnectionQuality {
    EXCELLENT, GOOD, FAIR, POOR, DISCONNECTED
}

data class QualityMetrics(
    val packetLoss: Double = 0.0,
    val roundTripTime: Double = 0.0,
    val jitter: Double = 0.0,
    val framesPerSecond: Double = 0.0,
    val bandwidth: Double = 0.0
)