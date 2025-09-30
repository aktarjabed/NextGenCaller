package com.nextgencaller.data.local.dao

import androidx.room.*
import com.nextgencaller.data.local.entity.CallLogEntity
import com.nextgencaller.data.local.entity.CallStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface CallLogDao {
    @Query("SELECT * FROM call_logs ORDER BY callStartTime DESC")
    fun getAllCallLogs(): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_logs WHERE callStatus = :status ORDER BY callStartTime DESC")
    fun getCallLogsByStatus(status: CallStatus): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_logs WHERE peerNumber = :phoneNumber ORDER BY callStartTime DESC")
    fun getCallLogsByNumber(phoneNumber: String): Flow<List<CallLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLog(callLog: CallLogEntity)

    @Update
    suspend fun updateCallLog(callLog: CallLogEntity)

    @Delete
    suspend fun deleteCallLog(callLog: CallLogEntity)

    @Query("DELETE FROM call_logs WHERE callStartTime < :timestamp")
    suspend fun deleteOldCallLogs(timestamp: Long)

    @Query("DELETE FROM call_logs")
    suspend fun deleteAllCallLogs()
}