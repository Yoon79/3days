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
            val goalId = intent.getStringExtra(EXTRA_GOAL_ID) ?: return
            val repo = GoalRepository(context)
            val scheduler = ReminderScheduler(context)
            CoroutineScope(Dispatchers.IO).launch {
                val updated = repo.resetGoalTimer(goalId)
                if (updated != null) {
                    NotificationManagerCompat.from(context)
                        .cancel(ReminderScheduler.NOTIF_ID_BASE + (goalId.hashCode() and 0x0FFF))
                    scheduler.scheduleReminderForGoal(updated)
                }
            }
        }
    }

    companion object {
        const val ACTION_REMIND_AGAIN: String = "com.goodafteryoon.threedays.ACTION_REMIND_AGAIN"
        const val EXTRA_GOAL_ID: String = "extra_goal_id"
    }
}
