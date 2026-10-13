package com.example.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log

data class AlarmToneInfo(
    val title: String,
    val uriString: String,
    val isSystemDefault: Boolean = false,
    val isMobileSelected: Boolean = false
)

object AlarmToneManager {

    private var activeRingtone: Ringtone? = null
    private var activeMediaPlayer: MediaPlayer? = null

    /**
     * Returns a list of inbuilt tones available on Android plus standard presets.
     */
    fun getInbuiltTones(context: Context): List<AlarmToneInfo> {
        val list = mutableListOf<AlarmToneInfo>()

        // 1. Default system alarm tone
        val defaultAlarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val defaultAlarmTitle = try {
            RingtoneManager.getRingtone(context, defaultAlarmUri)?.getTitle(context) ?: "System Alarm"
        } catch (e: Exception) {
            "System Alarm"
        }
        list.add(
            AlarmToneInfo(
                title = "Standard Alarm ($defaultAlarmTitle)",
                uriString = defaultAlarmUri?.toString() ?: "",
                isSystemDefault = true
            )
        )

        // 2. Default notification chime
        val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationTitle = try {
            RingtoneManager.getRingtone(context, notificationUri)?.getTitle(context) ?: "Gentle Chime"
        } catch (e: Exception) {
            "Gentle Chime"
        }
        if (notificationUri != null) {
            list.add(
                AlarmToneInfo(
                    title = "Gentle Notification Chime ($notificationTitle)",
                    uriString = notificationUri.toString()
                )
            )
        }

        // 3. Default ringtone
        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        val ringtoneTitle = try {
            RingtoneManager.getRingtone(context, ringtoneUri)?.getTitle(context) ?: "Phone Ringtone"
        } catch (e: Exception) {
            "Phone Ringtone"
        }
        if (ringtoneUri != null) {
            list.add(
                AlarmToneInfo(
                    title = "Classic Ringtone ($ringtoneTitle)",
                    uriString = ringtoneUri.toString()
                )
            )
        }

        // 4. Query system available alarm & notification ringtones to offer rich variety
        try {
            val ringtoneMgr = RingtoneManager(context).apply {
                setType(RingtoneManager.TYPE_ALARM or RingtoneManager.TYPE_NOTIFICATION)
            }
            val cursor = ringtoneMgr.cursor
            val count = minOf(cursor.count, 6)
            for (i in 0 until count) {
                if (cursor.moveToPosition(i)) {
                    val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                    val uri = ringtoneMgr.getRingtoneUri(i)
                    if (uri != null && list.none { it.uriString == uri.toString() }) {
                        list.add(
                            AlarmToneInfo(
                                title = title ?: "Alarm Tone ${i + 1}",
                                uriString = uri.toString()
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("AlarmToneManager", "Error querying system ringtones: ${e.message}")
        }

        return list
    }

    /**
     * Resolves human-readable title for a tone URI string
     */
    fun getToneTitle(context: Context, uriString: String?): String {
        if (uriString.isNullOrBlank()) return "System Alarm (Default)"
        return try {
            val uri = Uri.parse(uriString)
            val ringtone = RingtoneManager.getRingtone(context, uri)
            ringtone?.getTitle(context) ?: "Custom Mobile Tone"
        } catch (e: Exception) {
            "Custom Selected Tone"
        }
    }

    /**
     * Preview play audio tone
     */
    fun previewTone(context: Context, uriString: String?) {
        stopPreview()
        try {
            val uri = if (!uriString.isNullOrBlank()) {
                Uri.parse(uriString)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            }

            val ringtone = RingtoneManager.getRingtone(context, uri)
            if (ringtone != null) {
                ringtone.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                ringtone.play()
                activeRingtone = ringtone
            }
        } catch (e: Exception) {
            Log.e("AlarmToneManager", "Error playing tone preview: ${e.message}", e)
        }
    }

    /**
     * Stop preview audio
     */
    fun stopPreview() {
        try {
            activeRingtone?.stop()
            activeRingtone = null
        } catch (ignored: Exception) {}

        try {
            activeMediaPlayer?.stop()
            activeMediaPlayer?.release()
            activeMediaPlayer = null
        } catch (ignored: Exception) {}
    }
}
