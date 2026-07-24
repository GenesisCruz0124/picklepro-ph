package com.gentech.picklepro.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Requests a sync flush, constrained to run only when network is available
 * (spec §5.10 "flush when online"). [ExistingWorkPolicy.APPEND_OR_REPLACE]
 * chains onto any in-flight run rather than being dropped by
 * [ExistingWorkPolicy.KEEP] — a mutation enqueued while a flush is already
 * running still gets a guaranteed follow-up attempt.
 */
object SyncScheduler {
    fun requestSync(context: Context) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(SyncWorker.UNIQUE_WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }
}
