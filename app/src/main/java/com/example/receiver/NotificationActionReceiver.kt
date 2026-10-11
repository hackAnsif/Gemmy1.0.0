package com.example.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.GemmyApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
                    notificationManager.cancel(id.toInt())
                    CoroutineScope(Dispatchers.IO).launch {
                        repository.markReminderCompleted(id)
                    }
                }
            }
            ACTION_NOTIFICATION_SNOOZE -> {
                if (id != -1L) {
                    notificationManager.cancel(id.toInt())
                    notificationManager.cancel((250_000 + id).toInt())
                    notificationManager.cancel((300_000 + id).toInt())

                    CoroutineScope(Dispatchers.IO).launch {
                        if (type == "review") {
                            // Snooze review by 1 hour
                            val snoozeTime = System.currentTimeMillis() + 3600_000L
                            val item = repository.getLearningItemById(id)
                            if (item != null) {
                                val updated = item.copy(nextReviewEpochMs = snoozeTime)
                                repository.updateLearningItem(updated)
                                scheduler.scheduleReview(updated)
                            }
                        } else {
                            // Snooze reminder by 10 minutes
                            val snoozeTime = System.currentTimeMillis() + 600_000L
                            repository.snoozeReminder(id, snoozeTime)
                            val reminder = repository.getReminderById(id)
                            if (reminder != null) {
                                scheduler.scheduleReminder(reminder.copy(dueTimeEpochMs = snoozeTime, isSnoozed = true, snoozeUntilEpochMs = snoozeTime))
                            }
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
