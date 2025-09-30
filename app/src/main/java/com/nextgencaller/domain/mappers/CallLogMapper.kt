package com.nextgencaller.domain.mappers

import com.nextgencaller.data.local.entity.CallLogEntity
import com.nextgencaller.domain.model.CallLog

fun CallLogEntity.toDomain(): CallLog {
    return CallLog(
        callId = this.callId,
        peerName = this.peerName,
        peerNumber = this.peerNumber,
        callStartTime = this.callStartTime,
        callEndTime = this.callEndTime,
        callDuration = this.callDuration,
        callType = this.callType,
        callDirection = this.callDirection,
        callStatus = this.callStatus,
        callQuality = this.callQuality,
        thumbnailUrl = this.thumbnailUrl
    )
}

fun CallLog.toEntity(): CallLogEntity {
    return CallLogEntity(
        callId = this.callId,
        peerName = this.peerName,
        peerNumber = this.peerNumber,
        callStartTime = this.callStartTime,
        callEndTime = this.callEndTime,
        callDuration = this.callDuration,
        callType = this.callType,
        callDirection = this.callDirection,
        callStatus = this.callStatus,
        callQuality = this.callQuality,
        thumbnailUrl = this.thumbnailUrl
    )
}