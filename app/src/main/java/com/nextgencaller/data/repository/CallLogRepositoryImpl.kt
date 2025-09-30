package com.nextgencaller.data.repository

import com.nextgencaller.data.local.dao.CallLogDao
import com.nextgencaller.data.local.entity.CallLogEntity
import com.nextgencaller.data.local.entity.CallStatus
import com.nextgencaller.domain.repository.CallLogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallLogRepositoryImpl @Inject constructor(
    private val callLogDao: CallLogDao
) : CallLogRepository {

    override fun getAllCallLogs(): Flow<List<CallLogEntity>> {
        return callLogDao.getAllCallLogs()
    }

    override fun getCallLogsByStatus(status: CallStatus): Flow<List<CallLogEntity>> {
        return callLogDao.getCallLogsByStatus(status)
    }

    override fun getCallLogsByNumber(phoneNumber: String): Flow<List<CallLogEntity>> {
        return callLogDao.getCallLogsByNumber(phoneNumber)
    }

    override suspend fun insertCallLog(callLog: CallLogEntity) {
        withContext(Dispatchers.IO) {
            callLogDao.insertCallLog(callLog)
        }
    }

    override suspend fun updateCallLog(callLog: CallLogEntity) {
        withContext(Dispatchers.IO) {
            callLogDao.updateCallLog(callLog)
        }
    }

    override suspend fun deleteCallLog(callLog: CallLogEntity) {
        withContext(Dispatchers.IO) {
            callLogDao.deleteCallLog(callLog)
        }
    }

    override suspend fun deleteOldCallLogs(timestamp: Long) {
        withContext(Dispatchers.IO) {
            callLogDao.deleteOldCallLogs(timestamp)
        }
    }

    override suspend fun deleteAllCallLogs() {
        withContext(Dispatchers.IO) {
            callLogDao.deleteAllCallLogs()
        }
    }
}