package com.example.ui.screens

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.ReminderItem
import com.example.ui.viewmodel.GemmyViewModel
import com.example.ui.viewmodel.ReminderFilter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: GemmyViewModel,
    onAddReminder: (prefillText: String) -> Unit,
    onEditReminder: (Long) -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val buckets by viewModel.reminderBuckets.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()

    var hasNotificationPermission by remember {
        mutableStateOf(checkNotificationPermission(context))
    }
    var canScheduleExactAlarms by remember {
        mutableStateOf(checkExactAlarmPermission(context))
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted || checkNotificationPermission(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationPermission = checkNotificationPermission(context)
                canScheduleExactAlarms = checkExactAlarmPermission(context)
                viewModel.refreshDayBounds()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val formattedCurrentDate = remember {
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
    }

    val showOverdue = selectedFilter == ReminderFilter.ALL || selectedFilter == ReminderFilter.TODAY
    val showToday = selectedFilter == ReminderFilter.ALL || selectedFilter == ReminderFilter.TODAY
    val showUpcoming = selectedFilter == ReminderFilter.ALL || selectedFilter == ReminderFilter.UPCOMING
    val showCompleted = selectedFilter == ReminderFilter.ALL || selectedFilter == ReminderFilter.COMPLETED

    val visibleEmpty = when (selectedFilter) {
        ReminderFilter.ALL -> buckets.overdue.isEmpty() && buckets.today.isEmpty() && buckets.upcoming.isEmpty() && buckets.completed.isEmpty()
        ReminderFilter.TODAY -> buckets.overdue.isEmpty() && buckets.today.isEmpty()
        ReminderFilter.UPCOMING -> buckets.upcoming.isEmpty()
        ReminderFilter.COMPLETED -> buckets.completed.isEmpty()
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAddReminder("") },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.cd_add_reminder)
                    )
                },
                text = { Text(text = stringResource(R.string.action_add_reminder), fontWeight = FontWeight.SemiBold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_reminder_fab")
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header with Gemmy branding, icon, current date & settings button
                item(key = "header") {
                    GemmyHeader(
                        currentDateText = formattedCurrentDate,
                        activeCount = buckets.totalActiveCount,
                        onOpenSettings = onOpenSettings
                    )
                }

                // Permission reminder banner (shown only when missing)
                if (!hasNotificationPermission || !canScheduleExactAlarms) {
                    item(key = "permission_banner") {
                        PermissionBanner(
                            missingNotifications = !hasNotificationPermission,
                            missingExactAlarm = !canScheduleExactAlarms,
                            onGrantNotifications = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            onOpenExactAlarmSettings = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                }
                            }
                        )
                    }
                }

                // Search field & filter chips
                item(key = "search_and_filters") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text(stringResource(R.string.hint_search_reminders)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = stringResource(R.string.cd_search_reminders)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.setSearchQuery("") },
                                        modifier = Modifier.testTag("clear_search_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = stringResource(R.string.cd_clear_search)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_input")
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedFilter == ReminderFilter.ALL,
                                onClick = { viewModel.setFilter(ReminderFilter.ALL) },
                                label = { Text("All (${buckets.totalActiveCount + buckets.completed.size})") },
                                modifier = Modifier.testTag("filter_all")
                            )
                            FilterChip(
                                selected = selectedFilter == ReminderFilter.TODAY,
                                onClick = { viewModel.setFilter(ReminderFilter.TODAY) },
                                label = { Text("Today (${buckets.today.size + buckets.overdue.size})") },
                                modifier = Modifier.testTag("filter_today")
                            )
                            FilterChip(
                                selected = selectedFilter == ReminderFilter.UPCOMING,
                                onClick = { viewModel.setFilter(ReminderFilter.UPCOMING) },
                                label = { Text("Upcoming (${buckets.upcoming.size})") },
                                modifier = Modifier.testTag("filter_upcoming")
                            )
                            FilterChip(
                                selected = selectedFilter == ReminderFilter.COMPLETED,
                                onClick = { viewModel.setFilter(ReminderFilter.COMPLETED) },
                                label = { Text("Completed (${buckets.completed.size})") },
                                modifier = Modifier.testTag("filter_completed")
                            )
                        }
                    }
                }

                // Overdue Section
                if (showOverdue && buckets.overdue.isNotEmpty()) {
                    item(key = "section_overdue") {
                        SectionHeader(
                            title = stringResource(R.string.section_overdue),
                            count = buckets.overdue.size,
                            isUrgent = true
                        )
                    }
                    items(buckets.overdue, key = { "overdue_${it.id}" }) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            isOverdue = true,
                            onToggleComplete = { viewModel.toggleReminderCompleted(reminder) },
                            onEdit = { onEditReminder(reminder.id) },
                            onDelete = { viewModel.deleteReminder(reminder.id) }
                        )
                    }
                }

                // Today's Reminders Section
                if (showToday && buckets.today.isNotEmpty()) {
                    item(key = "section_today") {
                        SectionHeader(
                            title = stringResource(R.string.section_today),
                            count = buckets.today.size
                        )
                    }
                    items(buckets.today, key = { "today_${it.id}" }) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            isOverdue = false,
                            onToggleComplete = { viewModel.toggleReminderCompleted(reminder) },
                            onEdit = { onEditReminder(reminder.id) },
                            onDelete = { viewModel.deleteReminder(reminder.id) }
                        )
                    }
                }

                // Upcoming Reminders Section
                if (showUpcoming && buckets.upcoming.isNotEmpty()) {
                    item(key = "section_upcoming") {
                        SectionHeader(
                            title = stringResource(R.string.section_upcoming),
                            count = buckets.upcoming.size
                        )
                    }
                    items(buckets.upcoming, key = { "upcoming_${it.id}" }) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            isOverdue = false,
                            onToggleComplete = { viewModel.toggleReminderCompleted(reminder) },
                            onEdit = { onEditReminder(reminder.id) },
                            onDelete = { viewModel.deleteReminder(reminder.id) }
                        )
                    }
                }

                // Completed Reminders Section
                if (showCompleted && buckets.completed.isNotEmpty()) {
                    item(key = "section_completed") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SectionHeader(
                                title = stringResource(R.string.section_completed),
                                count = buckets.completed.size
                            )
                            TextButton(
                                onClick = { viewModel.clearCompletedReminders() },
                                modifier = Modifier.testTag("clear_completed_button")
                            ) {
                                Text(stringResource(R.string.action_clear_completed))
                            }
                        }
                    }
                    items(buckets.completed, key = { "completed_${it.id}" }) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            isOverdue = false,
                            onToggleComplete = { viewModel.toggleReminderCompleted(reminder) },
                            onEdit = { onEditReminder(reminder.id) },
                            onDelete = { viewModel.deleteReminder(reminder.id) }
                        )
                    }
                }

                // Empty State
                if (visibleEmpty) {
                    item(key = "empty_state") {
                        EmptyRemindersCard(
                            isFilteredOrSearching = searchQuery.isNotBlank() || selectedFilter != ReminderFilter.ALL,
                            onTryExample = { exampleText -> onAddReminder(exampleText) },
                            onCreateNew = { onAddReminder("") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GemmyHeader(
    currentDateText: String,
    activeCount: Int,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_gemmy_brand_icon),
                contentDescription = stringResource(R.string.cd_gemmy_logo),
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = if (activeCount == 1) "1 active" else "$activeCount active",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Text(
                    text = currentDateText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .size(48.dp)
                .testTag("settings_button")
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = stringResource(R.string.cd_open_settings),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PermissionBanner(
    missingNotifications: Boolean,
    missingExactAlarm: Boolean,
    onGrantNotifications: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("permission_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (missingNotifications) {
                        "Enable notifications"
                    } else {
                        "Allow exact alarm timing"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (missingNotifications) {
                        "Required so scheduled reminders can alert you on time."
                    } else {
                        "Grant exact alarm permission for minute-accurate alerts."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            FilledTonalButton(
                onClick = if (missingNotifications) onGrantNotifications else onOpenExactAlarmSettings,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.testTag("grant_permission_button")
            ) {
                Text(if (missingNotifications) "Allow" else "Settings")
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    isUrgent: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
        )
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isUrgent) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = if (isUrgent) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun ReminderCard(
    reminder: ReminderItem,
    isOverdue: Boolean,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val formattedSchedule = remember(reminder.dueTimeEpochMs, reminder.isSnoozed, reminder.snoozeUntilEpochMs) {
        val targetMs = if (reminder.isSnoozed && reminder.snoozeUntilEpochMs > 0L) {
            reminder.snoozeUntilEpochMs
        } else {
            reminder.dueTimeEpochMs
        }
        formatReminderDateTime(targetMs)
    }

    val containerColor = if (reminder.isCompleted) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    ElevatedCard(
        onClick = onEdit,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (reminder.isCompleted) 0.dp else 2.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reminder_card_${reminder.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleComplete,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("toggle_complete_${reminder.id}")
            ) {
                Icon(
                    imageVector = if (reminder.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = if (reminder.isCompleted) {
                        stringResource(R.string.cd_mark_incomplete)
                    } else {
                        stringResource(R.string.cd_mark_complete)
                    },
                    tint = if (reminder.isCompleted) {
                        MaterialTheme.colorScheme.secondary
                    } else if (isOverdue) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(26.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (reminder.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (reminder.isCompleted) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                if (reminder.description.isNotBlank()) {
                    Text(
                        text = reminder.description,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = if (isOverdue && !reminder.isCompleted) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formattedSchedule,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isOverdue && !reminder.isCompleted) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    if (!reminder.isCompleted) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (reminder.alarmEnabled) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (reminder.alarmEnabled) {
                                        Icons.Filled.NotificationsActive
                                    } else {
                                        Icons.Outlined.NotificationsOff
                                    },
                                    contentDescription = null,
                                    tint = if (reminder.alarmEnabled) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (reminder.alarmEnabled) "Alarm" else "Silent",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (reminder.alarmEnabled) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("delete_reminder_${reminder.id}")
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = stringResource(R.string.cd_delete_reminder),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyRemindersCard(
    isFilteredOrSearching: Boolean,
    onTryExample: (String) -> Unit,
    onCreateNew: () -> Unit
) {
    val examples = listOf(
        "Tomorrow 12 PM submit assignment",
        "Today 6 PM complete project",
        "November 25 at 8 AM study physics"
    )

    OutlinedCard(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .testTag("empty_reminders_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.EventNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Text(
                text = if (isFilteredOrSearching) {
                    stringResource(R.string.empty_filtered_title)
                } else {
                    stringResource(R.string.empty_home_title)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = if (isFilteredOrSearching) {
                    stringResource(R.string.empty_filtered_subtitle)
                } else {
                    stringResource(R.string.empty_home_subtitle)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!isFilteredOrSearching) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Quick try with smart date detection:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                examples.forEach { example ->
                    FilledTonalButton(
                        onClick = { onTryExample(example) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = example, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            } else {
                TextButton(onClick = onCreateNew) {
                    Text(stringResource(R.string.action_add_reminder))
                }
            }
        }
    }
}

private fun checkNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

private fun checkExactAlarmPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        am?.canScheduleExactAlarms() ?: true
    } else {
        true
    }
}

private fun formatReminderDateTime(epochMs: Long): String {
    val targetCal = Calendar.getInstance().apply { timeInMillis = epochMs }
    val todayCal = Calendar.getInstance()
    val tomorrowCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }

    val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(epochMs))
    val dateLabel = when {
        isSameDay(targetCal, todayCal) -> "Today"
        isSameDay(targetCal, tomorrowCal) -> "Tomorrow"
        else -> SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(epochMs))
    }
    return "$dateLabel • $timeStr"
}

private fun isSameDay(a: Calendar, b: Calendar): Boolean {
    return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
        a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
}
