package com.supernova.wheresmydevice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.supernova.wheresmydevice.data.WhitelistDao
import com.supernova.wheresmydevice.data.WhitelistEntry
import com.supernova.wheresmydevice.ui.theme.WheresMyDeviceTheme
import com.supernova.wheresmydevice.utils.AppSettings
import kotlinx.coroutines.launch

class WhitelistActivity : ComponentActivity() {
    private lateinit var appSettings: AppSettings
    private lateinit var whitelistDao: WhitelistDao
    
    // Create a mutableStateOf to hold the whitelist entries
    private val whitelistEntries = mutableStateOf<List<WhitelistEntry>>(emptyList())
    private var showManualEntryDialog by mutableStateOf(false)
    
    private val contactPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { contactUri ->
                val contactInfo = getContactInfo(contactUri)
                if (contactInfo.second.isNotEmpty()) {
                    // Add to whitelist with name
                    whitelistDao.addEntry(WhitelistEntry(
                        phoneNumber = contactInfo.second,
                        name = contactInfo.first
                    ))
                    
                    // Update the whitelist entries state
                    whitelistEntries.value = whitelistDao.getAllEntries()
                }
            }
        }
    }
    
    private val requestContactsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openContactPicker()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        appSettings = AppSettings(this)
        whitelistDao = WhitelistDao(this)
        
        // Initialize whitelist entries
        whitelistEntries.value = whitelistDao.getAllEntries()
        
        setContent {
            WheresMyDeviceTheme {
                WhitelistScreen(
                    onBackClick = { finish() },
                    onAddContactClick = { checkContactPermissionAndPick() },
                    onAddManualClick = { showManualEntryDialog = true },
                    whitelistEntries = whitelistEntries.value,
                    showManualEntryDialog = showManualEntryDialog,
                    onManualEntrySubmit = { name, phoneNumber ->
                        if (phoneNumber.isNotEmpty()) {
                            whitelistDao.addEntry(WhitelistEntry(
                                phoneNumber = phoneNumber.replace("[^0-9+]".toRegex(), ""),
                                name = name
                            ))
                            whitelistEntries.value = whitelistDao.getAllEntries()
                            showManualEntryDialog = false
                        }
                    },
                    onManualEntryDismiss = { showManualEntryDialog = false }
                )
            }
        }
    }
    
    private fun checkContactPermissionAndPick() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED -> {
                openContactPicker()
            }
            else -> {
                requestContactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        }
    }
    
    private fun openContactPicker() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
        }
        contactPickerLauncher.launch(intent)
    }
    
    private fun getContactInfo(contactUri: Uri): Pair<String, String> {
        var phoneNumber = ""
        var name = ""
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        )
        
        contentResolver.query(contactUri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                
                if (numberIndex >= 0) {
                    phoneNumber = cursor.getString(numberIndex)
                }
                if (nameIndex >= 0) {
                    name = cursor.getString(nameIndex)
                }
            }
        }
        
        // Clean the phone number (remove spaces, dashes, etc.)
        return Pair(name, phoneNumber.replace("[^0-9+]".toRegex(), ""))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhitelistScreen(
    onBackClick: () -> Unit,
    onAddContactClick: () -> Unit,
    onAddManualClick: () -> Unit,
    whitelistEntries: List<WhitelistEntry>,
    showManualEntryDialog: Boolean = false,
    onManualEntrySubmit: (String, String) -> Unit = { _, _ -> },
    onManualEntryDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val whitelistDao = remember { WhitelistDao(context) }
    
    // We'll use a mutable state to keep track of entries and allow dynamic updates
    val entries = remember(whitelistEntries) { mutableStateOf(whitelistEntries) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.whitelist)) },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (entries.value.isEmpty()) {
                // Show empty state with buttons stacked vertically
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.whitelist_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // First button: Add from Contacts
                    FilledTonalButton(
                        onClick = onAddContactClick,
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .padding(vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.add_from_contacts))
                    }
                    
                    // Second button: Add Manually
                    OutlinedButton(
                        onClick = onAddManualClick,
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .padding(vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.add_manually))
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Buttons at the top for non-empty state
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // First button: Add from Contacts
                            FilledTonalButton(
                                onClick = onAddContactClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.add_from_contacts))
                            }
                            
                            // Second button: Add Manually
                            OutlinedButton(
                                onClick = onAddManualClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.add_manually))
                            }
                        }
                    }
                    
                    // List of whitelist entries
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(entries.value) { entry ->
                            WhitelistEntryItem(
                                entry = entry,
                                onDeleteClick = {
                                    coroutineScope.launch {
                                        whitelistDao.deleteEntry(entry.id)
                                        entries.value = whitelistDao.getAllEntries()
                                    }
                                }
                            )
                        }
                        // Add some space at the bottom
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
        
        // Manual entry dialog
        if (showManualEntryDialog) {
            var name by remember { mutableStateOf("") }
            var phoneNumber by remember { mutableStateOf("") }
            var isPhoneNumberValid by remember { mutableStateOf(true) }
            
            Dialog(onDismissRequest = onManualEntryDismiss) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.add_contact_manually),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(stringResource(R.string.contact_name)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { 
                                phoneNumber = it
                                isPhoneNumberValid = phoneNumber.isNotEmpty() 
                            },
                            label = { Text(stringResource(R.string.phone_number)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            isError = !isPhoneNumberValid,
                            supportingText = {
                                if (!isPhoneNumberValid) {
                                    Text(stringResource(R.string.phone_number_error))
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true
                        )
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = onManualEntryDismiss) {
                                Text(stringResource(android.R.string.cancel))
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Button(
                                onClick = {
                                    if (phoneNumber.isNotEmpty()) {
                                        onManualEntrySubmit(name, phoneNumber)
                                    } else {
                                        isPhoneNumberValid = false
                                    }
                                }
                            ) {
                                Text(stringResource(R.string.add))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WhitelistEntryItem(
    entry: WhitelistEntry,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Column for name and number
            Column(
                modifier = Modifier.weight(1f)
            ) {
                if (entry.name.isNotEmpty()) {
                    Text(
                        text = entry.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = entry.phoneNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = entry.phoneNumber,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            
            // Provide better touch target for the delete button
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
} 