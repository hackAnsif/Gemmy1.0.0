package com.example.ui.screens

import android.app.TimePickerDialog
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AlarmItem
import com.example.ui.theme.GemmyIndigoPrimary
import com.example.ui.viewmodel.GemmyViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmClockScreen(
    viewModel: GemmyViewModel
) {
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }

    var selectedHour by remember { mutableIntStateOf(6) }
    var selectedMinute by remember { mutableIntStateOf(30) }
    var alarmLabel by remember { mutableStateOf("Morning Alarm") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alarm Clock", fontWeight = FontWeight.Bold) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val cal = Calendar.getInstance()
                    TimePickerDialog(
                        context,
                        { _, h, m ->
                            selectedHour = h
                            selectedMinute = m
                            showAddDialog = true
                        },
                        cal.get(Calendar.HOUR_OF_DAY),
                        cal.get(Calendar.MINUTE),
                        false
                    ).show()
                },
                containerColor = GemmyIndigoPrimary,
                modifier = Modifier.testTag("fab_add_alarm")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Alarm", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { padding ->
        if (alarms.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Alarms Set",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap + to set a real native clock alarm with sound and snooze.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(alarms, key = { it.id }) { alarm ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("alarm_item_${alarm.id}"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val timeFormatStr = "%02d:%02d".format(alarm.hour, alarm.minute)
                                val amPm = if (alarm.hour < 12) "AM" else "PM"
                                val display12h = when {
                                    alarm.hour == 0 -> 12
                                    alarm.hour > 12 -> alarm.hour - 12
                                    else -> alarm.hour
                                }
                                val formattedTime = "%d:%02d %s".format(display12h, alarm.minute, amPm)

                                Text(
                                    text = formattedTime,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (alarm.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = alarm.label.ifBlank { "Alarm" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = alarm.isEnabled,
                                    onCheckedChange = { isEnabled ->
                                        viewModel.toggleAlarm(alarm.id, isEnabled)
                                    },
                                    modifier = Modifier.testTag("alarm_switch_${alarm.id}")
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                IconButton(onClick = { viewModel.deleteAlarm(alarm.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Alarm Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Configure Alarm", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Time: %02d:%02d".format(selectedHour, selectedMinute),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = GemmyIndigoPrimary
                    )

                    OutlinedTextField(
                        value = alarmLabel,
                        onValueChange = { alarmLabel = it },
                        label = { Text("Alarm Label") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val alarm = AlarmItem(
                            hour = selectedHour,
                            minute = selectedMinute,
                            label = alarmLabel.trim().ifBlank { "Alarm" },
                            isEnabled = true
                        )
                        viewModel.saveAlarm(alarm)
                        showAddDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GemmyIndigoPrimary)
                ) {
                    Text("Save Alarm")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
