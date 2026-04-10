package com.resell.app.ui

import android.net.Uri
import android.content.Intent
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.resell.app.data.AppScreen
import com.resell.app.data.ProductRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: ProductRepository,
    onSelectScreen: (AppScreen) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var statusMessage by remember { mutableStateOf("Backup and restore your products.") }
    val backupPreferences by repository.backupPreferences.collectAsState(initial = com.resell.app.data.BackupPreferences())

    val createBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            statusMessage = if (repository.exportBackup(uri)) {
                "Backup saved successfully."
            } else {
                "Backup failed. Please try again."
            }
        }
    }

    val driveFolderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri == null) {
            scope.launch {
                repository.updateAutoBackup(false)
                statusMessage = "Google Drive backup was not connected."
            }
            return@rememberLauncherForActivityResult
        }

        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, flags)
        }

        scope.launch {
            repository.setDriveFolderUri(uri)
            repository.updateAutoBackup(true)
            statusMessage = "Google Drive backup is active for the selected Drive folder."
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            statusMessage = if (repository.restoreBackup(uri)) {
                "Backup restored successfully."
            } else {
                "Restore failed. Please choose a valid backup file."
            }
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Settings", style = MaterialTheme.typography.titleLarge)
                Text(statusMessage, style = MaterialTheme.typography.labelMedium, color = MutedInk)
                ScreenSelector(
                    current = AppScreen.SETTINGS,
                    onSelected = onSelectScreen,
                    modifier = Modifier.fillMaxWidth(0.45f)
                )
            }

            SectionCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Backup", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Export one backup file containing product data and images.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedInk
                    )
                    Button(
                        onClick = { createBackupLauncher.launch("resell-backup.json") },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)
                    ) {
                        Text("Export backup")
                    }
                }
            }

            SectionCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Restore", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Restore from a previously exported backup file. This replaces the current saved products.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedInk
                    )
                    Button(
                        onClick = { restoreBackupLauncher.launch(arrayOf("application/json")) },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)
                    ) {
                        Text("Restore backup")
                    }
                }
            }

            SectionCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Google Drive", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Automatic Google Drive backup", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                if (backupPreferences.driveFolderUri.isBlank()) {
                                    "Choose a Google Drive folder. The system Drive picker lets you choose the Google account and location."
                                } else {
                                    "Drive folder connected. Daily backup stays active unless permission is revoked or the folder/account changes."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedInk
                            )
                        }
                        Switch(
                            checked = backupPreferences.autoBackupEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    if (enabled) {
                                        if (backupPreferences.driveFolderUri.isBlank()) {
                                            driveFolderLauncher.launch(null)
                                        } else {
                                            repository.updateAutoBackup(true)
                                            statusMessage = "Google Drive backup is active."
                                        }
                                    } else {
                                        repository.updateAutoBackup(false)
                                        statusMessage = "Google Drive backup is off."
                                    }
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = BrandPurple
                            )
                        )
                    }
                    Button(
                        onClick = { driveFolderLauncher.launch(Uri.parse(backupPreferences.driveFolderUri).takeIf { backupPreferences.driveFolderUri.isNotBlank() }) },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)
                    ) {
                        Text(if (backupPreferences.driveFolderUri.isBlank()) "Choose Google Drive folder" else "Change Google Drive folder")
                    }
                    if (backupPreferences.driveFolderUri.isNotBlank()) {
                        Button(
                            onClick = {
                                scope.launch {
                                    statusMessage = if (repository.exportBackupToDriveFolder()) {
                                        "Backup uploaded to Google Drive."
                                    } else {
                                        "Drive backup failed. Reconnect the folder and try again."
                                    }
                                }
                            },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)
                        ) {
                            Text("Back up now")
                        }
                    }
                    Text(
                        "Android background work is inexact, so the daily upload is not guaranteed to run exactly at 00:00.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedInk
                    )
                }
            }
        }
    }
}
