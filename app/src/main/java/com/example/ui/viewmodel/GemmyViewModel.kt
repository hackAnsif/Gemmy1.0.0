package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.ReminderItem
import com.example.data.repository.GemmyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class ReminderFilter {
    ALL,
    TODAY,
    UPCOMING,
    COMPLETED
}

data class ReminderBuckets(
    val overdue: List<ReminderItem> = emptyList(),
    val today: List<ReminderItem> = emptyList(),
    val upcoming: List<ReminderItem> = emptyList(),
    val completed: List<ReminderItem> = emptyList(),
    val totalActiveCount: Int = 0
)

class GemmyViewModel(
    private val repository: GemmyRepository
) : ViewModel() {

    val themeMode: StateFlow<String> = repository.preferencesRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

    val defaultSnoozeMinutes: StateFlow<Int> = repository.preferencesRepository.defaultSnoozeMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10)

    val allReminders: StateFlow<List<ReminderItem>> = repository.allReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(ReminderFilter.ALL)
    val selectedFilter: StateFlow<ReminderFilter> = _selectedFilter.asStateFlow()

    private val _dayTick = MutableStateFlow(System.currentTimeMillis())

    val reminderBuckets: StateFlow<ReminderBuckets> = combine(
        allReminders,
        searchQuery,
        _dayTick
    ) { reminders, query, now ->
        val filtered = if (query.isBlank()) {
            reminders
        } else {
            val q = query.trim().lowercase()
            reminders.filter {
                it.title.lowercase().contains(q) || it.description.lowercase().contains(q)
            }
        }

        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfToday = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val endOfToday = cal.timeInMillis - 1

        val active = filtered.filter { !it.isCompleted }
        val overdue = active.filter { it.dueTimeEpochMs < startOfToday }
        val today = active.filter { it.dueTimeEpochMs in startOfToday..endOfToday }
        val upcoming = active.filter { it.dueTimeEpochMs > endOfToday }
        val completed = filtered.filter { it.isCompleted }
            .sortedByDescending { it.completedAtEpochMs }

        ReminderBuckets(
            overdue = overdue,
            today = today,
            upcoming = upcoming,
            completed = completed,
            totalActiveCount = active.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReminderBuckets())

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun refreshDayBounds() {
        _dayTick.value = System.currentTimeMillis()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: ReminderFilter) {
        _selectedFilter.value = filter
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun showUserMessage(message: String) {
        _userMessage.value = message
    }

    suspend fun getReminderById(id: Long): ReminderItem? {
        return repository.getReminderById(id)
    }

    fun saveOrUpdateReminder(
        existingReminder: ReminderItem?,
        descriptionText: String,
        dueTimeEpochMs: Long,
        alarmEnabled: Boolean
    ) {
        val cleanText = descriptionText.trim()
        if (cleanText.isBlank()) return

        viewModelScope.launch {
            refreshDayBounds()
            if (existingReminder != null) {
                val updated = existingReminder.copy(
                    title = cleanText,
                    description = "",
                    dueTimeEpochMs = dueTimeEpochMs,
                    alarmEnabled = alarmEnabled,
                    isSnoozed = false,
                    snoozeUntilEpochMs = 0L
                )
                repository.updateReminder(updated)
                showUserMessage("Reminder updated")
            } else {
                val created = ReminderItem(
                    title = cleanText,
                    description = "",
                    dueTimeEpochMs = dueTimeEpochMs,
                    alarmEnabled = alarmEnabled
                )
                repository.insertReminder(created)
                showUserMessage(if (alarmEnabled) "Reminder saved & scheduled" else "Reminder saved")
            }
        }
    }

    fun toggleReminderCompleted(reminder: ReminderItem) {
        viewModelScope.launch {
            val markDone = !reminder.isCompleted
            repository.markReminderCompleted(reminder.id, completed = markDone)
            showUserMessage(if (markDone) "Marked as completed" else "Moved back to active reminders")
        }
    }

    fun deleteReminder(id: Long) {
        viewModelScope.launch {
            repository.deleteReminder(id)
            showUserMessage("Reminder deleted")
        }
    }

    fun clearCompletedReminders() {
        viewModelScope.launch {
            repository.deleteCompletedReminders()
            showUserMessage("Cleared completed reminders")
        }
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch { repository.preferencesRepository.setThemeMode(mode) }
    }

    fun updateDefaultSnooze(minutes: Int) {
        viewModelScope.launch { repository.preferencesRepository.setDefaultSnoozeMinutes(minutes) }
    }

    fun clearAllReminders(context: Context? = null) {
        viewModelScope.launch {
            repository.clearAllReminders(context)
            showUserMessage("All reminders cleared")
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
