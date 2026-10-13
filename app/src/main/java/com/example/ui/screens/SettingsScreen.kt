package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.GemmyDarkSecondary
import com.example.ui.theme.GemmyIndigoPrimary
import com.example.ui.viewmodel.GemmyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: GemmyViewModel
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val userApiKey by viewModel.userApiKey.collectAsStateWithLifecycle()
    val useOnlineAi by viewModel.useOnlineAi.collectAsStateWithLifecycle()
    val defaultSnooze by viewModel.defaultSnoozeMinutes.collectAsStateWithLifecycle()
    val smartIntervals by viewModel.smartReviewIntervals.collectAsStateWithLifecycle()

    var apiKeyInput by remember(userApiKey) { mutableStateOf(userApiKey) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportedJsonText by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonInput by remember { mutableStateOf("") }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Privacy", fontWeight = FontWeight.Bold) }
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
            // Theme Mode
            SettingsGroupCard(title = "Appearance", icon = Icons.Default.Brightness4) {
                Text(text = "Theme", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("SYSTEM" to "System", "LIGHT" to "Light", "DARK" to "Dark").forEach { (mode, label) ->
                        FilterChip(
                            selected = themeMode == mode,
                            onClick = { viewModel.updateThemeMode(mode) },
                            label = { Text(label) }
                        )
                    }
                }
            }

            // Language Selection
            SettingsGroupCard(title = "Language / ഭാഷ", icon = Icons.Default.Language) {
                Text(text = "App Interface Language", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = appLanguage == "en",
                        onClick = { viewModel.updateLanguage("en") },
                        label = { Text("English") }
                    )
                    FilterChip(
                        selected = appLanguage == "ml",
                        onClick = { viewModel.updateLanguage("ml") },
                        label = { Text("മലയാളം (Malayalam)") }
                    )
                }
            }

            // Reminder & Review Defaults
            SettingsGroupCard(title = "Snooze & Review Defaults", icon = Icons.Default.Schedule) {
                Text(text = "Default Snooze Duration", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 15, 30).forEach { mins ->
                        FilterChip(
                            selected = defaultSnooze == mins,
                            onClick = { viewModel.updateDefaultSnooze(mins) },
                            label = { Text("$mins mins") }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Smart Review Intervals: $smartIntervals days (Day 1, 3, 7, 14, 30)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Gemini AI & Offline Mode
            SettingsGroupCard(title = "Gemini AI & Offline Intelligence", icon = Icons.Default.Psychology) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Use Online Gemini AI",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Improves multilingual & nuanced Malayalam voice commands. If off, Gemmy runs 100% offline rule-based parsing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = useOnlineAi,
                        onCheckedChange = { viewModel.updateOnlineAi(it) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text("Custom Gemini API Key (Optional)") },
                    placeholder = { Text("Auto-injected from AI Studio if empty") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gemini_api_key_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = {
                            viewModel.updateApiKey(apiKeyInput.trim())
                            Toast.makeText(context, "API Key updated", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GemmyIndigoPrimary)
                    ) {
                        Text("Save Key")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = GemmyDarkSecondary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Privacy first: Only voice command text is sent when AI is used. Your learning database is stored strictly on your device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // App Updates & GitHub Automation
            val githubRepo by viewModel.githubRepo.collectAsStateWithLifecycle()
            val autoCheckUpdates by viewModel.autoCheckUpdates.collectAsStateWithLifecycle()
            val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsStateWithLifecycle()
            val appUpdateInfo by viewModel.appUpdateInfo.collectAsStateWithLifecycle()
            val downloadProgress by viewModel.updateDownloadProgress.collectAsStateWithLifecycle()
            var repoInput by remember(githubRepo) { mutableStateOf(githubRepo) }

            SettingsGroupCard(title = "App Updates & Remote Updation", icon = Icons.Default.CloudDownload) {
                Text(
                    text = "Installed Version: v${com.example.BuildConfig.VERSION_NAME} (Build ${com.example.BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = GemmyIndigoPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "When you push code or releases to GitHub, Gemmy automatically detects the newest APK and offers a one-tap in-app update.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = repoInput,
                    onValueChange = { repoInput = it },
                    label = { Text("GitHub Repo (e.g. username/gemmy)") },
                    placeholder = { Text(com.example.update.AppUpdateManager.DEFAULT_REPO) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            viewModel.updateGithubRepo(repoInput.trim())
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GemmyIndigoPrimary)
                    ) {
                        Text("Save Repo")
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.checkForAppUpdates(context, manual = true)
                        },
                        enabled = !isCheckingUpdate
                    ) {
                        if (isCheckingUpdate) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = GemmyIndigoPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Checking...")
                        } else {
                            Text("Check Updates")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Auto-check on app launch",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = autoCheckUpdates,
                        onCheckedChange = { viewModel.updateAutoCheckUpdates(it) }
                    )
                }

                // If an update is ready
                if (appUpdateInfo != null && appUpdateInfo!!.isUpdateAvailable) {
                    val info = appUpdateInfo!!
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = GemmyIndigoPrimary.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "🎉 Update Ready: ${info.versionTag}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = GemmyIndigoPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = info.releaseNotes.ifBlank { "New features and performance improvements." },
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 4
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            if (downloadProgress != null) {
                                androidx.compose.material3.LinearProgressIndicator(
                                    progress = { downloadProgress!! },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Downloading APK: ${(downloadProgress!! * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GemmyIndigoPrimary
                                )
                            } else {
                                Button(
                                    onClick = { viewModel.downloadAndInstallUpdate(context) },
                                    colors = ButtonDefaults.buttonColors(containerColor = GemmyIndigoPrimary),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Download & Install Update")
                                }
                            }
                        }
                    }
                }
            }

            // Data Backup & Privacy
            SettingsGroupCard(title = "Data & Backup Management", icon = Icons.Default.Lock) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.exportBackup { json ->
                                exportedJsonText = json
                                showExportDialog = true
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export JSON")
                    }

                    OutlinedButton(
                        onClick = { showImportDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import JSON")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { showClearConfirmDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear All Local Data")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Export Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Backup Data (JSON)") },
            text = {
                Column {
                    Text("Your saved memory library and reminders in JSON format:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = exportedJsonText,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 8
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    clipboardManager.setText(AnnotatedString(exportedJsonText))
                    Toast.makeText(context, "Copied backup JSON to clipboard!", Toast.LENGTH_SHORT).show()
                    showExportDialog = false
                }) {
                    Text("Copy to Clipboard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Import Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Backup (JSON)") },
            text = {
                Column {
                    Text("Paste your backup JSON below to restore items:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importJsonInput,
                        onValueChange = { importJsonInput = it },
                        placeholder = { Text("Paste JSON here...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (importJsonInput.isNotBlank()) {
                        viewModel.importBackup(importJsonInput) { success ->
                            if (success) {
                                showImportDialog = false
                                importJsonInput = ""
                            }
                        }
                    }
                }) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear Confirmation Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear All Local Data?") },
            text = { Text("This will permanently remove all saved learning items, active reminders, alarms, and review history from this device.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData(context)
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingsGroupCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = GemmyIndigoPrimary, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}
