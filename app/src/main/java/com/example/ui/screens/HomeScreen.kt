package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.LearningItem
import com.example.data.model.ReminderItem
import com.example.ui.theme.GemmyDarkSecondary
import com.example.ui.theme.GemmyIndigoPrimary
import com.example.ui.theme.GemmyVioletTertiary
import com.example.ui.viewmodel.GemmyViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: GemmyViewModel,
    onNavigateToAddLearning: () -> Unit,
    onNavigateToCreateReminder: () -> Unit,
    onNavigateToAlarms: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToReview: (Long?) -> Unit,
    onStartVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalLearned by viewModel.totalLearnedCount.collectAsStateWithLifecycle()
    val dueCount by viewModel.dueCount.collectAsStateWithLifecycle()
    val rememberedCount by viewModel.rememberedCount.collectAsStateWithLifecycle()
    val reviewedToday by viewModel.reviewedTodayCount.collectAsStateWithLifecycle()

    val upcomingReminders by viewModel.upcomingReminders.collectAsStateWithLifecycle()
    val todayReminders by viewModel.todayReminders.collectAsStateWithLifecycle()
    val dueLearningItems by viewModel.dueLearningItems.collectAsStateWithLifecycle()

    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (currentHour) {
        in 5..11 -> "Good morning!"
        in 12..16 -> "Good afternoon!"
        in 17..21 -> "Good evening!"
        else -> "Good night!"
    }
    val todayDateFormatted = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen_content"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // A. Header Greeting
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = todayDateFormatted.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // B. Gemmy Assistant Card
        item {
            AssistantHeroCard(
                onStartVoice = onStartVoice,
                onAddReminder = onNavigateToCreateReminder,
                onAddAlarm = onNavigateToAlarms,
                onAddLearning = onNavigateToAddLearning
            )
        }

        // Due for Review Banner (if any items are due)
        if (dueLearningItems.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToReview(dueLearningItems.firstOrNull()?.id) }
                        .testTag("due_reviews_banner"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = GemmyDarkSecondary.copy(alpha = 0.15f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(GemmyDarkSecondary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = Color.Black
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${dueLearningItems.size} memory items due for review!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Test your recall: ${dueLearningItems.first().title}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { onNavigateToReview(dueLearningItems.firstOrNull()?.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = GemmyDarkSecondary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Review", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // E. Learning Memory Progress Stats
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Learning Memory Progress",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onNavigateToLibrary) {
                        Text("View Library")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Saved",
                        value = totalLearned.toString(),
                        subtitle = "Total Concepts",
                        icon = Icons.Default.AutoStories,
                        accentColor = GemmyIndigoPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Due Today",
                        value = dueCount.toString(),
                        subtitle = "To Review",
                        icon = Icons.Default.AccessTime,
                        accentColor = if (dueCount > 0) GemmyDarkSecondary else Color(0xFF94A3B8),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Remembered",
                        value = rememberedCount.toString(),
                        subtitle = "Mastered",
                        icon = Icons.Default.CheckCircle,
                        accentColor = GemmyDarkSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // D. Today's Schedule
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's Schedule",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onNavigateToReminders) {
                        Text("All Reminders")
                    }
                }

                if (todayReminders.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No tasks or reminders scheduled for today.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onNavigateToCreateReminder,
                                colors = ButtonDefaults.buttonColors(containerColor = GemmyIndigoPrimary)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Reminder")
                            }
                        }
                    }
                } else {
                    todayReminders.forEach { reminder ->
                        ReminderRowItem(
                            reminder = reminder,
                            onComplete = { viewModel.markReminderCompleted(reminder.id) },
                            onSnooze = { viewModel.snoozeReminder(reminder.id) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // C. Upcoming Reminders
        if (upcomingReminders.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Upcoming Reminders",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(upcomingReminders.take(6)) { reminder ->
                            UpcomingReminderMiniCard(reminder = reminder)
                        }
                    }
                }
            }
        }

        // F. Quick Action Buttons
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionButton(
                        label = "Add Memory",
                        icon = Icons.Default.School,
                        color = GemmyIndigoPrimary,
                        onClick = onNavigateToAddLearning,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionButton(
                        label = "New Reminder",
                        icon = Icons.Default.Notifications,
                        color = GemmyDarkSecondary,
                        onClick = onNavigateToCreateReminder,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionButton(
                        label = "Set Alarm",
                        icon = Icons.Default.Alarm,
                        color = GemmyVioletTertiary,
                        onClick = onNavigateToAlarms,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun AssistantHeroCard(
    onStartVoice: () -> Unit,
    onAddReminder: () -> Unit,
    onAddAlarm: () -> Unit,
    onAddLearning: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("gemmy_assistant_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            Brush.linearGradient(listOf(GemmyVioletTertiary, GemmyDarkSecondary)),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Gemmy",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Gemmy Assistant",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "What should I help you remember?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Voice Mic Button
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            Brush.linearGradient(listOf(GemmyVioletTertiary, GemmyDarkSecondary)),
                            CircleShape
                        )
                        .clip(CircleShape)
                        .clickable { onStartVoice() }
                        .testTag("home_mic_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Speak to Gemmy",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sub actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onAddLearning() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp), tint = GemmyIndigoPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Word", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onAddReminder() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp), tint = GemmyDarkSecondary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reminder", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onAddAlarm() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(16.dp), tint = GemmyVioletTertiary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Alarm", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ReminderRowItem(
    reminder: ReminderItem,
    onComplete: () -> Unit,
    onSnooze: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                onClick = onComplete,
                modifier = Modifier.size(36.dp)
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
                val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
                Text(
                    text = "${sdf.format(Date(reminder.dueTimeEpochMs))} • ${reminder.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(onClick = onSnooze) {
                Text("Snooze")
            }
        }
    }
}

@Composable
fun UpcomingReminderMiniCard(reminder: ReminderItem) {
    Surface(
        modifier = Modifier
            .width(180.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            val sdfDate = SimpleDateFormat("MMM d • h:mm a", Locale.getDefault())
            Text(
                text = reminder.category.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = GemmyDarkSecondary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = reminder.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = sdfDate.format(Date(reminder.dueTimeEpochMs)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun QuickActionButton(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
