package com.supernova.wheresmydevice

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.supernova.wheresmydevice.ui.theme.WheresMyDeviceTheme
import java.text.SimpleDateFormat
import java.util.*

class AboutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            WheresMyDeviceTheme {
                AboutScreen(
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about)) },
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo and Slogan Section
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // App Logo
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "App Logo",
                        modifier = Modifier
                            .size(120.dp)
                            .padding(8.dp)
                    )
                    
                    // App Name
                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // App Slogan
                    Text(
                        text = "Find and control your lost device",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 16.dp))
                    
                    // App Description
                    Text(
                        text = stringResource(R.string.app_description),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Version & Last Updated Section
            AboutSection {
                // Version Info
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val versionName = packageInfo.versionName
                
                AboutRow(
                    icon = Icons.Outlined.Info,
                    title = "${stringResource(R.string.app_version)} $versionName"
                )
                
                // Last Updated Info
                val lastUpdateTime = packageInfo.lastUpdateTime
                val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                val lastUpdated = dateFormat.format(Date(lastUpdateTime))
                
                AboutRow(
                    icon = Icons.Outlined.Update,
                    title = "Last Updated: $lastUpdated"
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // F-Droid & Donate Section
            AboutSection {
                // F-Droid
                ButtonRow(
                    icon = Icons.Outlined.Store,
                    title = "Get on F-Droid",
                    onClick = {
                        val browserIntent = Intent(Intent.ACTION_VIEW, 
                            Uri.parse("https://f-droid.org/packages/com.supernova.wheresmydevice/"))
                        context.startActivity(browserIntent)
                    }
                )
                
                // Donate
                ButtonRow(
                    icon = Icons.Outlined.Favorite,
                    title = "Donate",
                    onClick = {
                        val browserIntent = Intent(Intent.ACTION_VIEW, 
                            Uri.parse("https://buymeacoffee.com/aunchagaonkar"))
                        context.startActivity(browserIntent)
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // GitHub Section
            AboutSection {
                ButtonRow(
                    icon = Icons.Outlined.Code,
                    title = stringResource(R.string.view_source_code),
                    onClick = {
                        val browserIntent = Intent(Intent.ACTION_VIEW, 
                            Uri.parse("https://github.com/aunchagaonkar/wheresmydevice"))
                        context.startActivity(browserIntent)
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun AboutSection(
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            content()
        }
    }
}

@Composable
fun AboutRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun ButtonRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = title)
        }
    }
} 