package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class LearningStatus {
    NEW,
    LEARNING,
    REVIEWING,
    REMEMBERED
}

enum class RecallRating {
    AGAIN,
    HARD,
    GOOD,
    EASY
}

@Entity(tableName = "learning_items")
data class LearningItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val meaning: String = "",
    val example: String = "",
    val notes: String = "",
    val language: String = "English",
    val category: String = "English Vocabulary",
    val tags: String = "", // Comma-separated tags
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val nextReviewEpochMs: Long = System.currentTimeMillis() + 86400000L, // Default tomorrow
    val lastReviewedEpochMs: Long = 0L,
    val intervalDays: Int = 1,
    val repetitionStage: Int = 0,
    val recallCount: Int = 0,
    val easeFactor: Float = 2.5f,
    val isSpacedRepetitionEnabled: Boolean = true,
    val status: LearningStatus = LearningStatus.NEW
)
