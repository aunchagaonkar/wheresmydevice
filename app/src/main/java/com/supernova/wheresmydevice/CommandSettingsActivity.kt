package com.supernova.wheresmydevice

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.supernova.wheresmydevice.ui.theme.WheresMyDeviceTheme
import com.supernova.wheresmydevice.utils.AppSettings
import com.supernova.wheresmydevice.utils.SmsCommandProcessor

class CommandSettingsActivity : ComponentActivity() {
    private lateinit var appSettings: AppSettings
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        appSettings = AppSettings(this)
        
        setContent {
            WheresMyDeviceTheme {
                CommandSettingsScreen(
                    onBackClick = { finish() },
                    appSettings = appSettings
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandSettingsScreen(
    onBackClick: () -> Unit,
    appSettings: AppSettings
) {
    val context = LocalContext.current
    
    // State variables
    var commandPrefix by remember { mutableStateOf(appSettings.getString(AppSettings.SMS_COMMAND_PREFIX)) }
    var commandPassword by remember { mutableStateOf(appSettings.getString(AppSettings.SMS_COMMAND_PASSWORD)) }
    var prefixError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sms_commands)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.Help,
                            contentDescription = stringResource(R.string.help)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.command_prefix),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    OutlinedTextField(
                        value = commandPrefix,
                        onValueChange = { 
                            commandPrefix = it
                            prefixError = it.isBlank()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        isError = prefixError,
                        supportingText = {
                            if (prefixError) {
                                Text(stringResource(R.string.prefix_required))
                            } else {
                                Text(stringResource(R.string.command_prefix_description))
                            }
                        },
                        singleLine = true
                    )
                }
            }
            
            item {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.command_password),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    OutlinedTextField(
                        value = commandPassword,
                        onValueChange = { 
                            commandPassword = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) 
                                        Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) 
                            VisualTransformation.None else PasswordVisualTransformation(),
                        supportingText = {
                            Text(stringResource(R.string.command_password_description_required))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true
                    )
                }
            }
            
            item {
                Button(
                    onClick = {
                        if (commandPrefix.isBlank()) {
                            prefixError = true
                            return@Button
                        }
                        
                        appSettings.setString(AppSettings.SMS_COMMAND_PREFIX, commandPrefix)
                        appSettings.setString(AppSettings.SMS_COMMAND_PASSWORD, commandPassword)
                        
                        Toast.makeText(
                            context,
                            context.getString(R.string.settings_saved),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.save))
                }
            }
            
            item {
                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
                
                Text(
                    text = stringResource(R.string.available_commands_header),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            
            // Commands Categories
            item {
                CommandCategorySection(
                    title = "Location & Information",
                    commandPrefix = commandPrefix,
                    commandPassword = commandPassword
                ) {
                    EnhancedCommandItem(
                        icon = Icons.Outlined.LocationOn,
                        command = SmsCommandProcessor.COMMAND_LOCATE,
                        description = stringResource(R.string.command_locate_description),
                        prefix = commandPrefix,
                        password = commandPassword
                    )
                    
                    EnhancedCommandItem(
                        icon = Icons.Outlined.Info,
                        command = SmsCommandProcessor.COMMAND_INFO,
                        description = stringResource(R.string.command_info_description),
                        prefix = commandPrefix,
                        password = commandPassword
                    )
                }
            }
            
            item {
                CommandCategorySection(
                    title = "Alert & Communication",
                    commandPrefix = commandPrefix,
                    commandPassword = commandPassword
                ) {
                    EnhancedCommandItem(
                        icon = Icons.Outlined.Notifications,
                        command = SmsCommandProcessor.COMMAND_RING,
                        description = stringResource(R.string.command_ring_description),
                        prefix = commandPrefix,
                        password = commandPassword
                    )
                    
                    EnhancedCommandItem(
                        icon = Icons.Outlined.Call,
                        command = SmsCommandProcessor.COMMAND_CALLME,
                        description = stringResource(R.string.command_callme_description),
                        prefix = commandPrefix,
                        password = commandPassword
                    )
                }
            }
            
            item {
                CommandCategorySection(
                    title = "Device Control",
                    commandPrefix = commandPrefix,
                    commandPassword = commandPassword
                ) {
                    EnhancedCommandItem(
                        icon = Icons.Outlined.VolumeUp,
                        command = SmsCommandProcessor.COMMAND_SOUND,
                        description = stringResource(R.string.command_sound_description),
                        prefix = commandPrefix,
                        password = commandPassword,
                        hasParameter = true,
                        parameterDescription = "[normal/vibrate/silent]"
                    )
                }
            }
            
            item {
                CommandCategorySection(
                    title = "Utility & Emergency",
                    commandPrefix = commandPrefix,
                    commandPassword = commandPassword
                ) {
                    EnhancedCommandItem(
                        icon = Icons.Outlined.Help,
                        command = SmsCommandProcessor.COMMAND_HELP,
                        description = stringResource(R.string.command_help_description),
                        prefix = commandPrefix,
                        password = commandPassword
                    )
                }
            }
            
            item { 
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
    
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text(stringResource(R.string.how_commands_work)) },
            text = { 
                Text(
                    if (commandPassword.isBlank()) {
                        stringResource(R.string.commands_help_text, commandPrefix)
                    } else {
                        stringResource(R.string.commands_help_text_required, commandPrefix, commandPassword)
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text(stringResource(R.string.dismiss))
                }
            }
        )
    }
}

@Composable
fun CommandCategorySection(
    title: String,
    commandPrefix: String,
    commandPassword: String,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                content()
            }
        }
    }
}

@Composable
fun EnhancedCommandItem(
    icon: ImageVector,
    command: String,
    description: String,
    prefix: String,
    password: String,
    isWarning: Boolean = false,
    hasParameter: Boolean = false,
    parameterDescription: String? = null
) {
    val displayCommand = buildString {
        append(prefix)
        append(" ")
        append(command)
        
        if (password.isNotBlank()) {
            append(" ")
            append(password)
        }
        
        if (hasParameter && parameterDescription != null) {
            append(" ")
            append(parameterDescription)
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isWarning) 
                MaterialTheme.colorScheme.errorContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (isWarning) 
                    MaterialTheme.colorScheme.error 
                else 
                    MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = displayCommand,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isWarning) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
} 