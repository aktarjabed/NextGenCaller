package com.nextgencaller.domain.usecase

import com.nextgencaller.domain.mappers.toDomain
import com.nextgencaller.domain.model.Contact
import com.nextgencaller.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SearchContactsUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {
    operator fun invoke(query: String): Flow<List<Contact>> {
        return contactRepository.searchContacts(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }
}