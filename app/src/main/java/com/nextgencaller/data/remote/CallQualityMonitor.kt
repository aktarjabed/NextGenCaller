package com.nextgencaller.data.remote

import com.nextgencaller.domain.model.ConnectionQuality
import com.nextgencaller.domain.model.QualityMetrics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.webrtc.PeerConnection
import org.webrtc.RTCStatsReport
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallQualityMonitor @Inject constructor(
    private val scope: CoroutineScope
) {
    private val _qualityMetrics = MutableStateFlow(QualityMetrics())
    val qualityMetrics: StateFlow<QualityMetrics> = _qualityMetrics.asStateFlow()

    private val _connectionQuality = MutableStateFlow(ConnectionQuality.EXCELLENT)
    val connectionQuality: StateFlow<ConnectionQuality> = _connectionQuality.asStateFlow()

    private var monitoringJob: Job? = null

    fun startMonitoring(peerConnection: PeerConnection) {
        stopMonitoring()
        monitoringJob = scope.launch {
            while (isActive) {
                try {
                    peerConnection.getStats { report ->
                        processStatsReport(report)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error getting stats")
                }
                delay(1000)
            }
        }
        Timber.d("📊 Started quality monitoring")
    }

    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
        _connectionQuality.value = ConnectionQuality.DISCONNECTED
        Timber.d("📊 Stopped quality monitoring")
    }

    private fun processStatsReport(report: RTCStatsReport) {
        var totalPacketLoss = 0.0
        var totalRtt = 0.0
        var totalJitter = 0.0
        var fps = 0.0
        var bandwidth = 0.0
        var statCount = 0

        report.statsMap.values.forEach { stats ->
            when (stats.type) {
                "inbound-rtp" -> {
                    val packetsLost = stats.members["packetsLost"] as? Number
                    val packetsReceived = stats.members["packetsReceived"] as? Number
                    if (packetsLost != null && packetsReceived != null) {
                        val total = packetsLost.toLong() + packetsReceived.toLong()
                        if (total > 0) {
                            totalPacketLoss = (packetsLost.toDouble() / total) * 100
                        }
                    }

                    val jitter = stats.members["jitter"] as? Number
                    if (jitter != null) {
                        totalJitter = jitter.toDouble() * 1000
                    }

                    val framesDecoded = stats.members["framesDecoded"] as? Number
                    if (framesDecoded != null) {
                        fps = framesDecoded.toDouble()
                    }

                    val bytesReceived = stats.members["bytesReceived"] as? Number
                    if (bytesReceived != null) {
                        bandwidth = bytesReceived.toDouble() * 8 / 1000
                    }
                }

                "candidate-pair" -> {
                    val currentRtt = stats.members["currentRoundTripTime"] as? Number
                    if (currentRtt != null) {
                        totalRtt = currentRtt.toDouble() * 1000
                        statCount++
                    }
                }

                "remote-inbound-rtp" -> {
                    val roundTripTime = stats.members["roundTripTime"] as? Number
                    if (roundTripTime != null) {
                        totalRtt += roundTripTime.toDouble() * 1000
                        statCount++
                    }
                }
            }
        }

        val avgRtt = if (statCount > 0) totalRtt / statCount else totalRtt

        val metrics = QualityMetrics(
            packetLoss = totalPacketLoss,
            roundTripTime = avgRtt,
            jitter = totalJitter,
            framesPerSecond = fps,
            bandwidth = bandwidth
        )

        _qualityMetrics.value = metrics
        _connectionQuality.value = calculateConnectionQuality(metrics)
    }

    private fun calculateConnectionQuality(metrics: QualityMetrics): ConnectionQuality {
        return when {
            metrics.packetLoss > 5.0 || metrics.roundTripTime > 400 -> ConnectionQuality.POOR
            metrics.packetLoss > 2.0 || metrics.roundTripTime > 250 -> ConnectionQuality.FAIR
            metrics.packetLoss > 1.0 || metrics.roundTripTime > 150 -> ConnectionQuality.GOOD
            else -> ConnectionQuality.EXCELLENT
        }
    }
}