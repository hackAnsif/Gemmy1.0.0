package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.AlarmItem
import com.example.data.model.GemmyParsedAction
import com.example.data.model.LearningItem
import com.example.data.model.RecallRating
import com.example.data.model.ReminderItem
import com.example.data.repository.GemmyRepository
import com.example.voice.GemmyNluParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class VoiceAssistantUiState(
    val isListening: Boolean = false,
    val selectedLanguage: String = "en-IN", // "en-IN" or "ml-IN"
    val recognizedText: String = "",
    val isProcessing: Boolean = false,
    val parsedAction: GemmyParsedAction? = null,
    val showConfirmationDialog: Boolean = false,
    val followUpQuestion: String? = null,
    val errorMessage: String? = null
)

class GemmyViewModel(
    private val repository: GemmyRepository
) : ViewModel() {

    // Preferences & Settings State
    val themeMode: StateFlow<String> = repository.preferencesRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

    val appLanguage: StateFlow<String> = repository.preferencesRepository.appLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")

    val userApiKey: StateFlow<String> = repository.preferencesRepository.userGeminiApiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val useOnlineAi: StateFlow<Boolean> = repository.preferencesRepository.useOnlineAi
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val defaultSnoozeMinutes: StateFlow<Int> = repository.preferencesRepository.defaultSnoozeMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10)

    val smartReviewIntervals: StateFlow<String> = repository.preferencesRepository.smartReviewIntervals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "1,3,7,14,30")

    val hasSeenOnboarding: StateFlow<Boolean> = repository.preferencesRepository.hasSeenOnboarding
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Data lists
    val learningItems: StateFlow<List<LearningItem>> = repository.allLearningItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dueLearningItems: StateFlow<List<LearningItem>> = repository.getDueLearningItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upcomingReminders: StateFlow<List<ReminderItem>> = repository.getUpcomingReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayReminders: StateFlow<List<ReminderItem>> = repository.getTodayReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedReminders: StateFlow<List<ReminderItem>> = repository.getCompletedReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val overdueReminders: StateFlow<List<ReminderItem>> = repository.getOverdueReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val alarms: StateFlow<List<AlarmItem>> = repository.allAlarms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Statistics
    val totalLearnedCount: StateFlow<Int> = repository.totalLearnedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val rememberedCount: StateFlow<Int> = repository.rememberedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val dueCount: StateFlow<Int> = repository.getDueCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val reviewedTodayCount: StateFlow<Int> = repository.getReviewedTodayCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Voice Assistant UI State
    private val _voiceState = MutableStateFlow(VoiceAssistantUiState())
    val voiceState: StateFlow<VoiceAssistantUiState> = _voiceState.asStateFlow()

    // Library Search & Filter State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow("All")
    val selectedCategoryFilter = _selectedCategoryFilter.asStateFlow()

    private val _sortBy = MutableStateFlow("date") // "date", "alphabetical", "due"
    val sortBy = _sortBy.asStateFlow()

    val filteredLearningItems: StateFlow<List<LearningItem>> = combine(
        learningItems,
        searchQuery,
        selectedCategoryFilter,
        sortBy
    ) { items, query, category, sort ->
        var result = items
        if (query.isNotBlank()) {
            val q = query.lowercase()
            result = result.filter {
                it.title.lowercase().contains(q) ||
                        it.meaning.lowercase().contains(q) ||
                        it.example.lowercase().contains(q) ||
                        it.tags.lowercase().contains(q)
            }
        }
        if (category != "All") {
            result = result.filter { it.category == category }
        }
        when (sort) {
            "alphabetical" -> result.sortedBy { it.title.lowercase() }
            "due" -> result.sortedBy { it.nextReviewEpochMs }
            else -> result.sortedByDescending { it.createdAtEpochMs }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Feedback Banner/Snackbar
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage = _userMessage.asStateFlow()

    fun showUserMessage(message: String) {
        _userMessage.value = message
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    // Set Filters
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(category: String) {
        _selectedCategoryFilter.value = category
    }

    fun setSortBy(sort: String) {
        _sortBy.value = sort
    }

    // Onboarding
    fun completeOnboarding() {
        viewModelScope.launch {
            repository.preferencesRepository.setHasSeenOnboarding(true)
        }
    }

    // Learning Item Actions
    fun saveLearningItem(item: LearningItem) {
        viewModelScope.launch {
            repository.insertLearningItem(item)
            showUserMessage("Saved \"${item.title}\" to your memory library.")
        }
    }

    fun updateLearningItem(item: LearningItem) {
        viewModelScope.launch {
            repository.updateLearningItem(item)
            showUserMessage("Updated \"${item.title}\".")
        }
    }

    fun deleteLearningItem(id: Long) {
        viewModelScope.launch {
            repository.deleteLearningItem(id)
            showUserMessage("Deleted memory item.")
        }
    }

    fun recordReview(item: LearningItem, rating: RecallRating) {
        viewModelScope.launch {
            repository.recordRecallReview(item, rating)
            val msg = when (rating) {
                RecallRating.AGAIN -> "Reset interval to tomorrow. Keep practicing!"
                RecallRating.HARD -> "Reviewed! Rescheduled for next revision."
                RecallRating.GOOD -> "Great job! Interval increased."
                RecallRating.EASY -> "Awesome recall! Marked remembered."
            }
            showUserMessage(msg)
        }
    }

    // Reminder Actions
    fun saveReminder(reminder: ReminderItem) {
        viewModelScope.launch {
            repository.insertReminder(reminder)
            showUserMessage("Reminder scheduled: \"${reminder.title}\".")
        }
    }

    fun updateReminder(reminder: ReminderItem) {
        viewModelScope.launch {
            repository.updateReminder(reminder)
            showUserMessage("Reminder updated.")
        }
    }

    fun deleteReminder(id: Long) {
        viewModelScope.launch {
            repository.deleteReminder(id)
            showUserMessage("Reminder deleted.")
        }
    }

    fun markReminderCompleted(id: Long) {
        viewModelScope.launch {
            repository.markReminderCompleted(id)
            showUserMessage("Marked reminder as complete.")
        }
    }

    fun snoozeReminder(id: Long) {
        viewModelScope.launch {
            val snoozeMs = System.currentTimeMillis() + (defaultSnoozeMinutes.value * 60_000L)
            repository.snoozeReminder(id, snoozeMs)
            showUserMessage("Snoozed for ${defaultSnoozeMinutes.value} minutes.")
        }
    }

    // Alarm Actions
    fun saveAlarm(alarm: AlarmItem) {
        viewModelScope.launch {
            repository.insertAlarm(alarm)
            showUserMessage("Alarm set for %02d:%02d".format(alarm.hour, alarm.minute))
        }
    }

    fun toggleAlarm(id: Long, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.setAlarmEnabled(id, isEnabled)
        }
    }

    fun deleteAlarm(id: Long) {
        viewModelScope.launch {
            repository.deleteAlarm(id)
            showUserMessage("Alarm removed.")
        }
    }

    // Voice & NLU Assistant Handling
    fun setVoiceLanguage(lang: String) {
        _voiceState.value = _voiceState.value.copy(selectedLanguage = lang)
    }

    fun setVoiceListening(listening: Boolean) {
        _voiceState.value = _voiceState.value.copy(isListening = listening)
    }

    fun setRecognizedText(text: String) {
        _voiceState.value = _voiceState.value.copy(recognizedText = text)
    }

    fun processVoiceCommand(commandText: String) {
        val trimmed = commandText.trim()
        if (trimmed.isBlank()) return

        _voiceState.value = _voiceState.value.copy(
            recognizedText = trimmed,
            isProcessing = true,
            errorMessage = null
        )

        viewModelScope.launch {
            try {
                val action = GemmyNluParser.parseCommand(
                    input = trimmed,
                    useOnlineAi = useOnlineAi.value,
                    customApiKey = userApiKey.value
                )

                if (action.needsClarification && action.clarificationPrompt.isNotBlank()) {
                    _voiceState.value = _voiceState.value.copy(
                        isProcessing = false,
                        parsedAction = action,
                        followUpQuestion = action.clarificationPrompt,
                        showConfirmationDialog = false
                    )
                } else {
                    _voiceState.value = _voiceState.value.copy(
                        isProcessing = false,
                        parsedAction = action,
                        followUpQuestion = null,
                        showConfirmationDialog = true
                    )
                }
            } catch (e: Exception) {
                _voiceState.value = _voiceState.value.copy(
                    isProcessing = false,
                    errorMessage = "Error parsing: ${e.message}"
                )
            }
        }
    }

    fun answerClarification(clarificationAnswer: String) {
        val currentAction = _voiceState.value.parsedAction ?: return
        val combinedText = "${currentAction.originalText} $clarificationAnswer"
        processVoiceCommand(combinedText)
    }

    fun confirmVoiceAction(action: GemmyParsedAction) {
        viewModelScope.launch {
            when (action.actionType) {
                com.example.data.model.ActionType.CREATE_LEARNING_ITEM -> {
                    val dateMs = action.targetDateEpochMs ?: (System.currentTimeMillis() + 86400000L)
                    val item = LearningItem(
                        title = action.title,
                        meaning = action.meaning,
                        example = action.example,
                        category = action.category,
                        nextReviewEpochMs = dateMs,
                        isSpacedRepetitionEnabled = true
                    )
                    repository.insertLearningItem(item)
                    showUserMessage("Created learning review for \"${item.title}\"!")
                }
                com.example.data.model.ActionType.CREATE_REMINDER -> {
                    val cal = Calendar.getInstance()
                    if (action.targetDateEpochMs != null) {
                        cal.timeInMillis = action.targetDateEpochMs
                    }
                    cal.set(Calendar.HOUR_OF_DAY, action.timeHour ?: 9)
                    cal.set(Calendar.MINUTE, action.timeMinute ?: 0)
                    cal.set(Calendar.SECOND, 0)
                    val dueTimeMs = cal.timeInMillis

                    val reminder = ReminderItem(
                        title = action.title,
                        description = action.description,
                        dueTimeEpochMs = dueTimeMs,
                        category = action.category,
                        repeatSchedule = action.repeatSchedule
                    )
                    repository.insertReminder(reminder)
                    showUserMessage("Scheduled reminder for \"${reminder.title}\"!")
                }
                com.example.data.model.ActionType.CREATE_ALARM -> {
                    val alarm = AlarmItem(
                        hour = action.timeHour ?: 7,
                        minute = action.timeMinute ?: 0,
                        label = action.title
                    )
                    repository.insertAlarm(alarm)
                    showUserMessage("Alarm set for %02d:%02d".format(alarm.hour, alarm.minute))
                }
                else -> {
                    showUserMessage("Command processed.")
                }
            }

            _voiceState.value = VoiceAssistantUiState()
        }
    }

    fun dismissVoiceDialog() {
        _voiceState.value = VoiceAssistantUiState()
    }

    // Settings Updates
    fun updateThemeMode(mode: String) {
        viewModelScope.launch { repository.preferencesRepository.setThemeMode(mode) }
    }

    fun updateLanguage(lang: String) {
        viewModelScope.launch { repository.preferencesRepository.setAppLanguage(lang) }
    }

    fun updateApiKey(key: String) {
        viewModelScope.launch { repository.preferencesRepository.setGeminiApiKey(key) }
    }

    fun updateOnlineAi(enabled: Boolean) {
        viewModelScope.launch { repository.preferencesRepository.setUseOnlineAi(enabled) }
    }

    fun updateDefaultSnooze(minutes: Int) {
        viewModelScope.launch { repository.preferencesRepository.setDefaultSnoozeMinutes(minutes) }
    }

    // Backup & Restore
    fun exportBackup(onExportReady: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportDataToJson()
            onExportReady(json)
        }
    }

    fun importBackup(json: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.importDataFromJson(json)
            onComplete(success)
            if (success) showUserMessage("Data imported successfully!") else showUserMessage("Failed to import data. Please check JSON format.")
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            showUserMessage("All data cleared.")
        }
    }
}

class GemmyViewModelFactory(
    private val repository: GemmyRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GemmyViewModel::class.java)) {
            return GemmyViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
