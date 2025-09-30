package com.nextgencaller.domain.model

import com.nextgencaller.data.local.entity.CallDirection
import com.nextgencaller.data.local.entity.CallStatus
import com.nextgencaller.data.local.entity.CallType

data class CallLog(
    val callId: String,
    val peerName: String,
    val peerNumber: String,
    val callStartTime: Long,
    val callEndTime: Long?,
    val callDuration: Long,
    val callType: CallType,
    val callDirection: CallDirection,
    val callStatus: CallStatus,
    val callQuality: String,
    val thumbnailUrl: String?
)