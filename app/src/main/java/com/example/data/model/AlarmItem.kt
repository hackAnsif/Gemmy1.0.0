package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarm_items")
data class AlarmItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val hour: Int, // 0..23
    val minute: Int, // 0..59
    val label: String = "Alarm",
    val isEnabled: Boolean = true,
    val isRepeating: Boolean = false,
    val daysOfWeek: String = "", // e.g. "1,2,3,4,5" (1=Monday, 7=Sunday)
    val vibrateEnabled: Boolean = true,
    val snoozeMinutes: Int = 10,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)
