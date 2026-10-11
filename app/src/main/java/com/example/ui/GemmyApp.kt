package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
import com.example.ui.theme.GemmyTheme
import com.example.ui.viewmodel.GemmyViewModel

@Composable
fun GemmyApp(
    viewModel: GemmyViewModel,
    initialDestination: String = "home",
    initialStartVoice: Boolean = false,
    initialTargetId: Long? = null
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val hasSeenOnboarding by viewModel.hasSeenOnboarding.collectAsStateWithLifecycle()
    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    val darkTheme = when (themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }

    var currentScreen by remember {
        mutableStateOf(if (hasSeenOnboarding) initialDestination else "onboarding")
    }
    var reviewTargetId by remember { mutableStateOf<Long?>(initialTargetId) }
    var showVoiceDialog by remember { mutableStateOf(initialStartVoice) }

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

    GemmyTheme(darkTheme = darkTheme) {
        // Back handler for sub-screens
        if (currentScreen in listOf("add_learning", "create_reminder", "review")) {
            BackHandler {
                currentScreen = "home"
            }
        }

        if (currentScreen == "onboarding") {
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
                            onNavigateBack = { currentScreen = "home" },
                            onSaveItem = { item -> viewModel.saveLearningItem(item) }
                        )
                        "create_reminder" -> CreateReminderScreen(
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
        }
    }
}
