package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class SpeechRecognitionHelper(
    private val context: Context,
    private val onListeningStateChanged: (Boolean) -> Unit,
    private val onRmsChanged: (Float) -> Unit,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit
) {

    private var speechRecognizer: SpeechRecognizer? = null
    var isListening = false
        private set

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening(languageLocale: String = "en-IN") {
        stopListening()

        if (!isAvailable()) {
            onError("Speech recognition is not available on this device. You can type your command below.")
            return
        }

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                        onListeningStateChanged(true)
                    }

                    override fun onBeginningOfSpeech() {}

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
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Speech recognition client error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                            SpeechRecognizer.ERROR_NETWORK -> "Network required for online speech recognition. Try typing or verify offline engine."
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timed out"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Please try again or type."
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition engine busy"
                            SpeechRecognizer.ERROR_SERVER -> "Recognition server error"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                            else -> "Speech recognition error ($error)"
                        }
                        onError(message)
                    }

                    override fun onResults(results: Bundle?) {
                        isListening = false
                        onListeningStateChanged(false)
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            onResult(matches[0])
                        } else {
                            onError("Could not understand speech. Please try typing.")
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            onResult(matches[0])
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageLocale)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageLocale)
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, languageLocale)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("SpeechHelper", "Error initializing speech recognition: ${e.message}")
            isListening = false
            onListeningStateChanged(false)
            onError("Could not start speech recognition: ${e.message}")
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
            e.printStackTrace()
        }
    }
}
