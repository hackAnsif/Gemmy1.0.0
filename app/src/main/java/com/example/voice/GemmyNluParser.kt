package com.example.voice

import com.example.ai.GeminiApiService
import com.example.data.model.ActionType
import com.example.data.model.GemmyParsedAction
import com.example.data.model.RepeatSchedule
import java.util.Calendar
import java.util.regex.Pattern

object GemmyNluParser {

    suspend fun parseCommand(
        input: String,
        useOnlineAi: Boolean,
        customApiKey: String = ""
    ): GemmyParsedAction {
        val trimmed = input.trim()

        // 1. If online AI enabled, try Gemini first
        if (useOnlineAi) {
            val aiResult = GeminiApiService.parseNaturalLanguageCommand(trimmed, customApiKey)
            if (aiResult != null && aiResult.actionType != ActionType.UNKNOWN) {
                return aiResult
            }
        }

        // 2. Fall back to robust Offline Rule-Based Parser (English & Malayalam)
        return parseOffline(trimmed)
    }

    fun parseOffline(input: String): GemmyParsedAction {
        val lower = input.lowercase()

        // Check if Alarm
        val isAlarm = lower.contains("alarm") || lower.contains("അലാറം") || lower.contains("wake me")

        // Check if Learning / Revision / Vocabulary / Concept
        val isLearning = lower.contains("word") || lower.contains("meaning") ||
                lower.contains("revise") || lower.contains("revision") ||
                lower.contains("concept") || lower.contains("formula") ||
                lower.contains("പഠി") || lower.contains("വാക്ക്") ||
                lower.contains("ഫോർമുല") || lower.contains("അർത്ഥം")

        // Parse Time (e.g., "8 pm", "6:30 am", "7 മണിക്ക്", "രാവിലെ 7")
        val parsedTime = extractTime(input)

        // Parse Date (e.g. "tomorrow", "after two weeks", "നാളെ", "രണ്ട് ആഴ്ച കഴിഞ്ഞ്")
        val parsedDate = extractDate(input)

        // Check Repeat (e.g. "every sunday", "every day", "എല്ലാ ദിവസവും")
        val repeatSchedule = extractRepeat(input)

        val actionType = when {
            isAlarm -> ActionType.CREATE_ALARM
            isLearning -> ActionType.CREATE_LEARNING_ITEM
            lower.contains("remind") || lower.contains("reminder") || lower.contains("ഓർമ്മിപ്പി") -> ActionType.CREATE_REMINDER
            else -> ActionType.CREATE_REMINDER
        }

        // Extract title, meaning, example
        var title = extractTitle(input, actionType)
        var meaning = ""
        var example = ""

        if (isLearning) {
            val meaningIndex = input.indexOf("meaning:", ignoreCase = true)
            val meansIndex = input.indexOf(" means ", ignoreCase = true)
            if (meaningIndex != -1) {
                meaning = input.substring(meaningIndex + 8).trim()
            } else if (meansIndex != -1) {
                meaning = input.substring(meansIndex + 7).trim()
                val wordPart = input.substring(0, meansIndex)
                val cleanWord = wordPart.replace(Regex("(?i)save\\s+word|word|save"), "").trim()
                if (cleanWord.isNotBlank()) {
                    title = cleanWord
                }
            }
        }

        // Check Ambiguity / Missing Time:
        // E.g. User said "Remind me next Friday" or "നാളെ ഓർമ്മിപ്പിക്കണം" without time
        var needsClarification = false
        var clarificationPrompt = ""

        if (actionType != ActionType.QUERY_SCHEDULE) {
            if (parsedTime == null && actionType == ActionType.CREATE_ALARM) {
                needsClarification = true
                clarificationPrompt = "What time should I set the alarm for?"
            } else if (parsedTime == null && parsedDate != null) {
                needsClarification = true
                clarificationPrompt = "What time should I remind you?"
            } else if (parsedTime == null && parsedDate == null && !isLearning) {
                needsClarification = true
                clarificationPrompt = "When and what time should I remind you?"
            }
        }

        val defaultHour = parsedTime?.first ?: 9
        val defaultMinute = parsedTime?.second ?: 0
        val targetDateMs = parsedDate ?: run {
            // Default tomorrow if alarm, or today if later
            val cal = Calendar.getInstance()
            if (parsedTime != null && (parsedTime.first < cal.get(Calendar.HOUR_OF_DAY) ||
                        (parsedTime.first == cal.get(Calendar.HOUR_OF_DAY) && parsedTime.second <= cal.get(Calendar.MINUTE)))) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            cal.timeInMillis
        }

        val category = when {
            isLearning && (lower.contains("word") || lower.contains("vocabulary")) -> "English Vocabulary"
            isLearning && (lower.contains("formula") || lower.contains("math")) -> "Mathematics"
            isLearning && (lower.contains("mechanics") || lower.contains("engineer")) -> "Engineering"
            isLearning -> "Study Notes"
            lower.contains("assignment") || lower.contains("class") -> "Study"
            lower.contains("water") || lower.contains("medicine") -> "Health"
            else -> "General"
        }

        return GemmyParsedAction(
            actionType = actionType,
            title = title.ifBlank { "New Reminder" },
            meaning = meaning,
            example = example,
            description = input,
            category = category,
            targetDateEpochMs = targetDateMs,
            timeHour = defaultHour,
            timeMinute = defaultMinute,
            repeatSchedule = repeatSchedule,
            isSpacedRepetition = isLearning,
            needsClarification = needsClarification,
            clarificationPrompt = clarificationPrompt,
            originalText = input,
            confidenceSource = "Offline Parser"
        )
    }

    private fun extractTime(text: String): Pair<Int, Int>? {
        val lower = text.lowercase()

        // 1. Matches "6:30 am", "8:00 pm", "8 pm", "7 am"
        val timeRegex = Pattern.compile("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm|a\\.m\\.|p\\.m\\.)", Pattern.CASE_INSENSITIVE)
        val matcher = timeRegex.matcher(lower)
        if (matcher.find()) {
            var hour = matcher.group(1)?.toIntOrNull() ?: 9
            val minute = matcher.group(2)?.toIntOrNull() ?: 0
            val amPm = matcher.group(3)?.lowercase() ?: ""

            if ((amPm.contains("pm")) && hour < 12) {
                hour += 12
            } else if ((amPm.contains("am")) && hour == 12) {
                hour = 0
            }
            return Pair(hour, minute)
        }

        // 2. Matches 24h format "at 20:00" or "at 14:30"
        val h24Regex = Pattern.compile("\\b(\\d{1,2}):(\\d{2})\\b")
        val h24Matcher = h24Regex.matcher(lower)
        if (h24Matcher.find()) {
            val hour = h24Matcher.group(1)?.toIntOrNull() ?: 9
            val minute = h24Matcher.group(2)?.toIntOrNull() ?: 0
            if (hour in 0..23 && minute in 0..59) {
                return Pair(hour, minute)
            }
        }

        // 3. Malayalam time patterns: "7 മണിക്ക്" (at 7 o'clock), "രാവിലെ 7" (morning 7), "രാത്രി 8" (night 8), "വൈകുന്നേരം 7" (evening 7)
        val mlNumberMap = mapOf(
            "ഒന്ന്" to 1, "രണ്ട്" to 2, "മൂന്ന്" to 3, "നാല്" to 4, "അഞ്ച്" to 5,
            "ആറ്" to 6, "ഏഴ്" to 7, "എട്ട്" to 8, "ഒമ്പത്" to 9, "പത്ത്" to 10,
            "പതിനൊന്ന്" to 11, "പന്ത്രണ്ട്" to 12
        )

        var hour = 0
        var isPm = lower.contains("രാത്രി") || lower.contains("വൈകുന്നേരം") || lower.contains("ഉച്ചയ്ക്ക്")
        val isAm = lower.contains("രാവിലെ")

        val mlNumRegex = Pattern.compile("(\\d{1,2})(?::(\\d{2}))?\\s*(?:മണിക്ക്|ന്|മണി)")
        val mlNumMatcher = mlNumRegex.matcher(lower)
        if (mlNumMatcher.find()) {
            hour = mlNumMatcher.group(1)?.toIntOrNull() ?: 7
            val min = mlNumMatcher.group(2)?.toIntOrNull() ?: 0
            if (isPm && hour < 12) hour += 12
            if (isAm && hour == 12) hour = 0
            return Pair(hour, min)
        }

        for ((word, num) in mlNumberMap) {
            if (lower.contains(word)) {
                hour = num
                if (isPm && hour < 12) hour += 12
                return Pair(hour, 0)
            }
        }

        return null
    }

    private fun extractDate(text: String): Long? {
        val lower = text.lowercase()
        val cal = Calendar.getInstance()

        // 1. Tomorrow / നാളെ
        if (lower.contains("tomorrow") || lower.contains("നാളെ")) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            return cal.timeInMillis
        }

        // 2. Relative weeks: "after two weeks" / "in 2 weeks" / "രണ്ട് ആഴ്ച കഴിഞ്ഞ്" / "ഒരാഴ്ച കഴിഞ്ഞ്"
        if (lower.contains("two weeks") || lower.contains("2 weeks") || lower.contains("രണ്ട് ആഴ്ച")) {
            cal.add(Calendar.DAY_OF_YEAR, 14)
            return cal.timeInMillis
        }
        if (lower.contains("one week") || lower.contains("1 week") || lower.contains("a week") || lower.contains("ഒരാഴ്ച")) {
            cal.add(Calendar.DAY_OF_YEAR, 7)
            return cal.timeInMillis
        }

        // 3. Days offset: "in 3 days" / "after 3 days" / "3 ദിവസം കഴിഞ്ഞ്"
        val daysRegex = Pattern.compile("(?:in|after)\\s+(\\d+)\\s+days", Pattern.CASE_INSENSITIVE)
        val daysMatcher = daysRegex.matcher(lower)
        if (daysMatcher.find()) {
            val days = daysMatcher.group(1)?.toIntOrNull() ?: 1
            cal.add(Calendar.DAY_OF_YEAR, days)
            return cal.timeInMillis
        }

        // 4. Hours offset: "in 2 hours"
        val hoursRegex = Pattern.compile("in\\s+(\\d+)\\s+hours?", Pattern.CASE_INSENSITIVE)
        val hoursMatcher = hoursRegex.matcher(lower)
        if (hoursMatcher.find()) {
            val hours = hoursMatcher.group(1)?.toIntOrNull() ?: 2
            cal.add(Calendar.HOUR_OF_DAY, hours)
            return cal.timeInMillis
        }

        // 5. Day of week: "next monday", "every sunday"
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

    private fun extractRepeat(text: String): RepeatSchedule {
        val lower = text.lowercase()
        return when {
            lower.contains("every day") || lower.contains("daily") || lower.contains("എല്ലാ ദിവസവും") -> RepeatSchedule.DAILY
            lower.contains("every weekday") || lower.contains("weekdays") -> RepeatSchedule.WEEKDAYS
            lower.contains("every sunday") || lower.contains("every monday") ||
                    lower.contains("every week") || lower.contains("weekly") ||
                    lower.contains("എല്ലാ ആഴ്ചയും") || lower.contains("എല്ലാ ഞായറാഴ്ചയും") -> RepeatSchedule.WEEKLY
            lower.contains("every month") || lower.contains("monthly") -> RepeatSchedule.MONTHLY
            else -> RepeatSchedule.NONE
        }
    }

    private fun extractTitle(text: String, actionType: ActionType): String {
        var clean = text
        val prefixesToRemove = listOf(
            "remind me to revise the word",
            "remind me to revise",
            "remind me to learn the word",
            "remind me to learn",
            "remind me to submit",
            "remind me to",
            "remind me on",
            "remind me",
            "set an alarm for",
            "set alarm for",
            "set an alarm",
            "set alarm",
            "ഓർമ്മിപ്പിക്കണം",
            "പഠിക്കാൻ ഒരു reminder വേണം",
            "പഠിക്കാൻ ഓർമ്മിപ്പിക്കണം",
            "അലാറം വെക്കണം",
            "അലാറം വെക്കുക"
        )
        for (prefix in prefixesToRemove) {
            clean = clean.replace(Regex("(?i)\\b$prefix\\b"), "")
        }

        // Remove date and time suffixes
        clean = clean.replace(Regex("(?i)after\\s+\\w+\\s+weeks?"), "")
        clean = clean.replace(Regex("(?i)in\\s+\\d+\\s+days?"), "")
        clean = clean.replace(Regex("(?i)at\\s+\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)?"), "")
        clean = clean.replace(Regex("(?i)tomorrow\\s*(?:morning|evening|afternoon)?"), "")
        clean = clean.replace(Regex("(?i)നാളെ\\s*(?:രാവിലെ|വൈകുന്നേരം)?"), "")
        clean = clean.replace(Regex("(?i)\\d+\\s*(?:മണിക്ക്|ന്)"), "")

        clean = clean.replace(Regex("(?i)meaning:.*"), "")
        clean = clean.replace(Regex("(?i)means.*"), "")

        val trimmed = clean.trim(' ', ',', '.', '-', ':')
        return if (trimmed.isNotBlank()) {
            trimmed.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        } else {
            when (actionType) {
                ActionType.CREATE_LEARNING_ITEM -> "Learning Concept"
                ActionType.CREATE_ALARM -> "Alarm"
                else -> "Reminder"
            }
        }
    }
}
