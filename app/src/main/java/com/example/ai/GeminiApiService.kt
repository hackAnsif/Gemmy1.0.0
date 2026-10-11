package com.example.ai

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.ActionType
import com.example.data.model.GemmyParsedAction
import com.example.data.model.RepeatSchedule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

object GeminiApiService {

    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun parseNaturalLanguageCommand(
        prompt: String,
        customApiKey: String = ""
    ): GemmyParsedAction? = withContext(Dispatchers.IO) {
        val apiKey = when {
            customApiKey.isNotBlank() -> customApiKey.trim()
            BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" -> BuildConfig.GEMINI_API_KEY
            else -> ""
        }

        if (apiKey.isBlank()) {
            Log.d("GeminiApi", "No Gemini API key available, using offline parser.")
            return@withContext null
        }

        val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
        val currentTimeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Calendar.getInstance().time)

        val systemInstructionText = """
You are Gemmy, a smart personal memory & reminder assistant.
Current local device date: $todayDateStr, current time: $currentTimeStr.
The user speaks in English, Malayalam, or mixed Malayalam-English (Manglish).
Extract the user's intent into a STRICT JSON object with these exact keys:
- "action": One of ["CREATE_LEARNING_ITEM", "CREATE_REMINDER", "CREATE_ALARM", "QUERY_SCHEDULE", "UNKNOWN"]
- "title": Short title, term, or concept (e.g. "Ephemeral", "Fluid Mechanics", "Submit Assignment")
- "meaning": Meaning or formula if provided (e.g. "Lasting for a very short time"), else ""
- "example": Example sentence or context if provided, else ""
- "description": Additional notes or description, else ""
- "category": Category (e.g. "English Vocabulary", "Engineering", "Study", "Personal", "Health", "General")
- "date": Date in "YYYY-MM-DD" format. If user says "tomorrow", calculate based on current date. If user says "in two weeks", calculate +14 days. If user says "every Sunday", calculate next Sunday. If ambiguous or missing date, set null.
- "time": Time in 24-hour "HH:mm" format (e.g. "20:00", "07:30"). If user didn't specify a time, set null.
- "repeat": One of ["NONE", "DAILY", "WEEKDAYS", "WEEKLY", "MONTHLY"]
- "needsClarification": Boolean (true if essential date or time is missing or highly ambiguous, e.g. "remind me next Friday" without time)
- "clarificationPrompt": A concise question to ask the user if needsClarification is true (e.g. "What time should I remind you?"), else ""

Output ONLY valid raw JSON without markdown formatting.
""".trimIndent()

        val requestJson = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "User command: \"$prompt\"")
                        })
                    })
                })
            }
            put("contents", contentsArray)

            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", systemInstructionText)
                    })
                })
            })

            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.1)
            })
        }

        try {
            val url = "$BASE_URL?key=$apiKey"
            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w("GeminiApi", "Gemini HTTP error ${response.code}: ${response.message}")
                return@withContext null
            }

            val responseBody = response.body?.string() ?: return@withContext null
            val responseJson = JSONObject(responseBody)
            val candidates = responseJson.optJSONArray("candidates") ?: return@withContext null
            val firstCandidate = candidates.optJSONObject(0) ?: return@withContext null
            val content = firstCandidate.optJSONObject("content") ?: return@withContext null
            val parts = content.optJSONArray("parts") ?: return@withContext null
            val textPart = parts.optJSONObject(0)?.optString("text") ?: return@withContext null

            // Parse response JSON
            val cleanJson = textPart.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val parsedObj = JSONObject(cleanJson)

            val actionStr = parsedObj.optString("action", "UNKNOWN")
            val actionType = try {
                ActionType.valueOf(actionStr)
            } catch (e: Exception) {
                ActionType.UNKNOWN
            }

            val title = parsedObj.optString("title", prompt.take(30))
            val meaning = parsedObj.optString("meaning", "")
            val example = parsedObj.optString("example", "")
            val description = parsedObj.optString("description", "")
            val category = parsedObj.optString("category", "General")
            val dateStr = parsedObj.optString("date", "")
            val timeStr = parsedObj.optString("time", "")
            val repeatStr = parsedObj.optString("repeat", "NONE")
            val needsClarification = parsedObj.optBoolean("needsClarification", false)
            val clarificationPrompt = parsedObj.optString("clarificationPrompt", "")

            var targetDateMs: Long? = null
            var hour: Int? = null
            var minute: Int? = null

            if (dateStr.isNotBlank() && dateStr != "null") {
                try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val d = sdf.parse(dateStr)
                    targetDateMs = d?.time
                } catch (e: Exception) {
                    Log.e("GeminiApi", "Error parsing date: $dateStr")
                }
            }

            if (timeStr.isNotBlank() && timeStr != "null") {
                val partsTime = timeStr.split(":")
                if (partsTime.size >= 2) {
                    hour = partsTime[0].toIntOrNull()
                    minute = partsTime[1].toIntOrNull()
                }
            }

            val repeat = try {
                RepeatSchedule.valueOf(repeatStr)
            } catch (e: Exception) {
                RepeatSchedule.NONE
            }

            GemmyParsedAction(
                actionType = actionType,
                title = title,
                meaning = meaning,
                example = example,
                description = description,
                category = category,
                targetDateEpochMs = targetDateMs,
                timeHour = hour,
                timeMinute = minute,
                repeatSchedule = repeat,
                isSpacedRepetition = actionType == ActionType.CREATE_LEARNING_ITEM,
                needsClarification = needsClarification,
                clarificationPrompt = clarificationPrompt,
                originalText = prompt,
                confidenceSource = "Gemini AI"
            )
        } catch (e: Exception) {
            Log.e("GeminiApi", "Error calling Gemini: ${e.message}")
            null
        }
    }
}
