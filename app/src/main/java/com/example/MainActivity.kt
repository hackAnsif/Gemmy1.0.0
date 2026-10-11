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
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.receiver.ReminderBroadcastReceiver
import com.example.ui.GemmyApp
import com.example.ui.viewmodel.GemmyViewModel
import com.example.ui.viewmodel.GemmyViewModelFactory
import com.example.widget.GemmyWidgetProvider

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: GemmyViewModel

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission result handled gracefully
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = applicationContext as GemmyApplication
        val factory = GemmyViewModelFactory(app.repository)
        viewModel = ViewModelProvider(this, factory)[GemmyViewModel::class.java]

        checkNotificationPermission()

        val (destination, startVoice, targetId) = parseIntent(intent)

        setContent {
            GemmyApp(
                viewModel = viewModel,
                initialDestination = destination,
                initialStartVoice = startVoice,
                initialTargetId = targetId
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val (destination, startVoice, targetId) = parseIntent(intent)
        setContent {
            GemmyApp(
                viewModel = viewModel,
                initialDestination = destination,
                initialStartVoice = startVoice,
                initialTargetId = targetId
            )
        }
    }

    private fun parseIntent(intent: Intent?): Triple<String, Boolean, Long?> {
        if (intent == null) return Triple("home", false, null)

        val action = intent.action
        val startVoice = intent.getBooleanExtra("start_voice", false) ||
                action == GemmyWidgetProvider.ACTION_VOICE_COMMAND

        val destination = when {
            action == GemmyWidgetProvider.ACTION_NEW_REMINDER -> "create_reminder"
            intent.hasExtra(ReminderBroadcastReceiver.EXTRA_DESTINATION) ->
                intent.getStringExtra(ReminderBroadcastReceiver.EXTRA_DESTINATION) ?: "home"
            intent.hasExtra("destination") ->
                intent.getStringExtra("destination") ?: "home"
            else -> "home"
        }

        val targetId = if (intent.hasExtra(ReminderBroadcastReceiver.EXTRA_ID)) {
            val id = intent.getLongExtra(ReminderBroadcastReceiver.EXTRA_ID, -1L)
            if (id != -1L) id else null
        } else null

        return Triple(destination, startVoice, targetId)
    }

    private fun checkNotificationPermission() {
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
