package com.nextgencaller.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nextgencaller.data.local.entity.CallDirection
import com.nextgencaller.data.local.entity.CallLogEntity
import com.nextgencaller.data.local.entity.CallStatus
import com.nextgencaller.data.local.entity.CallType
import com.nextgencaller.utils.toCallDuration
import com.nextgencaller.utils.toFormattedDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallHistoryScreen(
    viewModel: CallHistoryViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onCallBack: (CallLogEntity) -> Unit
) {
    val filteredCallLogs by viewModel.getFilteredCallLogs().collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var showClearDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Call History") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear history")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CallFilter.values().forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { viewModel.setFilter(filter) },
                        label = {
                            Text(
                                text = when (filter) {
                                    CallFilter.ALL -> "All"
                                    CallFilter.MISSED -> "Missed"
                                    CallFilter.INCOMING -> "Incoming"
                                    CallFilter.OUTGOING -> "Outgoing"
                                }
                            )
                        },
                        leadingIcon = if (selectedFilter == filter) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else null
                    )
                }
            }

            Divider()

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                filteredCallLogs.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HistoryToggleOff,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "No call history",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn {
                        items(
                            items = filteredCallLogs,
                            key = { it.callId }
                        ) { callLog ->
                            CallHistoryItem(
                                callLog = callLog,
                                onClick = { onCallBack(callLog) }
                            )
                            Divider()
                        }
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear call history?") },
            text = { Text("This will delete all call history. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearDialog = false
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CallHistoryItem(
    callLog: CallLogEntity,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (callLog.callStatus == CallStatus.MISSED) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = callLog.peerName.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleMedium,
                color = if (callLog.callStatus == CallStatus.MISSED) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                }
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = callLog.peerName,
                style = MaterialTheme.typography.bodyLarge
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when {
                        callLog.callStatus == CallStatus.MISSED -> Icons.Default.CallMissed
                        callLog.callDirection == CallDirection.INCOMING -> Icons.Default.CallReceived
                        else -> Icons.Default.CallMade
                    },
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (callLog.callStatus == CallStatus.MISSED) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Text(
                    text = callLog.callStartTime.toFormattedDate(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (callLog.callDuration > 0) {
                    Text(
                        text = " • ${callLog.callDuration.toCallDuration()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        IconButton(onClick = onClick) {
            Icon(
                imageVector = if (callLog.callType == CallType.VIDEO) {
                    Icons.Default.Videocam
                } else {
                    Icons.Default.Call
                },
                contentDescription = "Call",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}