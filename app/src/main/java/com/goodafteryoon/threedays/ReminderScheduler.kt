package com.goodafteryoon.threedays

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class ReminderScheduler(private val context: Context) {

    fun scheduleReminderForGoal(goal: GoalItem) {
        val delayMs = (goal.dueEpochMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        val input = Data.Builder()
            .putString(ReminderWorker.KEY_GOAL_ID, goal.id)
            .build()
        val work = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(input)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(uniqueName(goal.id), ExistingWorkPolicy.REPLACE, work)
    }

    fun cancelReminderForGoal(goal: GoalItem) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueName(goal.id))
    }

    private fun uniqueName(id: String): String = "three_days_reminder_" + id

    companion object {
        const val CHANNEL_ID: String = "three_days_channel"
        const val NOTIF_ID_BASE: Int = 2000
    }
}
