package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.GemmyApplication
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GemmyWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, GemmyWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            if (appWidgetIds.isNotEmpty()) {
                updateWidgets(context, appWidgetManager, appWidgetIds)
            }
        }
    }

    private fun updateWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val app = context.applicationContext as? GemmyApplication ?: return
        val repository = app.repository

        CoroutineScope(Dispatchers.IO).launch {
            val nextReminder = repository.getNextUpcomingReminder(System.currentTimeMillis())

            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_gemmy)

                val addIntent = Intent(context, MainActivity::class.java).apply {
                    action = ACTION_NEW_REMINDER
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("destination", "add_reminder")
                }
                val addPendingIntent = PendingIntent.getActivity(
                    context,
                    1002,
                    addIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_add_button, addPendingIntent)

                val openIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val openPendingIntent = PendingIntent.getActivity(
                    context,
                    1000,
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, openPendingIntent)

                if (nextReminder != null) {
                    views.setTextViewText(R.id.widget_reminder_label, "NEXT UP")
                    views.setTextViewText(R.id.widget_reminder_title, nextReminder.title)
                    val timeFormat = SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault())
                    views.setTextViewText(R.id.widget_reminder_time, timeFormat.format(Date(nextReminder.dueTimeEpochMs)))
                } else {
                    views.setTextViewText(R.id.widget_reminder_label, "ALL CAUGHT UP")
                    views.setTextViewText(R.id.widget_reminder_title, "No upcoming reminders")
                    views.setTextViewText(R.id.widget_reminder_time, "Tap + to add a reminder")
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    companion object {
        const val ACTION_NEW_REMINDER = "com.example.gemmy.ACTION_NEW_REMINDER"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, GemmyWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(thisWidget)
            if (ids.isEmpty()) return

            val intent = Intent(context, GemmyWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}
