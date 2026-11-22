package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.receiver.ReminderBroadcastReceiver
import com.example.ui.GemmyApp
import com.example.ui.viewmodel.GemmyViewModel
import com.example.ui.viewmodel.GemmyViewModelFactory
import com.example.widget.GemmyWidgetProvider

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: GemmyViewModel

    private var initialDestination by mutableStateOf("home")
    private var initialTargetId by mutableStateOf<Long?>(null)
    private var launchTrigger by mutableIntStateOf(0)

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = applicationContext as GemmyApplication
        val factory = GemmyViewModelFactory(app.repository)
        viewModel = ViewModelProvider(this, factory)[GemmyViewModel::class.java]

        requestNotificationPermissionIfNeeded()
        applyIntent(intent)

        setContent {
            GemmyApp(
                viewModel = viewModel,
                initialDestination = initialDestination,
                initialTargetId = initialTargetId,
                launchTrigger = launchTrigger
            )
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshDayBounds()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyIntent(intent)
    }

    private fun applyIntent(intent: Intent?) {
        val (destination, targetId) = parseIntent(intent)
        initialDestination = destination
        initialTargetId = targetId
        launchTrigger += 1
    }

    private fun parseIntent(intent: Intent?): Pair<String, Long?> {
        if (intent == null) return Pair("home", null)

        val action = intent.action
        val destination = when {
            action == GemmyWidgetProvider.ACTION_NEW_REMINDER -> "add_reminder"
            intent.getStringExtra("destination") == "create_reminder" -> "add_reminder"
            intent.getStringExtra("destination") == "add_reminder" -> "add_reminder"
            else -> "home"
        }

        val targetId = if (intent.hasExtra(ReminderBroadcastReceiver.EXTRA_ID)) {
            val id = intent.getLongExtra(ReminderBroadcastReceiver.EXTRA_ID, -1L)
            if (id > 0L) id else null
        } else null

        return Pair(destination, targetId)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
