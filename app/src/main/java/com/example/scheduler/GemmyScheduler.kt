package com.example.scheduler

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.example.data.model.ReminderItem
import com.example.receiver.ReminderBroadcastReceiver
import com.example.widget.GemmyWidgetProvider

class GemmyScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun getExactAlarmPermissionIntent(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExactAlarms()) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            null
        }
    }

    fun scheduleReminder(reminder: ReminderItem) {
        if (reminder.isCompleted || !reminder.alarmEnabled) {
            cancelReminder(reminder.id)
            return
        }

        val now = System.currentTimeMillis()
        val isSnoozed = reminder.isSnoozed && reminder.snoozeUntilEpochMs > now
        val triggerAtMs = if (isSnoozed) reminder.snoozeUntilEpochMs else reminder.dueTimeEpochMs

        if (triggerAtMs <= now) {
            Log.d("GemmyScheduler", "Reminder trigger time is in the past, skipping exact alarm.")
            return
        }

        val requestCode = getReminderRequestCode(reminder.id)
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_FIRE_REMINDER
            data = Uri.parse("gemmy://reminder/${reminder.id}")
            putExtra(ReminderBroadcastReceiver.EXTRA_ID, reminder.id)
            putExtra(ReminderBroadcastReceiver.EXTRA_TITLE, reminder.title)
            putExtra(ReminderBroadcastReceiver.EXTRA_DESC, reminder.description)
            putExtra(ReminderBroadcastReceiver.EXTRA_DUE_TIME, triggerAtMs)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarmInternal(triggerAtMs, pendingIntent)
        updateHomeWidget()
    }

    fun cancelReminder(id: Long) {
        val requestCode = getReminderRequestCode(id)
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_FIRE_REMINDER
            data = Uri.parse("gemmy://reminder/$id")
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.cancel(requestCode)

        updateHomeWidget()
    }

    private fun scheduleAlarmInternal(triggerAtMs: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
                }
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
            }
        } catch (e: SecurityException) {
            Log.e("GemmyScheduler", "SecurityException scheduling alarm: ${e.message}")
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
            } catch (ex: Exception) {
                Log.e("GemmyScheduler", "Failed fallback schedule: ${ex.message}")
            }
        }
    }

    fun updateHomeWidget() {
        GemmyWidgetProvider.updateAllWidgets(context)
    }

    companion object {
        const val REQUEST_CODE_REMINDER_OFFSET = 100_000

        fun getReminderRequestCode(id: Long): Int {
            return REQUEST_CODE_REMINDER_OFFSET + (id and 0x7FFFFFFF).toInt()
        }
    }
}
