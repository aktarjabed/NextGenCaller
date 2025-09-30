package com.nextgencaller.di

import com.nextgencaller.data.repository.CallLogRepositoryImpl
import com.nextgencaller.data.repository.CallRepositoryImpl
import com.nextgencaller.data.repository.ContactRepositoryImpl
import com.nextgencaller.domain.repository.CallLogRepository
import com.nextgencaller.domain.repository.CallRepository
import com.nextgencaller.domain.repository.ContactRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCallLogRepository(
        callLogRepositoryImpl: CallLogRepositoryImpl
    ): CallLogRepository

    @Binds
    @Singleton
    abstract fun bindContactRepository(
        contactRepositoryImpl: ContactRepositoryImpl
    ): ContactRepository

    @Binds
    @Singleton
    abstract fun bindCallRepository(
        callRepositoryImpl: CallRepositoryImpl
    ): CallRepository
}