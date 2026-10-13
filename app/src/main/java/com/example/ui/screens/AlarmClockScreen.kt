package com.example.ui.screens

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.alarm.AlarmToneInfo
import com.example.alarm.AlarmToneManager
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

    // Expanded card tracking
    var expandedAlarmId by remember { mutableStateOf<Long?>(null) }

    // Dialog state for tone picker
    var tonePickerTargetAlarm by remember { mutableStateOf<AlarmItem?>(null) }

    // Stop any tone preview when exiting screen
    DisposableEffect(Unit) {
        onDispose {
            AlarmToneManager.stopPreview()
        }
    }

    // Mobile Ringtone Picker Launcher
    val mobileRingtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && tonePickerTargetAlarm != null) {
            val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }

            val target = tonePickerTargetAlarm!!
            if (uri != null) {
                val title = AlarmToneManager.getToneTitle(context, uri.toString())
                viewModel.updateAlarm(
                    target.copy(
                        toneTitle = title,
                        toneUri = uri.toString()
                    )
                )
            } else {
                // User chose default or silent
                viewModel.updateAlarm(
                    target.copy(
                        toneTitle = "System Default Alarm",
                        toneUri = ""
                    )
                )
            }
            tonePickerTargetAlarm = null
            AlarmToneManager.stopPreview()
        }
    }

    // Next alarm time preview calculation
    val nextAlarmRemainingText = remember(alarms) {
        calculateNextAlarmText(alarms)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Alarm Clock", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        if (nextAlarmRemainingText.isNotBlank()) {
                            Text(
                                text = nextAlarmRemainingText,
                                style = MaterialTheme.typography.bodySmall,
                                color = GemmyIndigoPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val cal = Calendar.getInstance()
                    TimePickerDialog(
                        context,
                        { _, h, m ->
                            val newAlarm = AlarmItem(
                                hour = h,
                                minute = m,
                                label = "Alarm",
                                isEnabled = true,
                                toneTitle = "System Alarm",
                                toneUri = ""
                            )
                            viewModel.saveAlarm(newAlarm)
                        },
                        cal.get(Calendar.HOUR_OF_DAY),
                        cal.get(Calendar.MINUTE),
                        false
                    ).show()
                },
                containerColor = GemmyIndigoPrimary,
                modifier = Modifier.testTag("fab_add_alarm")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Alarm",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
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
                Surface(
                    shape = CircleShape,
                    color = GemmyIndigoPrimary.copy(alpha = 0.12f),
                    modifier = Modifier.size(96.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = GemmyIndigoPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "No Alarms Set",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap + to set a friendly clock alarm with customizable ringtones, days, and snooze.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        val cal = Calendar.getInstance()
                        TimePickerDialog(
                            context,
                            { _, h, m ->
                                val newAlarm = AlarmItem(
                                    hour = h,
                                    minute = m,
                                    label = "Morning Alarm",
                                    isEnabled = true
                                )
                                viewModel.saveAlarm(newAlarm)
                            },
                            7, 0, false
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GemmyIndigoPrimary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Set Alarm")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(alarms, key = { it.id }) { alarm ->
                    val isExpanded = expandedAlarmId == alarm.id

                    AlarmCard(
                        alarm = alarm,
                        isExpanded = isExpanded,
                        onToggleExpand = {
                            expandedAlarmId = if (isExpanded) null else alarm.id
                        },
                        onTimeClick = {
                            TimePickerDialog(
                                context,
                                { _, h, m ->
                                    viewModel.updateAlarm(alarm.copy(hour = h, minute = m, isEnabled = true))
                                },
                                alarm.hour,
                                alarm.minute,
                                false
                            ).show()
                        },
                        onToggleEnabled = { enabled ->
                            viewModel.toggleAlarm(alarm.id, enabled)
                        },
                        onUpdate = { updated ->
                            viewModel.updateAlarm(updated)
                        },
                        onDelete = {
                            viewModel.deleteAlarm(alarm.id)
                        },
                        onOpenTonePicker = {
                            tonePickerTargetAlarm = alarm
                        }
                    )
                }
            }
        }
    }

    // Ringtone & Inbuilt Tone Selection Dialog
    if (tonePickerTargetAlarm != null) {
        val target = tonePickerTargetAlarm!!
        ToneSelectionDialog(
            currentToneTitle = target.toneTitle,
            currentToneUri = target.toneUri,
            onToneSelected = { title, uri ->
                viewModel.updateAlarm(
                    target.copy(
                        toneTitle = title,
                        toneUri = uri
                    )
                )
                AlarmToneManager.stopPreview()
                tonePickerTargetAlarm = null
            },
            onPickFromMobile = {
                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                    putExtra(
                        RingtoneManager.EXTRA_RINGTONE_TYPE,
                        RingtoneManager.TYPE_ALARM or RingtoneManager.TYPE_NOTIFICATION or RingtoneManager.TYPE_RINGTONE
                    )
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Choose Mobile Alarm Ringtone")
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                    if (target.toneUri.isNotBlank()) {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(target.toneUri))
                    }
                }
                mobileRingtonePickerLauncher.launch(intent)
            },
            onDismiss = {
                AlarmToneManager.stopPreview()
                tonePickerTargetAlarm = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlarmCard(
    alarm: AlarmItem,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onTimeClick: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onUpdate: (AlarmItem) -> Unit,
    onDelete: () -> Unit,
    onOpenTonePicker: () -> Unit
) {
    val context = LocalContext.current
    val timeRemaining = remember(alarm.hour, alarm.minute, alarm.isEnabled, alarm.daysOfWeek) {
        if (!alarm.isEnabled) "Alarm is off"
        else formatTimeRemaining(alarm.hour, alarm.minute, alarm.daysOfWeek)
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("alarm_item_${alarm.id}")
            .animateContentSize(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (alarm.isEnabled)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Main Row: Large Time, Info, Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time click triggers time change
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTimeClick() }
                ) {
                    val amPm = if (alarm.hour < 12) "AM" else "PM"
                    val display12h = when {
                        alarm.hour == 0 -> 12
                        alarm.hour > 12 -> alarm.hour - 12
                        else -> alarm.hour
                    }
                    val formattedTime = "%d:%02d".format(display12h, alarm.minute)

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = formattedTime,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (alarm.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = amPm,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GemmyIndigoPrimary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Text(
                        text = alarm.label.ifBlank { "Alarm" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = timeRemaining,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (alarm.isEnabled) GemmyIndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Normal
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = alarm.isEnabled,
                        onCheckedChange = { onToggleEnabled(it) },
                        modifier = Modifier.testTag("alarm_switch_${alarm.id}"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = GemmyIndigoPrimary
                        )
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(onClick = onToggleExpand) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand Details",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Expanded Details Section
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Day Picker (M, T, W, T, F, S, S)
                    Column {
                        Text(
                            text = "Repeat Days",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val days = listOf("1" to "M", "2" to "T", "3" to "W", "4" to "T", "5" to "F", "6" to "S", "7" to "S")
                        val activeDays = alarm.daysOfWeek.split(",").filter { it.isNotBlank() }.toSet()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            days.forEach { (dayIndex, dayLabel) ->
                                val isSelected = activeDays.contains(dayIndex)
                                Surface(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            val newDays = if (isSelected) {
                                                activeDays - dayIndex
                                            } else {
                                                activeDays + dayIndex
                                            }
                                            val sorted = newDays.sorted().joinToString(",")
                                            onUpdate(
                                                alarm.copy(
                                                    daysOfWeek = sorted,
                                                    isRepeating = sorted.isNotBlank()
                                                )
                                            )
                                        },
                                    color = if (isSelected) GemmyIndigoPrimary else MaterialTheme.colorScheme.surface,
                                    shape = CircleShape
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = dayLabel,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Tone Selection Row
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenTonePicker() },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = GemmyIndigoPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Alarm Tone",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = alarm.toneTitle.ifBlank { "System Alarm (Default)" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Text(
                                text = "Change",
                                style = MaterialTheme.typography.labelMedium,
                                color = GemmyIndigoPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Vibration and Snooze
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Vibration,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Vibrate",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Switch(
                            checked = alarm.vibrateEnabled,
                            onCheckedChange = { onUpdate(alarm.copy(vibrateEnabled = it)) }
                        )
                    }

                    // Snooze Duration Selector
                    Column {
                        Text(
                            text = "Snooze Duration",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(5, 10, 15).forEach { mins ->
                                val selected = alarm.snoozeMinutes == mins
                                FilterChip(
                                    selected = selected,
                                    onClick = { onUpdate(alarm.copy(snoozeMinutes = mins)) },
                                    label = { Text("$mins min") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GemmyIndigoPrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    // Label editor and Delete action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete Alarm")
                        }

                        Button(
                            onClick = onTimeClick,
                            colors = ButtonDefaults.buttonColors(containerColor = GemmyIndigoPrimary)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Change Time")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ToneSelectionDialog(
    currentToneTitle: String,
    currentToneUri: String,
    onToneSelected: (title: String, uri: String) -> Unit,
    onPickFromMobile: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val inbuiltTones = remember { AlarmToneManager.getInbuiltTones(context) }
    var currentlyPlayingUri by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = {
            AlarmToneManager.stopPreview()
            onDismiss()
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = GemmyIndigoPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Alarm Tone", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Pick an inbuilt alarm sound or choose any ringtone installed on your phone:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Mobile Tone Picker Button
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPickFromMobile() },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = GemmyIndigoPrimary.copy(alpha = 0.12f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = GemmyIndigoPrimary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Choose from Mobile / Device",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = GemmyIndigoPrimary
                                )
                                Text(
                                    text = "Select any ringtone, music, or alarm sound",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Inbuilt Tones",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(inbuiltTones) { tone ->
                        val isSelected = (tone.uriString == currentToneUri) ||
                                (tone.isSystemDefault && currentToneUri.isBlank())
                        val isPlaying = currentlyPlayingUri == tone.uriString

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onToneSelected(tone.title, tone.uriString)
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    GemmyIndigoPrimary.copy(alpha = 0.15f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = GemmyIndigoPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text(
                                        text = tone.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) GemmyIndigoPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        if (isPlaying) {
                                            AlarmToneManager.stopPreview()
                                            currentlyPlayingUri = null
                                        } else {
                                            AlarmToneManager.previewTone(context, tone.uriString)
                                            currentlyPlayingUri = tone.uriString
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Stop Preview" else "Play Preview",
                                        tint = GemmyIndigoPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    AlarmToneManager.stopPreview()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = GemmyIndigoPrimary)
            ) {
                Text("Done")
            }
        }
    )
}

/**
 * Calculates humanized time remaining until alarm fires
 */
private fun formatTimeRemaining(hour: Int, minute: Int, daysOfWeek: String): String {
    val now = Calendar.getInstance()
    val alarmCal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    if (daysOfWeek.isBlank()) {
        if (alarmCal.timeInMillis <= now.timeInMillis) {
            alarmCal.add(Calendar.DAY_OF_YEAR, 1)
        }
    } else {
        val daysList = daysOfWeek.split(",").mapNotNull { it.toIntOrNull() }
        // 1=Monday (Calendar.MONDAY=2), etc.
        while (alarmCal.timeInMillis <= now.timeInMillis || !matchesDay(alarmCal, daysList)) {
            alarmCal.add(Calendar.DAY_OF_YEAR, 1)
        }
    }

    val diffMs = alarmCal.timeInMillis - now.timeInMillis
    val totalMinutes = (diffMs / (1000 * 60)).toInt()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    return when {
        hours == 0 && minutes <= 1 -> "Alarm in less than a minute"
        hours == 0 -> "Alarm in $minutes min"
        minutes == 0 -> "Alarm in $hours hours"
        else -> "Alarm in ${hours}h ${minutes}m"
    }
}

private fun matchesDay(cal: Calendar, targetDays: List<Int>): Boolean {
    // Android Calendar.SUNDAY = 1, MONDAY = 2, ... SATURDAY = 7
    // App convention: 1=Mon, 2=Tue, 3=Wed, 4=Thu, 5=Fri, 6=Sat, 7=Sun
    val calDay = cal.get(Calendar.DAY_OF_WEEK)
    val appDay = when (calDay) {
        Calendar.MONDAY -> 1
        Calendar.TUESDAY -> 2
        Calendar.WEDNESDAY -> 3
        Calendar.THURSDAY -> 4
        Calendar.FRIDAY -> 5
        Calendar.SATURDAY -> 6
        Calendar.SUNDAY -> 7
        else -> 1
    }
    return targetDays.contains(appDay)
}

private fun calculateNextAlarmText(alarms: List<AlarmItem>): String {
    val activeAlarms = alarms.filter { it.isEnabled }
    if (activeAlarms.isEmpty()) return ""

    var minDiffMs = Long.MAX_VALUE
    val now = System.currentTimeMillis()

    for (alarm in activeAlarms) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (alarm.daysOfWeek.isBlank()) {
            if (cal.timeInMillis <= now) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
        } else {
            val daysList = alarm.daysOfWeek.split(",").mapNotNull { it.toIntOrNull() }
            while (cal.timeInMillis <= now || !matchesDay(cal, daysList)) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        val diff = cal.timeInMillis - now
        if (diff in 1 until minDiffMs) {
            minDiffMs = diff
        }
    }

    if (minDiffMs == Long.MAX_VALUE) return ""
    val totalMins = (minDiffMs / (1000 * 60)).toInt()
    val hours = totalMins / 60
    val mins = totalMins % 60
    return if (hours > 0) "Next alarm in ${hours}h ${mins}m" else "Next alarm in ${mins}m"
}
