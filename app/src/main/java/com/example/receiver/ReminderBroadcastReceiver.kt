package com.example.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.GemmyApplication
import com.example.MainActivity
import com.example.R

class ReminderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val id = intent.getLongExtra(EXTRA_ID, -1L)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Gemmy Reminder"
        val desc = intent.getStringExtra(EXTRA_DESC) ?: ""
        val category = intent.getStringExtra(EXTRA_CATEGORY) ?: "General"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (action) {
            ACTION_FIRE_REMINDER -> {
                showReminderNotification(context, notificationManager, id, title, desc, category)
            }
            ACTION_FIRE_REVIEW -> {
                showReviewNotification(context, notificationManager, id, title, desc, category)
            }
            ACTION_FIRE_ALARM -> {
                showAlarmNotification(context, notificationManager, id, title, desc)
            }
        }
    }

    private fun showReminderNotification(
        context: Context,
        notificationManager: NotificationManager,
        id: Long,
        title: String,
        desc: String,
        category: String
    ) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DESTINATION, "reminders")
            putExtra(EXTRA_ID, id)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            (id % 50_000).toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_NOTIFICATION_COMPLETE
            putExtra(EXTRA_ID, id)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            (100_000 + (id % 50_000)).toInt(),
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_NOTIFICATION_SNOOZE
            putExtra(EXTRA_ID, id)
            putExtra(EXTRA_TYPE, "reminder")
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (200_000 + (id % 50_000)).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, GemmyApplication.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_bell_white)
            .setContentTitle(title)
            .setContentText(desc.ifBlank { "Reminder: $category" })
            .setStyle(NotificationCompat.BigTextStyle().bigText(desc.ifBlank { "Reminder for $category" }))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .setContentIntent(openPendingIntent)
            .addAction(0, "Mark Done", completePendingIntent)
            .addAction(0, "Snooze 10m", snoozePendingIntent)

        notificationManager.notify(id.toInt(), builder.build())
    }

    private fun showReviewNotification(
        context: Context,
        notificationManager: NotificationManager,
        id: Long,
        title: String,
        desc: String,
        category: String
    ) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DESTINATION, "review")
            putExtra(EXTRA_ID, id)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            (id % 50_000).toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_NOTIFICATION_SNOOZE
            putExtra(EXTRA_ID, id)
            putExtra(EXTRA_TYPE, "review")
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (250_000 + (id % 50_000)).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

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

        notificationManager.notify((300_000 + id).toInt(), builder.build())
    }

    private fun showAlarmNotification(
        context: Context,
        notificationManager: NotificationManager,
        id: Long,
        title: String,
        desc: String
    ) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DESTINATION, "alarms")
            putExtra(EXTRA_ID, id)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            (id % 50_000).toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_DISMISS_ALARM
            putExtra(EXTRA_ID, id)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            (400_000 + (id % 50_000)).toInt(),
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        val builder = NotificationCompat.Builder(context, GemmyApplication.CHANNEL_ALARMS)
            .setSmallIcon(R.drawable.ic_bell_white)
            .setContentTitle(title)
            .setContentText(desc)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSound(alarmSound)
            .setVibrate(longArrayOf(0, 600, 400, 600, 400, 600))
            .setContentIntent(openPendingIntent)
            .addAction(0, "Dismiss", dismissPendingIntent)

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
    }
}
