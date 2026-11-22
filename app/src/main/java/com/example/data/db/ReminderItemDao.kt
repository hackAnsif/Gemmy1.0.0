package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ReminderItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderItemDao {

    @Query("SELECT * FROM reminder_items ORDER BY dueTimeEpochMs ASC")
    fun getAllReminders(): Flow<List<ReminderItem>>

    @Query("SELECT * FROM reminder_items WHERE id = :id")
    suspend fun getReminderById(id: Long): ReminderItem?

    @Query("SELECT * FROM reminder_items WHERE isCompleted = 0 AND dueTimeEpochMs >= :startEpochMs ORDER BY dueTimeEpochMs ASC LIMIT 1")
    suspend fun getNextUpcomingReminder(startEpochMs: Long): ReminderItem?

    @Query("SELECT * FROM reminder_items WHERE isCompleted = 0 AND alarmEnabled = 1")
    suspend fun getAllPendingRemindersOnce(): List<ReminderItem>

    @Query("SELECT * FROM reminder_items")
    suspend fun getAllRemindersOnce(): List<ReminderItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderItem): Long

    @Update
    suspend fun update(reminder: ReminderItem)

    @Delete
    suspend fun delete(reminder: ReminderItem)

    @Query("DELETE FROM reminder_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM reminder_items WHERE isCompleted = 1")
    suspend fun deleteCompletedReminders()

    @Query("UPDATE reminder_items SET isCompleted = 1, completedAtEpochMs = :completedAt WHERE id = :id")
    suspend fun markComplete(id: Long, completedAt: Long)

    @Query("UPDATE reminder_items SET isCompleted = 0, completedAtEpochMs = 0 WHERE id = :id")
    suspend fun markIncomplete(id: Long)

    @Query("UPDATE reminder_items SET isSnoozed = 1, snoozeUntilEpochMs = :snoozeUntil, dueTimeEpochMs = :snoozeUntil WHERE id = :id")
    suspend fun snoozeReminder(id: Long, snoozeUntil: Long)

    @Query("DELETE FROM reminder_items")
    suspend fun clearAll()
}
