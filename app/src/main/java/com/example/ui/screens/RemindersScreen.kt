package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ReminderItem
import com.example.ui.theme.GemmyDarkSecondary
import com.example.ui.theme.GemmyIndigoPrimary
import com.example.ui.viewmodel.GemmyViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    viewModel: GemmyViewModel,
    onNavigateToCreate: () -> Unit
) {
    val upcoming by viewModel.upcomingReminders.collectAsStateWithLifecycle()
    val today by viewModel.todayReminders.collectAsStateWithLifecycle()
    val completed by viewModel.completedReminders.collectAsStateWithLifecycle()
    val overdue by viewModel.overdueReminders.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Upcoming (${upcoming.size})", "Today (${today.size})", "Overdue (${overdue.size})", "Done (${completed.size})")

    val currentList = when (selectedTabIndex) {
        0 -> upcoming
        1 -> today
        2 -> overdue
        else -> completed
    }

    var selectedItemDetail by remember { mutableStateOf<ReminderItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reminders", fontWeight = FontWeight.Bold) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreate,
                containerColor = GemmyIndigoPrimary,
                modifier = Modifier.testTag("fab_create_reminder")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Create Reminder", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            if (currentList.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No reminders in this tab",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(currentList, key = { it.id }) { reminder ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedItemDetail = reminder }
                                .testTag("reminder_item_${reminder.id}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { viewModel.markReminderCompleted(reminder.id) },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = if (reminder.isCompleted) Icons.Default.CheckCircle else Icons.Default.Notifications,
                                        contentDescription = "Complete",
                                        tint = if (reminder.isCompleted) GemmyDarkSecondary else MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = reminder.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val sdf = SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault())
                                    Text(
                                        text = "${sdf.format(Date(reminder.dueTimeEpochMs))} • ${reminder.category}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (!reminder.isCompleted) {
                                    IconButton(
                                        onClick = { viewModel.snoozeReminder(reminder.id) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Snooze, contentDescription = "Snooze", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.deleteReminder(reminder.id) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Reminder details dialog
    selectedItemDetail?.let { reminder ->
        val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy • h:mm a", Locale.getDefault())
        AlertDialog(
            onDismissRequest = { selectedItemDetail = null },
            title = { Text(reminder.title, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (reminder.description.isNotBlank()) {
                        Text(text = "Notes: ${reminder.description}", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(text = "Scheduled: ${sdf.format(Date(reminder.dueTimeEpochMs))}", style = MaterialTheme.typography.bodySmall)
                    Text(text = "Category: ${reminder.category}", style = MaterialTheme.typography.bodySmall)
                    Text(text = "Status: ${if (reminder.isCompleted) "Completed" else "Pending"}", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.markReminderCompleted(reminder.id)
                    selectedItemDetail = null
                }) {
                    Text(if (reminder.isCompleted) "Done" else "Mark Complete")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedItemDetail = null }) {
                    Text("Close")
                }
            }
        )
    }
}
