package com.supernova.wheresmydevice.utils

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Singleton class to handle ringtone playback across the app
 */
object RingtonePlayer {
    private const val TAG = "RingtonePlayer"
    
    private var mediaPlayer: MediaPlayer? = null
    private var originalVolume: Int = 0
    private var audioManager: AudioManager? = null
    private var autoStopHandler: Handler? = null
    private var autoStopRunnable: Runnable? = null
    
    // Status tracking
    private var isPlaying = false
    
    /**
     * Start playing the default ringtone at maximum volume
     */
    fun playRingtone(context: Context, durationMs: Long = 30000) {
        // If already playing, stop first
        if (isPlaying) {
            stopRingtone()
        }
        
        try {
            Log.d(TAG, "Starting ringtone playback")
            
            // Get audio manager
            audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            
            // Save original volume
            originalVolume = audioManager?.getStreamVolume(AudioManager.STREAM_RING) ?: 0
            
            // Set volume to maximum
            val maxVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_RING) ?: 0
            audioManager?.setStreamVolume(AudioManager.STREAM_RING, maxVolume, 0)
            
            // Create and prepare media player
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, ringtoneUri)
                setAudioStreamType(AudioManager.STREAM_RING)
                isLooping = true
                prepare()
                start()
            }
            
            isPlaying = true
            
            // Set auto-stop after specified duration
            autoStopHandler = Handler(Looper.getMainLooper())
            autoStopRunnable = Runnable {
                stopRingtone()
            }
            
            autoStopHandler?.postDelayed(autoStopRunnable!!, durationMs)
            
            Log.d(TAG, "Ringtone playback started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error playing ringtone: ${e.message}")
            // Clean up in case of error
            stopRingtone()
        }
    }
    
    /**
     * Stop the currently playing ringtone and restore original volume
     */
    fun stopRingtone() {
        Log.d(TAG, "Stopping ringtone playback")
        
        try {
            // Cancel auto-stop if pending
            autoStopRunnable?.let { 
                autoStopHandler?.removeCallbacks(it)
            }
            autoStopRunnable = null
            autoStopHandler = null
            
            // Stop and release media player
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                reset()
                release()
            }
            mediaPlayer = null
            
            // Restore original volume
            audioManager?.setStreamVolume(AudioManager.STREAM_RING, originalVolume, 0)
            audioManager = null
            
            isPlaying = false
            Log.d(TAG, "Ringtone playback stopped successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping ringtone: ${e.message}")
        }
    }
    
    /**
     * Check if ringtone is currently playing
     */
    fun isRingtonePlaying(): Boolean {
        return isPlaying
    }
} 