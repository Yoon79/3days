package com.goodafteryoon.threedays

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReminderWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.Default) {
        val goalId = inputData.getString(KEY_GOAL_ID) ?: return@withContext Result.success()
        val repo = GoalRepository(applicationContext)
        val goal = repo.getGoal(goalId) ?: return@withContext Result.success()

        val openAppIntent = Intent(applicationContext, MainActivity::class.java)
        val openAppPending = PendingIntent.getActivity(
            applicationContext,
            goalId.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val remindAgainIntent = Intent(applicationContext, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_REMIND_AGAIN
            putExtra(ReminderActionReceiver.EXTRA_GOAL_ID, goalId)
        }
        val remindAgainPending = PendingIntent.getBroadcast(
            applicationContext,
            goalId.hashCode() + 1,
            remindAgainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(applicationContext, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(applicationContext.getString(R.string.notif_title))
            .setContentText(applicationContext.getString(R.string.notif_text, goal.text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(openAppPending)
            .setAutoCancel(true)
            .addAction(0, applicationContext.getString(R.string.notif_action_remind_again), remindAgainPending)
            .addAction(0, applicationContext.getString(R.string.notif_action_change_goal), openAppPending)

        // For pre-O devices, set sound and vibration directly on the notification
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            builder.setSound(alarmUri)
            builder.setVibrate(longArrayOf(0, 400, 200, 400))
        }

        NotificationManagerCompat.from(applicationContext)
            .notify(ReminderScheduler.NOTIF_ID_BASE + (goalId.hashCode() and 0x0FFF), builder.build())

        Result.success(workDataOf(KEY_GOAL_ID to goalId))
    }

    companion object {
        const val KEY_GOAL_ID: String = "goal_id"
    }
}
