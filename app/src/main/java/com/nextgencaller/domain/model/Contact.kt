package com.nextgencaller.domain.model

data class Contact(
    val contactId: String,
    val name: String,
    val phoneNumber: String,
    val email: String?,
    val photoUri: String?,
    val isFavorite: Boolean,
    val lastCallTime: Long?,
    val totalCalls: Int
)