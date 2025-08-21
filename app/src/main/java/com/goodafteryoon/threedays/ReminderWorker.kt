package com.goodafteryoon.threedays

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReminderWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.Default) {
        val repo = GoalRepository(applicationContext)
        val state = repo.getCurrentState()
        val goalText = state?.goal ?: ""

        val openAppIntent = Intent(applicationContext, MainActivity::class.java)
        val openAppPending = PendingIntent.getActivity(
            applicationContext,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val remindAgainIntent = Intent(applicationContext, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_REMIND_AGAIN
        }
        val remindAgainPending = PendingIntent.getBroadcast(
            applicationContext,
            1,
            remindAgainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(applicationContext, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(applicationContext.getString(R.string.notif_title))
            .setContentText(applicationContext.getString(R.string.notif_text, goalText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openAppPending)
            .setAutoCancel(true)
            .addAction(0, applicationContext.getString(R.string.notif_action_remind_again), remindAgainPending)
            .addAction(0, applicationContext.getString(R.string.notif_action_change_goal), openAppPending)

        NotificationManagerCompat.from(applicationContext)
            .notify(ReminderScheduler.NOTIF_ID, builder.build())

        Result.success()
    }
}
