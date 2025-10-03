package com.nextgencaller.domain.usecase

import com.nextgencaller.data.local.dao.ContactDao
import com.nextgencaller.data.local.entity.ContactEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFavoriteContactsUseCase @Inject constructor(
    private val contactDao: ContactDao
) {
    operator fun invoke(): Flow<List<ContactEntity>> {
        return contactDao.getFavoriteContacts()
    }
}