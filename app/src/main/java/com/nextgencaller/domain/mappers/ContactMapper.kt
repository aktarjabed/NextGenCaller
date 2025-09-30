package com.nextgencaller.domain.mappers

import com.nextgencaller.data.local.entity.ContactEntity
import com.nextgencaller.domain.model.Contact

fun ContactEntity.toDomain(): Contact {
    return Contact(
        contactId = this.contactId,
        name = this.name,
        phoneNumber = this.phoneNumber,
        email = this.email,
        photoUri = this.photoUri,
        isFavorite = this.isFavorite,
        lastCallTime = this.lastCallTime,
        totalCalls = this.totalCalls
    )
}

fun Contact.toEntity(): ContactEntity {
    return ContactEntity(
        contactId = this.contactId,
        name = this.name,
        phoneNumber = this.phoneNumber,
        email = this.email,
        photoUri = this.photoUri,
        isFavorite = this.isFavorite,
        lastCallTime = this.lastCallTime,
        totalCalls = this.totalCalls
    )
}