package com.supernova.wheresmydevice.utils

import android.content.Context
import android.content.SharedPreferences

class AppSettings(private val context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "wheremydevice.settings", Context.MODE_PRIVATE
    )
    
    companion object {
        const val SMS_COMMAND_PREFIX = "sms_command_prefix"
        const val WHITELIST_ENABLED = "whitelist_enabled"
        const val DO_NOT_SHOW_OVERLAY_PERMISSION_AGAIN = "do_not_show_overlay_permission_again"
        const val DO_NOT_SHOW_DEVICE_ADMIN_PERMISSION_AGAIN = "do_not_show_device_admin_permission_again"
        const val SECURE_MODE_ENABLED = "secure_mode_enabled"
        const val SMS_COMMAND_PASSWORD_ENABLED = "sms_command_password_enabled"
        const val SMS_COMMAND_PASSWORD = "sms_command_password"
        const val WHITELISTED_NUMBERS = "whitelisted_numbers"
        const val USE_WHITELIST = "use_whitelist"
        const val HAS_SEEN_DISCLAIMER = "has_seen_disclaimer"
    }
    
    fun setString(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }
    
    fun getString(key: String): String {
        return sharedPreferences.getString(key, defaultValues(key) as String) ?: defaultValues(key) as String
    }
    
    fun setBoolean(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }
    
    fun getBoolean(key: String): Boolean {
        return try {
            sharedPreferences.getBoolean(key, defaultValues(key) as Boolean)
        } catch (e: ClassCastException) {
            try {
                val stringValue = sharedPreferences.getString(key, defaultValues(key).toString())
                stringValue?.toBoolean() ?: defaultValues(key) as Boolean
            } catch (e: Exception) {
                defaultValues(key) as Boolean
            }
        }
    }
    
    fun setStringSet(key: String, values: Set<String>) {
        sharedPreferences.edit().putStringSet(key, values).apply()
    }
    
    fun getStringSet(key: String, defaultValue: Set<String>): Set<String> {
        return sharedPreferences.getStringSet(key, defaultValue) ?: defaultValue
    }
    
    private fun defaultValues(key: String): Any {
        return when (key) {
            SMS_COMMAND_PREFIX -> "WMD" // Where's My Device
            WHITELIST_ENABLED -> false
            DO_NOT_SHOW_OVERLAY_PERMISSION_AGAIN -> false
            DO_NOT_SHOW_DEVICE_ADMIN_PERMISSION_AGAIN -> false
            SECURE_MODE_ENABLED -> false
            SMS_COMMAND_PASSWORD_ENABLED -> false
            SMS_COMMAND_PASSWORD -> ""
            WHITELISTED_NUMBERS -> emptySet<String>()
            USE_WHITELIST -> false
            HAS_SEEN_DISCLAIMER -> false
            else -> ""
        }
    }
} 