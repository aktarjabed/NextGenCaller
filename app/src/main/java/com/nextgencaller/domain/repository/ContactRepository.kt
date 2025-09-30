package com.nextgencaller.domain.repository

import com.nextgencaller.data.local.entity.ContactEntity
import kotlinx.coroutines.flow.Flow

interface ContactRepository {
    fun getAllContacts(): Flow<List<ContactEntity>>
    fun getFavoriteContacts(): Flow<List<ContactEntity>>
    fun searchContacts(query: String): Flow<List<ContactEntity>>
    suspend fun getContactById(id: String): ContactEntity?
    suspend fun insertContact(contact: ContactEntity)
    suspend fun insertContacts(contacts: List<ContactEntity>)
    suspend fun updateContact(contact: ContactEntity)
    suspend fun deleteContact(contact: ContactEntity)
    suspend fun deleteAllContacts()
    suspend fun syncContactsFromDevice()
}