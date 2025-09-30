package com.nextgencaller.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nextgencaller.data.local.dao.CallLogDao
import com.nextgencaller.data.local.dao.ContactDao
import com.nextgencaller.data.local.entity.CallLogEntity
import com.nextgencaller.data.local.entity.ContactEntity
import com.nextgencaller.data.local.entity.Converters

@Database(
    entities = [CallLogEntity::class, ContactEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun callLogDao(): CallLogDao
    abstract fun contactDao(): ContactDao

    companion object {
        const val DATABASE_NAME = "nextgen_caller_db"
    }
}