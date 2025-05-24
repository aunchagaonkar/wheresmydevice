package com.supernova.wheresmydevice.utils

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.supernova.wheresmydevice.R
import com.supernova.wheresmydevice.dialogs.PermissionExplanationDialog
import android.content.Context
import android.os.Handler
import android.os.Looper

/**
 * Utility class to handle dynamic permission requests with explanations
 * Permissions are only requested when explicitly triggered by user action
 */
class PermissionManager(private val activity: ComponentActivity) {

    private val appSettings = AppSettings(activity)
    private val handler = Handler(Looper.getMainLooper())
    
    // Map of permissions to their explanations
    private val permissionExplanations = mapOf(
        Manifest.permission.RECEIVE_SMS to R.string.permission_explanation_sms_receive,
        Manifest.permission.SEND_SMS to R.string.permission_explanation_sms_send,
        Manifest.permission.ACCESS_COARSE_LOCATION to R.string.permission_explanation_location,
        Manifest.permission.ACCESS_FINE_LOCATION to R.string.permission_explanation_location_fine,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION to R.string.permission_explanation_location_background,
        Manifest.permission.CALL_PHONE to R.string.permission_explanation_call_phone,
        Manifest.permission.READ_CONTACTS to R.string.permission_explanation_contacts,
        Manifest.permission.MODIFY_AUDIO_SETTINGS to R.string.permission_explanation_audio
    )
    
    // Permission change listeners
    private val permissionChangeListeners = mutableMapOf<String, MutableList<() -> Unit>>()
    
    // Single permission request launcher
    private val requestPermissionLauncher: ActivityResultLauncher<String> = 
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            // Notify listeners that the permission state has changed
            val permission = permissionRequestInProgress
            if (permission != null) {
                notifyPermissionChanged(permission)
                permissionRequestInProgress = null
            }
        }
        
    // Multiple permissions request launcher
    private val requestMultiplePermissionsLauncher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            // Notify all listeners for changed permissions
            permissions.keys.forEach { permission ->
                notifyPermissionChanged(permission)
            }
        }
    
    // Track which permission is currently being requested
    private var permissionRequestInProgress: String? = null

    /**
     * Check if a specific permission is granted
     */
    fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            activity, 
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Add a listener to be notified when a specific permission's state changes
     */
    fun addPermissionChangeListener(permission: String, listener: () -> Unit) {
        val listeners = permissionChangeListeners.getOrPut(permission) { mutableListOf() }
        listeners.add(listener)
    }
    
    /**
     * Remove a permission change listener
     */
    fun removePermissionChangeListener(permission: String, listener: () -> Unit) {
        permissionChangeListeners[permission]?.remove(listener)
    }
    
    /**
     * Notify all listeners that a permission's state has changed
     */
    private fun notifyPermissionChanged(permission: String) {
        permissionChangeListeners[permission]?.forEach { it.invoke() }
    }
    
    /**
     * Request a single permission with explanation dialog
     * Only called when explicitly triggered by user action
     */
    fun requestPermissionWithExplanation(permission: String) {
        if (hasPermission(permission)) {
            return
        }
        
        // First check if disclaimer has been seen
        if (!appSettings.getBoolean(AppSettings.HAS_SEEN_DISCLAIMER)) {
            return
        }
        
        val explanationResId = permissionExplanations[permission] ?: R.string.permission_explanation_generic
        val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            activity, permission
        )
        
        if (shouldShowRationale) {
            // Show explanation dialog
            PermissionExplanationDialog(
                activity = activity,
                explanationResId = explanationResId,
                onConfirm = { 
                    permissionRequestInProgress = permission
                    requestPermissionLauncher.launch(permission) 
                },
                onDismiss = { /* User declined explanation */ }
            ).show()
        } else {
            // Request permission directly
            permissionRequestInProgress = permission
            requestPermissionLauncher.launch(permission)
        }
    }
    
    /**
     * Request background location permission (Android 10+)
     * Only called when explicitly triggered by user action
     */
    fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        
        // First check if disclaimer has been seen
        if (!appSettings.getBoolean(AppSettings.HAS_SEEN_DISCLAIMER)) {
            return
        }
        
        if (!hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
            val explanationResId = R.string.permission_explanation_location_background
            
            PermissionExplanationDialog(
                activity = activity,
                explanationResId = explanationResId,
                onConfirm = {
                    permissionRequestInProgress = Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                        Utils.PERMISSION_ACCESS_BACKGROUND_LOCATION
                    )
                    // We need to notify listeners manually for this case since we're not using the launcher
                    handler.postDelayed({ notifyPermissionChanged(Manifest.permission.ACCESS_BACKGROUND_LOCATION) }, 500)
                },
                onDismiss = { /* User declined explanation */ }
            ).show()
        }
    }
    
    /**
     * Request overlay permission with explanation
     * Only called when explicitly triggered by user action
     */
    fun requestOverlayPermission() {
        if (Settings.canDrawOverlays(activity)) return
        
        // First check if disclaimer has been seen
        if (!appSettings.getBoolean(AppSettings.HAS_SEEN_DISCLAIMER)) {
            return
        }
        
        if (!appSettings.getBoolean(AppSettings.DO_NOT_SHOW_OVERLAY_PERMISSION_AGAIN)) {
            PermissionExplanationDialog(
                activity = activity,
                explanationResId = R.string.permission_explanation_overlay,
                onConfirm = {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                    intent.data = Uri.parse("package:${activity.packageName}")
                    activity.startActivity(intent)
                    
                    // We need to create a way to notify when coming back from settings
                    // Start our overlay permission result tracker
                    OverlayPermissionResultTracker.start(activity) {
                        notifyPermissionChanged("overlay_permission")
                    }
                },
                onDismiss = { /* User declined explanation */ }
            ).show()
        }
    }
    
    /**
     * Check if overlay permission is granted
     */
    fun hasOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(activity)
    }
    
    /**
     * Open app permission settings
     * Only called when explicitly triggered by user action
     */
    fun openAppPermissionSettings() {
        // First check if disclaimer has been seen
        if (!appSettings.getBoolean(AppSettings.HAS_SEEN_DISCLAIMER)) {
            return
        }
        
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.fromParts("package", activity.packageName, null)
        activity.startActivity(intent)
    }
    
    /**
     * Helper to register for permission change notification from onRequestPermissionsResult
     * To be called from the activity's onRequestPermissionsResult method
     */
    fun handlePermissionResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        permissions.forEach { permission ->
            notifyPermissionChanged(permission)
        }
    }
}

/**
 * Helper object to track overlay permission results
 */
object OverlayPermissionResultTracker {
    private var callback: (() -> Unit)? = null
    private var activity: Activity? = null
    private var wasChecking = false
    
    fun start(activity: Activity, onResult: () -> Unit) {
        this.activity = activity
        this.callback = onResult
        wasChecking = true
        
        // Start a periodic check
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                val activityRef = activity ?: return
                if (!wasChecking) return
                
                if (Settings.canDrawOverlays(activityRef)) {
                    wasChecking = false
                    callback?.invoke()
                    callback = null
                    return
                }
                
                // Check again after a delay
                handler.postDelayed(this, 500)
            }
        }
        
        handler.postDelayed(runnable, 500)
    }
} 