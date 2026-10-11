package com.example.data.repository

import com.example.data.db.AlarmItemDao
import com.example.data.db.LearningItemDao
import com.example.data.db.ReminderItemDao
import com.example.data.db.ReviewLogDao
import com.example.data.model.AlarmItem
import com.example.data.model.LearningItem
import com.example.data.model.LearningStatus
import com.example.data.model.RecallRating
import com.example.data.model.ReminderItem
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
        scheduler.scheduleReview(item)
    }

    suspend fun deleteLearningItem(id: Long) {
        scheduler.cancelReview(id)
        learningDao.deleteById(id)
    }

    /**
     * Spaced-Repetition Review Algorithm (SuperMemo SM-2 inspired with user intervals)
     */
    suspend fun recordRecallReview(item: LearningItem, rating: RecallRating) {
        val currentInterval = if (item.intervalDays <= 0) 1 else item.intervalDays
        val nextIntervalDays: Int
        val nextStage: Int
        val nextStatus: LearningStatus

        when (rating) {
            RecallRating.AGAIN -> {
                // Forgot: Reset interval to 1 day
                nextIntervalDays = 1
                nextStage = 0
                nextStatus = LearningStatus.LEARNING
            }
            RecallRating.HARD -> {
                // Difficult recall: slight interval increase
                nextIntervalDays = kotlin.math.max(1, (currentInterval * 1.2).toInt())
                nextStage = item.repetitionStage
                nextStatus = LearningStatus.LEARNING
            }
            RecallRating.GOOD -> {
                // Standard recall: progressive intervals [1 -> 3 -> 7 -> 14 -> 30]
                nextIntervalDays = when (item.repetitionStage) {
                    0 -> 3
                    1 -> 7
                    2 -> 14
                    3 -> 30
                    else -> kotlin.math.max(currentInterval * 2, 45)
                }
                nextStage = item.repetitionStage + 1
                nextStatus = if (nextIntervalDays >= 30) LearningStatus.REMEMBERED else LearningStatus.REVIEWING
            }
            RecallRating.EASY -> {
                // Confident recall: accelerated interval
                nextIntervalDays = when (item.repetitionStage) {
                    0 -> 5
                    1 -> 12
                    2 -> 25
                    else -> kotlin.math.max(currentInterval * 2 + 10, 60)
                }
                nextStage = item.repetitionStage + 2
                nextStatus = LearningStatus.REMEMBERED
            }
        }

        val nextReviewEpochMs = System.currentTimeMillis() + (nextIntervalDays * 86400000L)
        val updatedItem = item.copy(
            nextReviewEpochMs = nextReviewEpochMs,
            lastReviewedEpochMs = System.currentTimeMillis(),
            intervalDays = nextIntervalDays,
            repetitionStage = nextStage,
            recallCount = item.recallCount + 1,
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

        scheduler.scheduleReview(updatedItem)
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

    // --- Data Export & Import (JSON) ---
    suspend fun exportDataToJson(): String {
        val root = JSONObject()

        // 1. Learning items
        val learningList = mutableListOf<LearningItem>()
        // Collect items from DAO
        val items = learningDao.getItemsDueForReviewOnce(Long.MAX_VALUE) // Gets all items
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
                put("intervalDays", item.intervalDays)
                put("repetitionStage", item.repetitionStage)
                put("status", item.status.name)
            }
            learningArray.put(obj)
        }
        root.put("learning_items", learningArray)

        // 2. Reminders
        val reminders = reminderDao.getAllPendingRemindersOnce()
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
            }
            reminderArray.put(obj)
        }
        root.put("reminders", reminderArray)

        // 3. Alarms
        val alarms = alarmDao.getActiveAlarmsOnce()
        val alarmArray = JSONArray()
        for (a in alarms) {
            val obj = JSONObject().apply {
                put("id", a.id)
                put("hour", a.hour)
                put("minute", a.minute)
                put("label", a.label)
                put("isEnabled", a.isEnabled)
            }
            alarmArray.put(obj)
        }
        root.put("alarms", alarmArray)
        root.put("exportedAtEpochMs", System.currentTimeMillis())

        return root.toString(2)
    }

    suspend fun importDataFromJson(jsonStr: String): Boolean {
        return try {
            val root = JSONObject(jsonStr)

            // Import learning items
            if (root.has("learning_items")) {
                val array = root.getJSONArray("learning_items")
                for (i in 0 until array.length()) {
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
                        intervalDays = obj.optInt("intervalDays", 1),
                        repetitionStage = obj.optInt("repetitionStage", 0),
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
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val reminder = ReminderItem(
                        title = obj.getString("title"),
                        description = obj.optString("description", ""),
                        dueTimeEpochMs = obj.optLong("dueTimeEpochMs", System.currentTimeMillis() + 3600000L),
                        category = obj.optString("category", "General")
                    )
                    insertReminder(reminder)
                }
            }

            // Import alarms
            if (root.has("alarms")) {
                val array = root.getJSONArray("alarms")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val alarm = AlarmItem(
                        hour = obj.getInt("hour"),
                        minute = obj.getInt("minute"),
                        label = obj.optString("label", "Alarm"),
                        isEnabled = obj.optBoolean("isEnabled", true)
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

    suspend fun clearAllData() {
        learningDao.clearAll()
        reminderDao.clearAll()
        alarmDao.clearAll()
        reviewLogDao.clearAll()
        scheduler.updateHomeWidget()
    }
}
