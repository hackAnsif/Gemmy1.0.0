package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Schedule
import com.example.ui.components.AudioTranscribeInputDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LearningItem
import com.example.ui.theme.GemmyDarkSecondary
import com.example.ui.theme.GemmyIndigoPrimary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

import com.example.ui.viewmodel.GemmyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLearningScreen(
    viewModel: GemmyViewModel,
    onNavigateBack: () -> Unit,
    onSaveItem: (LearningItem) -> Unit,
    initialTitle: String = "",
    initialMeaning: String = "",
    initialExample: String = ""
) {
    val context = LocalContext.current

    var title by rememberSaveable { mutableStateOf(initialTitle) }
    var meaning by rememberSaveable { mutableStateOf(initialMeaning) }
    var example by rememberSaveable { mutableStateOf(initialExample) }
    var notes by rememberSaveable { mutableStateOf("") }
    var language by rememberSaveable { mutableStateOf("English") }
    var category by rememberSaveable { mutableStateOf("English Vocabulary") }
    var tags by rememberSaveable { mutableStateOf("") }
    var activeTranscribeTarget by rememberSaveable { mutableStateOf<String?>(null) }

    var isSmartReview by rememberSaveable { mutableStateOf(true) }

    // Custom review date & time with zeroed seconds and milliseconds
    val defaultCalendar = remember {
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    var reviewDateMs by rememberSaveable { mutableStateOf(defaultCalendar.timeInMillis) }
    val displayDateStr = remember(reviewDateMs) {
        SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(reviewDateMs)
    }
    val displayTimeStr = remember(reviewDateMs) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(reviewDateMs)
    }

    val categories = listOf(
        "English Vocabulary",
        "Science",
        "Mathematics",
        "Engineering",
        "Definitions",
        "General Knowledge",
        "Study Notes"
    )

    val languages = listOf("English", "Malayalam", "Hindi", "German", "Spanish", "Other")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Memory Item", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
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
            // Title / Term
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title / Term / Formula / Concept *") },
                placeholder = { Text("e.g. Ephemeral or Bernoulli Equation") },
                trailingIcon = {
                    IconButton(onClick = { activeTranscribeTarget = "Title" }) {
                        Icon(Icons.Default.GraphicEq, contentDescription = "Transcribe Title", tint = GemmyIndigoPrimary)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("learning_title_input"),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            // Meaning / Definition
            OutlinedTextField(
                value = meaning,
                onValueChange = { meaning = it },
                label = { Text("Meaning / Explanation / Formula") },
                placeholder = { Text("e.g. Lasting for a very short time.") },
                trailingIcon = {
                    IconButton(onClick = { activeTranscribeTarget = "Meaning" }) {
                        Icon(Icons.Default.GraphicEq, contentDescription = "Transcribe Meaning", tint = GemmyIndigoPrimary)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("learning_meaning_input"),
                minLines = 2,
                shape = RoundedCornerShape(14.dp)
            )

            // Example Sentence
            OutlinedTextField(
                value = example,
                onValueChange = { example = it },
                label = { Text("Example Sentence / Context") },
                placeholder = { Text("e.g. The beauty of the sunset was ephemeral.") },
                trailingIcon = {
                    IconButton(onClick = { activeTranscribeTarget = "Example" }) {
                        Icon(Icons.Default.GraphicEq, contentDescription = "Transcribe Example", tint = GemmyIndigoPrimary)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("learning_example_input"),
                minLines = 2,
                shape = RoundedCornerShape(14.dp)
            )

            // Category Selection
            Column {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.take(3).forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.drop(3).take(3).forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }

            // Language Selection
            Column {
                Text(
                    text = "Language",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    languages.take(4).forEach { lang ->
                        FilterChip(
                            selected = language == lang,
                            onClick = { language = lang },
                            label = { Text(lang) }
                        )
                    }
                }
            }

            // Spaced Repetition Mode Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Smart Spaced-Repetition",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Gemmy will suggest future review sessions (Day 1, Day 3, Day 7, Day 14, Day 30)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isSmartReview,
                            onCheckedChange = { isSmartReview = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (isSmartReview) "First Review Scheduled:" else "Custom Review Reminder:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Date Picker trigger
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val c = Calendar.getInstance().apply { timeInMillis = reviewDateMs }
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            c.set(Calendar.YEAR, y)
                                            c.set(Calendar.MONTH, m)
                                            c.set(Calendar.DAY_OF_MONTH, d)
                                            c.set(Calendar.SECOND, 0)
                                            c.set(Calendar.MILLISECOND, 0)
                                            reviewDateMs = c.timeInMillis
                                        },
                                        c.get(Calendar.YEAR),
                                        c.get(Calendar.MONTH),
                                        c.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = GemmyIndigoPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = displayDateStr, style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        // Time Picker trigger
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val c = Calendar.getInstance().apply { timeInMillis = reviewDateMs }
                                    TimePickerDialog(
                                        context,
                                        { _, h, min ->
                                            c.set(Calendar.HOUR_OF_DAY, h)
                                            c.set(Calendar.MINUTE, min)
                                            c.set(Calendar.SECOND, 0)
                                            c.set(Calendar.MILLISECOND, 0)
                                            reviewDateMs = c.timeInMillis
                                        },
                                        c.get(Calendar.HOUR_OF_DAY),
                                        c.get(Calendar.MINUTE),
                                        false
                                    ).show()
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = GemmyDarkSecondary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = displayTimeStr, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            // Optional tags
            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                label = { Text("Tags (comma separated)") },
                placeholder = { Text("e.g. GRE, physics, vocabulary, exam") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            // Save Button
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val item = LearningItem(
                            title = title.trim(),
                            meaning = meaning.trim(),
                            example = example.trim(),
                            notes = notes.trim(),
                            language = language,
                            category = category,
                            tags = tags.trim(),
                            nextReviewEpochMs = reviewDateMs,
                            isSpacedRepetitionEnabled = isSmartReview
                        )
                        onSaveItem(item)
                        onNavigateBack()
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_learning_button"),
                colors = ButtonDefaults.buttonColors(containerColor = GemmyIndigoPrimary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Save Memory Item", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }

    if (activeTranscribeTarget != null) {
        AudioTranscribeInputDialog(
            fieldLabel = activeTranscribeTarget!!,
            viewModel = viewModel,
            onDismiss = { activeTranscribeTarget = null },
            onTranscriptionConfirmed = { text ->
                when (activeTranscribeTarget) {
                    "Title" -> title = text
                    "Meaning" -> meaning = text
                    "Example" -> example = text
                }
                activeTranscribeTarget = null
            }
        )
    }
}
