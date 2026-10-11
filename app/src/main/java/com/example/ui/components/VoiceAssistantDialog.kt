package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.model.ActionType
import com.example.data.model.GemmyParsedAction
import com.example.ui.theme.GemmyDarkSecondary
import com.example.ui.theme.GemmyIndigoPrimary
import com.example.ui.theme.GemmyVioletTertiary
import com.example.ui.viewmodel.GemmyViewModel
import com.example.voice.SpeechRecognitionHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VoiceAssistantDialog(
    viewModel: GemmyViewModel,
    voiceState: com.example.ui.viewmodel.VoiceAssistantUiState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
    }

    var manualInputText by remember { mutableStateOf("") }
    var clarificationAnswer by remember { mutableStateOf("") }

    // Speech Recognizer Helper
    val speechHelper = remember {
        SpeechRecognitionHelper(
            context = context,
            onListeningStateChanged = { listening ->
                viewModel.setVoiceListening(listening)
            },
            onRmsChanged = {},
            onResult = { text ->
                viewModel.setRecognizedText(text)
                viewModel.processVoiceCommand(text)
            },
            onError = { error ->
                viewModel.showUserMessage(error)
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            speechHelper.stopListening()
        }
    }

    // Auto-listen if permission granted
    LaunchedEffect(hasMicPermission) {
        if (hasMicPermission && !voiceState.showConfirmationDialog && voiceState.followUpQuestion == null) {
            speechHelper.startListening(voiceState.selectedLanguage)
        }
    }

    Dialog(
        onDismissRequest = {
            speechHelper.stopListening()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    Brush.linearGradient(listOf(GemmyVioletTertiary, GemmyDarkSecondary)),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Gemmy AI",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Gemmy Assistant",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Language Selector (English / Malayalam)
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "EN",
                            modifier = Modifier
                                .clickable {
                                    viewModel.setVoiceLanguage("en-IN")
                                    speechHelper.startListening("en-IN")
                                }
                                .background(
                                    if (voiceState.selectedLanguage == "en-IN") GemmyIndigoPrimary else Color.Transparent,
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            color = if (voiceState.selectedLanguage == "en-IN") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "മലയാളം",
                            modifier = Modifier
                                .clickable {
                                    viewModel.setVoiceLanguage("ml-IN")
                                    speechHelper.startListening("ml-IN")
                                }
                                .background(
                                    if (voiceState.selectedLanguage == "ml-IN") GemmyIndigoPrimary else Color.Transparent,
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            color = if (voiceState.selectedLanguage == "ml-IN") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Case 1: Clarification needed (e.g. missing time)
                if (voiceState.followUpQuestion != null) {
                    ClarificationQuestionView(
                        question = voiceState.followUpQuestion,
                        answerText = clarificationAnswer,
                        onAnswerChange = { clarificationAnswer = it },
                        onSubmit = {
                            if (clarificationAnswer.isNotBlank()) {
                                viewModel.answerClarification(clarificationAnswer)
                                clarificationAnswer = ""
                            }
                        }
                    )
                }
                // Case 2: Confirmation Dialog Preview
                else if (voiceState.showConfirmationDialog && voiceState.parsedAction != null) {
                    ActionConfirmationPreview(
                        action = voiceState.parsedAction,
                        onConfirm = { viewModel.confirmVoiceAction(voiceState.parsedAction) },
                        onCancel = { viewModel.dismissVoiceDialog() }
                    )
                }
                // Case 3: Listening / Recording View
                else {
                    ListeningInteractionView(
                        isListening = voiceState.isListening,
                        isProcessing = voiceState.isProcessing,
                        recognizedText = voiceState.recognizedText,
                        hasMicPermission = hasMicPermission,
                        onRequestPermission = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        onToggleListening = {
                            if (voiceState.isListening) {
                                speechHelper.stopListening()
                            } else {
                                if (hasMicPermission) {
                                    speechHelper.startListening(voiceState.selectedLanguage)
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        manualInputText = manualInputText,
                        onManualInputChange = { manualInputText = it },
                        onManualSubmit = {
                            if (manualInputText.isNotBlank()) {
                                viewModel.processVoiceCommand(manualInputText)
                                manualInputText = ""
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = {
                        speechHelper.stopListening()
                        onDismiss()
                    },
                    modifier = Modifier.testTag("close_voice_button")
                ) {
                    Text("Close Assistant")
                }
            }
        }
    }
}

@Composable
fun ListeningInteractionView(
    isListening: Boolean,
    isProcessing: Boolean,
    recognizedText: String,
    hasMicPermission: Boolean,
    onRequestPermission: () -> Unit,
    onToggleListening: () -> Unit,
    manualInputText: String,
    onManualInputChange: (String) -> Unit,
    onManualSubmit: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Pulsing Microphone Button
        Box(
            modifier = Modifier
                .size(100.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isListening) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .scale(pulseScale)
                        .background(
                            Brush.radialGradient(
                                listOf(GemmyVioletTertiary.copy(alpha = 0.4f), Color.Transparent)
                            ),
                            CircleShape
                        )
                )
            }

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        Brush.linearGradient(
                            if (isListening) listOf(GemmyVioletTertiary, GemmyDarkSecondary)
                            else listOf(Color(0xFF334155), Color(0xFF1E293B))
                        ),
                        CircleShape
                    )
                    .clickable { onToggleListening() }
                    .testTag("mic_toggle_button"),
                contentAlignment = Alignment.Center
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = Color.White,
                        strokeWidth = 3.dp
                    )
                } else {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = "Voice Input",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = when {
                !hasMicPermission -> "Tap microphone to grant permission"
                isProcessing -> "Gemmy is understanding your request..."
                isListening -> "Listening... Speak naturally in English or മലയാളം"
                else -> "Tap microphone to speak"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (recognizedText.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "\"$recognizedText\"",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(14.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Text input fallback
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = manualInputText,
                onValueChange = onManualInputChange,
                placeholder = { Text("Or type command here...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("voice_manual_input"),
                singleLine = true,
                shape = RoundedCornerShape(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onManualSubmit,
                enabled = manualInputText.isNotBlank(),
                modifier = Modifier
                    .background(GemmyIndigoPrimary, CircleShape)
                    .size(48.dp)
                    .testTag("voice_manual_submit")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun ClarificationQuestionView(
    question: String,
    answerText: String,
    onAnswerChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                .padding(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "Clarification",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Gemmy asks:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = question,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Quick suggested answers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("8:00 PM", "9:00 AM", "Tomorrow", "In 1 hour").forEach { suggestion ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable {
                        onAnswerChange(suggestion)
                    }
                ) {
                    Text(
                        text = suggestion,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = answerText,
            onValueChange = onAnswerChange,
            placeholder = { Text("E.g. At 8 PM") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("clarification_input"),
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("clarification_submit"),
            colors = ButtonDefaults.buttonColors(containerColor = GemmyIndigoPrimary)
        ) {
            Text("Update Reminder")
        }
    }
}

@Composable
fun ActionConfirmationPreview(
    action: GemmyParsedAction,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (action.actionType) {
                        ActionType.CREATE_LEARNING_ITEM -> "LEARNING REVIEW"
                        ActionType.CREATE_ALARM -> "ALARM CLOCK"
                        else -> "REMINDER"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = GemmyDarkSecondary,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = action.confidenceSource,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = action.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (action.meaning.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Meaning: ${action.meaning}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (action.targetDateEpochMs != null) {
                Spacer(modifier = Modifier.height(6.dp))
                val sdf = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault())
                val timeStr = "%02d:%02d".format(action.timeHour ?: 9, action.timeMinute ?: 0)
                Text(
                    text = "Scheduled: ${sdf.format(Date(action.targetDateEpochMs))} at $timeStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            } else if (action.timeHour != null) {
                Spacer(modifier = Modifier.height(6.dp))
                val timeStr = "%02d:%02d".format(action.timeHour, action.timeMinute ?: 0)
                Text(
                    text = "Time: $timeStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.testTag("action_cancel_button")
                ) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = GemmyDarkSecondary),
                    modifier = Modifier.testTag("action_confirm_button")
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Confirm & Save")
                }
            }
        }
    }
}
