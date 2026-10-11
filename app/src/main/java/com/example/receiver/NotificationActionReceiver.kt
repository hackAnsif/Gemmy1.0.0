package com.example.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.GemmyApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val id = intent.getLongExtra(ReminderBroadcastReceiver.EXTRA_ID, -1L)
        val type = intent.getStringExtra(ReminderBroadcastReceiver.EXTRA_TYPE) ?: "reminder"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val app = context.applicationContext as GemmyApplication
        val repository = app.repository
        val scheduler = app.scheduler

        when (action) {
            ACTION_NOTIFICATION_COMPLETE -> {
                if (id != -1L) {
                    notificationManager.cancel((100_000 + id).toInt())
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            repository.markReminderCompleted(id)
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
            ACTION_NOTIFICATION_SNOOZE -> {
                if (id != -1L) {
                    when (type) {
                        "review" -> notificationManager.cancel((300_000 + id).toInt())
                        "alarm" -> notificationManager.cancel((200_000 + id).toInt())
                        else -> notificationManager.cancel((100_000 + id).toInt())
                    }

                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            when (type) {
                                "review" -> {
                                    // Snooze review by 1 hour
                                    val snoozeTime = System.currentTimeMillis() + 3600_000L
                                    val item = repository.getLearningItemById(id)
                                    if (item != null) {
                                        val updated = item.copy(nextReviewEpochMs = snoozeTime)
                                        repository.updateLearningItem(updated)
                                    }
                                }
                                "alarm" -> {
                                    // Snooze alarm by 10 minutes
                                    val cal = Calendar.getInstance().apply {
                                        add(Calendar.MINUTE, 10)
                                    }
                                    val alarm = app.database.alarmItemDao().getAlarmById(id)
                                    if (alarm != null) {
                                        val snoozedAlarm = alarm.copy(
                                            hour = cal.get(Calendar.HOUR_OF_DAY),
                                            minute = cal.get(Calendar.MINUTE),
                                            isEnabled = true
                                        )
                                        scheduler.scheduleAlarm(snoozedAlarm)
                                    }
                                }
                                else -> {
                                    // Snooze reminder by 10 minutes
                                    val snoozeTime = System.currentTimeMillis() + 600_000L
                                    repository.snoozeReminder(id, snoozeTime)
                                }
                            }
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
            ACTION_DISMISS_ALARM -> {
                if (id != -1L) {
                    notificationManager.cancel((200_000 + id).toInt())
                }
            }
        }
    }

    companion object {
        const val ACTION_NOTIFICATION_COMPLETE = "com.example.gemmy.ACTION_NOTIFICATION_COMPLETE"
        const val ACTION_NOTIFICATION_SNOOZE = "com.example.gemmy.ACTION_NOTIFICATION_SNOOZE"
        const val ACTION_DISMISS_ALARM = "com.example.gemmy.ACTION_DISMISS_ALARM"
    }
}
