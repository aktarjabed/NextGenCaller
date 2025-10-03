package com.nextgencaller.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextgencaller.domain.usecase.GetFavoriteContactsUseCase
import com.nextgencaller.domain.usecase.GetRecentCallsUseCase
import com.nextgencaller.domain.usecase.ManageCallUseCase
import com.nextgencaller.presentation.home.state.HomeUiState
import com.nextgencaller.utils.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getRecentCallsUseCase: GetRecentCallsUseCase,
    private val getFavoriteContactsUseCase: GetFavoriteContactsUseCase,
    private val manageCallUseCase: ManageCallUseCase,
    private val networkUtils: NetworkUtils
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<HomeUiEvent>()
    val uiEvent: SharedFlow<HomeUiEvent> = _uiEvent.asSharedFlow()

    init {
        loadData()
    }

    private fun loadData() {
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val networkAvailable = try {
                networkUtils.isNetworkAvailable()
            } catch (e: Exception) {
                Timber.e(e, "❌ Failed to check network status")
                false
            }
            _uiState.update { it.copy(isNetworkAvailable = networkAvailable) }
        }

        viewModelScope.launch {
            getRecentCallsUseCase()
                .catch { e ->
                    Timber.e(e, "❌ Failed to load recent calls")
                    _uiState.update { it.copy(error = "Failed to load call history") }
                }
                .collect { calls ->
                    _uiState.update { it.copy(recentCalls = calls) }
                }
        }

        viewModelScope.launch {
            getFavoriteContactsUseCase()
                .catch { e ->
                    Timber.e(e, "❌ Failed to load favorite contacts")
                    _uiState.update { it.copy(error = "Failed to load favorites") }
                }
                .collect { contacts ->
                    _uiState.update { it.copy(favoriteContacts = contacts) }
                }
        }

        _uiState.update { it.copy(isLoading = false) }
    }

    fun startCall(toUserId: String, callerName: String, phoneNumber: String, isVideo: Boolean) {
        viewModelScope.launch {
            if (!_uiState.value.isNetworkAvailable) {
                _uiEvent.emit(HomeUiEvent.Error("No network connection available"))
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }
            try {
                Timber.d("📞 Starting call to $callerName")
                manageCallUseCase.startOutgoingCall(toUserId, callerName, phoneNumber, isVideo)
                _uiEvent.emit(HomeUiEvent.CallStarted)
            } catch (e: Exception) {
                Timber.e(e, "❌ Failed to start call")
                _uiEvent.emit(HomeUiEvent.Error("Failed to start call: ${e.message}"))
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun refreshData() {
        loadData()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

sealed class HomeUiEvent {
    object CallStarted : HomeUiEvent()
    data class Error(val message: String) : HomeUiEvent()
}