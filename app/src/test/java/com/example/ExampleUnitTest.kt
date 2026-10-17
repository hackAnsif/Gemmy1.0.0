package com.example

import com.example.ai.OfflineAiEngine
import com.example.data.model.ActionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testTomorrowSubmitAssignmentReminder() {
        val result = OfflineAiEngine.parse("tomorrow I have to submit my assignment")
        assertEquals(ActionType.CREATE_REMINDER, result.actionType)
        assertEquals("Study", result.category)
        assertTrue("Title should contain submit assignment: ${result.title}", result.title.contains("Submit", ignoreCase = true))
        assertNotNull(result.targetDateEpochMs)
    }

    @Test
    fun testParentsMeansLearningCard() {
        val result = OfflineAiEngine.parse("parents means mother and father")
        assertEquals(ActionType.CREATE_LEARNING_ITEM, result.actionType)
        assertEquals("Parents", result.title)
        assertEquals("mother and father", result.meaning)
        assertTrue("Should enable spaced repetition", result.isSpacedRepetition)
    }

    @Test
    fun testParentsMeansExactPhrase() {
        val result = OfflineAiEngine.parse("parents means")
        assertEquals(ActionType.CREATE_LEARNING_ITEM, result.actionType)
        assertEquals("Parents", result.title)
        assertTrue("Should have a valid definition/meaning placeholder", result.meaning.isNotBlank())
        assertTrue("Should enable spaced repetition", result.isSpacedRepetition)
    }

    @Test
    fun testSetAlarmTomorrow12pm() {
        val result = OfflineAiEngine.parse("set an alarm tomorrow 12:00 p.m.")
        assertEquals(ActionType.CREATE_ALARM, result.actionType)
        assertEquals(12, result.timeHour)
        assertEquals(0, result.timeMinute)
        assertNotNull(result.targetDateEpochMs)
    }

    @Test
    fun testRemindMeToCallParentsAt5pm() {
        val result = OfflineAiEngine.parse("remind me to call parents tomorrow at 5 pm")
        assertEquals(ActionType.CREATE_REMINDER, result.actionType)
        assertEquals(17, result.timeHour)
        assertEquals(0, result.timeMinute)
        assertTrue("Title should contain Call Parents: ${result.title}", result.title.contains("Call Parents", ignoreCase = true))
        assertEquals("Personal", result.category)
    }

    @Test
    fun testFormulaOfKineticEnergy() {
        val result = OfflineAiEngine.parse("formula of kinetic energy is half m v squared")
        assertEquals(ActionType.CREATE_LEARNING_ITEM, result.actionType)
        assertEquals("Kinetic Energy", result.title)
        assertEquals("half m v squared", result.meaning)
        assertEquals("Formulas & Science", result.category)
    }

    @Test
    fun testMalayalamAlarmTomorrow12pm() {
        val result = OfflineAiEngine.parse("നാളെ ഉച്ചയ്ക്ക് 12 മണിക്ക് അലാറം")
        assertEquals(ActionType.CREATE_ALARM, result.actionType)
        assertEquals(12, result.timeHour)
        assertEquals(0, result.timeMinute)
        assertNotNull(result.targetDateEpochMs)
    }

    @Test
    fun testRelativeOffsetMinutes() {
        val result = OfflineAiEngine.parse("remind me in 15 minutes to take medicine")
        assertEquals(ActionType.CREATE_REMINDER, result.actionType)
        assertEquals("Health", result.category)
        assertNotNull(result.timeHour)
        assertNotNull(result.timeMinute)
    }
}
