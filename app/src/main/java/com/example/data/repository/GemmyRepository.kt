package com.example.data.repository

import android.app.NotificationManager
import android.content.Context
import com.example.data.db.ReminderItemDao
import com.example.data.model.ReminderItem
import com.example.data.preferences.UserPreferencesRepository
import com.example.scheduler.GemmyScheduler
import kotlinx.coroutines.flow.Flow

class GemmyRepository(
    private val reminderDao: ReminderItemDao,
    val preferencesRepository: UserPreferencesRepository,
    private val scheduler: GemmyScheduler
) {

    val allReminders: Flow<List<ReminderItem>> = reminderDao.getAllReminders()

    suspend fun getNextUpcomingReminder(startEpochMs: Long): ReminderItem? {
        return reminderDao.getNextUpcomingReminder(startEpochMs)
    }

    suspend fun getReminderById(id: Long): ReminderItem? {
        return reminderDao.getReminderById(id)
    }

    suspend fun insertReminder(reminder: ReminderItem): Long {
        val id = reminderDao.insert(reminder)
        val saved = reminder.copy(id = id)
        if (!saved.isCompleted && saved.alarmEnabled) {
            scheduler.scheduleReminder(saved)
        } else {
            scheduler.updateHomeWidget()
        }
        return id
    }

    suspend fun updateReminder(reminder: ReminderItem) {
        reminderDao.update(reminder)
        if (!reminder.isCompleted && reminder.alarmEnabled) {
            scheduler.scheduleReminder(reminder)
        } else {
            scheduler.cancelReminder(reminder.id)
        }
    }

    suspend fun deleteReminder(id: Long) {
        scheduler.cancelReminder(id)
        reminderDao.deleteById(id)
        scheduler.updateHomeWidget()
    }

    suspend fun deleteCompletedReminders() {
        val all = reminderDao.getAllRemindersOnce()
        all.filter { it.isCompleted }.forEach { scheduler.cancelReminder(it.id) }
        reminderDao.deleteCompletedReminders()
        scheduler.updateHomeWidget()
    }

    suspend fun markReminderCompleted(id: Long, completed: Boolean = true) {
        if (completed) {
            scheduler.cancelReminder(id)
            reminderDao.markComplete(id, System.currentTimeMillis())
        } else {
            reminderDao.markIncomplete(id)
            val reminder = reminderDao.getReminderById(id)
            if (reminder != null && reminder.alarmEnabled && reminder.dueTimeEpochMs > System.currentTimeMillis()) {
                scheduler.scheduleReminder(reminder)
            }
        }
        scheduler.updateHomeWidget()
    }

    suspend fun snoozeReminder(id: Long, snoozeUntilMs: Long) {
        reminderDao.snoozeReminder(id, snoozeUntilMs)
        val reminder = reminderDao.getReminderById(id)
        if (reminder != null && !reminder.isCompleted && reminder.alarmEnabled) {
            scheduler.scheduleReminder(
                reminder.copy(
                    dueTimeEpochMs = snoozeUntilMs,
                    isSnoozed = true,
                    snoozeUntilEpochMs = snoozeUntilMs
                )
            )
        }
    }

    suspend fun getAllPendingRemindersOnce(): List<ReminderItem> {
        return reminderDao.getAllPendingRemindersOnce()
    }

    suspend fun clearAllReminders(context: Context? = null) {
        val reminders = reminderDao.getAllRemindersOnce()
        reminders.forEach { scheduler.cancelReminder(it.id) }

        context?.let {
            val nm = it.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.cancelAll()
        }

        reminderDao.clearAll()
        scheduler.updateHomeWidget()
    }
}
