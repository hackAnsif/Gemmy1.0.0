package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.LearningItem
import com.example.data.model.RecallRating
import com.example.ui.theme.GemmyDarkSecondary
import com.example.ui.theme.GemmyError
import com.example.ui.theme.GemmyIndigoPrimary
import com.example.ui.theme.GemmyVioletTertiary
import com.example.ui.theme.GemmyWarning
import com.example.ui.viewmodel.GemmyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningReviewScreen(
    viewModel: GemmyViewModel,
    targetItemId: Long?,
    onNavigateBack: () -> Unit
) {
    val allItems by viewModel.learningItems.collectAsStateWithLifecycle()
    val dueItems by viewModel.dueLearningItems.collectAsStateWithLifecycle()

    // Determine current item to review
    val currentItem = remember(targetItemId, allItems, dueItems) {
        if (targetItemId != null && targetItemId > 0) {
            allItems.find { it.id == targetItemId } ?: dueItems.firstOrNull() ?: allItems.firstOrNull()
        } else {
            dueItems.firstOrNull() ?: allItems.firstOrNull()
        }
    }

    var isAnswerRevealed by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spaced-Repetition Review", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (currentItem == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(GemmyDarkSecondary.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = GemmyDarkSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "No Reviews Pending!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "You are all caught up on your spaced-repetition memory sessions. Great job!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onNavigateBack,
                    colors = ButtonDefaults.buttonColors(containerColor = GemmyIndigoPrimary)
                ) {
                    Text("Return to Home")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Flashcard Stage Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "${currentItem.category} • Stage ${currentItem.repetitionStage + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    Text(
                        text = "Interval: ${currentItem.intervalDays}d",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // The Recall Prompt Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("review_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Can you recall the meaning?",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = currentItem.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Answer reveal section
                if (!isAnswerRevealed) {
                    Button(
                        onClick = { isAnswerRevealed = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("reveal_answer_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = GemmyIndigoPrimary),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Visibility, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reveal Answer", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    AnimatedVisibility(
                        visible = isAnswerRevealed,
                        enter = fadeIn() + slideInVertically()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text(
                                        text = "MEANING / EXPLANATION",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GemmyDarkSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = currentItem.meaning.ifBlank { "No explanation saved." },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    if (currentItem.example.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Text(
                                            text = "EXAMPLE",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "\"${currentItem.example}\"",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "How well did you recall it?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // 4 Recall Rating Options (SuperMemo SM-2)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RecallButton(
                                    label = "Again",
                                    subLabel = "Forgot",
                                    color = GemmyError,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        viewModel.recordReview(currentItem, RecallRating.AGAIN)
                                        isAnswerRevealed = false
                                        onNavigateBack()
                                    }
                                )
                                RecallButton(
                                    label = "Hard",
                                    subLabel = "Difficult",
                                    color = GemmyWarning,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        viewModel.recordReview(currentItem, RecallRating.HARD)
                                        isAnswerRevealed = false
                                        onNavigateBack()
                                    }
                                )
                                RecallButton(
                                    label = "Good",
                                    subLabel = "Remembered",
                                    color = GemmyIndigoPrimary,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        viewModel.recordReview(currentItem, RecallRating.GOOD)
                                        isAnswerRevealed = false
                                        onNavigateBack()
                                    }
                                )
                                RecallButton(
                                    label = "Easy",
                                    subLabel = "Confident",
                                    color = GemmyDarkSecondary,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        viewModel.recordReview(currentItem, RecallRating.EASY)
                                        isAnswerRevealed = false
                                        onNavigateBack()
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                // Explanation note about spaced repetition
                Text(
                    text = "Spaced repetition surfaces cards right before you forget them, strengthening long-term memory retrieval.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun RecallButton(
    label: String,
    subLabel: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .testTag("recall_${label.lowercase()}"),
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.15f),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}
