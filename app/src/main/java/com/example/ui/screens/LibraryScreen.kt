package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.LearningItem
import com.example.data.model.LearningStatus
import com.example.ui.theme.GemmyDarkSecondary
import com.example.ui.theme.GemmyIndigoPrimary
import com.example.ui.viewmodel.GemmyViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: GemmyViewModel,
    onNavigateToAdd: () -> Unit,
    onNavigateToReview: (Long) -> Unit
) {
    val items by viewModel.filteredLearningItems.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val sortBy by viewModel.sortBy.collectAsStateWithLifecycle()

    var selectedItemForDetail by remember { mutableStateOf<LearningItem?>(null) }
    var itemToDelete by remember { mutableStateOf<LearningItem?>(null) }

    val categories = listOf("All", "English Vocabulary", "Science", "Mathematics", "Engineering", "General Knowledge", "Study Notes")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Memory Library", fontWeight = FontWeight.Bold) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = GemmyIndigoPrimary,
                modifier = Modifier.testTag("fab_add_learning")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Item", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search title, meaning, example...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("library_search_input"),
                singleLine = true,
                shape = RoundedCornerShape(24.dp)
            )

            // Category Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { viewModel.setCategoryFilter(cat) },
                        label = { Text(cat) }
                    )
                }
            }

            // Sort Selector Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${items.size} memory items",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Sort:",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    TextButton(onClick = {
                        val next = when (sortBy) {
                            "date" -> "alphabetical"
                            "alphabetical" -> "due"
                            else -> "date"
                        }
                        viewModel.setSortBy(next)
                    }) {
                        Text(
                            when (sortBy) {
                                "alphabetical" -> "A to Z"
                                "due" -> "Next Review"
                                else -> "Recent First"
                            }
                        )
                    }
                }
            }

            // Items List
            if (items.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isBlank()) "No items saved yet" else "No matching memory items",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Save English words, engineering formulas, definitions, or study facts to review over time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        LibraryItemCard(
                            item = item,
                            onClick = { selectedItemForDetail = item },
                            onReview = { onNavigateToReview(item.id) }
                        )
                    }
                }
            }
        }
    }

    // Item Detail Dialog
    selectedItemForDetail?.let { item ->
        LearningItemDetailDialog(
            item = item,
            onDismiss = { selectedItemForDetail = null },
            onReviewNow = {
                selectedItemForDetail = null
                onNavigateToReview(item.id)
            },
            onDelete = {
                selectedItemForDetail = null
                itemToDelete = item
            },
            onMarkRemembered = {
                viewModel.updateLearningItem(item.copy(status = LearningStatus.REMEMBERED))
                selectedItemForDetail = null
            }
        )
    }

    // Delete Confirmation Dialog
    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Delete Memory Item") },
            text = { Text("Are you sure you want to delete \"${item.title}\"? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteLearningItem(item.id)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { itemToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun LibraryItemCard(
    item: LearningItem,
    onClick: () -> Unit,
    onReview: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("learning_item_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.category.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = GemmyDarkSecondary,
                    fontWeight = FontWeight.Bold
                )

                if (item.status == LearningStatus.REMEMBERED) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = GemmyDarkSecondary.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "Remembered",
                            style = MaterialTheme.typography.labelSmall,
                            color = GemmyDarkSecondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = item.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (item.meaning.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.meaning,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                val reviewText = if (item.nextReviewEpochMs <= System.currentTimeMillis()) {
                    "Review Due Now"
                } else {
                    "Next: ${sdf.format(Date(item.nextReviewEpochMs))}"
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (item.nextReviewEpochMs <= System.currentTimeMillis()) GemmyDarkSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = reviewText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.nextReviewEpochMs <= System.currentTimeMillis()) GemmyDarkSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                TextButton(
                    onClick = onReview,
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Review", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun LearningItemDetailDialog(
    item: LearningItem,
    onDismiss: () -> Unit,
    onReviewNow: () -> Unit,
    onDelete: () -> Unit,
    onMarkRemembered: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = item.category.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = GemmyDarkSecondary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = item.title, fontWeight = FontWeight.ExtraBold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (item.meaning.isNotBlank()) {
                    Column {
                        Text(text = "Meaning / Explanation", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(text = item.meaning, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                if (item.example.isNotBlank()) {
                    Column {
                        Text(text = "Example Sentence", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(text = item.example, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                if (item.tags.isNotBlank()) {
                    Column {
                        Text(text = "Tags", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(text = item.tags, style = MaterialTheme.typography.bodySmall)
                    }
                }

                val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy • h:mm a", Locale.getDefault())
                Column {
                    Text(text = "Next Scheduled Review", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(text = sdf.format(Date(item.nextReviewEpochMs)), style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onReviewNow,
                colors = ButtonDefaults.buttonColors(containerColor = GemmyIndigoPrimary)
            ) {
                Text("Review Now")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onMarkRemembered) {
                    Text("Remembered")
                }
            }
        }
    )
}
