package com.nextgencaller.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val contactId: String,
    val name: String,
    val phoneNumber: String,
    val email: String? = null,
    val photoUri: String? = null,
    val isFavorite: Boolean = false,
    val lastCallTime: Long? = null,
    val totalCalls: Int = 0
)