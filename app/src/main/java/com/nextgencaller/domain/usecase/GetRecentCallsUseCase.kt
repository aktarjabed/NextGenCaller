package com.nextgencaller.domain.usecase

import com.nextgencaller.data.local.dao.CallLogDao
import com.nextgencaller.data.local.entity.CallLogEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetRecentCallsUseCase @Inject constructor(
    private val callLogDao: CallLogDao
) {
    operator fun invoke(): Flow<List<CallLogEntity>> {
        return callLogDao.getAllCallLogs().map { it.take(10) }
    }
}