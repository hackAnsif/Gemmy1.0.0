package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "review_logs")
data class ReviewLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val learningItemId: Long,
    val learningItemTitle: String,
    val rating: RecallRating,
    val intervalDaysAssigned: Int,
    val reviewedAtEpochMs: Long = System.currentTimeMillis()
)
