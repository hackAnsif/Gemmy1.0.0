package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
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
            reminderDao = database.reminderItemDao(),
            preferencesRepository = preferencesRepository,
            scheduler = scheduler
        )

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                getString(R.string.notif_channel_reminders),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Scheduled reminder alerts and notifications"
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(reminderChannel)
        }
    }

    companion object {
        lateinit var instance: GemmyApplication
            private set

        const val CHANNEL_REMINDERS = "gemmy_reminders_channel"
    }
}
