package com.nextgencaller.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextgencaller.data.local.dao.CallLogDao
import com.nextgencaller.data.local.entity.CallDirection
import com.nextgencaller.data.local.entity.CallLogEntity
import com.nextgencaller.data.local.entity.CallStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class CallHistoryViewModel @Inject constructor(
    private val callLogDao: CallLogDao
) : ViewModel() {

    private val _callLogs = MutableStateFlow<List<CallLogEntity>>(emptyList())
    val callLogs: StateFlow<List<CallLogEntity>> = _callLogs.asStateFlow()

    private val _selectedFilter = MutableStateFlow(CallFilter.ALL)
    val selectedFilter: StateFlow<CallFilter> = _selectedFilter.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadCallHistory()
    }

    private fun loadCallHistory() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                callLogDao.getAllCallLogs()
                    .catch { e ->
                        Timber.e(e, "❌ Failed to load call history")
                        _error.value = "Failed to load call history"
                    }
                    .collect { logs ->
                        _callLogs.value = logs
                        _error.value = null
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                Timber.e(e, "❌ Error in call history flow")
                _isLoading.value = false
            }
        }
    }

    fun setFilter(filter: CallFilter) {
        _selectedFilter.value = filter
    }

    fun getFilteredCallLogs(): StateFlow<List<CallLogEntity>> {
        return combine(_callLogs, _selectedFilter) { logs, filter ->
            when (filter) {
                CallFilter.ALL -> logs
                CallFilter.MISSED -> logs.filter { it.callStatus == CallStatus.MISSED }
                CallFilter.INCOMING -> logs.filter { it.callDirection == CallDirection.INCOMING }
                CallFilter.OUTGOING -> logs.filter { it.callDirection == CallDirection.OUTGOING }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun deleteCallLog(callLog: CallLogEntity) {
        viewModelScope.launch {
            try {
                callLogDao.deleteCallLog(callLog)
                Timber.d("🗑️ Deleted call log ${callLog.callId}")
            } catch (e: Exception) {
                Timber.e(e, "❌ Failed to delete call log")
                _error.value = "Failed to delete call log"
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            try {
                callLogDao.deleteAllCallLogs()
                Timber.d("🗑️ Cleared all call history")
            } catch (e: Exception) {
                Timber.e(e, "❌ Failed to clear call history")
                _error.value = "Failed to clear history"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}

enum class CallFilter {
    ALL, MISSED, INCOMING, OUTGOING
}