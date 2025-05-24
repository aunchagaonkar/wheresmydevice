package com.supernova.wheresmydevice

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.supernova.wheresmydevice.ui.theme.WheresMyDeviceTheme
import com.supernova.wheresmydevice.utils.AppSettings

class SettingsActivity : ComponentActivity() {
    
    private lateinit var appSettings: AppSettings
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appSettings = AppSettings(this)
        enableEdgeToEdge()
        
        setContent {
            WheresMyDeviceTheme {
                SettingsScreen(
                    onBackClick = { finish() },
                    appSettings = appSettings
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    appSettings: AppSettings
) {
    var whitelistEnabled by remember { mutableStateOf(appSettings.getBoolean(AppSettings.WHITELIST_ENABLED)) }
    val context = LocalContext.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
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
        // Adding a ScrollableColumn to contain all settings
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Security Section
            SettingsSection(
                title = stringResource(R.string.security)
            ) {
                // Whitelist Card
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = stringResource(R.string.enable_whitelist),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.whitelist_description),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = whitelistEnabled,
                                onCheckedChange = { isChecked ->
                                    whitelistEnabled = isChecked
                                    appSettings.setBoolean(AppSettings.WHITELIST_ENABLED, isChecked)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Manage whitelist button - only enabled if whitelist is enabled
                        Button(
                            onClick = {
                                context.startActivity(Intent(context, WhitelistActivity::class.java))
                            },
                            enabled = whitelistEnabled,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.manage_whitelist))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Command Settings
                SettingsItem(
                    title = stringResource(R.string.command_settings),
                    subtitle = stringResource(R.string.command_settings_description),
                    onClick = {
                        val intent = Intent(context, CommandSettingsActivity::class.java)
                        context.startActivity(intent)
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Permissions
                SettingsItem(
                    title = stringResource(R.string.permissions),
                    subtitle = stringResource(R.string.app_permission),
                    onClick = {
                        val intent = Intent(context, PermissionsActivity::class.java)
                        context.startActivity(intent)
                    }
                )
            }
            
            // App Information Section
            SettingsSection(
                title = stringResource(R.string.app_information)
            ) {
                // About
                SettingsItem(
                    title = stringResource(R.string.about),
                    subtitle = stringResource(R.string.click_here_for_more_info),
                    onClick = {
                        val intent = Intent(context, AboutActivity::class.java)
                        context.startActivity(intent)
                    }
                )
            }
            
            // Add spacer at bottom to ensure the last item isn't cut off
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        content()
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
} 