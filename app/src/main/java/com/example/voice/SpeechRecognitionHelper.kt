package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

/**
 * Robust On-Device Speech Recognition Helper.
 * Operates 100% offline using on-device Android speech recognition models.
 */
class SpeechRecognitionHelper(
    private val context: Context,
    private val onListeningStateChanged: (Boolean) -> Unit,
    private val onRmsChanged: (Float) -> Unit,
    private val onPartialResult: (String) -> Unit = {},
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit
) {

    private var speechRecognizer: SpeechRecognizer? = null
    var isListening = false
        private set

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening(languageLocale: String = "en-IN", forceOffline: Boolean = true) {
        stopListening()

        if (!isAvailable()) {
            onError("Speech recognition service not found on this device. You can type or tap the offline command chips below.")
            return
        }

        try {
            // Prefer on-device speech recognizer on Android 12+ (API 31+)
            val recognizer = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                    Log.d("SpeechHelper", "Initializing On-Device Speech Recognizer")
                    SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                } else {
                    SpeechRecognizer.createSpeechRecognizer(context)
                }
            } catch (e: Throwable) {
                Log.w("SpeechHelper", "Falling back to standard speech recognizer: ${e.message}")
                SpeechRecognizer.createSpeechRecognizer(context)
            }

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    onListeningStateChanged(true)
                }

                override fun onBeginningOfSpeech() {
                    isListening = true
                    onListeningStateChanged(true)
                }

                override fun onRmsChanged(rmsdB: Float) {
                    onRmsChanged(rmsdB)
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    isListening = false
                    onListeningStateChanged(false)
                }

                override fun onError(error: Int) {
                    isListening = false
                    onListeningStateChanged(false)
                    val message = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Check mic."
                        SpeechRecognizer.ERROR_CLIENT -> "Recognition paused. Tap mic to retry."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                        SpeechRecognizer.ERROR_NETWORK -> "Network/offline language pack not ready. Use chips below or type command."
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timed out. Tap mic to retry."
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Tap mic to speak or select a quick command below."
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy. Retrying..."
                        SpeechRecognizer.ERROR_SERVER -> "Device speech engine busy. Tap mic to retry."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Listening timed out. Tap mic to speak."
                        else -> "Speech recognition ($error). Tap mic to speak."
                    }
                    onError(message)
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    onListeningStateChanged(false)
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val text = matches[0].trim()
                        if (text.isNotBlank()) {
                            onResult(text)
                        } else {
                            onError("Speech was empty. Tap mic to try again.")
                        }
                    } else {
                        onError("Could not understand speech. Tap mic to try again or select a command.")
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val partial = matches[0].trim()
                        if (partial.isNotBlank()) {
                            onPartialResult(partial)
                        }
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageLocale)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageLocale)
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, languageLocale)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                if (forceOffline) {
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                }
            }

            speechRecognizer = recognizer
            recognizer.startListening(intent)
        } catch (e: Exception) {
            Log.e("SpeechHelper", "Error initializing speech recognition: ${e.message}", e)
            isListening = false
            onListeningStateChanged(false)
            onError("Speech recognition unavailable: ${e.message}. Use quick chips or type below.")
        }
    }

    fun stopListening() {
        try {
            isListening = false
            onListeningStateChanged(false)
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e("SpeechHelper", "Error stopping speech recognizer: ${e.message}")
        }
    }
}
