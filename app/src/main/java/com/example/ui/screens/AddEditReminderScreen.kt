package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ReminderItem
import com.example.ui.viewmodel.GemmyViewModel
import com.example.util.SmartDateTimeParser
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditReminderScreen(
    viewModel: GemmyViewModel,
    existingReminderId: Long? = null,
    initialPrefillText: String = "",
    onNavigateBack: () -> Unit
) {
    val isEditing = existingReminderId != null
    var existingReminder by remember { mutableStateOf<ReminderItem?>(null) }

    val defaultCal = remember {
        Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    var descriptionText by remember { mutableStateOf(initialPrefillText) }
    var selectedDateLocalMidnightMs by remember {
        mutableLongStateOf(toLocalMidnightMs(defaultCal.timeInMillis))
    }
    var selectedHour by remember { mutableIntStateOf(defaultCal.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableIntStateOf(defaultCal.get(Calendar.MINUTE)) }
    var alarmEnabled by remember { mutableStateOf(true) }
    var lastAppliedRawText by remember { mutableStateOf<String?>(null) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(existingReminderId) {
        if (existingReminderId != null) {
            val loaded = viewModel.getReminderById(existingReminderId)
            if (loaded != null) {
                existingReminder = loaded
                descriptionText = if (loaded.description.isNotBlank() && loaded.title != loaded.description) {
                    "${loaded.title} — ${loaded.description}"
                } else {
                    loaded.title
                }
                val cal = Calendar.getInstance().apply { timeInMillis = loaded.dueTimeEpochMs }
                selectedDateLocalMidnightMs = toLocalMidnightMs(loaded.dueTimeEpochMs)
                selectedHour = cal.get(Calendar.HOUR_OF_DAY)
                selectedMinute = cal.get(Calendar.MINUTE)
                alarmEnabled = loaded.alarmEnabled
                lastAppliedRawText = descriptionText
            }
        }
    }

    val parsedPreview = remember(descriptionText) {
        SmartDateTimeParser.parse(descriptionText)
    }
    val showSmartParseConfirmation = !isEditing &&
        parsedPreview.hasAnyMatch &&
        parsedPreview.originalText != lastAppliedRawText

    val formattedSelectedDate = remember(selectedDateLocalMidnightMs) {
        SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(Date(selectedDateLocalMidnightMs))
    }

    val formattedSelectedTime = remember(selectedHour, selectedMinute) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, selectedHour)
            set(Calendar.MINUTE, selectedMinute)
        }
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(cal.time)
    }

    val todayMidnightMs = remember { toLocalMidnightMs(System.currentTimeMillis()) }
    val tomorrowMidnightMs = remember(todayMidnightMs) {
        Calendar.getInstance().apply {
            timeInMillis = todayMidnightMs
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) {
                            stringResource(R.string.title_edit_reminder)
                        } else {
                            stringResource(R.string.title_add_reminder)
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back)
                        )
                    }
                },
                actions = {
                    if (isEditing && existingReminder != null) {
                        IconButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("edit_delete_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = stringResource(R.string.cd_delete_reminder),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Reminder Description
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.label_reminder_description),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        value = descriptionText,
                        onValueChange = { descriptionText = it },
                        placeholder = { Text(stringResource(R.string.hint_reminder_description)) },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reminder_description_input")
                    )
                    Text(
                        text = stringResource(R.string.helper_smart_parse),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Smart Date/Time Extraction Preview & Confirmation
                if (showSmartParseConfirmation) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("smart_parse_preview_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.EventAvailable,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = stringResource(R.string.title_detected_schedule),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            Text(
                                text = "Task: ${parsedPreview.description}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                parsedPreview.formattedDate()?.let { d ->
                                    Text(
                                        text = "Date: $d",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                parsedPreview.formattedTime()?.let { t ->
                                    Text(
                                        text = "Time: $t",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            FilledTonalButton(
                                onClick = {
                                    descriptionText = parsedPreview.description
                                    if (parsedPreview.hasDate && parsedPreview.dateEpochMs != null) {
                                        selectedDateLocalMidnightMs = toLocalMidnightMs(parsedPreview.dateEpochMs)
                                    }
                                    if (parsedPreview.hasTime && parsedPreview.hour != null) {
                                        selectedHour = parsedPreview.hour
                                        selectedMinute = parsedPreview.minute ?: 0
                                    }
                                    lastAppliedRawText = parsedPreview.description
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("apply_smart_parse_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.action_confirm_extracted))
                            }
                        }
                    }
                }

                // 2. Date Picker Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.label_date),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedDateLocalMidnightMs == todayMidnightMs,
                            onClick = { selectedDateLocalMidnightMs = todayMidnightMs },
                            label = { Text("Today") },
                            modifier = Modifier.testTag("quick_date_today")
                        )
                        FilterChip(
                            selected = selectedDateLocalMidnightMs == tomorrowMidnightMs,
                            onClick = { selectedDateLocalMidnightMs = tomorrowMidnightMs },
                            label = { Text("Tomorrow") },
                            modifier = Modifier.testTag("quick_date_tomorrow")
                        )
                        FilterChip(
                            selected = selectedDateLocalMidnightMs != todayMidnightMs &&
                                selectedDateLocalMidnightMs != tomorrowMidnightMs,
                            onClick = { showDatePicker = true },
                            label = { Text("Custom Date") },
                            modifier = Modifier.testTag("quick_date_custom")
                        )
                    }

                    OutlinedCard(
                        onClick = { showDatePicker = true },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .testTag("date_picker_button")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarToday,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = formattedSelectedDate,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = "Change",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // 3. Time Picker Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.label_time),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple(8, 0, "8:00 AM"),
                            Triple(12, 0, "12:00 PM"),
                            Triple(18, 0, "6:00 PM"),
                            Triple(20, 0, "8:00 PM")
                        ).forEach { (h, m, label) ->
                            FilterChip(
                                selected = selectedHour == h && selectedMinute == m,
                                onClick = {
                                    selectedHour = h
                                    selectedMinute = m
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("quick_time_${h}_$m")
                            )
                        }
                    }

                    OutlinedCard(
                        onClick = { showTimePicker = true },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .testTag("time_picker_button")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.AccessTime,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = formattedSelectedTime,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = "Change",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // 4. Alarm Notification Toggle
                OutlinedCard(
                    onClick = { alarmEnabled = !alarmEnabled },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("alarm_toggle_row")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (alarmEnabled) {
                                    Icons.Outlined.NotificationsActive
                                } else {
                                    Icons.Outlined.NotificationsOff
                                },
                                contentDescription = null,
                                tint = if (alarmEnabled) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.label_alarm_notification),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (alarmEnabled) {
                                        stringResource(R.string.desc_alarm_enabled)
                                    } else {
                                        stringResource(R.string.desc_alarm_disabled)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = alarmEnabled,
                            onCheckedChange = { alarmEnabled = it },
                            modifier = Modifier.testTag("alarm_notification_switch")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 5. Save Button
                Button(
                    onClick = {
                        val combinedEpochMs = combineDateAndTime(
                            localMidnightMs = selectedDateLocalMidnightMs,
                            hour = selectedHour,
                            minute = selectedMinute
                        )
                        viewModel.saveOrUpdateReminder(
                            existingReminder = existingReminder,
                            descriptionText = descriptionText,
                            dueTimeEpochMs = combinedEpochMs,
                            alarmEnabled = alarmEnabled
                        )
                        onNavigateBack()
                    },
                    enabled = descriptionText.isNotBlank(),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("save_reminder_button")
                ) {
                    Text(
                        text = if (isEditing) {
                            stringResource(R.string.action_update_reminder)
                        } else {
                            stringResource(R.string.action_save_reminder)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showDatePicker) {
        val initialUtcDateMillis = remember(selectedDateLocalMidnightMs) {
            localMidnightToUtcMidnight(selectedDateLocalMidnightMs)
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialUtcDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMs ->
                            selectedDateLocalMidnightMs = utcMidnightToLocalMidnight(utcMs)
                        }
                        showDatePicker = false
                    },
                    modifier = Modifier.testTag("confirm_date_picker")
                ) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.title_select_time), fontWeight = FontWeight.Bold) },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedHour = timePickerState.hour
                        selectedMinute = timePickerState.minute
                        showTimePicker = false
                    },
                    modifier = Modifier.testTag("confirm_time_picker")
                ) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showDeleteConfirmDialog && existingReminder != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(stringResource(R.string.title_delete_reminder), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.msg_delete_reminder_confirm)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteReminder(existingReminder!!.id)
                        showDeleteConfirmDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

private fun toLocalMidnightMs(epochMs: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = epochMs
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun localMidnightToUtcMidnight(localMidnightMs: Long): Long {
    val localCal = Calendar.getInstance().apply { timeInMillis = localMidnightMs }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(
            localCal.get(Calendar.YEAR),
            localCal.get(Calendar.MONTH),
            localCal.get(Calendar.DAY_OF_MONTH),
            0,
            0,
            0
        )
    }.timeInMillis
}

private fun utcMidnightToLocalMidnight(utcMidnightMs: Long): Long {
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMidnightMs }
    return Calendar.getInstance().apply {
        clear()
        set(
            utcCal.get(Calendar.YEAR),
            utcCal.get(Calendar.MONTH),
            utcCal.get(Calendar.DAY_OF_MONTH),
            0,
            0,
            0
        )
    }.timeInMillis
}

private fun combineDateAndTime(localMidnightMs: Long, hour: Int, minute: Int): Long {
    return Calendar.getInstance().apply {
        timeInMillis = localMidnightMs
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
