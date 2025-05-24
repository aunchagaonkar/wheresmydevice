package com.supernova.wheresmydevice.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.supernova.wheresmydevice.R
import com.supernova.wheresmydevice.service.OverlayDisplayService
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.Manifest
import android.annotation.SuppressLint
import com.supernova.wheresmydevice.utils.SoundModeManager
import android.content.BroadcastReceiver
import android.content.IntentFilter
import com.supernova.wheresmydevice.utils.RingtonePlayer
import android.content.SharedPreferences

class SmsCommandProcessor(private val context: Context) {

    companion object {
        private const val TAG = "SmsCommandProcessor"

        // Command constants
        const val COMMAND_LOCATE = "locate"
        const val COMMAND_RING = "ring"
        const val COMMAND_INFO = "info"
        const val COMMAND_HELP = "help"
        const val COMMAND_CALLME = "callme"
        const val COMMAND_SOUND = "sound"
        const val COMMAND_PING = "ping"
    }

    private val appSettings = AppSettings(context)
    private val soundModeManager = SoundModeManager(context)

    fun processCommand(rawCommand: String, senderNumber: String) {
        Log.d(TAG, "Processing command: $rawCommand")

        // Get the command password
        val commandPassword = appSettings.getString(AppSettings.SMS_COMMAND_PASSWORD)

        // Split the raw command by whitespace to extract actual command and parameters
        val parts = rawCommand.trim().split("\\s+".toRegex())

        // We expect at least command and password
        if (parts.size < 2) {
            sendSmsResponse(senderNumber, context.getString(R.string.missing_password))
            return
        }

        val command = parts[0].toLowerCase(Locale.ROOT)
        val password = parts[1]

        // Check if the password is correct
        if (password != commandPassword) {
            sendSmsResponse(senderNumber, context.getString(R.string.invalid_password))
            return
        }

        // Process the command with remaining parameters
        val params = if (parts.size > 2) parts.subList(2, parts.size) else emptyList()
        processVerifiedCommand(command, params, senderNumber)
    }

    private fun processVerifiedCommand(command: String, params: List<String>, senderNumber: String) {
        when (command) {
            COMMAND_LOCATE -> handleLocateCommand(senderNumber)
            COMMAND_RING -> handleRingCommand()
            COMMAND_INFO -> handleInfoCommand(senderNumber)
            COMMAND_HELP -> handleHelpCommand(senderNumber)
            COMMAND_CALLME -> handleCallMeCommand(senderNumber)
            COMMAND_SOUND -> handleSoundCommand(senderNumber, params)
            COMMAND_PING -> handlePingCommand(senderNumber)
            else -> sendSmsResponse(senderNumber, context.getString(R.string.unknown_command))
        }
    }

    private fun handleLocateCommand(senderNumber: String) {
        // Get location and send it back
        LocationUtils(context).getCurrentLocation { location ->
            if (location != null) {
                val locationMessage = context.getString(
                    R.string.device_location,
                    location.latitude,
                    location.longitude
                )
                val googleMapsLink = "https://maps.google.com/maps?q=${location.latitude},${location.longitude}"

                sendSmsResponse(senderNumber, "$locationMessage\n$googleMapsLink")
            } else {
                sendSmsResponse(senderNumber, context.getString(R.string.location_not_available))
            }
        }
    }

    private fun handleRingCommand() {
        try {
            // Use the RingtonePlayer singleton to play the ringtone
            RingtonePlayer.playRingtone(context)

            // Display a message on screen with dismiss button
            val intent = Intent(context, OverlayDisplayService::class.java)
            intent.putExtra("message", context.getString(R.string.ring_command_received))
            intent.putExtra("isRingCommand", true) // Flag to identify this is from ring command
            context.startService(intent)

            Log.d(TAG, "Ring command executed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error handling ring command", e)
        }
    }

    @SuppressLint("StringFormatMatches")
    private fun handleInfoCommand(senderNumber: String) {
        val batteryInfo = BatteryUtils.getBatteryPercentage(context)
        val isCharging = BatteryUtils.isCharging(context)
        
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val isScreenOn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            powerManager.isInteractive
        } else {
            @Suppress("DEPRECATION")
            powerManager.isScreenOn
        }
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val currentDateTime = dateFormat.format(Date())
        
        val infoMessage = context.getString(
            R.string.device_info,
            batteryInfo,
            if (isCharging) context.getString(R.string.yes) else context.getString(R.string.no),
            if (isScreenOn) context.getString(R.string.yes) else context.getString(R.string.no),
            currentDateTime
        )
        
        sendSmsResponse(senderNumber, infoMessage)
    }
    
    private fun handleHelpCommand(senderNumber: String) {
        // Always get the latest values from settings when generating help
        val commandPrefix = appSettings.getString(AppSettings.SMS_COMMAND_PREFIX)
        val commandPassword = appSettings.getString(AppSettings.SMS_COMMAND_PASSWORD)
        
        // Create a detailed help message with actual examples using current settings
        val helpMessage = context.getString(
            R.string.available_commands_with_password,
            commandPrefix,
            commandPassword
        )
        
        sendSmsResponse(senderNumber, helpMessage)
    }
    
    private fun handleCallMeCommand(senderNumber: String) {
        try {
            // Create an intent to make a phone call
            val callIntent = Intent(Intent.ACTION_CALL)
            callIntent.data = Uri.parse("tel:$senderNumber")
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            // Check if we have the permission to make calls
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == 
                    PackageManager.PERMISSION_GRANTED) {
                // Start the call
                context.startActivity(callIntent)
            } else {
                sendSmsResponse(senderNumber, context.getString(R.string.call_permission_required))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error making call: ${e.message}")
            sendSmsResponse(senderNumber, context.getString(R.string.error_making_call))
        }
    }
    
    private fun handleSoundCommand(senderNumber: String, params: List<String>) {
        val soundModeManager = SoundModeManager(context)
        
        when {
            params.contains("normal") -> {
                soundModeManager.setNormalMode()
                sendSmsResponse(senderNumber, "Sound mode set to normal")
            }
            params.contains("vibrate") -> {
                soundModeManager.setVibrateMode()
                sendSmsResponse(senderNumber, "Sound mode set to vibrate")
            }
            params.contains("silent") -> {
                soundModeManager.setSilentMode()
                sendSmsResponse(senderNumber, "Sound mode set to silent")
            }
            else -> {
                val currentMode = soundModeManager.getCurrentModeName()
                sendSmsResponse(senderNumber, "Current sound mode: $currentMode")
            }
        }
    }
    
    /**
     * Handle the ping command which confirms that the WMD service is running
     */
    private fun handlePingCommand(senderNumber: String) {
        sendSmsResponse(senderNumber, context.getString(R.string.wmd_ping_response))
    }
    
    private fun sendSmsResponse(phoneNumber: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            
            // If message is too long, divide it into parts
            if (message.length > 160) {
                val messageParts = smsManager.divideMessage(message)
                smsManager.sendMultipartTextMessage(phoneNumber, null, messageParts, null, null)
            } else {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            }
            
            Log.d(TAG, "SMS response sent to $phoneNumber")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending SMS response", e)
        }
    }
} 