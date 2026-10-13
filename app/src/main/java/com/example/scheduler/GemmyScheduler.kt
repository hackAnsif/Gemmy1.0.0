package com.example.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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

        val requestCode = getReminderRequestCode(reminder.id)
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_FIRE_REMINDER
            data = Uri.parse("gemmy://reminder/${reminder.id}")
            putExtra(ReminderBroadcastReceiver.EXTRA_ID, reminder.id)
            putExtra(ReminderBroadcastReceiver.EXTRA_TITLE, reminder.title)
            putExtra(ReminderBroadcastReceiver.EXTRA_DESC, reminder.description)
            putExtra(ReminderBroadcastReceiver.EXTRA_CATEGORY, reminder.category)
            putExtra(ReminderBroadcastReceiver.EXTRA_REPEAT, reminder.repeatSchedule.name)
            putExtra(ReminderBroadcastReceiver.EXTRA_SOUND, reminder.soundEnabled)
            putExtra(ReminderBroadcastReceiver.EXTRA_VIBRATE, reminder.vibrateEnabled)
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
        val requestCode = getAlarmRequestCode(alarm.id)
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_FIRE_ALARM
            data = Uri.parse("gemmy://alarm/${alarm.id}")
            putExtra(ReminderBroadcastReceiver.EXTRA_ID, alarm.id)
            putExtra(ReminderBroadcastReceiver.EXTRA_TITLE, alarm.label.ifBlank { "Alarm" })
            putExtra(ReminderBroadcastReceiver.EXTRA_DESC, "Alarm set for %02d:%02d".format(alarm.hour, alarm.minute))
            putExtra(ReminderBroadcastReceiver.EXTRA_VIBRATE, alarm.vibrateEnabled)
            putExtra(ReminderBroadcastReceiver.EXTRA_SNOOZE_MINS, alarm.snoozeMinutes)
            putExtra(ReminderBroadcastReceiver.EXTRA_TONE_URI, alarm.toneUri)
            putExtra(ReminderBroadcastReceiver.EXTRA_TONE_TITLE, alarm.toneTitle)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, com.example.MainActivity::class.java).apply {
            data = Uri.parse("gemmy://show_alarm/${alarm.id}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
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
        updateHomeWidget()
    }

    fun cancelAlarm(id: Long) {
        val requestCode = getAlarmRequestCode(id)
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_FIRE_ALARM
            data = Uri.parse("gemmy://alarm/$id")
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
        // Enforce isSpacedRepetitionEnabled flag
        if (!learningItem.isSpacedRepetitionEnabled) {
            return
        }

        val triggerAtMs = learningItem.nextReviewEpochMs
        if (triggerAtMs <= System.currentTimeMillis()) {
            return
        }

        val requestCode = getReviewRequestCode(learningItem.id)
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_FIRE_REVIEW
            data = Uri.parse("gemmy://review/${learningItem.id}")
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
        val requestCode = getReviewRequestCode(id)
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_FIRE_REVIEW
            data = Uri.parse("gemmy://review/$id")
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

    fun cancelAllScheduledAlarms(
        reminders: List<ReminderItem>,
        alarms: List<AlarmItem>,
        reviews: List<LearningItem>
    ) {
        for (r in reminders) cancelReminder(r.id)
        for (a in alarms) cancelAlarm(a.id)
        for (l in reviews) cancelReview(l.id)
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

        fun getReminderRequestCode(id: Long): Int = (REQUEST_CODE_REMINDER_OFFSET + (id and 0x7FFFFFFF)).toInt()
        fun getAlarmRequestCode(id: Long): Int = (REQUEST_CODE_ALARM_OFFSET + (id and 0x7FFFFFFF)).toInt()
        fun getReviewRequestCode(id: Long): Int = (REQUEST_CODE_REVIEW_OFFSET + (id and 0x7FFFFFFF)).toInt()
    }
}
