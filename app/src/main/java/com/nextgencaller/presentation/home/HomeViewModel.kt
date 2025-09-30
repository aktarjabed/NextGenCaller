package com.nextgencaller.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextgencaller.data.local.dao.CallLogDao
import com.nextgencaller.data.local.dao.ContactDao
import com.nextgencaller.data.local.entity.CallLogEntity
import com.nextgencaller.data.local.entity.ContactEntity
import com.nextgencaller.domain.usecase.ManageCallUseCase
import com.nextgencaller.utils.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val manageCallUseCase: ManageCallUseCase,
    private val callLogDao: CallLogDao,
    private val contactDao: ContactDao,
    private val networkUtils: NetworkUtils
) : ViewModel() {

    private val _recentCalls = MutableStateFlow<List<CallLogEntity>>(emptyList())
    val recentCalls: StateFlow<List<CallLogEntity>> = _recentCalls.asStateFlow()

    private val _favoriteContacts = MutableStateFlow<List<ContactEntity>>(emptyList())
    val favoriteContacts: StateFlow<List<ContactEntity>> = _favoriteContacts.asStateFlow()

    private val _isNetworkAvailable = MutableStateFlow(true)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    private val _uiEvent = MutableSharedFlow<HomeUiEvent>()
    val uiEvent: SharedFlow<HomeUiEvent> = _uiEvent.asSharedFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadRecentCalls()
        loadFavoriteContacts()
        checkNetworkStatus()
    }

    private fun loadRecentCalls() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                callLogDao.getAllCallLogs()
                    .map { it.take(10) }
                    .catch { e ->
                        Timber.e(e, "❌ Failed to load recent calls")
                        _error.value = "Failed to load call history"
                    }
                    .collect { calls ->
                        _recentCalls.value = calls
                        _error.value = null
                    }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadFavoriteContacts() {
        viewModelScope.launch {
            try {
                contactDao.getFavoriteContacts()
                    .catch { e ->
                        Timber.e(e, "❌ Failed to load favorite contacts")
                        _error.value = "Failed to load favorites"
                    }
                    .collect { contacts ->
                        _favoriteContacts.value = contacts
                    }
            } catch (e: Exception) {
                Timber.e(e, "❌ Error in favorite contacts flow")
            }
        }
    }

    private fun checkNetworkStatus() {
        viewModelScope.launch {
            try {
                _isNetworkAvailable.value = networkUtils.isNetworkAvailable()
            } catch (e: Exception) {
                Timber.e(e, "❌ Failed to check network status")
                _isNetworkAvailable.value = false
            }
        }
    }

    fun startCall(toUserId: String, callerName: String, phoneNumber: String, isVideo: Boolean) {
        viewModelScope.launch {
            try {
                if (!networkUtils.isNetworkAvailable()) {
                    _uiEvent.emit(HomeUiEvent.Error("No network connection available"))
                    return@launch
                }

                _isLoading.value = true
                Timber.d("📞 Starting call to $callerName")
                manageCallUseCase.startOutgoingCall(toUserId, callerName, phoneNumber, isVideo)
                _uiEvent.emit(HomeUiEvent.CallStarted)
            } catch (e: Exception) {
                Timber.e(e, "❌ Failed to start call")
                _uiEvent.emit(HomeUiEvent.Error("Failed to start call: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshData() {
        loadRecentCalls()
        loadFavoriteContacts()
        checkNetworkStatus()
    }

    fun clearError() {
        _error.value = null
    }
}

sealed class HomeUiEvent {
    object CallStarted : HomeUiEvent()
    data class Error(val message: String) : HomeUiEvent()
}