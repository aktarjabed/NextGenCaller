package com.nextgencaller.presentation.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextgencaller.data.local.dao.ContactDao
import com.nextgencaller.data.local.entity.ContactEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val contactDao: ContactDao
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _contacts = MutableStateFlow<List<ContactEntity>>(emptyList())
    val contacts: StateFlow<List<ContactEntity>> = _contacts.asStateFlow()

    private val _filteredContacts = MutableStateFlow<List<ContactEntity>>(emptyList())
    val filteredContacts: StateFlow<List<ContactEntity>> = _filteredContacts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadContacts()
        observeSearchQuery()
    }

    private fun loadContacts() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                contactDao.getAllContacts()
                    .catch { e ->
                        Timber.e(e, "❌ Failed to load contacts")
                        _error.value = "Failed to load contacts"
                    }
                    .collect { contactList ->
                        _contacts.value = contactList
                        filterContacts(_searchQuery.value)
                        _error.value = null
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                Timber.e(e, "❌ Error in contacts flow")
                _isLoading.value = false
            }
        }
    }

    private fun observeSearchQuery() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .catch { e ->
                    Timber.e(e, "❌ Error in search query")
                }
                .collect { query ->
                    filterContacts(query)
                }
        }
    }

    private fun filterContacts(query: String) {
        _filteredContacts.value = if (query.isEmpty()) {
            _contacts.value
        } else {
            _contacts.value.filter { contact ->
                contact.name.contains(query, ignoreCase = true) ||
                contact.phoneNumber.contains(query, ignoreCase = true)
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavorite(contact: ContactEntity) {
        viewModelScope.launch {
            try {
                val updatedContact = contact.copy(isFavorite = !contact.isFavorite)
                contactDao.updateContact(updatedContact)
                Timber.d("⭐ Toggled favorite for ${contact.name}")
            } catch (e: Exception) {
                Timber.e(e, "❌ Failed to toggle favorite")
                _error.value = "Failed to update favorite"
            }
        }
    }

    fun deleteContact(contact: ContactEntity) {
        viewModelScope.launch {
            try {
                contactDao.deleteContact(contact)
                Timber.d("🗑️ Deleted contact ${contact.name}")
            } catch (e: Exception) {
                Timber.e(e, "❌ Failed to delete contact")
                _error.value = "Failed to delete contact"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}