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
            Log.d("BootReceiver", "System event ($action): restoring pending reminder schedules...")
            val app = context.applicationContext as GemmyApplication
            val repository = app.repository
            val scheduler = app.scheduler

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val pendingReminders = repository.getAllPendingRemindersOnce()
                    for (reminder in pendingReminders) {
                        scheduler.scheduleReminder(reminder)
                    }
                    scheduler.updateHomeWidget()
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Error restoring reminders on boot: ${e.message}")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
