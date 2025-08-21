package com.goodafteryoon.threedays

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_REMIND_AGAIN) {
            NotificationManagerCompat.from(context).cancel(ReminderScheduler.NOTIF_ID)
            val repo = GoalRepository(context)
            val scheduler = ReminderScheduler(context)
            CoroutineScope(Dispatchers.IO).launch {
                val current = repo.getCurrentState()
                val goal = current?.goal ?: ""
                val newState = repo.setGoalAndResetTimer(goal)
                scheduler.scheduleReminder(newState.dueEpochMillis)
            }
        }
    }

    companion object {
        const val ACTION_REMIND_AGAIN: String = "com.goodafteryoon.threedays.ACTION_REMIND_AGAIN"
    }
}
