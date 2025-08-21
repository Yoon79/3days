package com.goodafteryoon.threedays

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class ReminderScheduler(private val context: Context) {

    fun scheduleReminder(dueEpochMillis: Long) {
        val delayMs = (dueEpochMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        val work = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.REPLACE, work)
    }

    companion object {
        const val UNIQUE_WORK_NAME: String = "three_days_reminder"
        const val CHANNEL_ID: String = "three_days_channel"
        const val NOTIF_ID: Int = 1001
    }
}
