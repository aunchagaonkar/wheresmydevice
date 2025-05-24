package com.supernova.wheresmydevice.utils

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
import com.supernova.wheresmydevice.R

class SoundModeManager(private val context: Context) {
    
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notificationManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        context.getSystemService(NotificationManager::class.java)
    } else {
        null
    }
    
    /**
     * Set the sound mode for the device
     * @param mode "normal", "vibrate", or "silent"
     * @return A response message indicating success or failure
     */
    fun setSoundMode(mode: String): String {
        return when (mode.lowercase()) {
            "normal" -> {
                setNormalMode()
                context.getString(R.string.sound_mode_normal)
            }
            "vibrate" -> {
                setVibrateMode()
                context.getString(R.string.sound_mode_vibrate)
            }
            "silent" -> {
                setSilentMode()
                context.getString(R.string.sound_mode_silent)
            }
            else -> {
                // If no mode specified or invalid mode, return current status
                getCurrentSoundMode()
            }
        }
    }
    
    /**
     * Get the current sound mode
     * @return A message describing the current sound mode
     */
    fun getCurrentSoundMode(): String {
        val modeName = when {
            isInSilentMode() -> context.getString(R.string.sound_mode_silent_label)
            isInVibrateMode() -> context.getString(R.string.sound_mode_vibrate_label)
            else -> context.getString(R.string.sound_mode_normal_label)
        }
        return context.getString(R.string.sound_mode_current, modeName)
    }
    
    fun setNormalMode(): Boolean {
        try {
            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            return true
        } catch (e: Exception) {
            return false
        }
    }
    
    fun setVibrateMode(): Boolean {
        try {
            audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            return true
        } catch (e: Exception) {
            return false
        }
    }
    
    fun setSilentMode(): Boolean {
        try {
            audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
            return true
        } catch (e: Exception) {
            return false
        }
    }
    
    fun getCurrentModeName(): String {
        return when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> "Normal"
            AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
            AudioManager.RINGER_MODE_SILENT -> "Silent"
            else -> "Unknown"
        }
    }
    
    private fun isInSilentMode(): Boolean {
        return audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT
    }
    
    private fun isInVibrateMode(): Boolean {
        return audioManager.ringerMode == AudioManager.RINGER_MODE_VIBRATE
    }
} 