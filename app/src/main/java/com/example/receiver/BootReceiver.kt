package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.GemmyApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            Log.d("BootReceiver", "Device restarted or time changed ($action), rescheduling alarms & reviews...")
            val app = context.applicationContext as GemmyApplication
            val repository = app.repository
            val scheduler = app.scheduler

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val now = System.currentTimeMillis()

                    // 1. Reschedule active reminders
                    val pendingReminders = repository.getAllPendingRemindersOnce()
                    for (reminder in pendingReminders) {
                        scheduler.scheduleReminder(reminder)
                    }

                    // 2. Reschedule active alarms
                    val activeAlarms = repository.getActiveAlarmsOnce()
                    for (alarm in activeAlarms) {
                        scheduler.scheduleAlarm(alarm)
                    }

                    // 3. Reschedule active learning reviews
                    val futureReviews = repository.getFutureScheduledReviewsOnce(now)
                    for (item in futureReviews) {
                        scheduler.scheduleReview(item)
                    }

                    // 4. Update home widgets
                    scheduler.updateHomeWidget()
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Error rescheduling items on boot: ${e.message}")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
