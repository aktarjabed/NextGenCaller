package com.nextgencaller.domain.repository

import com.nextgencaller.data.local.entity.CallLogEntity
import com.nextgencaller.data.local.entity.CallStatus
import kotlinx.coroutines.flow.Flow

interface CallLogRepository {
    fun getAllCallLogs(): Flow<List<CallLogEntity>>
    fun getCallLogsByStatus(status: CallStatus): Flow<List<CallLogEntity>>
    fun getCallLogsByNumber(phoneNumber: String): Flow<List<CallLogEntity>>
    suspend fun insertCallLog(callLog: CallLogEntity)
    suspend fun updateCallLog(callLog: CallLogEntity)
    suspend fun deleteCallLog(callLog: CallLogEntity)
    suspend fun deleteOldCallLogs(timestamp: Long)
    suspend fun deleteAllCallLogs()
}