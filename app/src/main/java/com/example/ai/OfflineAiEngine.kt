package com.example.ai

import com.example.data.model.ActionType
import com.example.data.model.GemmyParsedAction
import com.example.data.model.RepeatSchedule
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

/**
 * 100% Offline AI Natural Language Understanding (NLU) & Intent Engine.
 * Operates entirely on-device without internet or external cloud APIs.
 *
 * Capabilities:
 * - Intent Classification: Alarms, Reminders, Learning Memory/Vocabulary Cards.
 * - Temporal Entity Extraction: Dates (Tomorrow, relative days, weekdays, next Friday),
 *   Times (12h with AM/PM like "12:00 p.m.", 24h, relative offsets like "in 10 mins"),
 *   Malayalam time and dates.
 * - Concept & Definition Extraction: "parents means...", "formula of X is Y", "word ephemeral", "X means Y", "വാക്ക് എന്നാൽ...".
 * - Smart Categorization: Study, Health, Work, Personal, Vocabulary, Formulas.
 */
object OfflineAiEngine {

    fun parse(input: String): GemmyParsedAction {
        val raw = input.trim()
        val lower = raw.lowercase(Locale.ROOT)

        // 1. Detect Intent Type
        val isLearningConcept = detectLearningIntent(lower)
        val isAlarm = !isLearningConcept && detectAlarmIntent(lower)
        val isReminder = !isLearningConcept && !isAlarm

        // 2. Extract Temporal Entities (Date & Time)
        val parsedTime = extractTime(lower)
        val parsedDate = extractDate(lower)
        val repeatSchedule = extractRepeat(lower)

        val actionType = when {
            isLearningConcept -> ActionType.CREATE_LEARNING_ITEM
            isAlarm -> ActionType.CREATE_ALARM
            else -> ActionType.CREATE_REMINDER
        }

        // 3. Extract Titles, Definitions, Meanings
        val title: String
        var meaning = ""
        var example = ""

        if (isLearningConcept) {
            val conceptPair = extractConceptAndMeaning(raw)
            title = conceptPair.first.ifBlank { "Learning Concept" }
            meaning = conceptPair.second.ifBlank { "Definition for ${title.ifBlank { "concept" }}" }
        } else {
            title = extractCleanTaskTitle(raw, actionType)
        }

        // 4. Determine Smart Categories
        val category = determineCategory(lower, isLearningConcept)

        // 5. Calculate Target Timestamps
        val cal = Calendar.getInstance()
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val currentMinute = cal.get(Calendar.MINUTE)

        val defaultHour = parsedTime?.first ?: when (actionType) {
            ActionType.CREATE_ALARM -> 7 // Default 7:00 AM for alarm if unspecified
            else -> 9 // Default 9:00 AM for reminders
        }
        val defaultMinute = parsedTime?.second ?: 0

        val targetDateMs = when {
            parsedDate != null -> {
                val dateCal = Calendar.getInstance().apply { timeInMillis = parsedDate }
                dateCal.set(Calendar.HOUR_OF_DAY, defaultHour)
                dateCal.set(Calendar.MINUTE, defaultMinute)
                dateCal.set(Calendar.SECOND, 0)
                dateCal.set(Calendar.MILLISECOND, 0)
                dateCal.timeInMillis
            }
            parsedTime != null -> {
                // If only time was provided (e.g. "at 12:00 pm")
                val timeCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, defaultHour)
                    set(Calendar.MINUTE, defaultMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                // If time already passed today, advance to tomorrow
                if (timeCal.timeInMillis <= System.currentTimeMillis()) {
                    timeCal.add(Calendar.DAY_OF_YEAR, 1)
                }
                timeCal.timeInMillis
            }
            actionType == ActionType.CREATE_ALARM -> {
                // Alarm defaults to tomorrow morning
                val alarmCal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, defaultHour)
                    set(Calendar.MINUTE, defaultMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                alarmCal.timeInMillis
            }
            else -> {
                // Default reminder for tomorrow 9 AM if no date/time given
                val reminderCal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, defaultHour)
                    set(Calendar.MINUTE, defaultMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                reminderCal.timeInMillis
            }
        }

        // Check if essential information is missing and requires a follow-up
        var needsClarification = false
        var clarificationPrompt = ""

        if (actionType == ActionType.CREATE_ALARM && parsedTime == null) {
            needsClarification = true
            clarificationPrompt = "What time should I set the alarm for?"
        } else if (actionType == ActionType.CREATE_REMINDER && (title.isBlank() || title.equals("New Task", ignoreCase = true) || title.equals("Reminder", ignoreCase = true))) {
            needsClarification = true
            clarificationPrompt = "What would you like me to remind you about?"
        }

        return GemmyParsedAction(
            actionType = actionType,
            title = title.ifBlank { if (actionType == ActionType.CREATE_ALARM) "Alarm" else "Reminder" },
            meaning = meaning,
            example = example,
            description = raw,
            category = category,
            targetDateEpochMs = targetDateMs,
            timeHour = defaultHour,
            timeMinute = defaultMinute,
            repeatSchedule = repeatSchedule,
            isSpacedRepetition = isLearningConcept,
            needsClarification = needsClarification,
            clarificationPrompt = clarificationPrompt,
            originalText = raw,
            confidenceSource = "Offline AI Model (On-Device)"
        )
    }

    private fun detectLearningIntent(lower: String): Boolean {
        val trimmed = lower.trim()
        // Matches definitions like: "parents means...", "parents means", "parents meaning", "formula of kinetic energy is...", "revise the word ephemeral"
        if (trimmed.endsWith(" means") || trimmed.contains(" means ") || trimmed.contains(" means:") ||
            trimmed.endsWith(" mean") || trimmed.contains(" mean ") || trimmed.contains(" meant ") ||
            trimmed.endsWith(" meaning") || trimmed.contains(" meaning ") || trimmed.contains(" meaning:") ||
            trimmed.contains("meaning of ") || trimmed.contains("definition of ") || trimmed.contains("defined as ") ||
            trimmed.contains("formula of ") || trimmed.contains("formula for ") ||
            trimmed.contains("revise the word") || trimmed.contains("the word") || trimmed.contains("save word") ||
            trimmed.contains("learn word") || trimmed.contains("vocabulary") || trimmed.contains("concept") ||
            trimmed.contains("പഠി") || trimmed.contains("വാക്ക്") || trimmed.contains("അർത്ഥം") ||
            trimmed.contains("എന്നാൽ")
        ) {
            return true
        }
        return false
    }

    private fun detectAlarmIntent(lower: String): Boolean {
        return lower.contains("alarm") || lower.contains("wake me up") || lower.contains("wake up") ||
                lower.contains("അലാറം") || lower.contains("എഴുന്നേൽക്ക")
    }

    private fun extractConceptAndMeaning(text: String): Pair<String, String> {
        val lower = text.lowercase(Locale.ROOT)

        // 1. "Formula of X is Y"
        val formulaRegex = Pattern.compile("(?i)formula\\s+(?:of|for)\\s+(.+?)\\s+is\\s+(.+)")
        val formulaMatcher = formulaRegex.matcher(text)
        if (formulaMatcher.find()) {
            val concept = formulaMatcher.group(1)?.trim() ?: ""
            val meaning = formulaMatcher.group(2)?.trim() ?: ""
            return Pair(cleanTitle(concept), meaning)
        }

        // 2. "Revise the word X" or "the word X"
        val wordRegex = Pattern.compile("(?i)(?:revise\\s+)?(?:the\\s+)?word\\s+([a-zA-Z0-9_-]+)", Pattern.CASE_INSENSITIVE)
        val wordMatcher = wordRegex.matcher(text)
        if (wordMatcher.find()) {
            val word = wordMatcher.group(1)?.trim() ?: ""
            return Pair(cleanTitle(word), "Vocabulary item: $word")
        }

        // 3. Malayalam "X പഠിക്കാൻ" -> concept "X"
        val mlStudyRegex = Pattern.compile("(?i)(?:രാവിലെ|വൈകുന്നേരം|ഉച്ചയ്ക്ക്)?\\s*\\d*\\s*(?:മണിക്ക്)?\\s*(.+?)\\s*പഠിക്കാൻ")
        val mlStudyMatcher = mlStudyRegex.matcher(text)
        if (mlStudyMatcher.find()) {
            val concept = mlStudyMatcher.group(1)?.trim() ?: ""
            return Pair(cleanTitle(concept), "പഠന വിഷയം: $concept")
        }

        // 4. "X means Y" or "X means"
        val meansIndex = lower.indexOf(" means")
        if (meansIndex != -1) {
            val concept = text.substring(0, meansIndex)
                .replace(Regex("(?i)\\b(?:save\\s+word|word|concept|save|learn|what\\s+does|meaning\\s+of)\\b"), "")
                .trim()
            val afterMeans = text.substring(meansIndex + 6).trim()
            val meaning = afterMeans.removePrefix(":").removePrefix("is").removePrefix("are").trim()
            return Pair(cleanTitle(concept), meaning)
        }

        // 5. "Meaning of X is Y" or "Meaning of X"
        val meaningOfRegex = Pattern.compile("(?i)meaning\\s+of\\s+([^:\\s]+(?:\\s+[^:\\s]+)?)(?:\\s+(?:is|means|:)\\s+(.+))?")
        val meaningOfMatcher = meaningOfRegex.matcher(text)
        if (meaningOfMatcher.find()) {
            val concept = meaningOfMatcher.group(1)?.trim() ?: ""
            val meaning = meaningOfMatcher.group(2)?.trim() ?: ""
            return Pair(cleanTitle(concept), meaning)
        }

        // 6. "X meaning: Y" or "X meaning Y"
        val meaningRegex = Pattern.compile("(?i)(.+?)\\s+meaning[:\\s]+(.+)")
        val meaningMatcher = meaningRegex.matcher(text)
        if (meaningMatcher.find()) {
            val concept = meaningMatcher.group(1)?.replace(Regex("(?i)\\b(?:save\\s+word|word|concept|save|learn)\\b"), "")?.trim() ?: ""
            val meaning = meaningMatcher.group(2)?.trim() ?: ""
            return Pair(cleanTitle(concept), meaning)
        }

        // 7. "Definition of X is Y"
        val defRegex = Pattern.compile("(?i)definition\\s+of\\s+(.+?)\\s+is\\s+(.+)")
        val defMatcher = defRegex.matcher(text)
        if (defMatcher.find()) {
            val concept = defMatcher.group(1)?.trim() ?: ""
            val meaning = defMatcher.group(2)?.trim() ?: ""
            return Pair(cleanTitle(concept), meaning)
        }

        // 8. Malayalam "X എന്നാൽ Y"
        val mlMeansIndex = lower.indexOf("എന്നാൽ")
        if (mlMeansIndex != -1) {
            val concept = text.substring(0, mlMeansIndex)
                .replace("വാക്ക്", "").replace("പഠിക്കുക", "").trim()
            val meaning = text.substring(mlMeansIndex + "എന്നാൽ".length).trim()
            return Pair(cleanTitle(concept), meaning)
        }

        // Fallback for single phrase concept
        val clean = text.replace(Regex("(?i)\\b(?:save\\s+word|word|concept|save|learn|meaning)\\b"), "").trim()
        return Pair(cleanTitle(clean), "")
    }

    private fun extractCleanTaskTitle(text: String, actionType: ActionType): String {
        var clean = text

        // Remove standard command frames
        val commandFrames = listOf(
            "tomorrow i have to",
            "tomorrow we have to",
            "tomorrow i need to",
            "tomorrow i must",
            "i have to",
            "i need to",
            "we need to",
            "need to",
            "have to",
            "don't forget to",
            "dont forget to",
            "remember to",
            "remind me to",
            "remind me on",
            "remind me about",
            "remind me",
            "set an alarm tomorrow for",
            "set an alarm for",
            "set alarm for",
            "set an alarm tomorrow at",
            "set an alarm at",
            "set alarm tomorrow at",
            "set alarm at",
            "set an alarm tomorrow",
            "set an alarm",
            "set alarm tomorrow",
            "set alarm",
            "wake me up tomorrow at",
            "wake me up at",
            "wake me up tomorrow",
            "wake me up",
            "wake up at",
            "wake up",
            "alarm at",
            "alarm for",
            "ഓർമ്മിപ്പിക്കണം",
            "അലാറം വെക്കണം",
            "നാളെ ഞാൻ ചെയ്യണം",
            "നാളെ"
        )

        for (frame in commandFrames) {
            clean = clean.replace(Regex("(?i)\\b$frame\\b"), "")
        }

        // Remove temporal clauses at beginning or end
        clean = clean.replace(Regex("(?i)\\bnext\\s+(?:friday|monday|tuesday|wednesday|thursday|saturday|sunday)\\b"), "")
        clean = clean.replace(Regex("(?i)\\bthis\\s+(?:friday|monday|tuesday|wednesday|thursday|saturday|sunday)\\b"), "")
        clean = clean.replace(Regex("(?i)\\bon\\s+(?:friday|monday|tuesday|wednesday|thursday|saturday|sunday)\\b"), "")
        clean = clean.replace(Regex("(?i)\\b(?:friday|monday|tuesday|wednesday|thursday|saturday|sunday)\\b"), "")
        clean = clean.replace(Regex("(?i)\\btomorrow\\s*(?:morning|evening|afternoon|night)?\\b"), "")
        clean = clean.replace(Regex("(?i)\\btonight\\b"), "")
        clean = clean.replace(Regex("(?i)\\btoday\\b"), "")
        clean = clean.replace(Regex("(?i)\\bat\\s+\\d{1,2}(?::\\d{2})?\\s*(?:am|pm|a\\.m\\.|p\\.m\\.)?\\b"), "")
        clean = clean.replace(Regex("(?i)\\b\\d{1,2}(?::\\d{2})?\\s*(?:am|pm|a\\.m\\.|p\\.m\\.)\\b"), "")
        clean = clean.replace(Regex("(?i)\\b(?:in|after)\\s+\\d+\\s+(?:hours?|minutes?|days?|weeks?|mins?|hrs?)\\b"), "")
        clean = clean.replace(Regex("(?i)\\b\\d+\\s*(?:മണിക്ക്|ന്)\\b"), "")

        val trimmed = clean.trim(' ', ',', '.', '-', ':', ';')
        return if (trimmed.isNotBlank()) {
            cleanTitle(trimmed)
        } else {
            when (actionType) {
                ActionType.CREATE_ALARM -> "Alarm"
                ActionType.CREATE_LEARNING_ITEM -> "Learning Concept"
                else -> "New Task"
            }
        }
    }

    private fun cleanTitle(str: String): String {
        val trimmed = str.trim()
        if (trimmed.isEmpty()) return ""
        return trimmed.split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }
    }

    /**
     * Extracts time from speech, supporting:
     * - "12:00 p.m.", "12:00 pm", "12 pm", "12pm", "12:00 a.m.", "7 am", "8:30 a.m.", "8 pm"
     * - "12:00" (24h)
     * - "at noon", "at midnight"
     * - Relative offsets: "in 10 minutes", "in 1 hour", "after 30 mins"
     * - Malayalam: "ഉച്ചയ്ക്ക് 12 മണിക്ക്", "രാവിലെ 7 മണിക്ക്", "വൈകുന്നേരം 6 ന്"
     */
    private fun extractTime(lower: String): Pair<Int, Int>? {
        // 1. Matches "12:00 p.m.", "12:00 pm", "12 pm", "7 am", "8:30 a.m.", "8 pm"
        val timeRegex = Pattern.compile("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm|a\\.m\\.|p\\.m\\.)", Pattern.CASE_INSENSITIVE)
        val matcher = timeRegex.matcher(lower)
        if (matcher.find()) {
            var hour = matcher.group(1)?.toIntOrNull() ?: 9
            val minute = matcher.group(2)?.toIntOrNull() ?: 0
            val amPm = matcher.group(3)?.lowercase(Locale.ROOT) ?: ""

            if (amPm.contains("pm") || amPm.contains("p.m.")) {
                if (hour < 12) hour += 12
            } else if (amPm.contains("am") || amPm.contains("a.m.")) {
                if (hour == 12) hour = 0
            }
            return Pair(hour, minute)
        }

        // 2. Relative offsets: "in 15 minutes", "in 1 hour", "after 30 mins"
        val offsetRegex = Pattern.compile("(?:in|after)\\s+(\\d+)\\s+(minutes?|mins?|hours?|hrs?)", Pattern.CASE_INSENSITIVE)
        val offsetMatcher = offsetRegex.matcher(lower)
        if (offsetMatcher.find()) {
            val amount = offsetMatcher.group(1)?.toIntOrNull() ?: 10
            val unit = offsetMatcher.group(2)?.lowercase(Locale.ROOT) ?: "minutes"
            val cal = Calendar.getInstance()
            if (unit.startsWith("h")) {
                cal.add(Calendar.HOUR_OF_DAY, amount)
            } else {
                cal.add(Calendar.MINUTE, amount)
            }
            return Pair(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
        }

        // 3. Special names: noon, midnight
        if (lower.contains("noon") || lower.contains("midday")) {
            return Pair(12, 0)
        }
        if (lower.contains("midnight")) {
            return Pair(0, 0)
        }

        // 4. Matches "at 12:00" or "at 20:30" (24h)
        val h24Regex = Pattern.compile("\\b(?:at\\s+)?(\\d{1,2}):(\\d{2})\\b")
        val h24Matcher = h24Regex.matcher(lower)
        if (h24Matcher.find()) {
            val hour = h24Matcher.group(1)?.toIntOrNull() ?: 9
            val minute = h24Matcher.group(2)?.toIntOrNull() ?: 0
            if (hour in 0..23 && minute in 0..59) {
                return Pair(hour, minute)
            }
        }

        // 5. Matches "at 12", "at 7", "at 6" (assumes morning for 6-11, evening/afternoon for 1-5, 12 as noon)
        val simpleAtRegex = Pattern.compile("\\bat\\s+(\\d{1,2})\\b")
        val simpleMatcher = simpleAtRegex.matcher(lower)
        if (simpleMatcher.find()) {
            val hourRaw = simpleMatcher.group(1)?.toIntOrNull() ?: 9
            if (hourRaw in 1..12) {
                val hour = if (lower.contains("night") || lower.contains("evening") || (hourRaw in 1..5 && !lower.contains("morning"))) {
                    hourRaw + 12
                } else if (hourRaw == 12 && (lower.contains("noon") || lower.contains("afternoon") || !lower.contains("night"))) {
                    12
                } else {
                    hourRaw
                }
                return Pair(hour, 0)
            }
        }

        // 6. Malayalam Time Parsing
        val isPm = lower.contains("രാത്രി") || lower.contains("വൈകുന്നേരം") || lower.contains("ഉച്ചയ്ക്ക്")
        val isAm = lower.contains("രാവിലെ")
        val mlRegex = Pattern.compile("(\\d{1,2})(?::(\\d{2}))?\\s*(?:മണിക്ക്|ന്|മണി)")
        val mlMatcher = mlRegex.matcher(lower)
        if (mlMatcher.find()) {
            var hour = mlMatcher.group(1)?.toIntOrNull() ?: 7
            val min = mlMatcher.group(2)?.toIntOrNull() ?: 0
            if (isPm && hour < 12) hour += 12
            if (isAm && hour == 12) hour = 0
            return Pair(hour, min)
        }

        return null
    }

    /**
     * Extracts date expressions from speech
     */
    private fun extractDate(lower: String): Long? {
        val cal = Calendar.getInstance()

        // 1. Tomorrow / നാളെ
        if (lower.contains("tomorrow") || lower.contains("നാളെ")) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            return cal.timeInMillis
        }

        // 2. Day after tomorrow / മറ്റന്നാൾ
        if (lower.contains("day after tomorrow") || lower.contains("മറ്റന്നാൾ")) {
            cal.add(Calendar.DAY_OF_YEAR, 2)
            return cal.timeInMillis
        }

        // 3. Today / tonight / ഈ വൈകുന്നേരം
        if (lower.contains("today") || lower.contains("tonight") || lower.contains("ഇന്ന്")) {
            return cal.timeInMillis
        }

        // 4. Relative offset: "in 3 days" / "after 2 days"
        val daysRegex = Pattern.compile("(?:in|after)\\s+(\\d+)\\s+days?")
        val daysMatcher = daysRegex.matcher(lower)
        if (daysMatcher.find()) {
            val days = daysMatcher.group(1)?.toIntOrNull() ?: 1
            cal.add(Calendar.DAY_OF_YEAR, days)
            return cal.timeInMillis
        }

        // 5. Relative weeks: "after two weeks" / "in 2 weeks" / "next week"
        if (lower.contains("two weeks") || lower.contains("2 weeks") || lower.contains("രണ്ട് ആഴ്ച")) {
            cal.add(Calendar.DAY_OF_YEAR, 14)
            return cal.timeInMillis
        }
        if (lower.contains("next week") || lower.contains("one week") || lower.contains("1 week") || lower.contains("ഒരാഴ്ച")) {
            cal.add(Calendar.DAY_OF_YEAR, 7)
            return cal.timeInMillis
        }

        // 6. Day of the week (e.g. "on friday", "next friday", "this sunday")
        val daysOfWeek = listOf(
            "sunday" to Calendar.SUNDAY,
            "monday" to Calendar.MONDAY,
            "tuesday" to Calendar.TUESDAY,
            "wednesday" to Calendar.WEDNESDAY,
            "thursday" to Calendar.THURSDAY,
            "friday" to Calendar.FRIDAY,
            "saturday" to Calendar.SATURDAY
        )
        for ((name, calDay) in daysOfWeek) {
            if (lower.contains(name)) {
                val currentDay = cal.get(Calendar.DAY_OF_WEEK)
                var diff = calDay - currentDay
                if (diff <= 0) diff += 7
                cal.add(Calendar.DAY_OF_YEAR, diff)
                return cal.timeInMillis
            }
        }

        return null
    }

    private fun extractRepeat(lower: String): RepeatSchedule {
        return when {
            lower.contains("every day") || lower.contains("daily") || lower.contains("എല്ലാ ദിവസവും") -> RepeatSchedule.DAILY
            lower.contains("every weekday") || lower.contains("weekdays") -> RepeatSchedule.WEEKDAYS
            lower.contains("every sunday") || lower.contains("every monday") ||
                    lower.contains("every week") || lower.contains("weekly") ||
                    lower.contains("എല്ലാ ആഴ്ചയും") -> RepeatSchedule.WEEKLY
            lower.contains("every month") || lower.contains("monthly") -> RepeatSchedule.MONTHLY
            else -> RepeatSchedule.NONE
        }
    }

    private fun determineCategory(lower: String, isLearning: Boolean): String {
        return when {
            isLearning && (lower.contains("word") || lower.contains("vocabulary") || lower.contains("meaning") || lower.contains("parent")) -> "Vocabulary"
            isLearning && (lower.contains("formula") || lower.contains("math") || lower.contains("physics") || lower.contains("energy")) -> "Formulas & Science"
            isLearning -> "Concepts & Notes"
            lower.contains("assignment") || lower.contains("homework") || lower.contains("exam") ||
                    lower.contains("study") || lower.contains("project") || lower.contains("class") ||
                    lower.contains("submission") || lower.contains("submit") -> "Study"
            lower.contains("medicine") || lower.contains("doctor") || lower.contains("water") ||
                    lower.contains("pills") || lower.contains("gym") || lower.contains("workout") -> "Health"
            lower.contains("meeting") || lower.contains("office") || lower.contains("client") ||
                    lower.contains("email") || lower.contains("report") -> "Work"
            lower.contains("parent") || lower.contains("mom") || lower.contains("dad") ||
                    lower.contains("family") || lower.contains("grocery") || lower.contains("shopping") -> "Personal"
            else -> "General"
        }
    }
}
