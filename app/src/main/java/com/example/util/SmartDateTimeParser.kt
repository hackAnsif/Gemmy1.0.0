package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class ParsedReminderPreview(
    val originalText: String,
    val description: String,
    val dateEpochMs: Long?,
    val hour: Int?,
    val minute: Int?,
    val hasDate: Boolean,
    val hasTime: Boolean
) {
    val hasAnyMatch: Boolean get() = hasDate || hasTime

    fun formattedDate(): String? {
        val ms = dateEpochMs ?: return null
        return SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(ms)
    }

    fun formattedTime(): String? {
        val h = hour ?: return null
        val m = minute ?: 0
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
        }
        return SimpleDateFormat("h:mm a", Locale.getDefault()).format(cal.time)
    }
}

/**
 * Lightweight, 100% offline deterministic natural-language date and time parser.
 * Zero AI APIs or network calls required.
 */
object SmartDateTimeParser {

    private val monthMap = mapOf(
        "january" to Calendar.JANUARY, "jan" to Calendar.JANUARY,
        "february" to Calendar.FEBRUARY, "feb" to Calendar.FEBRUARY,
        "march" to Calendar.MARCH, "mar" to Calendar.MARCH,
        "april" to Calendar.APRIL, "apr" to Calendar.APRIL,
        "may" to Calendar.MAY,
        "june" to Calendar.JUNE, "jun" to Calendar.JUNE,
        "july" to Calendar.JULY, "jul" to Calendar.JULY,
        "august" to Calendar.AUGUST, "aug" to Calendar.AUGUST,
        "september" to Calendar.SEPTEMBER, "sep" to Calendar.SEPTEMBER, "sept" to Calendar.SEPTEMBER,
        "october" to Calendar.OCTOBER, "oct" to Calendar.OCTOBER,
        "november" to Calendar.NOVEMBER, "nov" to Calendar.NOVEMBER,
        "december" to Calendar.DECEMBER, "dec" to Calendar.DECEMBER
    )

    private val weekdayMap = mapOf(
        "sunday" to Calendar.SUNDAY, "sun" to Calendar.SUNDAY,
        "monday" to Calendar.MONDAY, "mon" to Calendar.MONDAY,
        "tuesday" to Calendar.TUESDAY, "tue" to Calendar.TUESDAY, "tues" to Calendar.TUESDAY,
        "wednesday" to Calendar.WEDNESDAY, "wed" to Calendar.WEDNESDAY,
        "thursday" to Calendar.THURSDAY, "thu" to Calendar.THURSDAY, "thur" to Calendar.THURSDAY, "thurs" to Calendar.THURSDAY,
        "friday" to Calendar.FRIDAY, "fri" to Calendar.FRIDAY,
        "saturday" to Calendar.SATURDAY, "sat" to Calendar.SATURDAY
    )

    private val monthNamesRegex = monthMap.keys.sortedByDescending { it.length }.joinToString("|")
    private val weekdayNamesRegex = weekdayMap.keys.sortedByDescending { it.length }.joinToString("|")

    // Track matched character ranges so we can strip them cleanly from description
    private data class MatchSpan(val range: IntRange)

    fun parse(input: String, nowMillis: Long = System.currentTimeMillis()): ParsedReminderPreview {
        val raw = input.trim()
        if (raw.isBlank()) {
            return ParsedReminderPreview(raw, "", null, null, null, hasDate = false, hasTime = false)
        }

        val spans = mutableListOf<MatchSpan>()
        val workingCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        var hasDate = false
        var hasTime = false
        var parsedHour: Int? = null
        var parsedMinute: Int? = null

        // 1. Check relative offset: "in 15 minutes", "after 2 hours", "in 3 days"
        val relativeRegex = Regex(
            """\b(?:in|after)\s+(\d{1,3})\s+(minute|minutes|min|mins|hour|hours|hr|hrs|day|days|week|weeks)\b""",
            RegexOption.IGNORE_CASE
        )
        relativeRegex.find(raw)?.let { match ->
            val amount = match.groupValues[1].toIntOrNull() ?: 0
            val unit = match.groupValues[2].lowercase(Locale.US)
            if (amount > 0) {
                when {
                    unit.startsWith("min") -> {
                        workingCal.add(Calendar.MINUTE, amount)
                        hasDate = true
                        hasTime = true
                    }
                    unit.startsWith("h") -> {
                        workingCal.add(Calendar.HOUR_OF_DAY, amount)
                        hasDate = true
                        hasTime = true
                    }
                    unit.startsWith("d") -> {
                        workingCal.add(Calendar.DAY_OF_YEAR, amount)
                        hasDate = true
                    }
                    unit.startsWith("w") -> {
                        workingCal.add(Calendar.WEEK_OF_YEAR, amount)
                        hasDate = true
                    }
                }
                if (hasTime) {
                    parsedHour = workingCal.get(Calendar.HOUR_OF_DAY)
                    parsedMinute = workingCal.get(Calendar.MINUTE)
                }
                spans.add(MatchSpan(match.range))
            }
        }

        // 2. Date extraction (if not already set by relative minute/hour/day/week)
        if (!hasDate) {
            // 2a. Explicit ISO yyyy-MM-dd
            val isoDateRegex = Regex("""\b(\d{4})-(\d{1,2})-(\d{1,2})\b""")
            isoDateRegex.find(raw)?.let { match ->
                val y = match.groupValues[1].toIntOrNull()
                val m = match.groupValues[2].toIntOrNull()
                val d = match.groupValues[3].toIntOrNull()
                if (y != null && m != null && d != null && m in 1..12 && d in 1..31) {
                    workingCal.set(Calendar.YEAR, y)
                    workingCal.set(Calendar.MONTH, m - 1)
                    workingCal.set(Calendar.DAY_OF_MONTH, d)
                    hasDate = true
                    spans.add(MatchSpan(match.range))
                }
            }
        }

        if (!hasDate) {
            // 2b. Month DD(, YYYY)? e.g. "November 25", "Nov 25th, 2026"
            val monthDayRegex = Regex(
                """\b(?:on\s+)?($monthNamesRegex)\s+(\d{1,2})(?:st|nd|rd|th)?(?:\s*,?\s*(\d{4}))?\b""",
                RegexOption.IGNORE_CASE
            )
            monthDayRegex.find(raw)?.let { match ->
                val month = monthMap[match.groupValues[1].lowercase(Locale.US)]
                val day = match.groupValues[2].toIntOrNull()
                val explicitYear = match.groupValues[3].toIntOrNull()
                if (month != null && day != null && day in 1..31) {
                    applyMonthDay(workingCal, nowMillis, month, day, explicitYear)
                    hasDate = true
                    spans.add(MatchSpan(match.range))
                }
            }
        }

        if (!hasDate) {
            // 2c. DD Month(, YYYY)? e.g. "25 November", "25th Nov 2026"
            val dayMonthRegex = Regex(
                """\b(?:on\s+)?(\d{1,2})(?:st|nd|rd|th)?\s+(?:of\s+)?($monthNamesRegex)(?:\s*,?\s*(\d{4}))?\b""",
                RegexOption.IGNORE_CASE
            )
            dayMonthRegex.find(raw)?.let { match ->
                val day = match.groupValues[1].toIntOrNull()
                val month = monthMap[match.groupValues[2].lowercase(Locale.US)]
                val explicitYear = match.groupValues[3].toIntOrNull()
                if (month != null && day != null && day in 1..31) {
                    applyMonthDay(workingCal, nowMillis, month, day, explicitYear)
                    hasDate = true
                    spans.add(MatchSpan(match.range))
                }
            }
        }

        if (!hasDate) {
            // 2d. Slash date DD/MM(/YYYY)? or MM/DD(/YYYY)? — interpret conservatively
            val slashRegex = Regex("""\b(?:on\s+)?(\d{1,2})/(\d{1,2})(?:/(\d{2,4}))?\b""")
            slashRegex.find(raw)?.let { match ->
                val p1 = match.groupValues[1].toIntOrNull()
                val p2 = match.groupValues[2].toIntOrNull()
                val yRaw = match.groupValues[3].toIntOrNull()
                if (p1 != null && p2 != null) {
                    val (day, monthNum) = if (p1 > 12 && p2 in 1..12) {
                        p1 to p2
                    } else if (p2 > 12 && p1 in 1..12) {
                        p2 to p1
                    } else if (p1 in 1..31 && p2 in 1..12) {
                        p1 to p2
                    } else {
                        -1 to -1
                    }
                    if (day in 1..31 && monthNum in 1..12) {
                        val explicitYear = yRaw?.let { if (it < 100) 2000 + it else it }
                        applyMonthDay(workingCal, nowMillis, monthNum - 1, day, explicitYear)
                        hasDate = true
                        spans.add(MatchSpan(match.range))
                    }
                }
            }
        }

        if (!hasDate) {
            // 2e. Relative day keywords: today, tonight, tomorrow, day after tomorrow
            val dayAfterRegex = Regex("""\b(?:on\s+)?day\s+after\s+tomorrow\b""", RegexOption.IGNORE_CASE)
            val tomorrowRegex = Regex("""\b(?:on\s+)?(?:tomorrow|tmrw)\b""", RegexOption.IGNORE_CASE)
            val todayRegex = Regex("""\b(?:on\s+)?today\b""", RegexOption.IGNORE_CASE)
            val tonightRegex = Regex("""\btonight\b""", RegexOption.IGNORE_CASE)

            when {
                dayAfterRegex.containsMatchIn(raw) -> {
                    val m = dayAfterRegex.find(raw)!!
                    workingCal.add(Calendar.DAY_OF_YEAR, 2)
                    hasDate = true
                    spans.add(MatchSpan(m.range))
                }
                tomorrowRegex.containsMatchIn(raw) -> {
                    val m = tomorrowRegex.find(raw)!!
                    workingCal.add(Calendar.DAY_OF_YEAR, 1)
                    hasDate = true
                    spans.add(MatchSpan(m.range))
                }
                todayRegex.containsMatchIn(raw) -> {
                    val m = todayRegex.find(raw)!!
                    hasDate = true
                    spans.add(MatchSpan(m.range))
                }
                tonightRegex.containsMatchIn(raw) -> {
                    val m = tonightRegex.find(raw)!!
                    hasDate = true
                    if (!hasTime) {
                        parsedHour = 20
                        parsedMinute = 0
                        hasTime = true
                    }
                    spans.add(MatchSpan(m.range))
                }
            }
        }

        if (!hasDate) {
            // 2f. Weekday names: "next Monday", "this Friday", "on Wednesday"
            val weekdayRegex = Regex(
                """\b(?:(this|next|on)\s+)?($weekdayNamesRegex)\b""",
                RegexOption.IGNORE_CASE
            )
            weekdayRegex.find(raw)?.let { match ->
                val modifier = match.groupValues[1].lowercase(Locale.US)
                val targetDow = weekdayMap[match.groupValues[2].lowercase(Locale.US)]
                if (targetDow != null) {
                    val todayDow = workingCal.get(Calendar.DAY_OF_WEEK)
                    var daysAhead = (targetDow - todayDow + 7) % 7
                    if (daysAhead == 0) daysAhead = 7
                    if (modifier == "next" && daysAhead < 7 && targetDow != todayDow) {
                        // Keep within upcoming week or next week cleanly
                    }
                    workingCal.add(Calendar.DAY_OF_YEAR, daysAhead)
                    hasDate = true
                    spans.add(MatchSpan(match.range))
                }
            }
        }

        // 3. Time extraction
        if (!hasTime || (parsedHour == 20 && parsedMinute == 0 && raw.contains("tonight", ignoreCase = true))) {
            // 3a. 12-hour format with AM/PM: "12 PM", "12:00 p.m.", "8 AM", "8:30am", "at 6 PM"
            val amPmRegex = Regex(
                """\b(?:at\s+)?(\d{1,2})(?::(\d{2}))?\s*(a\.?\s*m\.?|p\.?\s*m\.?)\b""",
                RegexOption.IGNORE_CASE
            )
            amPmRegex.find(raw)?.let { match ->
                val rawHour = match.groupValues[1].toIntOrNull()
                val rawMin = match.groupValues[2].ifBlank { "0" }.toIntOrNull()
                val period = match.groupValues[3].lowercase(Locale.US).replace(".", "").replace(" ", "")
                if (rawHour != null && rawHour in 1..12 && rawMin != null && rawMin in 0..59) {
                    val hour24 = when {
                        period == "am" && rawHour == 12 -> 0
                        period == "am" -> rawHour
                        period == "pm" && rawHour == 12 -> 12
                        else -> rawHour + 12
                    }
                    parsedHour = hour24
                    parsedMinute = rawMin
                    hasTime = true
                    spans.add(MatchSpan(match.range))
                }
            }
        }

        if (!hasTime) {
            // 3b. 24-hour HH:MM e.g. "at 14:30", "18:00"
            val time24Regex = Regex("""\b(?:at\s+)?([01]?\d|2[0-3]):([0-5]\d)\b""", RegexOption.IGNORE_CASE)
            time24Regex.find(raw)?.let { match ->
                val h = match.groupValues[1].toIntOrNull()
                val m = match.groupValues[2].toIntOrNull()
                if (h != null && m != null) {
                    parsedHour = h
                    parsedMinute = m
                    hasTime = true
                    spans.add(MatchSpan(match.range))
                }
            }
        }

        if (!hasTime) {
            // 3c. "at <hour>" without am/pm e.g. "at 6", "at 10"
            val atHourRegex = Regex("""\bat\s+(\d{1,2})\b""", RegexOption.IGNORE_CASE)
            atHourRegex.find(raw)?.let { match ->
                val h = match.groupValues[1].toIntOrNull()
                if (h != null && h in 0..23) {
                    parsedHour = if (h in 1..6) h + 12 else h
                    parsedMinute = 0
                    hasTime = true
                    spans.add(MatchSpan(match.range))
                }
            }
        }

        if (!hasTime) {
            // 3d. Time-of-day keywords
            val todRegex = Regex("""\b(?:at\s+|in\s+the\s+|this\s+)?(noon|midnight|morning|afternoon|evening)\b""", RegexOption.IGNORE_CASE)
            todRegex.find(raw)?.let { match ->
                val word = match.groupValues[1].lowercase(Locale.US)
                when (word) {
                    "noon" -> { parsedHour = 12; parsedMinute = 0 }
                    "midnight" -> { parsedHour = 0; parsedMinute = 0 }
                    "morning" -> { parsedHour = 9; parsedMinute = 0 }
                    "afternoon" -> { parsedHour = 14; parsedMinute = 0 }
                    "evening" -> { parsedHour = 18; parsedMinute = 0 }
                }
                hasTime = true
                spans.add(MatchSpan(match.range))
            }
        }

        // If time was extracted without a date, set date to Today (or Tomorrow if that time already passed today)
        if (hasTime && !hasDate && parsedHour != null) {
            val testCal = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                set(Calendar.HOUR_OF_DAY, parsedHour!!)
                set(Calendar.MINUTE, parsedMinute ?: 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (testCal.timeInMillis <= nowMillis) {
                workingCal.add(Calendar.DAY_OF_YEAR, 1)
            }
            hasDate = true
        }

        // Normalize workingCal date to start/exact time if available
        val finalDateMs = if (hasDate) {
            workingCal.apply {
                if (hasTime && parsedHour != null) {
                    set(Calendar.HOUR_OF_DAY, parsedHour!!)
                    set(Calendar.MINUTE, parsedMinute ?: 0)
                }
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        } else {
            null
        }

        val cleanedDescription = buildCleanDescription(raw, spans)

        return ParsedReminderPreview(
            originalText = raw,
            description = cleanedDescription,
            dateEpochMs = finalDateMs,
            hour = parsedHour,
            minute = parsedMinute,
            hasDate = hasDate,
            hasTime = hasTime
        )
    }

    private fun applyMonthDay(
        cal: Calendar,
        nowMillis: Long,
        month: Int,
        day: Int,
        explicitYear: Int?
    ) {
        val nowCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        cal.set(Calendar.MONTH, month)
        cal.set(Calendar.DAY_OF_MONTH, day)
        if (explicitYear != null) {
            cal.set(Calendar.YEAR, explicitYear)
        } else {
            cal.set(Calendar.YEAR, nowCal.get(Calendar.YEAR))
            // Compare day-of-year so today's date stays this year while past dates roll to next year
            val targetDay = Calendar.getInstance().apply {
                timeInMillis = cal.timeInMillis
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
            }
            if (targetDay.timeInMillis < nowMillis) {
                cal.add(Calendar.YEAR, 1)
            }
        }
    }

    private fun buildCleanDescription(raw: String, spans: List<MatchSpan>): String {
        if (spans.isEmpty()) return raw

        val removeFlags = BooleanArray(raw.length)
        for (span in spans) {
            for (i in span.range) {
                if (i in removeFlags.indices) removeFlags[i] = true
            }
        }

        val sb = StringBuilder()
        for (i in raw.indices) {
            if (!removeFlags[i]) sb.append(raw[i])
        }

        var cleaned = sb.toString()
            .replace(Regex("""\s+"""), " ")
            .trim()
            .trim(',', '-', ':', ';')
            .trim()

        // Strip common leading/trailing filler phrases left over after date/time removal
        cleaned = cleaned
            .replace(Regex("""^(?:remind\s+me\s+(?:to\s+)?|to\s+|at\s+|on\s+|by\s+|for\s+)""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+(?:at|on|by|for)$""", RegexOption.IGNORE_CASE), "")
            .trim()

        if (cleaned.isBlank()) return raw

        return cleaned.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
    }
}
