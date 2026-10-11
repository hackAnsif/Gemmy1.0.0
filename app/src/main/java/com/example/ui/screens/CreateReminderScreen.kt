package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ReminderItem
import com.example.data.model.ReminderPriority
import com.example.data.model.RepeatSchedule
import com.example.ui.theme.GemmyDarkSecondary
import com.example.ui.theme.GemmyIndigoPrimary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReminderScreen(
    onNavigateBack: () -> Unit,
    onSaveReminder: (ReminderItem) -> Unit
) {
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("General") }
    var priority by remember { mutableStateOf(ReminderPriority.MEDIUM) }
    var repeatSchedule by remember { mutableStateOf(RepeatSchedule.NONE) }
    var soundEnabled by remember { mutableStateOf(true) }
    var vibrateEnabled by remember { mutableStateOf(true) }

    val calendar = remember {
        Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 2)
            set(Calendar.MINUTE, 0)
        }
    }
    var scheduledDateMs by remember { mutableStateOf(calendar.timeInMillis) }
    var displayDateStr by remember {
        mutableStateOf(SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(calendar.time))
    }
    var displayTimeStr by remember {
        mutableStateOf(SimpleDateFormat("h:mm a", Locale.getDefault()).format(calendar.time))
    }

    val categories = listOf("General", "Study", "Work", "Personal", "Health")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Reminder", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Reminder Title *") },
                placeholder = { Text("e.g. Submit engineering assignment") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reminder_title_input"),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description / Notes") },
                placeholder = { Text("e.g. Include problem 4 and 5 solutions") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reminder_desc_input"),
                minLines = 2,
                shape = RoundedCornerShape(14.dp)
            )

            // Date and Time Pickers
            Text(text = "Date & Time", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            val c = Calendar.getInstance().apply { timeInMillis = scheduledDateMs }
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    c.set(Calendar.YEAR, y)
                                    c.set(Calendar.MONTH, m)
                                    c.set(Calendar.DAY_OF_MONTH, d)
                                    scheduledDateMs = c.timeInMillis
                                    displayDateStr = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(c.time)
                                },
                                c.get(Calendar.YEAR),
                                c.get(Calendar.MONTH),
                                c.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = GemmyIndigoPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = displayDateStr, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            val c = Calendar.getInstance().apply { timeInMillis = scheduledDateMs }
                            TimePickerDialog(
                                context,
                                { _, h, min ->
                                    c.set(Calendar.HOUR_OF_DAY, h)
                                    c.set(Calendar.MINUTE, min)
                                    scheduledDateMs = c.timeInMillis
                                    displayTimeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(c.time)
                                },
                                c.get(Calendar.HOUR_OF_DAY),
                                c.get(Calendar.MINUTE),
                                false
                            ).show()
                        },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = GemmyDarkSecondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = displayTimeStr, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Category Selection
            Column {
                Text(text = "Category", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }

            // Repeat Schedule
            Column {
                Text(text = "Repeat", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RepeatSchedule.values().forEach { rep ->
                        FilterChip(
                            selected = repeatSchedule == rep,
                            onClick = { repeatSchedule = rep },
                            label = {
                                Text(
                                    when (rep) {
                                        RepeatSchedule.NONE -> "One-time"
                                        RepeatSchedule.DAILY -> "Daily"
                                        RepeatSchedule.WEEKDAYS -> "Weekdays"
                                        RepeatSchedule.WEEKLY -> "Weekly"
                                        RepeatSchedule.MONTHLY -> "Monthly"
                                    }
                                )
                            }
                        )
                    }
                }
            }

            // Sound and Vibration Preferences
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Notification Sound", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Switch(checked = soundEnabled, onCheckedChange = { soundEnabled = it })
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Vibration", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Switch(checked = vibrateEnabled, onCheckedChange = { vibrateEnabled = it })
                    }
                }
            }

            // Submit Button
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val reminder = ReminderItem(
                            title = title.trim(),
                            description = description.trim(),
                            dueTimeEpochMs = scheduledDateMs,
                            category = category,
                            priority = priority,
                            repeatSchedule = repeatSchedule,
                            soundEnabled = soundEnabled,
                            vibrateEnabled = vibrateEnabled
                        )
                        onSaveReminder(reminder)
                        onNavigateBack()
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_reminder_button"),
                colors = ButtonDefaults.buttonColors(containerColor = GemmyIndigoPrimary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Schedule Reminder", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
