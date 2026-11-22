package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.GemmyApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val id = intent.getLongExtra(ReminderBroadcastReceiver.EXTRA_ID, -1L)
        if (id <= 0L) return

        val app = context.applicationContext as GemmyApplication
        val repository = app.repository

        when (action) {
            ACTION_NOTIFICATION_COMPLETE -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        repository.markReminderCompleted(id, completed = true)
                    } finally {
                        pendingResult?.finish()
                    }
                }
            }
            ACTION_NOTIFICATION_SNOOZE -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val snoozeMinutes = repository.preferencesRepository.defaultSnoozeMinutes.first()
                        val snoozeTime = System.currentTimeMillis() + (snoozeMinutes * 60_000L)
                        app.scheduler.cancelReminder(id)
                        repository.snoozeReminder(id, snoozeTime)
                    } finally {
                        pendingResult?.finish()
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_NOTIFICATION_COMPLETE = "com.example.gemmy.ACTION_NOTIFICATION_COMPLETE"
        const val ACTION_NOTIFICATION_SNOOZE = "com.example.gemmy.ACTION_NOTIFICATION_SNOOZE"
    }
}
