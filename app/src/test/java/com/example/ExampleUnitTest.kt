package com.example

import com.example.data.model.ReminderItem
import com.example.util.SmartDateTimeParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ExampleUnitTest {

    private fun fixedNow(
        year: Int = 2026,
        month: Int = Calendar.NOVEMBER,
        day: Int = 22,
        hour: Int = 10,
        minute: Int = 0
    ): Long {
        return Calendar.getInstance().apply {
            set(year, month, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun parseTomorrowNoonReminder() {
        val now = fixedNow()
        val parsed = SmartDateTimeParser.parse("Tomorrow 12 PM submit assignment", now)

        assertTrue(parsed.hasDate)
        assertTrue(parsed.hasTime)
        assertEquals("Submit assignment", parsed.description)
        assertEquals(12, parsed.hour)
        assertEquals(0, parsed.minute)

        val cal = Calendar.getInstance().apply { timeInMillis = parsed.dateEpochMs!! }
        assertEquals(2026, cal.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, cal.get(Calendar.MONTH))
        assertEquals(23, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun parseMonthDayAndMorningTime() {
        val now = fixedNow()
        val parsed = SmartDateTimeParser.parse("November 25 at 8 AM study physics", now)

        assertTrue(parsed.hasDate)
        assertTrue(parsed.hasTime)
        assertEquals("Study physics", parsed.description)
        assertEquals(8, parsed.hour)
        assertEquals(0, parsed.minute)

        val cal = Calendar.getInstance().apply { timeInMillis = parsed.dateEpochMs!! }
        assertEquals(2026, cal.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, cal.get(Calendar.MONTH))
        assertEquals(25, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun parseTodayEveningReminder() {
        val now = fixedNow()
        val parsed = SmartDateTimeParser.parse("Today 6 PM complete project", now)

        assertTrue(parsed.hasDate)
        assertTrue(parsed.hasTime)
        assertEquals("Complete project", parsed.description)
        assertEquals(18, parsed.hour)
        assertEquals(0, parsed.minute)

        val cal = Calendar.getInstance().apply { timeInMillis = parsed.dateEpochMs!! }
        assertEquals(22, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun parseMidnightTwelveAm() {
        val now = fixedNow()
        val parsed = SmartDateTimeParser.parse("Tomorrow 12 AM backup files", now)

        assertTrue(parsed.hasDate)
        assertTrue(parsed.hasTime)
        assertEquals(0, parsed.hour)
        assertEquals(0, parsed.minute)
        assertEquals("Backup files", parsed.description)
    }

    @Test
    fun parseRelativeMinutesOffset() {
        val now = fixedNow(hour = 10, minute = 15)
        val parsed = SmartDateTimeParser.parse("in 30 minutes drink water", now)

        assertTrue(parsed.hasDate)
        assertTrue(parsed.hasTime)
        assertEquals(10, parsed.hour)
        assertEquals(45, parsed.minute)
        assertEquals("Drink water", parsed.description)
    }

    @Test
    fun parsePlainTextFallsBackGracefully() {
        val now = fixedNow()
        val parsed = SmartDateTimeParser.parse("Review English vocabulary list", now)

        assertFalse(parsed.hasAnyMatch)
        assertFalse(parsed.hasDate)
        assertFalse(parsed.hasTime)
        assertEquals("Review English vocabulary list", parsed.description)
    }

    @Test
    fun reminderDefaultsAreOfflineAndAlarmReady() {
        val reminder = ReminderItem(
            title = "Pay electricity bill",
            dueTimeEpochMs = System.currentTimeMillis() + 3_600_000L
        )
        assertTrue(reminder.alarmEnabled)
        assertFalse(reminder.isCompleted)
        assertFalse(reminder.isSnoozed)
    }
}
