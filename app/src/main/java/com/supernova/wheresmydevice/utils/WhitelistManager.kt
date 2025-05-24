package com.supernova.wheresmydevice.utils

import android.content.Context
import com.supernova.wheresmydevice.data.WhitelistDao

class WhitelistManager(private val context: Context) {
    private val appSettings = AppSettings(context)
    private val whitelistDao = WhitelistDao(context)
    
    /**
     * Checks if a phone number is allowed to interact with the device.
     * Returns true if:
     * 1. Whitelist is disabled, or
     * 2. The phone number is in the whitelist
     */
    fun isPhoneNumberAllowed(phoneNumber: String): Boolean {
        // If whitelist is disabled, allow all phone numbers
        if (!isWhitelistEnabled()) {
            return true
        }
        
        // Normalize the phone number (remove spaces, dashes, etc.)
        val normalizedNumber = phoneNumber.replace("[^0-9+]".toRegex(), "")
        
        // Check if the phone number is in the whitelist
        return whitelistDao.isPhoneNumberWhitelisted(normalizedNumber)
    }

    fun isWhitelistEnabled(): Boolean {
        return appSettings.getBoolean(AppSettings.WHITELIST_ENABLED)
    }
} 