package com.example.data.repository

import android.app.NotificationManager
import android.content.Context
import com.example.data.db.AlarmItemDao
import com.example.data.db.LearningItemDao
import com.example.data.db.ReminderItemDao
import com.example.data.db.ReviewLogDao
import com.example.data.model.AlarmItem
import com.example.data.model.LearningItem
import com.example.data.model.LearningStatus
import com.example.data.model.RecallRating
import com.example.data.model.ReminderItem
import com.example.data.model.ReminderPriority
import com.example.data.model.RepeatSchedule
import com.example.data.model.ReviewLog
import com.example.data.preferences.UserPreferencesRepository
import com.example.scheduler.GemmyScheduler
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

class GemmyRepository(
    private val learningDao: LearningItemDao,
    private val reminderDao: ReminderItemDao,
    private val alarmDao: AlarmItemDao,
    private val reviewLogDao: ReviewLogDao,
    val preferencesRepository: UserPreferencesRepository,
    private val scheduler: GemmyScheduler
) {

    // --- Learning Items ---
    val allLearningItems: Flow<List<LearningItem>> = learningDao.getAllLearningItems()

    fun getDueLearningItems(): Flow<List<LearningItem>> {
        return learningDao.getItemsDueForReview(System.currentTimeMillis())
    }

    fun searchLearningItems(query: String): Flow<List<LearningItem>> {
        return learningDao.searchLearningItems(query)
    }

    suspend fun getLearningItemById(id: Long): LearningItem? {
        return learningDao.getLearningItemById(id)
    }

    suspend fun insertLearningItem(item: LearningItem): Long {
        val id = learningDao.insert(item)
        val savedItem = item.copy(id = id)
        scheduler.scheduleReview(savedItem)
        return id
    }

    suspend fun updateLearningItem(item: LearningItem) {
        learningDao.update(item)
        if (item.isSpacedRepetitionEnabled) {
            scheduler.scheduleReview(item)
        } else {
            scheduler.cancelReview(item.id)
        }
    }

    suspend fun deleteLearningItem(id: Long) {
        scheduler.cancelReview(id)
        learningDao.deleteById(id)
    }

    suspend fun getFutureScheduledReviewsOnce(now: Long): List<LearningItem> {
        return learningDao.getFutureScheduledReviewsOnce(now)
    }

    /**
     * True Cognitive Spaced-Repetition SM-2 Algorithm with Dynamic Ease Factor
     */
    suspend fun recordRecallReview(item: LearningItem, rating: RecallRating) {
        val currentInterval = if (item.intervalDays <= 0) 1 else item.intervalDays
        val quality = when (rating) {
            RecallRating.AGAIN -> 1
            RecallRating.HARD -> 3
            RecallRating.GOOD -> 4
            RecallRating.EASY -> 5
        }

        // SM-2 Ease Factor calculation
        val efDelta = 0.1f - (5 - quality) * (0.08f + (5 - quality) * 0.02f)
        val newEaseFactor = kotlin.math.max(1.3f, item.easeFactor + efDelta)

        val nextIntervalDays: Int
        val nextStage: Int
        val nextStatus: LearningStatus

        if (quality < 3) {
            // Forgotten: Reset interval to 1 day
            nextIntervalDays = 1
            nextStage = 0
            nextStatus = LearningStatus.LEARNING
        } else {
            // Recalled successfully
            nextIntervalDays = when (item.repetitionStage) {
                0 -> if (rating == RecallRating.EASY) 2 else 1
                1 -> if (rating == RecallRating.EASY) 6 else 3
                else -> kotlin.math.max(1, (currentInterval * newEaseFactor).toInt())
            }
            nextStage = item.repetitionStage + 1
            nextStatus = if (nextIntervalDays >= 21) LearningStatus.REMEMBERED else LearningStatus.REVIEWING
        }

        val nextReviewEpochMs = System.currentTimeMillis() + (nextIntervalDays * 86400000L)
        val updatedItem = item.copy(
            nextReviewEpochMs = nextReviewEpochMs,
            lastReviewedEpochMs = System.currentTimeMillis(),
            intervalDays = nextIntervalDays,
            repetitionStage = nextStage,
            recallCount = item.recallCount + 1,
            easeFactor = newEaseFactor,
            status = nextStatus
        )

        learningDao.update(updatedItem)
        reviewLogDao.insert(
            ReviewLog(
                learningItemId = item.id,
                learningItemTitle = item.title,
                rating = rating,
                intervalDaysAssigned = nextIntervalDays
            )
        )

        if (updatedItem.isSpacedRepetitionEnabled) {
            scheduler.scheduleReview(updatedItem)
        }
    }

    // --- Reminders ---
    val allReminders: Flow<List<ReminderItem>> = reminderDao.getAllReminders()

    fun getUpcomingReminders(): Flow<List<ReminderItem>> {
        return reminderDao.getUpcomingReminders(System.currentTimeMillis())
    }

    suspend fun getNextUpcomingReminder(startEpochMs: Long): ReminderItem? {
        return reminderDao.getNextUpcomingReminder(startEpochMs)
    }

    fun getTodayReminders(): Flow<List<ReminderItem>> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val endOfDay = calendar.timeInMillis - 1
        return reminderDao.getTodayReminders(startOfDay, endOfDay)
    }

    fun getCompletedReminders(): Flow<List<ReminderItem>> {
        return reminderDao.getCompletedReminders()
    }

    fun getOverdueReminders(): Flow<List<ReminderItem>> {
        return reminderDao.getOverdueReminders(System.currentTimeMillis())
    }

    suspend fun getReminderById(id: Long): ReminderItem? {
        return reminderDao.getReminderById(id)
    }

    suspend fun insertReminder(reminder: ReminderItem): Long {
        val id = reminderDao.insert(reminder)
        val saved = reminder.copy(id = id)
        scheduler.scheduleReminder(saved)
        return id
    }

    suspend fun updateReminder(reminder: ReminderItem) {
        reminderDao.update(reminder)
        scheduler.scheduleReminder(reminder)
    }

    suspend fun deleteReminder(id: Long) {
        scheduler.cancelReminder(id)
        reminderDao.deleteById(id)
    }

    suspend fun markReminderCompleted(id: Long) {
        scheduler.cancelReminder(id)
        reminderDao.markComplete(id, System.currentTimeMillis())
    }

    suspend fun snoozeReminder(id: Long, snoozeUntilMs: Long) {
        reminderDao.snoozeReminder(id, snoozeUntilMs)
        val reminder = reminderDao.getReminderById(id)
        if (reminder != null) {
            scheduler.scheduleReminder(reminder.copy(dueTimeEpochMs = snoozeUntilMs, isSnoozed = true, snoozeUntilEpochMs = snoozeUntilMs))
        }
    }

    suspend fun getAllPendingRemindersOnce(): List<ReminderItem> {
        return reminderDao.getAllPendingRemindersOnce()
    }

    // --- Alarms ---
    val allAlarms: Flow<List<AlarmItem>> = alarmDao.getAllAlarms()

    suspend fun insertAlarm(alarm: AlarmItem): Long {
        val id = alarmDao.insert(alarm)
        val saved = alarm.copy(id = id)
        scheduler.scheduleAlarm(saved)
        return id
    }

    suspend fun updateAlarm(alarm: AlarmItem) {
        alarmDao.update(alarm)
        scheduler.scheduleAlarm(alarm)
    }

    suspend fun deleteAlarm(id: Long) {
        scheduler.cancelAlarm(id)
        alarmDao.deleteById(id)
    }

    suspend fun setAlarmEnabled(id: Long, isEnabled: Boolean) {
        alarmDao.setAlarmEnabled(id, isEnabled)
        val alarm = alarmDao.getAlarmById(id)
        if (alarm != null) {
            if (isEnabled) scheduler.scheduleAlarm(alarm) else scheduler.cancelAlarm(id)
        }
    }

    suspend fun getActiveAlarmsOnce(): List<AlarmItem> {
        return alarmDao.getActiveAlarmsOnce()
    }

    // --- Statistics ---
    val totalLearnedCount: Flow<Int> = learningDao.getTotalCount()
    val rememberedCount: Flow<Int> = learningDao.getRememberedCount()

    fun getDueCount(): Flow<Int> {
        return learningDao.getDueCount(System.currentTimeMillis())
    }

    fun getReviewedTodayCount(): Flow<Int> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return reviewLogDao.getCountReviewedToday(calendar.timeInMillis)
    }

    // --- Lossless Data Export & Import (JSON) ---
    suspend fun exportDataToJson(): String {
        val root = JSONObject()

        // 1. All Learning Items
        val items = learningDao.getAllItemsOnce()
        val learningArray = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("meaning", item.meaning)
                put("example", item.example)
                put("notes", item.notes)
                put("language", item.language)
                put("category", item.category)
                put("tags", item.tags)
                put("createdAtEpochMs", item.createdAtEpochMs)
                put("nextReviewEpochMs", item.nextReviewEpochMs)
                put("lastReviewedEpochMs", item.lastReviewedEpochMs)
                put("intervalDays", item.intervalDays)
                put("repetitionStage", item.repetitionStage)
                put("recallCount", item.recallCount)
                put("easeFactor", item.easeFactor.toDouble())
                put("isSpacedRepetitionEnabled", item.isSpacedRepetitionEnabled)
                put("status", item.status.name)
            }
            learningArray.put(obj)
        }
        root.put("learning_items", learningArray)

        // 2. All Reminders (pending and completed)
        val reminders = reminderDao.getAllRemindersOnce()
        val reminderArray = JSONArray()
        for (r in reminders) {
            val obj = JSONObject().apply {
                put("id", r.id)
                put("title", r.title)
                put("description", r.description)
                put("dueTimeEpochMs", r.dueTimeEpochMs)
                put("category", r.category)
                put("priority", r.priority.name)
                put("repeatSchedule", r.repeatSchedule.name)
                put("isCompleted", r.isCompleted)
                put("completedAtEpochMs", r.completedAtEpochMs)
                put("isSnoozed", r.isSnoozed)
                put("snoozeUntilEpochMs", r.snoozeUntilEpochMs)
                put("soundEnabled", r.soundEnabled)
                put("vibrateEnabled", r.vibrateEnabled)
                put("createdAtEpochMs", r.createdAtEpochMs)
            }
            reminderArray.put(obj)
        }
        root.put("reminders", reminderArray)

        // 3. All Alarms
        val alarms = alarmDao.getAllAlarmsOnce()
        val alarmArray = JSONArray()
        for (a in alarms) {
            val obj = JSONObject().apply {
                put("id", a.id)
                put("hour", a.hour)
                put("minute", a.minute)
                put("label", a.label)
                put("isEnabled", a.isEnabled)
                put("isRepeating", a.isRepeating)
                put("daysOfWeek", a.daysOfWeek)
                put("vibrateEnabled", a.vibrateEnabled)
                put("snoozeMinutes", a.snoozeMinutes)
                put("createdAtEpochMs", a.createdAtEpochMs)
            }
            alarmArray.put(obj)
        }
        root.put("alarms", alarmArray)

        // 4. Review Logs
        val logs = reviewLogDao.getAllLogsOnce()
        val logsArray = JSONArray()
        for (log in logs) {
            val obj = JSONObject().apply {
                put("id", log.id)
                put("learningItemId", log.learningItemId)
                put("learningItemTitle", log.learningItemTitle)
                put("rating", log.rating.name)
                put("intervalDaysAssigned", log.intervalDaysAssigned)
                put("reviewedAtEpochMs", log.reviewedAtEpochMs)
            }
            logsArray.put(obj)
        }
        root.put("review_logs", logsArray)
        root.put("exportedAtEpochMs", System.currentTimeMillis())

        return root.toString(2)
    }

    suspend fun importDataFromJson(jsonStr: String): Boolean {
        return try {
            // Payload safety check: maximum 25 MB string
            if (jsonStr.length > 25 * 1024 * 1024) return false

            val root = JSONObject(jsonStr)

            // Import learning items
            if (root.has("learning_items")) {
                val array = root.getJSONArray("learning_items")
                val count = kotlin.math.min(array.length(), 10_000)
                for (i in 0 until count) {
                    val obj = array.getJSONObject(i)
                    val item = LearningItem(
                        title = obj.getString("title"),
                        meaning = obj.optString("meaning", ""),
                        example = obj.optString("example", ""),
                        notes = obj.optString("notes", ""),
                        language = obj.optString("language", "English"),
                        category = obj.optString("category", "General"),
                        tags = obj.optString("tags", ""),
                        createdAtEpochMs = obj.optLong("createdAtEpochMs", System.currentTimeMillis()),
                        nextReviewEpochMs = obj.optLong("nextReviewEpochMs", System.currentTimeMillis() + 86400000L),
                        lastReviewedEpochMs = obj.optLong("lastReviewedEpochMs", 0L),
                        intervalDays = obj.optInt("intervalDays", 1),
                        repetitionStage = obj.optInt("repetitionStage", 0),
                        recallCount = obj.optInt("recallCount", 0),
                        easeFactor = obj.optDouble("easeFactor", 2.5).toFloat(),
                        isSpacedRepetitionEnabled = obj.optBoolean("isSpacedRepetitionEnabled", true),
                        status = try {
                            LearningStatus.valueOf(obj.optString("status", "NEW"))
                        } catch (e: Exception) {
                            LearningStatus.NEW
                        }
                    )
                    insertLearningItem(item)
                }
            }

            // Import reminders
            if (root.has("reminders")) {
                val array = root.getJSONArray("reminders")
                val count = kotlin.math.min(array.length(), 10_000)
                for (i in 0 until count) {
                    val obj = array.getJSONObject(i)
                    val reminder = ReminderItem(
                        title = obj.getString("title"),
                        description = obj.optString("description", ""),
                        dueTimeEpochMs = obj.optLong("dueTimeEpochMs", System.currentTimeMillis() + 3600000L),
                        category = obj.optString("category", "General"),
                        priority = try {
                            ReminderPriority.valueOf(obj.optString("priority", "MEDIUM"))
                        } catch (e: Exception) {
                            ReminderPriority.MEDIUM
                        },
                        repeatSchedule = try {
                            RepeatSchedule.valueOf(obj.optString("repeatSchedule", "NONE"))
                        } catch (e: Exception) {
                            RepeatSchedule.NONE
                        },
                        isCompleted = obj.optBoolean("isCompleted", false),
                        completedAtEpochMs = obj.optLong("completedAtEpochMs", 0L),
                        isSnoozed = obj.optBoolean("isSnoozed", false),
                        snoozeUntilEpochMs = obj.optLong("snoozeUntilEpochMs", 0L),
                        soundEnabled = obj.optBoolean("soundEnabled", true),
                        vibrateEnabled = obj.optBoolean("vibrateEnabled", true),
                        createdAtEpochMs = obj.optLong("createdAtEpochMs", System.currentTimeMillis())
                    )
                    insertReminder(reminder)
                }
            }

            // Import alarms
            if (root.has("alarms")) {
                val array = root.getJSONArray("alarms")
                val count = kotlin.math.min(array.length(), 1_000)
                for (i in 0 until count) {
                    val obj = array.getJSONObject(i)
                    val alarm = AlarmItem(
                        hour = obj.getInt("hour"),
                        minute = obj.getInt("minute"),
                        label = obj.optString("label", "Alarm"),
                        isEnabled = obj.optBoolean("isEnabled", true),
                        isRepeating = obj.optBoolean("isRepeating", false),
                        daysOfWeek = obj.optString("daysOfWeek", ""),
                        vibrateEnabled = obj.optBoolean("vibrateEnabled", true),
                        snoozeMinutes = obj.optInt("snoozeMinutes", 10),
                        createdAtEpochMs = obj.optLong("createdAtEpochMs", System.currentTimeMillis())
                    )
                    insertAlarm(alarm)
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun clearAllData(context: Context? = null) {
        val reminders = reminderDao.getAllRemindersOnce()
        val alarms = alarmDao.getAllAlarmsOnce()
        val reviews = learningDao.getAllItemsOnce()

        // Cancel all pending AlarmManager intents
        scheduler.cancelAllScheduledAlarms(reminders, alarms, reviews)

        // Cancel all active system notifications
        context?.let {
            val nm = it.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.cancelAll()
        }

        learningDao.clearAll()
        reminderDao.clearAll()
        alarmDao.clearAll()
        reviewLogDao.clearAll()
        scheduler.updateHomeWidget()
    }
}
