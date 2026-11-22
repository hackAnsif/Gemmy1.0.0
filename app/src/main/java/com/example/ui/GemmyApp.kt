package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.AddEditReminderScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.GemmyTheme
import com.example.ui.viewmodel.GemmyViewModel

private sealed interface GemmyRoute {
    data object Home : GemmyRoute
    data class AddEditReminder(
        val reminderId: Long? = null,
        val prefillText: String = ""
    ) : GemmyRoute
    data object Settings : GemmyRoute
}

@Composable
fun GemmyApp(
    viewModel: GemmyViewModel,
    initialDestination: String = "home",
    initialTargetId: Long? = null,
    launchTrigger: Int = 0
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    val darkTheme = when (themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }

    var currentRoute by remember {
        mutableStateOf<GemmyRoute>(
            if (initialDestination == "add_reminder") GemmyRoute.AddEditReminder() else GemmyRoute.Home
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    LaunchedEffect(launchTrigger, initialDestination, initialTargetId) {
        if (launchTrigger > 0) {
            currentRoute = when (initialDestination) {
                "add_reminder" -> GemmyRoute.AddEditReminder()
                else -> GemmyRoute.Home
            }
        }
    }

    GemmyTheme(darkTheme = darkTheme) {
        if (currentRoute !is GemmyRoute.Home) {
            BackHandler {
                currentRoute = GemmyRoute.Home
            }
        }

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (val route = currentRoute) {
                    is GemmyRoute.Home -> HomeScreen(
                        viewModel = viewModel,
                        onAddReminder = { prefill ->
                            currentRoute = GemmyRoute.AddEditReminder(
                                reminderId = null,
                                prefillText = prefill
                            )
                        },
                        onEditReminder = { reminderId ->
                            currentRoute = GemmyRoute.AddEditReminder(reminderId = reminderId)
                        },
                        onOpenSettings = {
                            currentRoute = GemmyRoute.Settings
                        }
                    )

                    is GemmyRoute.AddEditReminder -> AddEditReminderScreen(
                        viewModel = viewModel,
                        existingReminderId = route.reminderId,
                        initialPrefillText = route.prefillText,
                        onNavigateBack = {
                            currentRoute = GemmyRoute.Home
                        }
                    )

                    is GemmyRoute.Settings -> SettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = {
                            currentRoute = GemmyRoute.Home
                        }
                    )
                }
            }
        }
    }
}
