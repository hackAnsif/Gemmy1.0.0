package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.provider.Settings
import com.example.data.db.GemmyDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.GemmyRepository
import com.example.scheduler.GemmyScheduler

class GemmyApplication : Application() {

    lateinit var database: GemmyDatabase
        private set

    lateinit var preferencesRepository: UserPreferencesRepository
        private set

    lateinit var repository: GemmyRepository
        private set

    lateinit var scheduler: GemmyScheduler
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = GemmyDatabase.getDatabase(this)
        preferencesRepository = UserPreferencesRepository(this)
        scheduler = GemmyScheduler(this)
        repository = GemmyRepository(
            learningDao = database.learningItemDao(),
            reminderDao = database.reminderItemDao(),
            alarmDao = database.alarmItemDao(),
            reviewLogDao = database.reviewLogDao(),
            preferencesRepository = preferencesRepository,
            scheduler = scheduler
        )

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. Reminders Channel
            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                getString(R.string.notif_channel_reminders),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for scheduled tasks and reminders"
                enableVibration(true)
                setShowBadge(true)
            }

            // 2. Learning Reviews Channel
            val reviewChannel = NotificationChannel(
                CHANNEL_REVIEWS,
                getString(R.string.notif_channel_reviews),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for spaced-repetition vocabulary and concept reviews"
                enableVibration(true)
                setShowBadge(true)
            }

            // 3. Alarms Channel
            val alarmChannel = NotificationChannel(
                CHANNEL_ALARMS,
                getString(R.string.notif_channel_alarms),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority alarm clock notifications"
                enableVibration(true)
                setShowBadge(true)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
                setSound(Settings.System.DEFAULT_ALARM_ALERT_URI, audioAttributes)
            }

            notificationManager.createNotificationChannels(listOf(reminderChannel, reviewChannel, alarmChannel))
        }
    }

    companion object {
        lateinit var instance: GemmyApplication
            private set

        const val CHANNEL_REMINDERS = "gemmy_reminders_channel"
        const val CHANNEL_REVIEWS = "gemmy_reviews_channel"
        const val CHANNEL_ALARMS = "gemmy_alarms_channel"
    }
}
