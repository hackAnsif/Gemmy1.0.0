package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.VoiceAssistantDialog
import com.example.ui.screens.AddLearningScreen
import com.example.ui.screens.AlarmClockScreen
import com.example.ui.screens.CreateReminderScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LearningReviewScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.GemmyIndigoPrimary
import com.example.ui.theme.GemmyTheme
import com.example.ui.viewmodel.GemmyViewModel

@Composable
fun GemmyApp(
    viewModel: GemmyViewModel,
    initialDestination: String = "home",
    initialStartVoice: Boolean = false,
    initialTargetId: Long? = null
) {
    val context = LocalContext.current
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val hasSeenOnboarding by viewModel.hasSeenOnboarding.collectAsStateWithLifecycle()
    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    val autoCheckUpdates by viewModel.autoCheckUpdates.collectAsStateWithLifecycle()
    val appUpdateInfo by viewModel.appUpdateInfo.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.updateDownloadProgress.collectAsStateWithLifecycle()

    val darkTheme = when (themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }

    var currentScreen by remember { mutableStateOf(initialDestination) }
    var reviewTargetId by remember { mutableStateOf<Long?>(initialTargetId) }
    var showVoiceDialog by remember { mutableStateOf(initialStartVoice) }
    var updateDialogDismissedByUser by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    LaunchedEffect(initialDestination) {
        if (initialDestination != "home" && hasSeenOnboarding) {
            currentScreen = initialDestination
        }
    }

    LaunchedEffect(initialStartVoice) {
        if (initialStartVoice) {
            showVoiceDialog = true
        }
    }

    // Auto-check for GitHub release updates on startup if enabled
    LaunchedEffect(autoCheckUpdates) {
        if (autoCheckUpdates) {
            viewModel.checkForAppUpdates(context, manual = false)
        }
    }

    GemmyTheme(darkTheme = darkTheme) {
        // Back handler for sub-screens
        if (currentScreen in listOf("add_learning", "create_reminder", "review")) {
            BackHandler {
                currentScreen = "home"
            }
        }

        // Only show onboarding for true first-time use
        if (!hasSeenOnboarding) {
            OnboardingScreen(
                onFinish = {
                    viewModel.completeOnboarding()
                    currentScreen = "home"
                }
            )
        } else {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (currentScreen in listOf("home", "library", "reminders", "alarms", "settings")) {
                        NavigationBar(modifier = Modifier.testTag("bottom_nav_bar")) {
                            NavigationBarItem(
                                selected = currentScreen == "home",
                                onClick = { currentScreen = "home" },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                label = { Text("Home") },
                                modifier = Modifier.testTag("nav_home")
                            )
                            NavigationBarItem(
                                selected = currentScreen == "library",
                                onClick = { currentScreen = "library" },
                                icon = { Icon(Icons.Default.AutoStories, contentDescription = "Library") },
                                label = { Text("Library") },
                                modifier = Modifier.testTag("nav_library")
                            )
                            NavigationBarItem(
                                selected = currentScreen == "reminders",
                                onClick = { currentScreen = "reminders" },
                                icon = { Icon(Icons.Default.Notifications, contentDescription = "Reminders") },
                                label = { Text("Reminders") },
                                modifier = Modifier.testTag("nav_reminders")
                            )
                            NavigationBarItem(
                                selected = currentScreen == "alarms",
                                onClick = { currentScreen = "alarms" },
                                icon = { Icon(Icons.Default.Alarm, contentDescription = "Alarms") },
                                label = { Text("Alarms") },
                                modifier = Modifier.testTag("nav_alarms")
                            )
                            NavigationBarItem(
                                selected = currentScreen == "settings",
                                onClick = { currentScreen = "settings" },
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                label = { Text("Settings") },
                                modifier = Modifier.testTag("nav_settings")
                            )
                        }
                    }
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    when (currentScreen) {
                        "home" -> HomeScreen(
                            viewModel = viewModel,
                            onNavigateToAddLearning = { currentScreen = "add_learning" },
                            onNavigateToCreateReminder = { currentScreen = "create_reminder" },
                            onNavigateToAlarms = { currentScreen = "alarms" },
                            onNavigateToLibrary = { currentScreen = "library" },
                            onNavigateToReminders = { currentScreen = "reminders" },
                            onNavigateToReview = { id ->
                                reviewTargetId = id
                                currentScreen = "review"
                            },
                            onStartVoice = { showVoiceDialog = true }
                        )
                        "library" -> LibraryScreen(
                            viewModel = viewModel,
                            onNavigateToAdd = { currentScreen = "add_learning" },
                            onNavigateToReview = { id ->
                                reviewTargetId = id
                                currentScreen = "review"
                            }
                        )
                        "reminders" -> RemindersScreen(
                            viewModel = viewModel,
                            onNavigateToCreate = { currentScreen = "create_reminder" }
                        )
                        "alarms" -> AlarmClockScreen(
                            viewModel = viewModel
                        )
                        "settings" -> SettingsScreen(
                            viewModel = viewModel
                        )
                        "add_learning" -> AddLearningScreen(
                            viewModel = viewModel,
                            onNavigateBack = { currentScreen = "home" },
                            onSaveItem = { item -> viewModel.saveLearningItem(item) }
                        )
                        "create_reminder" -> CreateReminderScreen(
                            viewModel = viewModel,
                            onNavigateBack = { currentScreen = "home" },
                            onSaveReminder = { reminder -> viewModel.saveReminder(reminder) }
                        )
                        "review" -> LearningReviewScreen(
                            viewModel = viewModel,
                            targetItemId = reviewTargetId,
                            onNavigateBack = { currentScreen = "home" }
                        )
                    }
                }
            }

            // Global Voice Assistant Dialog
            if (showVoiceDialog || voiceState.showConfirmationDialog || voiceState.followUpQuestion != null) {
                VoiceAssistantDialog(
                    viewModel = viewModel,
                    voiceState = voiceState,
                    onDismiss = {
                        showVoiceDialog = false
                        viewModel.dismissVoiceDialog()
                    }
                )
            }

            // Remote In-App Update Prompt Dialog
            if (!updateDialogDismissedByUser && appUpdateInfo != null && appUpdateInfo!!.isUpdateAvailable) {
                val update = appUpdateInfo!!
                AlertDialog(
                    onDismissRequest = {
                        updateDialogDismissedByUser = true
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = GemmyIndigoPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    },
                    title = {
                        Text("New Version Available", fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Column {
                            Text(
                                text = "A new version of Gemmy (${update.versionTag}) is available on GitHub!",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = update.releaseNotes.ifBlank { "Performance improvements and new features." },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (downloadProgress != null) {
                                Spacer(modifier = Modifier.height(14.dp))
                                LinearProgressIndicator(
                                    progress = { downloadProgress!! },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Downloading APK: ${(downloadProgress!! * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GemmyIndigoPrimary
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.downloadAndInstallUpdate(context)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GemmyIndigoPrimary)
                        ) {
                            Text("Download & Install")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                updateDialogDismissedByUser = true
                            }
                        ) {
                            Text("Later")
                        }
                    }
                )
            }
        }
    }
}
