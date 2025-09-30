package com.nextgencaller.domain.usecase

import com.nextgencaller.domain.repository.ContactRepository
import javax.inject.Inject

class SyncContactsUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {
    suspend operator fun invoke() {
        contactRepository.syncContactsFromDevice()
    }
}