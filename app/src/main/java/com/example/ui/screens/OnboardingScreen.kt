package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GemmyDarkSecondary
import com.example.ui.theme.GemmyIndigoPrimary
import com.example.ui.theme.GemmyVioletTertiary

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(24.dp))

            // Glowing Gemmy Hero Icon
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(
                        Brush.linearGradient(listOf(GemmyVioletTertiary, GemmyDarkSecondary)),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Gemmy",
                    tint = Color.White,
                    modifier = Modifier.size(50.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Welcome to Gemmy",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Smart Memory & Reminder Assistant",
                style = MaterialTheme.typography.titleMedium,
                color = GemmyDarkSecondary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Value props
            OnboardingFeatureRow(
                icon = Icons.Default.School,
                title = "Learning Memory Notebook",
                desc = "Save vocabulary, mathematical formulas, and engineering principles. Review them right before you forget."
            )

            Spacer(modifier = Modifier.height(16.dp))

            OnboardingFeatureRow(
                icon = Icons.Default.Mic,
                title = "Malayalam & English Voice Assistant",
                desc = "Speak naturally in Malayalam or English. Gemmy extracts dates, times, and creates reminders automatically."
            )

            Spacer(modifier = Modifier.height(16.dp))

            OnboardingFeatureRow(
                icon = Icons.Default.Widgets,
                title = "Home-Screen Widget",
                desc = "Tap the microphone directly from your Android home screen launcher to add reminders on the fly."
            )

            Spacer(modifier = Modifier.height(16.dp))

            OnboardingFeatureRow(
                icon = Icons.Default.Alarm,
                title = "Reliable Alarms & Offline First",
                desc = "Real Android alarms and notifications. Never misses a reminder even without internet access."
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = onFinish,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("get_started_button"),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GemmyIndigoPrimary)
            ) {
                Text("Get Started", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun OnboardingFeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(GemmyIndigoPrimary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = GemmyIndigoPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
