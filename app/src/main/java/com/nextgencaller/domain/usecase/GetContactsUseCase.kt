package com.nextgencaller.domain.usecase

import com.nextgencaller.domain.mappers.toDomain
import com.nextgencaller.domain.model.Contact
import com.nextgencaller.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetContactsUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {
    operator fun invoke(): Flow<List<Contact>> {
        return contactRepository.getAllContacts().map { entities ->
            entities.map { it.toDomain() }
        }
    }
}