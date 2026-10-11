package com.example.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.model.AlarmItem
import com.example.data.model.LearningItem
import com.example.data.model.ReminderItem
import com.example.receiver.ReminderBroadcastReceiver
import com.example.widget.GemmyWidgetProvider
import java.util.Calendar

class GemmyScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun scheduleReminder(reminder: ReminderItem) {
        if (reminder.isCompleted) return

        val triggerAtMs = if (reminder.isSnoozed && reminder.snoozeUntilEpochMs > System.currentTimeMillis()) {
            reminder.snoozeUntilEpochMs
        } else {
            reminder.dueTimeEpochMs
        }

        if (triggerAtMs <= System.currentTimeMillis()) {
            Log.d("GemmyScheduler", "Reminder is in the past, skipping exact alarm.")
            return
        }

        val requestCode = REQUEST_CODE_REMINDER_OFFSET + (reminder.id % 100_000).toInt()
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_FIRE_REMINDER
            putExtra(ReminderBroadcastReceiver.EXTRA_ID, reminder.id)
            putExtra(ReminderBroadcastReceiver.EXTRA_TITLE, reminder.title)
            putExtra(ReminderBroadcastReceiver.EXTRA_DESC, reminder.description)
            putExtra(ReminderBroadcastReceiver.EXTRA_CATEGORY, reminder.category)
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
        val requestCode = REQUEST_CODE_REMINDER_OFFSET + (id % 100_000).toInt()
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_FIRE_REMINDER
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
        updateHomeWidget()
    }

    fun scheduleAlarm(alarm: AlarmItem) {
        if (!alarm.isEnabled) return

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val triggerAtMs = calendar.timeInMillis
        val requestCode = REQUEST_CODE_ALARM_OFFSET + (alarm.id % 100_000).toInt()
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_FIRE_ALARM
            putExtra(ReminderBroadcastReceiver.EXTRA_ID, alarm.id)
            putExtra(ReminderBroadcastReceiver.EXTRA_TITLE, alarm.label.ifBlank { "Alarm" })
            putExtra(ReminderBroadcastReceiver.EXTRA_DESC, "Alarm set for %02d:%02d".format(alarm.hour, alarm.minute))
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val showIntent = Intent(context, com.example.MainActivity::class.java)
            val showPendingIntent = PendingIntent.getActivity(
                context,
                requestCode,
                showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMs, showPendingIntent)
            try {
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            } catch (e: SecurityException) {
                scheduleAlarmInternal(triggerAtMs, pendingIntent)
            }
        } else {
            scheduleAlarmInternal(triggerAtMs, pendingIntent)
        }
        updateHomeWidget()
    }

    fun cancelAlarm(id: Long) {
        val requestCode = REQUEST_CODE_ALARM_OFFSET + (id % 100_000).toInt()
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_FIRE_ALARM
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
        updateHomeWidget()
    }

    fun scheduleReview(learningItem: LearningItem) {
        val triggerAtMs = learningItem.nextReviewEpochMs
        if (triggerAtMs <= System.currentTimeMillis()) {
            return
        }

        val requestCode = REQUEST_CODE_REVIEW_OFFSET + (learningItem.id % 100_000).toInt()
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_FIRE_REVIEW
            putExtra(ReminderBroadcastReceiver.EXTRA_ID, learningItem.id)
            putExtra(ReminderBroadcastReceiver.EXTRA_TITLE, "Time to Review: ${learningItem.title}")
            putExtra(ReminderBroadcastReceiver.EXTRA_DESC, learningItem.meaning.ifBlank { "Review your saved concept and test your memory." })
            putExtra(ReminderBroadcastReceiver.EXTRA_CATEGORY, learningItem.category)
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

    fun cancelReview(id: Long) {
        val requestCode = REQUEST_CODE_REVIEW_OFFSET + (id % 100_000).toInt()
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_FIRE_REVIEW
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
        const val REQUEST_CODE_ALARM_OFFSET = 200_000
        const val REQUEST_CODE_REVIEW_OFFSET = 300_000
    }
}
