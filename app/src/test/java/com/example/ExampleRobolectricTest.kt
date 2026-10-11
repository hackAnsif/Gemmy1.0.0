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
}
