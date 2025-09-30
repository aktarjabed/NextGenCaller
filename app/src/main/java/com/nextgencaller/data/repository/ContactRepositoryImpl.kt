package com.nextgencaller.data.repository

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import com.nextgencaller.data.local.dao.ContactDao
import com.nextgencaller.data.local.entity.ContactEntity
import com.nextgencaller.domain.repository.ContactRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactDao: ContactDao
) : ContactRepository {

    private val contentResolver: ContentResolver = context.contentResolver

    override fun getAllContacts(): Flow<List<ContactEntity>> = contactDao.getAllContacts()

    override fun getFavoriteContacts(): Flow<List<ContactEntity>> = contactDao.getFavoriteContacts()

    override fun searchContacts(query: String): Flow<List<ContactEntity>> = contactDao.searchContacts(query)

    override suspend fun getContactById(id: String): ContactEntity? = withContext(Dispatchers.IO) {
        contactDao.getContactById(id)
    }

    override suspend fun insertContact(contact: ContactEntity) = withContext(Dispatchers.IO) {
        contactDao.insertContact(contact)
    }

    override suspend fun insertContacts(contacts: List<ContactEntity>) = withContext(Dispatchers.IO) {
        contactDao.insertContacts(contacts)
    }

    override suspend fun updateContact(contact: ContactEntity) = withContext(Dispatchers.IO) {
        contactDao.updateContact(contact)
    }

    override suspend fun deleteContact(contact: ContactEntity) = withContext(Dispatchers.IO) {
        contactDao.deleteContact(contact)
    }

    override suspend fun deleteAllContacts() = withContext(Dispatchers.IO) {
        contactDao.deleteAllContacts()
    }

    @SuppressLint("Range")
    override suspend fun syncContactsFromDevice() = withContext(Dispatchers.IO) {
        val contactsList = mutableListOf<ContactEntity>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI
        )

        val cursor: Cursor? = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            val nameMap = mutableMapOf<String, ContactEntity>()
            while (it.moveToNext()) {
                val name = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                val number = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)).replace("\\s".toRegex(), "")
                val photoUri = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI))

                if (nameMap.containsKey(name)) {
                    // Contact with this name already exists, you might want to handle multiple numbers for a contact
                } else {
                    nameMap[name] = ContactEntity(
                        contactId = UUID.randomUUID().toString(),
                        name = name,
                        phoneNumber = number,
                        photoUri = photoUri
                    )
                }
            }
            contactsList.addAll(nameMap.values)
        }

        if (contactsList.isNotEmpty()) {
            contactDao.deleteAllContacts()
            contactDao.insertContacts(contactsList)
        }
    }
}