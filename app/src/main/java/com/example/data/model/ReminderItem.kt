package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ReminderPriority {
    LOW,
    MEDIUM,
    HIGH
}

enum class RepeatSchedule {
    NONE,
    DAILY,
    WEEKDAYS,
    WEEKLY,
    MONTHLY
}

@Entity(tableName = "reminder_items")
data class ReminderItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val dueTimeEpochMs: Long,
    val category: String = "General",
    val priority: ReminderPriority = ReminderPriority.MEDIUM,
    val repeatSchedule: RepeatSchedule = RepeatSchedule.NONE,
    val isCompleted: Boolean = false,
    val completedAtEpochMs: Long = 0L,
    val isSnoozed: Boolean = false,
    val snoozeUntilEpochMs: Long = 0L,
    val soundEnabled: Boolean = true,
    val vibrateEnabled: Boolean = true,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)
