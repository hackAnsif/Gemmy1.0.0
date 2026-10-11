package com.example.data.model

enum class ActionType {
    CREATE_LEARNING_ITEM,
    CREATE_REMINDER,
    CREATE_ALARM,
    QUERY_SCHEDULE,
    UNKNOWN
}

data class GemmyParsedAction(
    val actionType: ActionType,
    val title: String = "",
    val meaning: String = "",
    val example: String = "",
    val description: String = "",
    val category: String = "General",
    val targetDateEpochMs: Long? = null,
    val timeHour: Int? = null,
    val timeMinute: Int? = null,
    val repeatSchedule: RepeatSchedule = RepeatSchedule.NONE,
    val isSpacedRepetition: Boolean = false,
    val needsClarification: Boolean = false,
    val clarificationPrompt: String = "",
    val originalText: String = "",
    val confidenceSource: String = "Offline Parser" // "Gemini AI" or "Offline Parser"
)
