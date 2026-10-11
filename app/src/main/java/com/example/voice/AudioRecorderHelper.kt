package com.example.voice

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException

class AudioRecorderHelper(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null

    var isRecording: Boolean = false
        private set

    fun startRecording(outputFile: File): Boolean {
        if (isRecording) {
            stopRecording()
        }

        return try {
            currentOutputFile = outputFile
            if (outputFile.exists()) {
                outputFile.delete()
            }

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64000)
                setAudioSamplingRate(16000)
                setAudioChannels(1)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            isRecording = true
            Log.d("AudioRecorder", "Recording started -> ${outputFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to start recording: ${e.message}", e)
            cleanUp()
            false
        }
    }

    fun stopRecording(): File? {
        if (!isRecording) return null

        return try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (e: RuntimeException) {
                    Log.w("AudioRecorder", "stop() failed or was called too quickly: ${e.message}")
                }
                reset()
                release()
            }
            mediaRecorder = null
            isRecording = false

            val file = currentOutputFile
            if (file != null && file.exists() && file.length() > 0) {
                Log.d("AudioRecorder", "Recording saved: ${file.length()} bytes")
                file
            } else {
                Log.w("AudioRecorder", "Recorded file is empty or missing")
                null
            }
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error stopping recording: ${e.message}", e)
            cleanUp()
            null
        }
    }

    fun cancelRecording() {
        try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (ignored: Exception) {}
                reset()
                release()
            }
        } catch (ignored: Exception) {}
        currentOutputFile?.delete()
        cleanUp()
    }

    fun getMaxAmplitude(): Int {
        return try {
            if (isRecording) mediaRecorder?.maxAmplitude ?: 0 else 0
        } catch (e: Exception) {
            0
        }
    }

    private fun cleanUp() {
        mediaRecorder = null
        currentOutputFile = null
        isRecording = false
    }
}
