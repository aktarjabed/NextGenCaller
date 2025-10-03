package com.nextgencaller.presentation.home.state

import com.nextgencaller.data.local.entity.CallLogEntity
import com.nextgencaller.data.local.entity.ContactEntity

data class HomeUiState(
    val recentCalls: List<CallLogEntity> = emptyList(),
    val favoriteContacts: List<ContactEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isNetworkAvailable: Boolean = true
)