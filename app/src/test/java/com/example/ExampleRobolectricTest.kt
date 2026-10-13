package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ActionType
import com.example.voice.GemmyNluParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Gemmy", appName)
    }

    @Test
    fun testEnglishLearningCommandParsing() {
        val command = "Remind me to revise the word ephemeral after two weeks at 8 PM."
        val action = GemmyNluParser.parseOffline(command)

        assertEquals(ActionType.CREATE_LEARNING_ITEM, action.actionType)
        assertEquals(20, action.timeHour)
        assertEquals(0, action.timeMinute)
        assertTrue(action.targetDateEpochMs != null)
    }

    @Test
    fun testMalayalamLearningCommandParsing() {
        val command = "നാളെ രാവിലെ 7 മണിക്ക് Fluid Mechanics പഠിക്കാൻ ഓർമ്മിപ്പിക്കണം."
        val action = GemmyNluParser.parseOffline(command)

        assertEquals(ActionType.CREATE_LEARNING_ITEM, action.actionType)
        assertEquals(7, action.timeHour)
        assertEquals(0, action.timeMinute)
    }

    @Test
    fun testAlarmCommandParsing() {
        val command = "Set an alarm for 6:30 AM tomorrow"
        val action = GemmyNluParser.parseOffline(command)

        assertEquals(ActionType.CREATE_ALARM, action.actionType)
        assertEquals(6, action.timeHour)
        assertEquals(30, action.timeMinute)
    }

    @Test
    fun testAmbiguousCommandRequiresClarification() {
        val command = "Remind me next Friday"
        val action = GemmyNluParser.parseOffline(command)

        assertTrue(action.needsClarification)
        assertTrue(action.clarificationPrompt.isNotBlank())
    }

    @Test
    fun testGeminiTranscriptionModelTarget() {
        assertEquals("gemini-3.5-flash", com.example.ai.GeminiApiService.MODEL_TRANSCRIBE)
    }

    @Test
    fun testAudioRecorderInstantiation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val recorder = com.example.voice.AudioRecorderHelper(context)
        org.junit.Assert.assertFalse(recorder.isRecording)
        assertEquals(0, recorder.getMaxAmplitude())
    }

    @Test
    fun testAudioTranscriptionUiState() {
        val state = com.example.ui.viewmodel.AudioTranscriptionUiState()
        org.junit.Assert.assertFalse(state.isRecording)
        org.junit.Assert.assertFalse(state.isTranscribing)
        assertEquals("", state.transcribedText)
        assertEquals(0, state.recordingDurationSec)
    }

    @Test
    fun testRoomDatabaseAccess() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.example.data.db.GemmyDatabase.getDatabase(context)
        org.junit.Assert.assertNotNull(db)
        val reminders = db.reminderItemDao().getAllPendingRemindersOnce()
        org.junit.Assert.assertNotNull(reminders)
        val alarms = db.alarmItemDao().getActiveAlarmsOnce()
        org.junit.Assert.assertNotNull(alarms)
    }
}
