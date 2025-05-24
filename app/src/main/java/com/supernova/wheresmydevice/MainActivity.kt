package com.supernova.wheresmydevice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.supernova.wheresmydevice.ui.theme.WheresMyDeviceTheme
import com.supernova.wheresmydevice.utils.AppSettings
import com.supernova.wheresmydevice.utils.Utils
import com.supernova.wheresmydevice.dialogs.DisclaimerDialog
import com.supernova.wheresmydevice.utils.PermissionManager

class MainActivity : ComponentActivity() {
    private lateinit var appSettings: AppSettings
    private lateinit var permissionManager: PermissionManager
    private val showDisclaimerDialogState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        appSettings = AppSettings(this)
        permissionManager = PermissionManager(this)
        
        // Check if this is first launch to show disclaimer
        if (!appSettings.getBoolean(AppSettings.HAS_SEEN_DISCLAIMER)) {
            showDisclaimerDialogState.value = true
        }
        
        // Ensure a default command password exists
        if (appSettings.getString(AppSettings.SMS_COMMAND_PASSWORD).isEmpty()) {
            val defaultPassword = "password" + (1000..9999).random()
            appSettings.setString(AppSettings.SMS_COMMAND_PASSWORD, defaultPassword)
            Log.d("MainActivity", "Generated default SMS command password: $defaultPassword")
        }
        
        setContent {
            WheresMyDeviceTheme {
                // Show disclaimer dialog if needed
                if (showDisclaimerDialogState.value) {
                    DisclaimerDialog(
                        onDismiss = {
                            // User declined - exit app
                            finish()
                        },
                        onAccept = {
                            // User accepted - mark as seen and continue
                            appSettings.setBoolean(AppSettings.HAS_SEEN_DISCLAIMER, true)
                            showDisclaimerDialogState.value = false
                            // Do not request any permissions here
                        }
                    )
                }
                
                MainScreen(
                    onSettingsClick = { navigateToSettings() },
                    onPermissionsClick = { navigateToPermissions() },
                    onOverlayPermissionRequest = { permissionManager.requestOverlayPermission() }
                )
            }
        }
    }

    private fun navigateToSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToPermissions() {
        val intent = Intent(this, PermissionsActivity::class.java)
        startActivity(intent)
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionManager.handlePermissionResult(requestCode, permissions, grantResults)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onSettingsClick: () -> Unit,
    onPermissionsClick: () -> Unit,
    onOverlayPermissionRequest: () -> Unit
) {
    val context = LocalContext.current
    val appSettings = remember { AppSettings(context) }
    
    // We won't automatically show dialogs at startup anymore
    val showOverlayDialog = remember { mutableStateOf(false) }
    val secureMode = remember { 
        mutableStateOf(appSettings.getBoolean(AppSettings.SECURE_MODE_ENABLED))
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Where's My Device") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        // Make the content scrollable to handle smaller screens
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Spacer for top padding
            Spacer(modifier = Modifier.height(32.dp))
            
            // App logo with adaptive sizing
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(dimensionResource(id = R.dimen.logo_size))
                    .padding(bottom = 24.dp)
            )
            
            Text(
                text = stringResource(R.string.locate_my_device_is_running),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onPermissionsClick,
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(0.8f)
            ) {
                Text(stringResource(R.string.permissions))
            }
            
            // Spacing between buttons
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onSettingsClick,
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(0.8f)
            ) {
                Text(stringResource(R.string.settings))
            }
            
            // Bottom spacer
            Spacer(modifier = Modifier.height(64.dp))
        }
    }
    
    // Permission dialogs - only shown when explicitly triggered
    if (showOverlayDialog.value) {
        OverlayPermissionDialog(
            onConfirm = {
                onOverlayPermissionRequest()
                showOverlayDialog.value = false
            },
            onDismiss = { showOverlayDialog.value = false }
        )
    }
}

@Composable
fun OverlayPermissionDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val appSettings = remember { AppSettings(context) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Display Over Other Apps") },
        text = { Text("This permission is required for the app to display messages and alerts when your device is lost.") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                appSettings.setBoolean(AppSettings.DO_NOT_SHOW_OVERLAY_PERMISSION_AGAIN, true)
                onDismiss()
            }) {
                Text("Don't Show Again")
            }
        }
    )
}