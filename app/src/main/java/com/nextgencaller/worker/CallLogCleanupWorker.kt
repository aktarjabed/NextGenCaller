package com.nextgencaller.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.nextgencaller.data.local.dao.CallLogDao
import com.nextgencaller.utils.Constants
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.util.concurrent.TimeUnit

@HiltWorker
class CallLogCleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val callLogDao: CallLogDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val retentionTime = System.currentTimeMillis() -
                TimeUnit.DAYS.toMillis(Constants.CALL_LOG_RETENTION_DAYS)

            callLogDao.deleteOldCallLogs(retentionTime)

            Timber.d("✅ Call log cleanup completed successfully")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "❌ Call log cleanup failed")
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val WORK_NAME = "call_log_cleanup_worker"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<CallLogCleanupWorker>(
                7, TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}