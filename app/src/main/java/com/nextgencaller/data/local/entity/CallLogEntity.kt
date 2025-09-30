package com.nextgencaller.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey val callId: String,
    val peerName: String,
    val peerNumber: String,
    val callStartTime: Long,
    val callEndTime: Long?,
    val callDuration: Long,
    val callType: CallType,
    val callDirection: CallDirection,
    val callStatus: CallStatus,
    val callQuality: String,
    val thumbnailUrl: String? = null
)

enum class CallType { AUDIO, VIDEO }
enum class CallDirection { INCOMING, OUTGOING }
enum class CallStatus { ANSWERED, MISSED, REJECTED, FAILED, ONGOING, CANCELLED }