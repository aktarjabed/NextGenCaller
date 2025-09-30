package com.nextgencaller.presentation.call.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nextgencaller.domain.model.ConnectionQuality
import com.nextgencaller.domain.model.QualityMetrics

@Composable
fun NetworkQualityIndicator(
    quality: ConnectionQuality,
    metrics: QualityMetrics,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val (icon, color, label) = when (quality) {
        ConnectionQuality.EXCELLENT -> Triple(
            Icons.Default.SignalCellular4Bar,
            Color(0xFF4CAF50),
            "Excellent"
        )
        ConnectionQuality.GOOD -> Triple(
            Icons.Default.SignalCellular3Bar,
            Color(0xFF8BC34A),
            "Good"
        )
        ConnectionQuality.FAIR -> Triple(
            Icons.Default.SignalCellular2Bar,
            Color(0xFFFFA726),
            "Fair"
        )
        ConnectionQuality.POOR -> Triple(
            Icons.Default.SignalCellular1Bar,
            Color(0xFFF44336),
            "Poor"
        )
        ConnectionQuality.DISCONNECTED -> Triple(
            Icons.Default.SignalCellularOff,
            Color(0xFF757575),
            "Disconnected"
        )
    }

    Column(modifier = modifier) {
        Surface(
            modifier = Modifier
                .padding(8.dp)
                .clickable { expanded = !expanded },
            shape = RoundedCornerShape(20.dp),
            color = color.copy(alpha = 0.15f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = color
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            Surface(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .width(200.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MetricRow("Packet Loss", "${String.format("%.2f", metrics.packetLoss)}%")
                    MetricRow("RTT", "${metrics.roundTripTime.toInt()}ms")
                    MetricRow("Jitter", "${metrics.jitter.toInt()}ms")
                    if (metrics.framesPerSecond > 0) {
                        MetricRow("FPS", "${metrics.framesPerSecond.toInt()}")
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}