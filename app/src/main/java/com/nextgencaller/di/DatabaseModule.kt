package com.nextgencaller.di

import android.content.Context
import androidx.room.Room
import com.nextgencaller.data.local.AppDatabase
import com.nextgencaller.data.local.dao.CallLogDao
import com.nextgencaller.data.local.dao.ContactDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    fun provideCallLogDao(appDatabase: AppDatabase): CallLogDao {
        return appDatabase.callLogDao()
    }

    @Provides
    fun provideContactDao(appDatabase: AppDatabase): ContactDao {
        return appDatabase.contactDao()
    }
}