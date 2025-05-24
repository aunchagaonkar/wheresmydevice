package com.supernova.wheresmydevice.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.supernova.wheresmydevice.R
import com.supernova.wheresmydevice.utils.RingtonePlayer
import java.util.Timer
import java.util.TimerTask

class OverlayDisplayService : Service() {
    
    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private var timer: Timer? = null
    private var isRingCommand = false
    
    companion object {
        private const val TAG = "OverlayDisplayService"
    }
    
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        
        val message = intent.getStringExtra("message") ?: getString(R.string.app_name)
        isRingCommand = intent.getBooleanExtra("isRingCommand", false)
        
        if (!Settings.canDrawOverlays(this)) {
            // If this was from a ring command, stop the ringtone
            if (isRingCommand) {
                RingtonePlayer.stopRingtone()
            }
            stopSelf()
            return START_NOT_STICKY
        }
        
        showOverlay(message)
        
        // Automatically dismiss after 30 seconds
        scheduleAutoDismiss(30000) // 30 seconds
        
        return START_STICKY
    }
    
    private fun showOverlay(message: String) {
        // Remove any existing overlay
        removeOverlay()
        
        try {
            // Inflate the overlay layout
            val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            overlayView = inflater.inflate(R.layout.overlay_message, null)
            
            // Set the message
            val messageTextView = overlayView.findViewById<TextView>(R.id.overlayMessage)
            messageTextView.text = message
            
            // Set up dismiss button
            val dismissButton = overlayView.findViewById<Button>(R.id.dismissButton)
            dismissButton.setOnClickListener {
                // If this was from a ring command, stop the ringtone
                if (isRingCommand && RingtonePlayer.isRingtonePlaying()) {
                    Log.d(TAG, "Stopping ringtone from dismiss button")
                    RingtonePlayer.stopRingtone()
                }
                removeOverlay()
                stopSelf()
            }
            
            // Create layout parameters for the overlay
            val layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
                PixelFormat.TRANSLUCENT
            )
            
            layoutParams.gravity = Gravity.CENTER
            
            // Add the view to the window
            windowManager.addView(overlayView, layoutParams)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing overlay: ${e.message}")
            stopSelf()
        }
    }
    
    private fun removeOverlay() {
        try {
            if (::overlayView.isInitialized && overlayView.parent != null) {
                windowManager.removeView(overlayView)
            }
        } catch (e: Exception) {
            // View might have already been removed
            Log.e(TAG, "Error removing overlay: ${e.message}")
        }
    }
    
    private fun scheduleAutoDismiss(delayMillis: Long) {
        timer?.cancel()
        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                // Run on main thread to stop ringtone and remove overlay
                android.os.Handler(mainLooper).post {
                    // If this was from a ring command, stop the ringtone
                    if (isRingCommand && RingtonePlayer.isRingtonePlaying()) {
                        Log.d(TAG, "Stopping ringtone from auto-dismiss")
                        RingtonePlayer.stopRingtone()
                    }
                    removeOverlay()
                    stopSelf()
                }
            }
        }, delayMillis)
    }
    
    override fun onDestroy() {
        removeOverlay()
        timer?.cancel()
        timer = null
        super.onDestroy()
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
} 