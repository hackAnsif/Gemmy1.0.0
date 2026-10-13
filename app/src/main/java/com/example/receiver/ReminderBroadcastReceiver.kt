package com.example.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.example.GemmyApplication
import com.example.MainActivity
import com.example.R
import com.example.data.model.RepeatSchedule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class ReminderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val id = intent.getLongExtra(EXTRA_ID, -1L)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Gemmy Reminder"
        val desc = intent.getStringExtra(EXTRA_DESC) ?: ""
        val category = intent.getStringExtra(EXTRA_CATEGORY) ?: "General"
        val repeatStr = intent.getStringExtra(EXTRA_REPEAT) ?: "NONE"
        val soundEnabled = intent.getBooleanExtra(EXTRA_SOUND, true)
        val vibrateEnabled = intent.getBooleanExtra(EXTRA_VIBRATE, true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val app = context.applicationContext as GemmyApplication
        val repository = app.repository
        val scheduler = app.scheduler

        when (action) {
            ACTION_FIRE_REMINDER -> {
                showReminderNotification(context, notificationManager, id, title, desc, category, soundEnabled, vibrateEnabled)

                // Handle Recurring Reminders: schedule next occurrence
                val repeatSchedule = try {
                    RepeatSchedule.valueOf(repeatStr)
                } catch (e: Exception) {
                    RepeatSchedule.NONE
                }

                if (repeatSchedule != RepeatSchedule.NONE && id != -1L) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val reminder = repository.getReminderById(id)
                            if (reminder != null && !reminder.isCompleted) {
                                val nextDue = calculateNextOccurrence(reminder.dueTimeEpochMs, reminder.repeatSchedule)
                                val updated = reminder.copy(
                                    dueTimeEpochMs = nextDue,
                                    isSnoozed = false,
                                    snoozeUntilEpochMs = 0L
                                )
                                repository.updateReminder(updated)
                            }
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
            ACTION_FIRE_REVIEW -> {
                showReviewNotification(context, notificationManager, id, title, desc, category, soundEnabled, vibrateEnabled)
            }
            ACTION_FIRE_ALARM -> {
                val toneUri = intent.getStringExtra(EXTRA_TONE_URI) ?: ""
                showAlarmNotification(context, notificationManager, id, title, desc, vibrateEnabled, toneUri)

                // For one-time alarms, update enabled status to false in DB
                if (id != -1L) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val alarm = app.database.alarmItemDao().getAlarmById(id)
                            if (alarm != null && !alarm.isRepeating) {
                                repository.setAlarmEnabled(id, false)
                            }
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
        }
    }

    private fun showReminderNotification(
        context: Context,
        notificationManager: NotificationManager,
        id: Long,
        title: String,
        desc: String,
        category: String,
        soundEnabled: Boolean,
        vibrateEnabled: Boolean
    ) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            data = Uri.parse("gemmy://open/reminder/$id")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DESTINATION, "reminders")
            putExtra(EXTRA_ID, id)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            (100_000 + (id and 0x7FFFFFFF)).toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_NOTIFICATION_COMPLETE
            data = Uri.parse("gemmy://action/complete/reminder/$id")
            putExtra(EXTRA_ID, id)
            putExtra(EXTRA_TYPE, "reminder")
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            (110_000 + (id and 0x7FFFFFFF)).toInt(),
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_NOTIFICATION_SNOOZE
            data = Uri.parse("gemmy://action/snooze/reminder/$id")
            putExtra(EXTRA_ID, id)
            putExtra(EXTRA_TYPE, "reminder")
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (120_000 + (id and 0x7FFFFFFF)).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = if (soundEnabled) RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) else null

        val builder = NotificationCompat.Builder(context, GemmyApplication.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_bell_white)
            .setContentTitle(title)
            .setContentText(desc.ifBlank { "Reminder: $category" })
            .setStyle(NotificationCompat.BigTextStyle().bigText(desc.ifBlank { "Reminder for $category" }))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(0, "Mark Done", completePendingIntent)
            .addAction(0, "Snooze 10m", snoozePendingIntent)

        if (soundUri != null) builder.setSound(soundUri)
        if (vibrateEnabled) builder.setVibrate(longArrayOf(0, 300, 200, 300))

        notificationManager.notify((100_000 + id).toInt(), builder.build())
    }

    private fun showReviewNotification(
        context: Context,
        notificationManager: NotificationManager,
        id: Long,
        title: String,
        desc: String,
        category: String,
        soundEnabled: Boolean,
        vibrateEnabled: Boolean
    ) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            data = Uri.parse("gemmy://open/review/$id")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DESTINATION, "review")
            putExtra(EXTRA_ID, id)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            (300_000 + (id and 0x7FFFFFFF)).toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_NOTIFICATION_SNOOZE
            data = Uri.parse("gemmy://action/snooze/review/$id")
            putExtra(EXTRA_ID, id)
            putExtra(EXTRA_TYPE, "review")
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (320_000 + (id and 0x7FFFFFFF)).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = if (soundEnabled) RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) else null

        val builder = NotificationCompat.Builder(context, GemmyApplication.CHANNEL_REVIEWS)
            .setSmallIcon(R.drawable.ic_bell_white)
            .setContentTitle(title)
            .setContentText("Test your recall: $desc")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Time to review your saved learning concept: $desc"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(0, "Review Now", openPendingIntent)
            .addAction(0, "Later (1h)", snoozePendingIntent)

        if (soundUri != null) builder.setSound(soundUri)
        if (vibrateEnabled) builder.setVibrate(longArrayOf(0, 300, 200, 300))

        notificationManager.notify((300_000 + id).toInt(), builder.build())
    }

    private fun showAlarmNotification(
        context: Context,
        notificationManager: NotificationManager,
        id: Long,
        title: String,
        desc: String,
        vibrateEnabled: Boolean,
        toneUriStr: String = ""
    ) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            data = Uri.parse("gemmy://open/alarm/$id")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DESTINATION, "alarms")
            putExtra(EXTRA_ID, id)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            (200_000 + (id and 0x7FFFFFFF)).toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_NOTIFICATION_SNOOZE
            data = Uri.parse("gemmy://action/snooze/alarm/$id")
            putExtra(EXTRA_ID, id)
            putExtra(EXTRA_TYPE, "alarm")
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (220_000 + (id and 0x7FFFFFFF)).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_DISMISS_ALARM
            data = Uri.parse("gemmy://action/dismiss/alarm/$id")
            putExtra(EXTRA_ID, id)
            putExtra(EXTRA_TYPE, "alarm")
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            (240_000 + (id and 0x7FFFFFFF)).toInt(),
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmSound = if (toneUriStr.isNotBlank()) {
            try {
                Uri.parse(toneUriStr)
            } catch (e: Exception) {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            }
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        }

        val builder = NotificationCompat.Builder(context, GemmyApplication.CHANNEL_ALARMS)
            .setSmallIcon(R.drawable.ic_bell_white)
            .setContentTitle(title)
            .setContentText(desc)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSound(alarmSound)
            .setContentIntent(openPendingIntent)
            .addAction(0, "Snooze 10m", snoozePendingIntent)
            .addAction(0, "Dismiss", dismissPendingIntent)

        if (vibrateEnabled) builder.setVibrate(longArrayOf(0, 600, 400, 600, 400, 600))

        notificationManager.notify((200_000 + id).toInt(), builder.build())
    }

    companion object {
        const val ACTION_FIRE_REMINDER = "com.example.gemmy.ACTION_FIRE_REMINDER"
        const val ACTION_FIRE_REVIEW = "com.example.gemmy.ACTION_FIRE_REVIEW"
        const val ACTION_FIRE_ALARM = "com.example.gemmy.ACTION_FIRE_ALARM"

        const val EXTRA_ID = "extra_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_DESC = "extra_desc"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_TYPE = "extra_type"
        const val EXTRA_DESTINATION = "extra_destination"
        const val EXTRA_REPEAT = "extra_repeat"
        const val EXTRA_SOUND = "extra_sound"
        const val EXTRA_VIBRATE = "extra_vibrate"
        const val EXTRA_SNOOZE_MINS = "extra_snooze_mins"
        const val EXTRA_TONE_URI = "extra_tone_uri"
        const val EXTRA_TONE_TITLE = "extra_tone_title"

        fun calculateNextOccurrence(dueEpochMs: Long, repeat: RepeatSchedule): Long {
            val cal = Calendar.getInstance().apply {
                timeInMillis = dueEpochMs
            }
            val now = System.currentTimeMillis()
            while (cal.timeInMillis <= now) {
                when (repeat) {
                    RepeatSchedule.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
                    RepeatSchedule.WEEKDAYS -> {
                        do {
                            cal.add(Calendar.DAY_OF_YEAR, 1)
                        } while (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
                    }
                    RepeatSchedule.WEEKLY -> cal.add(Calendar.DAY_OF_YEAR, 7)
                    RepeatSchedule.MONTHLY -> cal.add(Calendar.MONTH, 1)
                    RepeatSchedule.NONE -> return dueEpochMs
                }
            }
            return cal.timeInMillis
        }
    }
}
