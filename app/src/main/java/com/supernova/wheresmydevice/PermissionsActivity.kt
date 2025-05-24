package com.supernova.wheresmydevice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.supernova.wheresmydevice.ui.theme.WheresMyDeviceTheme
import com.supernova.wheresmydevice.utils.AppSettings
import com.supernova.wheresmydevice.utils.PermissionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.ui.graphics.Color

class PermissionsActivity : ComponentActivity() {
    private lateinit var permissionManager: PermissionManager
    private lateinit var appSettings: AppSettings
    
    // Permission refresh trigger
    private val permissionRefreshTrigger = mutableStateOf(0)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        permissionManager = PermissionManager(this)
        appSettings = AppSettings(this)
        
        // If disclaimer hasn't been seen, return to MainActivity
        if (!appSettings.getBoolean(AppSettings.HAS_SEEN_DISCLAIMER)) {
            finish()
            return
        }
        
        setContent {
            WheresMyDeviceTheme {
                PermissionsScreen(
                    onBackClick = { finish() },
                    permissionManager = permissionManager,
                    permissionRefreshTrigger = permissionRefreshTrigger.value
                )
            }
        }
        
        // Register listeners for permission changes
        registerPermissionListeners()
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionManager.handlePermissionResult(requestCode, permissions, grantResults)
        refreshPermissions()
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh permissions when coming back to the activity
        refreshPermissions()
    }
    
    private fun refreshPermissions() {
        // Trigger a refresh by incrementing the state value
        permissionRefreshTrigger.value++
    }
    
    private fun registerPermissionListeners() {
        // SMS permissions
        permissionManager.addPermissionChangeListener(Manifest.permission.RECEIVE_SMS) {
            refreshPermissions()
        }
        permissionManager.addPermissionChangeListener(Manifest.permission.SEND_SMS) {
            refreshPermissions()
        }
        
        // Location permissions
        permissionManager.addPermissionChangeListener(Manifest.permission.ACCESS_FINE_LOCATION) {
            refreshPermissions()
        }
        permissionManager.addPermissionChangeListener(Manifest.permission.ACCESS_COARSE_LOCATION) {
            refreshPermissions()
        }
        
        // Background location permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionManager.addPermissionChangeListener(Manifest.permission.ACCESS_BACKGROUND_LOCATION) {
                refreshPermissions()
            }
        }
        
        // Call permission
        permissionManager.addPermissionChangeListener(Manifest.permission.CALL_PHONE) {
            refreshPermissions()
        }
        
        // Contacts permission
        permissionManager.addPermissionChangeListener(Manifest.permission.READ_CONTACTS) {
            refreshPermissions()
        }
        
        // Special permissions
        permissionManager.addPermissionChangeListener("overlay_permission") {
            refreshPermissions()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Unregister all listeners to prevent memory leaks
        unregisterPermissionListeners()
    }
    
    private fun unregisterPermissionListeners() {
        val permissions = listOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS,
            "overlay_permission"
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions + Manifest.permission.ACCESS_BACKGROUND_LOCATION
        }
        
        permissions.forEach { permission ->
            permissionManager.removePermissionChangeListener(permission) { refreshPermissions() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    onBackClick: () -> Unit,
    permissionManager: PermissionManager,
    permissionRefreshTrigger: Int
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Permission states - these will update when permissionRefreshTrigger changes
    val smsPermissionGranted = remember(permissionRefreshTrigger) { 
        mutableStateOf(
            permissionManager.hasPermission(Manifest.permission.RECEIVE_SMS) &&
            permissionManager.hasPermission(Manifest.permission.SEND_SMS)
        ) 
    }
    
    val locationPermissionGranted = remember(permissionRefreshTrigger) { 
        mutableStateOf(
            permissionManager.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        ) 
    }
    
    val backgroundLocationPermissionGranted = remember(permissionRefreshTrigger) {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                permissionManager.hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                true // Background location isn't a separate permission pre-Android 10
            }
        )
    }
    
    val overlayPermissionGranted = remember(permissionRefreshTrigger) { 
        mutableStateOf(permissionManager.hasOverlayPermission()) 
    }
    
    val callsPermissionGranted = remember(permissionRefreshTrigger) { 
        mutableStateOf(
            permissionManager.hasPermission(Manifest.permission.CALL_PHONE)
        ) 
    }
    
    val contactsPermissionGranted = remember(permissionRefreshTrigger) {
        mutableStateOf(
            permissionManager.hasPermission(Manifest.permission.READ_CONTACTS)
        )
    }
    
    // Track recently granted permissions for animation
    val recentlyGrantedPermissions = remember { mutableStateListOf<String>() }
    
    // Function to show the "granted" animation
    fun showGrantedAnimation(permissionId: String) {
        scope.launch {
            recentlyGrantedPermissions.add(permissionId)
            delay(1500) // Show the animation for 1.5 seconds
            recentlyGrantedPermissions.remove(permissionId)
        }
    }
    
    // Track granted permissions to detect changes
    val previousPermissionState = remember { 
        mutableStateMapOf(
            "sms" to smsPermissionGranted.value,
            "location" to locationPermissionGranted.value,
            "background_location" to backgroundLocationPermissionGranted.value,
            "overlay" to overlayPermissionGranted.value,
            "calls" to callsPermissionGranted.value,
            "contacts" to contactsPermissionGranted.value
        )
    }
    
    // Check if any permissions changed to true and show animations
    LaunchedEffect(
        smsPermissionGranted.value,
        locationPermissionGranted.value,
        backgroundLocationPermissionGranted.value,
        overlayPermissionGranted.value,
        callsPermissionGranted.value,
        contactsPermissionGranted.value
    ) {
        // Check SMS
        if (smsPermissionGranted.value && !previousPermissionState["sms"]!!) {
            showGrantedAnimation("sms")
            previousPermissionState["sms"] = true
        }
        
        // Check location
        if (locationPermissionGranted.value && !previousPermissionState["location"]!!) {
            showGrantedAnimation("location")
            previousPermissionState["location"] = true
        }
        
        // Check background location
        if (backgroundLocationPermissionGranted.value && !previousPermissionState["background_location"]!!) {
            showGrantedAnimation("background_location")
            previousPermissionState["background_location"] = true
        }
        
        // Check overlay
        if (overlayPermissionGranted.value && !previousPermissionState["overlay"]!!) {
            showGrantedAnimation("overlay")
            previousPermissionState["overlay"] = true
        }
        
        // Check calls
        if (callsPermissionGranted.value && !previousPermissionState["calls"]!!) {
            showGrantedAnimation("calls")
            previousPermissionState["calls"] = true
        }
        
        // Check contacts
        if (contactsPermissionGranted.value && !previousPermissionState["contacts"]!!) {
            showGrantedAnimation("contacts")
            previousPermissionState["contacts"] = true
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.permissions)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Tap on Settings to request each permission individually",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // CORE PERMISSIONS SECTION
            Text(
                text = "Core Features (Required)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )
            
            Text(
                text = "These permissions are required for the app's basic functionality",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // SMS permission - Core
            PermissionItemEnhanced(
                title = stringResource(R.string.sms),
                description = "Required to receive and respond to SMS commands",
                isGranted = smsPermissionGranted.value,
                showAnimation = recentlyGrantedPermissions.contains("sms"),
                onClick = { 
                    // Request SMS permissions one by one with explanations
                    if (!permissionManager.hasPermission(Manifest.permission.RECEIVE_SMS)) {
                        permissionManager.requestPermissionWithExplanation(Manifest.permission.RECEIVE_SMS)
                    } else if (!permissionManager.hasPermission(Manifest.permission.SEND_SMS)) {
                        permissionManager.requestPermissionWithExplanation(Manifest.permission.SEND_SMS)
                    }
                }
            )
            
            // Location permission - Core
            PermissionItemEnhanced(
                title = stringResource(R.string.location),
                description = "Required to locate your device when requested",
                isGranted = locationPermissionGranted.value,
                showAnimation = recentlyGrantedPermissions.contains("location"),
                onClick = {
                    // Request location permissions with explanation
                    if (!permissionManager.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        permissionManager.requestPermissionWithExplanation(Manifest.permission.ACCESS_FINE_LOCATION)
                    } else if (!permissionManager.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                        permissionManager.requestPermissionWithExplanation(Manifest.permission.ACCESS_COARSE_LOCATION)
                    }
                }
            )
            
            // OPTIONAL PERMISSIONS SECTION
            Text(
                text = "Optional Features (Grant only if needed)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )
            
            Text(
                text = "These permissions enable additional features but aren't required for basic functionality",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Background location (Android 10+) - Optional
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                PermissionItemEnhanced(
                    title = stringResource(R.string.background_location),
                    description = "Enables location tracking when app is not in use",
                    isGranted = backgroundLocationPermissionGranted.value,
                    showAnimation = recentlyGrantedPermissions.contains("background_location"),
                    isOptional = true,
                    onClick = { 
                        // Request background location with clear explanation
                        permissionManager.requestBackgroundLocationPermission() 
                    }
                )
            }
            
            // Overlay permission - Optional
            PermissionItemEnhanced(
                title = stringResource(R.string.display_over_other_apps),
                description = "Allows showing messages on locked screen",
                isGranted = overlayPermissionGranted.value,
                showAnimation = recentlyGrantedPermissions.contains("overlay"),
                isOptional = true,
                onClick = { permissionManager.requestOverlayPermission() }
            )
            
            // Call permission - Optional
            PermissionItemEnhanced(
                title = stringResource(R.string.calls),
                description = "Enables the 'callme' command to call you back",
                isGranted = callsPermissionGranted.value,
                showAnimation = recentlyGrantedPermissions.contains("calls"),
                isOptional = true,
                onClick = { 
                    permissionManager.requestPermissionWithExplanation(Manifest.permission.CALL_PHONE)
                }
            )
            
            // Contacts permission - Optional
            PermissionItemEnhanced(
                title = "Contacts",
                description = "Allows adding contacts to whitelist from your address book",
                isGranted = contactsPermissionGranted.value,
                showAnimation = recentlyGrantedPermissions.contains("contacts"),
                isOptional = true,
                onClick = {
                    permissionManager.requestPermissionWithExplanation(Manifest.permission.READ_CONTACTS)
                }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun PermissionItemEnhanced(
    title: String,
    description: String = "",
    isGranted: Boolean,
    isOptional: Boolean = false,
    showAnimation: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        if (isOptional) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                Text(
                                    text = "Optional",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    
                    if (description.isNotEmpty()) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    
                    Text(
                        text = if (isGranted) stringResource(R.string.granted) 
                               else stringResource(R.string.not_granted),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isGranted) MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                Button(
                    onClick = onClick,
                    enabled = !isGranted
                ) {
                    Text(stringResource(R.string.settings))
                }
            }
            
            // Overlay animation when permission is granted
            if (showAnimation) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Permission Granted",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Permission Granted!",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
} 