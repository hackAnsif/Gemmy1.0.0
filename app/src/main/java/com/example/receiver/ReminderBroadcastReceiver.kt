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
import com.example.scheduler.GemmyScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE_REMINDER) return

        val id = intent.getLongExtra(EXTRA_ID, -1L)
        val fallbackTitle = intent.getStringExtra(EXTRA_TITLE) ?: "Gemmy Reminder"
        val fallbackDesc = intent.getStringExtra(EXTRA_DESC) ?: ""

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val app = context.applicationContext as GemmyApplication
        val repository = app.repository

        if (id <= 0L) {
            showReminderNotification(context, notificationManager, id, fallbackTitle, fallbackDesc)
            return
        }

        // Verify live state in Room before posting so completed, deleted, or muted reminders never fire duplicates
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reminder = repository.getReminderById(id)
                if (reminder != null && !reminder.isCompleted && reminder.alarmEnabled) {
                    val contentTitle = reminder.title.ifBlank { fallbackTitle }
                    val contentText = reminder.description.ifBlank { "Scheduled reminder" }
                    showReminderNotification(context, notificationManager, id, contentTitle, contentText)
                }
            } finally {
                pendingResult?.finish()
            }
        }
    }

    private fun showReminderNotification(
        context: Context,
        notificationManager: NotificationManager,
        id: Long,
        title: String,
        desc: String
    ) {
        val requestCode = GemmyScheduler.getReminderRequestCode(id)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            data = Uri.parse("gemmy://open/reminder/$id")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DESTINATION, "home")
            putExtra(EXTRA_ID, id)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_NOTIFICATION_COMPLETE
            data = Uri.parse("gemmy://action/complete/reminder/$id")
            putExtra(EXTRA_ID, id)
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
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (120_000 + (id and 0x7FFFFFFF)).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, GemmyApplication.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_bell_white)
            .setContentTitle(title)
            .setContentText(desc)
            .setStyle(NotificationCompat.BigTextStyle().bigText(if (desc.isNotBlank() && desc != "Scheduled reminder") desc else title))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openPendingIntent)
            .addAction(0, "Mark Done", completePendingIntent)
            .addAction(0, "Snooze 10m", snoozePendingIntent)
            .setVibrate(longArrayOf(0, 250, 150, 250))

        if (soundUri != null) {
            builder.setSound(soundUri)
        }

        notificationManager.notify(requestCode, builder.build())
    }

    companion object {
        const val ACTION_FIRE_REMINDER = "com.example.gemmy.ACTION_FIRE_REMINDER"

        const val EXTRA_ID = "extra_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_DESC = "extra_desc"
        const val EXTRA_DESTINATION = "extra_destination"
        const val EXTRA_DUE_TIME = "extra_due_time"
    }
}
