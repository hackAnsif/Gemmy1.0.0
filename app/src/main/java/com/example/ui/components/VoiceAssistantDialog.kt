package com.example.ui.components

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Stop
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ActionType
import com.example.data.model.GemmyParsedAction
import com.example.data.model.LearningItem
import com.example.data.model.ReminderItem
import com.example.ui.theme.GemmyDarkSecondary
import com.example.ui.theme.GemmyIndigoPrimary
import com.example.ui.theme.GemmyVioletTertiary
import com.example.ui.viewmodel.AudioTranscriptionUiState
import com.example.ui.viewmodel.GemmyViewModel
import com.example.ui.viewmodel.VoiceAssistantTab
import com.example.ui.viewmodel.VoiceAssistantUiState
import com.example.voice.SpeechRecognitionHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VoiceAssistantDialog(
    viewModel: GemmyViewModel,
    voiceState: VoiceAssistantUiState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val audioState by viewModel.audioTranscriptionState.collectAsStateWithLifecycle()

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

    // Speech Recognizer Helper (for Voice Command tab)
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
            viewModel.cancelAudioRecording()
        }
    }

    // Auto-listen if permission granted and in voice command mode
    LaunchedEffect(hasMicPermission, voiceState.activeTab) {
        if (hasMicPermission &&
            voiceState.activeTab == VoiceAssistantTab.VOICE_COMMAND &&
            !voiceState.showConfirmationDialog &&
            voiceState.followUpQuestion == null
        ) {
            speechHelper.startListening(voiceState.selectedLanguage)
        } else {
            speechHelper.stopListening()
        }
    }

    Dialog(
        onDismissRequest = {
            speechHelper.stopListening()
            viewModel.dismissVoiceDialog()
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
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
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
                                    if (voiceState.activeTab == VoiceAssistantTab.VOICE_COMMAND) {
                                        speechHelper.startListening("en-IN")
                                    }
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
                                    if (voiceState.activeTab == VoiceAssistantTab.VOICE_COMMAND) {
                                        speechHelper.startListening("ml-IN")
                                    }
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

                Spacer(modifier = Modifier.height(16.dp))

                // Mode Tabs: Voice Command vs Transcribe Audio (Gemini 3.5 Transcribe)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isVoiceCommand = voiceState.activeTab == VoiceAssistantTab.VOICE_COMMAND
                    val isTranscribe = voiceState.activeTab == VoiceAssistantTab.AUDIO_TRANSCRIBE

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                viewModel.setVoiceAssistantTab(VoiceAssistantTab.VOICE_COMMAND)
                                if (hasMicPermission) speechHelper.startListening(voiceState.selectedLanguage)
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isVoiceCommand) GemmyIndigoPrimary else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = if (isVoiceCommand) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Voice Command",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isVoiceCommand) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1.2f)
                            .clickable {
                                speechHelper.stopListening()
                                viewModel.setVoiceAssistantTab(VoiceAssistantTab.AUDIO_TRANSCRIBE)
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isTranscribe) GemmyVioletTertiary else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = if (isTranscribe) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Transcribe Audio",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isTranscribe) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab Content Display
                if (voiceState.activeTab == VoiceAssistantTab.AUDIO_TRANSCRIBE) {
                    AudioTranscribeInteractionView(
                        audioState = audioState,
                        hasMicPermission = hasMicPermission,
                        onRequestPermission = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        onStartRecord = {
                            if (hasMicPermission) {
                                viewModel.startAudioRecording(context)
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        onStopRecord = {
                            viewModel.stopAudioRecordingAndTranscribe(context)
                        },
                        onProcessAsCommand = { transcript ->
                            viewModel.setVoiceAssistantTab(VoiceAssistantTab.VOICE_COMMAND)
                            viewModel.processVoiceCommand(transcript)
                        },
                        onSaveAsLearning = { transcript ->
                            val item = LearningItem(
                                title = transcript.take(40),
                                meaning = transcript,
                                category = "Audio Notes",
                                isSpacedRepetitionEnabled = true
                            )
                            viewModel.saveLearningItem(item)
                            viewModel.showUserMessage("Saved as Memory card: ${item.title}")
                            viewModel.dismissVoiceDialog()
                            onDismiss()
                        },
                        onSaveAsReminder = { transcript ->
                            val reminder = ReminderItem(
                                title = transcript.take(50),
                                description = transcript,
                                dueTimeEpochMs = System.currentTimeMillis() + 3600_000L
                            )
                            viewModel.saveReminder(reminder)
                            viewModel.showUserMessage("Scheduled reminder: ${reminder.title}")
                            viewModel.dismissVoiceDialog()
                            onDismiss()
                        },
                        onClear = {
                            viewModel.clearTranscribedText()
                        }
                    )
                } else {
                    // Voice Command Tab
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
                    } else if (voiceState.showConfirmationDialog && voiceState.parsedAction != null) {
                        ActionConfirmationPreview(
                            action = voiceState.parsedAction,
                            onConfirm = {
                                viewModel.confirmVoiceAction(voiceState.parsedAction)
                                onDismiss()
                            },
                            onCancel = { viewModel.dismissVoiceDialog() }
                        )
                    } else {
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
                            onSwitchToTranscribe = {
                                speechHelper.stopListening()
                                viewModel.setVoiceAssistantTab(VoiceAssistantTab.AUDIO_TRANSCRIBE)
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
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = {
                        speechHelper.stopListening()
                        viewModel.dismissVoiceDialog()
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
fun AudioTranscribeInteractionView(
    audioState: AudioTranscriptionUiState,
    hasMicPermission: Boolean,
    onRequestPermission: () -> Unit,
    onStartRecord: () -> Unit,
    onStopRecord: () -> Unit,
    onProcessAsCommand: (String) -> Unit,
    onSaveAsLearning: (String) -> Unit,
    onSaveAsReminder: (String) -> Unit,
    onClear: () -> Unit
) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "audio_rec_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rec_pulse"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Model badge
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = GemmyVioletTertiary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Model: gemini-3.5-transcribe",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        // Recording Button Area
        Box(
            modifier = Modifier.size(100.dp),
            contentAlignment = Alignment.Center
        ) {
            if (audioState.isRecording) {
                Box(
                    modifier = Modifier
                        .size(94.dp)
                        .scale(pulseScale)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFFEF4444).copy(alpha = 0.4f), Color.Transparent)
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
                            if (audioState.isRecording) listOf(Color(0xFFEF4444), Color(0xFFDC2626))
                            else listOf(GemmyVioletTertiary, GemmyIndigoPrimary)
                        ),
                        CircleShape
                    )
                    .clickable {
                        if (!hasMicPermission) {
                            onRequestPermission()
                        } else if (audioState.isRecording) {
                            onStopRecord()
                        } else if (!audioState.isTranscribing) {
                            onStartRecord()
                        }
                    }
                    .testTag("audio_transcribe_record_button"),
                contentAlignment = Alignment.Center
            ) {
                if (audioState.isTranscribing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = Color.White,
                        strokeWidth = 3.dp
                    )
                } else if (audioState.isRecording) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop Recording",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Start Recording",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // State indicator text & timer
        if (audioState.isRecording) {
            val minutes = audioState.recordingDurationSec / 60
            val seconds = audioState.recordingDurationSec % 60
            val timeFormatted = "%02d:%02d".format(minutes, seconds)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFFEF4444), CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Recording: $timeFormatted",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF4444)
                )
            }
            Text(
                text = "Tap red button to stop & transcribe with Gemini 3.5",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (audioState.isTranscribing) {
            Text(
                text = "Transcribing with gemini-3.5-transcribe...",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = GemmyIndigoPrimary
            )
            Text(
                text = "Converting speech to text...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = if (!hasMicPermission) "Microphone permission required" else "Tap microphone to record audio",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Speaks in English, Malayalam, or any concept",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Error message if any
        if (audioState.errorMessage != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = audioState.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(10.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Transcribed result display
        if (audioState.transcribedText.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Transcribed Text",
                            style = MaterialTheme.typography.labelMedium,
                            color = GemmyIndigoPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Gemmy Transcription", audioState.transcribedText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = audioState.transcribedText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action buttons with the transcribed text
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onProcessAsCommand(audioState.transcribedText) },
                            colors = ButtonDefaults.buttonColors(containerColor = GemmyIndigoPrimary),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Process", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { onSaveAsLearning(audioState.transcribedText) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Memory", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { onSaveAsReminder(audioState.transcribedText) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Remind", fontSize = 12.sp)
                        }
                    }
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
    onSwitchToTranscribe: () -> Unit,
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
            modifier = Modifier.size(100.dp),
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

        Spacer(modifier = Modifier.height(12.dp))

        // Switch to Audio Transcription button
        OutlinedButton(
            onClick = onSwitchToTranscribe,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Switch to Direct Audio Recording (Gemini 3.5)", fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

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
                    imageVector = Icons.AutoMirrored.Filled.Send,
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
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Gemmy Needs Clarification",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = question,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = answerText,
                onValueChange = onAnswerChange,
                placeholder = { Text("e.g. 8:00 PM or Tomorrow") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("clarification_input"),
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onSubmit,
                enabled = answerText.isNotBlank(),
                modifier = Modifier
                    .background(GemmyIndigoPrimary, CircleShape)
                    .size(48.dp)
                    .testTag("clarification_submit")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send Answer",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun ActionConfirmationPreview(
    action: GemmyParsedAction,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Ready to create this for you!",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val typeLabel = when (action.actionType) {
                        ActionType.CREATE_LEARNING_ITEM -> "Learning Memory"
                        ActionType.CREATE_REMINDER -> "Scheduled Reminder"
                        ActionType.CREATE_ALARM -> "Device Alarm"
                        ActionType.QUERY_SCHEDULE -> "Schedule Query"
                        ActionType.UNKNOWN -> "Action"
                    }
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = GemmyIndigoPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = action.confidenceSource,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = action.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (action.meaning.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Meaning: ${action.meaning}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (action.example.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Example: ${action.example}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Time / Date info
                Spacer(modifier = Modifier.height(8.dp))
                val timeString = if (action.timeHour != null) {
                    "%02d:%02d".format(action.timeHour, action.timeMinute ?: 0)
                } else null

                val dateString = if (action.targetDateEpochMs != null) {
                    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(action.targetDateEpochMs))
                } else null

                if (timeString != null || dateString != null) {
                    Text(
                        text = "Scheduled for: ${dateString ?: "Today"} ${timeString?.let { "at $it" } ?: ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = GemmyVioletTertiary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Cancel")
            }

            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .weight(1f)
                    .testTag("confirm_voice_action_button"),
                colors = ButtonDefaults.buttonColors(containerColor = GemmyIndigoPrimary),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Confirm")
            }
        }
    }
}
