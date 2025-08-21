package com.goodafteryoon.threedays

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.goodafteryoon.threedays.databinding.ActivityMainBinding
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var goalRepository: GoalRepository
    private lateinit var reminderScheduler: ReminderScheduler

    private lateinit var adapter: GoalAdapter
    private var tickerJob: Job? = null

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

        setupRecycler()
        bindActions()
        observeGoals()
        setupAds()
    }

    override fun onResume() {
        super.onResume()
        showRandomQuote()
    }

    private fun setupAds() {
        MobileAds.initialize(this) {}
        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)
    }

    private fun showRandomQuote() {
        val quotes = resources.getStringArray(R.array.motivational_quotes)
        if (quotes.isNotEmpty()) {
            val idx = (quotes.indices).random()
            binding.textQuote.text = quotes[idx]
        }
    }

    private fun setupRecycler() {
        adapter = GoalAdapter(
            onEdit = { item -> promptEdit(item) },
            onDelete = { item -> lifecycleScope.launch { deleteGoal(item) } }
        )
        binding.recyclerGoals.layoutManager = LinearLayoutManager(this)
        binding.recyclerGoals.adapter = adapter
    }

    private fun bindActions() {
        binding.buttonStart.setOnClickListener {
            val goalText = binding.editGoal.text?.toString()?.trim().orEmpty()
            if (goalText.isEmpty()) {
                Toast.makeText(this, getString(R.string.toast_enter_goal), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                val item = goalRepository.addGoal(goalText)
                reminderScheduler.scheduleReminderForGoal(item)
                binding.editGoal.setText("")
            }
        }
    }

    private fun observeGoals() {
        lifecycleScope.launch {
            goalRepository.observeGoals().collectLatest { list ->
                adapter.submitList(list)
                restartTicker()
            }
        }
    }

    private fun restartTicker() {
        tickerJob?.cancel()
        tickerJob = lifecycleScope.launch {
            while (true) {
                adapter.notifyItemRangeChanged(0, adapter.itemCount, Unit)
                delay(1000L)
            }
        }
    }

    private fun promptEdit(item: GoalItem) {
        val input = EditText(this)
        input.setText(item.text)
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("목표 수정")
            .setView(input)
            .setPositiveButton("저장") { d, _ ->
                val newText = input.text?.toString()?.trim().orEmpty()
                if (newText.isNotEmpty()) {
                    lifecycleScope.launch {
                        val updated = goalRepository.updateGoal(item.id, newText)
                        if (updated != null) reminderScheduler.scheduleReminderForGoal(updated)
                    }
                }
                d.dismiss()
            }
            .setNegativeButton("취소") { d, _ -> d.dismiss() }
            .show()
    }

    private suspend fun deleteGoal(item: GoalItem) {
        goalRepository.deleteGoal(item.id)
        reminderScheduler.cancelReminderForGoal(item)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ReminderScheduler.CHANNEL_ID,
                getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notif_channel_desc)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400)
                val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                val attributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                setSound(alarmUri, attributes)
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
