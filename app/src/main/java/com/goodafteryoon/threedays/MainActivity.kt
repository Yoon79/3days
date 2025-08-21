package com.goodafteryoon.threedays

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.goodafteryoon.threedays.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var goalRepository: GoalRepository
    private lateinit var reminderScheduler: ReminderScheduler

    private val requestNotifPerm = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        goalRepository = GoalRepository(applicationContext)
        reminderScheduler = ReminderScheduler(applicationContext)

        createNotificationChannel()
        maybeRequestNotificationPermission()

        lifecycleScope.launch {
            goalRepository.observeGoalState()
                .filterNotNull()
                .collect { state ->
                    binding.editGoal.setText(state.goal)
                    binding.textCurrentDue(state)
                }
        }

        binding.buttonStart.setOnClickListener {
            val goalText = binding.editGoal.text?.toString()?.trim().orEmpty()
            if (goalText.isEmpty()) {
                Toast.makeText(this, getString(R.string.toast_enter_goal), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                val newState = goalRepository.setGoalAndResetTimer(goalText)
                reminderScheduler.scheduleReminder(newState.dueEpochMillis)
                binding.textCurrentDue(newState)
            }
        }
    }

    private fun ActivityMainBinding.textCurrentDue(state: GoalState) {
        val dueText = DateFormat.format("yyyy-MM-dd HH:mm", state.dueEpochMillis).toString()
        textDue.text = getString(R.string.label_due) + ": " + dueText
        textCurrent_goalText(state.goal)
    }

    private fun ActivityMainBinding.textCurrent_goalText(goal: String) {
        textCurrentGoal.text = getString(R.string.label_current_goal) + ": " + goal
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ReminderScheduler.CHANNEL_ID,
                getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.notif_channel_desc)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val perm = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                requestNotifPerm.launch(perm)
            }
        }
    }
}
