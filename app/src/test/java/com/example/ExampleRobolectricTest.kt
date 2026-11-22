package com.example

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.GemmyDatabase
import com.example.data.model.ReminderItem
import com.example.receiver.BootReceiver
import com.example.receiver.NotificationActionReceiver
import com.example.receiver.ReminderBroadcastReceiver
import com.example.ui.GemmyApp
import com.example.ui.theme.GemmyTheme
import com.example.ui.viewmodel.GemmyViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var app: GemmyApplication

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        app = context.applicationContext as GemmyApplication
        app.repository.clearAllReminders(context)
    }

    @After
    fun tearDown() = runBlocking {
        app.repository.clearAllReminders(context)
    }

    @Test
    fun verifyAppNameAndLauncherResourcesExist() {
        assertEquals("Gemmy", context.getString(R.string.app_name))
        assertNotNull(context.getDrawable(R.drawable.ic_gemmy_brand_icon))
        assertNotNull(context.getDrawable(R.mipmap.ic_launcher))
        assertNotNull(context.getDrawable(R.mipmap.ic_launcher_round))
    }

    @Test
    fun testReminderCrudScheduleAndCancelFlow() = runBlocking {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = shadowOf(alarmManager)

        val futureTime = System.currentTimeMillis() + 3_600_000L
        val id = app.repository.insertReminder(
            ReminderItem(
                title = "Submit assignment",
                dueTimeEpochMs = futureTime,
                alarmEnabled = true
            )
        )
        assertTrue(id > 0L)
        assertEquals(1, shadowAlarmManager.scheduledAlarms.size)

        // Update description and toggle alarm off -> alarm must be cancelled
        val saved = app.repository.getReminderById(id)!!
        app.repository.updateReminder(saved.copy(title = "Submit final assignment", alarmEnabled = false))
        assertEquals(0, shadowAlarmManager.scheduledAlarms.size)

        // Re-enable alarm and then mark completed -> schedules then cancels
        app.repository.updateReminder(saved.copy(title = "Submit final assignment", alarmEnabled = true))
        assertEquals(1, shadowAlarmManager.scheduledAlarms.size)

        app.repository.markReminderCompleted(id, completed = true)
        assertTrue(app.repository.getReminderById(id)!!.isCompleted)
        assertEquals(0, shadowAlarmManager.scheduledAlarms.size)

        // Mark incomplete -> reschedules future reminder
        app.repository.markReminderCompleted(id, completed = false)
        assertFalse(app.repository.getReminderById(id)!!.isCompleted)
        assertEquals(1, shadowAlarmManager.scheduledAlarms.size)

        // Delete reminder -> cancels alarm and removes from DB
        app.repository.deleteReminder(id)
        assertNull(app.repository.getReminderById(id))
        assertEquals(0, shadowAlarmManager.scheduledAlarms.size)
    }

    @Test
    fun testNotificationDeliveryAndDuplicateSuppression() = runBlocking {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNm = shadowOf(nm)

        val futureTime = System.currentTimeMillis() + 600_000L
        val id = app.repository.insertReminder(
            ReminderItem(
                title = "Study physics",
                dueTimeEpochMs = futureTime,
                alarmEnabled = true
            )
        )

        val receiver = ReminderBroadcastReceiver()
        val fireIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_FIRE_REMINDER
            putExtra(ReminderBroadcastReceiver.EXTRA_ID, id)
            putExtra(ReminderBroadcastReceiver.EXTRA_TITLE, "Study physics")
        }

        receiver.onReceive(context, fireIntent)
        waitUntil { shadowNm.allNotifications.size == 1 }
        assertEquals(1, shadowNm.allNotifications.size)

        // Mark completed, clear notifications, fire stale broadcast -> must NOT notify
        app.repository.markReminderCompleted(id, completed = true)
        nm.cancelAll()
        receiver.onReceive(context, fireIntent)
        Thread.sleep(300)
        assertEquals(0, shadowNm.allNotifications.size)
    }

    @Test
    fun testNotificationActionsAndBootRestore() = runBlocking {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = shadowOf(alarmManager)

        val activeId = app.repository.insertReminder(
            ReminderItem(
                title = "Attend meeting",
                dueTimeEpochMs = System.currentTimeMillis() + 1_800_000L,
                alarmEnabled = true
            )
        )
        val completedId = app.repository.insertReminder(
            ReminderItem(
                title = "Completed task",
                dueTimeEpochMs = System.currentTimeMillis() + 1_800_000L,
                alarmEnabled = true,
                isCompleted = true
            )
        )

        // Cancel existing scheduled alarms to simulate device reboot clearing AlarmManager
        while (shadowAlarmManager.scheduledAlarms.isNotEmpty()) {
            shadowAlarmManager.scheduledAlarms.removeAt(0)
        }
        assertEquals(0, shadowAlarmManager.scheduledAlarms.size)

        val bootReceiver = BootReceiver()
        bootReceiver.onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))
        waitUntil { shadowAlarmManager.scheduledAlarms.size == 1 }

        // Only active reminder should be restored after reboot (not completedId)
        assertEquals(1, shadowAlarmManager.scheduledAlarms.size)
        assertFalse(app.repository.getReminderById(activeId)!!.isCompleted)
        assertTrue(app.repository.getReminderById(completedId)!!.isCompleted)

        // Trigger snooze action via NotificationActionReceiver
        val actionReceiver = NotificationActionReceiver()
        actionReceiver.onReceive(
            context,
            Intent(NotificationActionReceiver.ACTION_NOTIFICATION_SNOOZE).apply {
                putExtra(ReminderBroadcastReceiver.EXTRA_ID, activeId)
            }
        )
        waitUntil { app.repository.getReminderById(activeId)?.isSnoozed == true }
        assertTrue(app.repository.getReminderById(activeId)!!.isSnoozed)

        // Trigger complete action via NotificationActionReceiver
        actionReceiver.onReceive(
            context,
            Intent(NotificationActionReceiver.ACTION_NOTIFICATION_COMPLETE).apply {
                putExtra(ReminderBroadcastReceiver.EXTRA_ID, activeId)
            }
        )
        waitUntil { app.repository.getReminderById(activeId)?.isCompleted == true }
        assertTrue(app.repository.getReminderById(activeId)!!.isCompleted)
    }

    private suspend fun waitUntil(timeoutMs: Long = 3000L, condition: suspend () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return
            Thread.sleep(50)
        }
    }

    @Test
    fun testComposeCreateAndCompleteReminderFlow() {
        val viewModel = GemmyViewModel(app.repository)

        composeRule.setContent {
            GemmyTheme {
                GemmyApp(viewModel = viewModel)
            }
        }

        // Open Add Reminder screen
        composeRule.onNodeWithTag("add_reminder_fab").performClick()

        // Enter natural-language reminder
        composeRule.onNodeWithTag("reminder_description_input")
            .performTextInput("Tomorrow 12 PM submit assignment")

        // Smart parse preview should appear and apply extracted description/date/time
        composeRule.onNodeWithTag("smart_parse_preview_card").assertIsDisplayed()
        composeRule.onNodeWithTag("apply_smart_parse_button").performClick()

        // Save reminder
        composeRule.onNodeWithTag("save_reminder_button").performClick()

        // Returned to Home screen and reminder appears
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Submit assignment").assertIsDisplayed()

        // Verify persisted in Room
        val all = runBlocking { GemmyDatabase.getDatabase(context).reminderItemDao().getAllRemindersOnce() }
        assertEquals(1, all.size)
        assertEquals("Submit assignment", all.first().title)
        assertTrue(all.first().alarmEnabled)
    }
}
