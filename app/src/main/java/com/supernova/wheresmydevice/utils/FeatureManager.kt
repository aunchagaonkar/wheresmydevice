package com.supernova.wheresmydevice.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Manages feature availability based on granted permissions
 * This allows the app to offer functionality modularly
 * Features that require special permissions will only be available if those permissions are granted
 */
class FeatureManager(private val context: Context) {

    /**
     * Core feature - SMS control (receiving and sending SMS commands)
     * Required for basic app functionality
     */
    fun isSmsFeatureEnabled(): Boolean {
        return hasPermission(Manifest.permission.RECEIVE_SMS) && 
               hasPermission(Manifest.permission.SEND_SMS)
    }
    
    /**
     * Core feature - Basic location (can locate device when requested)
     * Required for location functionality
     */
    fun isLocationFeatureEnabled(): Boolean {
        return hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
               hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    }
    
    /**
     * Optional feature - Enhanced location (background location tracking)
     * Only needed for location tracking when app is not in foreground
     */
    fun isBackgroundLocationEnabled(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // Before Android 10, background location was part of location permission
            return isLocationFeatureEnabled()
        }
        
        return hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }
    
    /**
     * Optional feature - Call back (make device call the owner's phone)
     * Only needed for the "callme" command
     */
    fun isCallFeatureEnabled(): Boolean {
        return hasPermission(Manifest.permission.CALL_PHONE)
    }
    
    /**
     * Optional feature - Contact whitelist selection
     * Only needed if user wants to add contacts from their address book
     */
    fun isContactsFeatureEnabled(): Boolean {
        return hasPermission(Manifest.permission.READ_CONTACTS)
    }
    
    /**
     * Optional feature - Overlay messages
     * Enhances user experience but not required for core functionality
     */
    fun isOverlayFeatureEnabled(): Boolean {
        return Settings.canDrawOverlays(context)
    }
    
    /**
     * Helper method to check if a permission is granted
     */
    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context, 
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }
} 