package com.nextgencaller.domain.usecase

import com.nextgencaller.domain.mappers.toDomain
import com.nextgencaller.domain.model.CallLog
import com.nextgencaller.domain.repository.CallLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetCallLogsUseCase @Inject constructor(
    private val callLogRepository: CallLogRepository
) {
    operator fun invoke(): Flow<List<CallLog>> {
        return callLogRepository.getAllCallLogs().map { entities ->
            entities.map { it.toDomain() }
        }
    }
}