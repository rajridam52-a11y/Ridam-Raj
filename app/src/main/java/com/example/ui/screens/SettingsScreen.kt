package com.example.ui.screens

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.cloud.ConflictPolicy
import com.example.data.cloud.UserBackupSummary
import com.example.data.prefs.AppUserPreferences
import com.example.security.SecurityManager
import com.example.ui.theme.GlassCard
import com.example.ui.theme.LocalRrColors
import com.example.ui.theme.SectionHeader
import com.example.ui.theme.StatusPill
import com.example.ui.viewmodel.CloudSyncUiState
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    prefs: AppUserPreferences,
    isOnline: Boolean,
    currentUser: FirebaseUser?,
    authLoading: Boolean,
    authError: String?,
    cloudSyncState: CloudSyncUiState,
    cloudSummary: UserBackupSummary?,
    conflictPolicy: ConflictPolicy,
    onSignInGoogle: () -> Unit,
    onSignOutGoogle: () -> Unit,
    onSetConflictPolicy: (ConflictPolicy) -> Unit,
    onBackupNow: () -> Unit,
    onRestoreNow: () -> Unit,
    onDeleteCloudData: () -> Unit,
    onSetClockFormat: (Boolean) -> Unit,
    onSetDefaultSnooze: (Int) -> Unit,
    onSetNotificationReader: (Boolean) -> Unit,
    onSetOfflineMode: (Boolean) -> Unit,
    onSetAutoCloudSync: (Boolean) -> Unit,
    onConfigurePin: (Boolean, String?, Boolean) -> Unit,
    onLockNow: () -> Unit,
    onExportJson: suspend () -> String,
    onDeleteLocalData: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ext = LocalRrColors.current

    var showPinDialog by remember { mutableStateOf(false) }
    var showConfirmDeleteLocal by remember { mutableStateOf(false) }
    var showConfirmDeleteCloud by remember { mutableStateOf(false) }
    var exportStatus by remember { mutableStateOf<String?>(null) }

    val biometricSupported = remember(context) { SecurityManager.canUseBiometrics(context) }
    val micGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    val locGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else true
    val exactAlarmAllowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
    } else true

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(ext.glowPrimary, ext.glowSecondary)))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "PREFERENCES & CONTROL",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text("Settings", style = MaterialTheme.typography.headlineMedium)
            }
        }

        // 1. Profile & Cloud Account Hero Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                accentStripeColor = ext.glowPrimary
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val initials = currentUser?.displayName
                        ?.trim()
                        ?.firstOrNull()
                        ?.uppercaseChar()
                        ?.toString()
                    val avatarContentTint = if ((ext.glowPrimary.luminance() + ext.glowSecondary.luminance()) / 2f > 0.62f) {
                        Color(0xFF0A0E17)
                    } else {
                        Color.White
                    }
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(ext.glowPrimary, ext.glowSecondary))),
                        contentAlignment = Alignment.Center
                    ) {
                        if (initials != null) {
                            Text(
                                text = initials,
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = avatarContentTint
                            )
                        } else {
                            Icon(
                                Icons.Rounded.Person,
                                contentDescription = "Profile",
                                tint = avatarContentTint,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Local Profile",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = currentUser?.email ?: "Offline device storage • Sign in to sync",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (currentUser != null) {
                        StatusPill(text = "Synced", color = ext.success)
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (currentUser == null) {
                    if (!authError.isNullOrBlank()) {
                        Text(authError, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(6.dp))
                    }
                    com.example.ui.theme.NeonGradientButton(
                        text = if (authLoading) "Signing in..." else "Sign in with Google",
                        icon = Icons.AutoMirrored.Rounded.Login,
                        enabled = !authLoading,
                        onClick = onSignInGoogle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_google_signin_button")
                    )
                } else {
                    OutlinedButton(
                        onClick = onSignOutGoogle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_google_signout_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Sign Out")
                    }
                }
            }
        }

        // 2. Cloud Backup & Restore
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader("Cloud Backup & Restore", Icons.Rounded.CloudDone)
                Spacer(Modifier.height(8.dp))

                val lastSyncStr = remember(prefs.lastCloudBackupMillis, cloudSummary) {
                    val cloudTs = cloudSummary?.updatedAt?.toDate()?.time ?: 0L
                    val ts = maxOf(prefs.lastCloudBackupMillis, cloudTs)
                    if (ts <= 0L) "Never backed up"
                    else SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date(ts))
                }
                Text("Last backup: $lastSyncStr", style = MaterialTheme.typography.bodyMedium)
                if (cloudSummary != null) {
                    Text(
                        "Cloud snapshot: ${cloudSummary.totalItems} items (${cloudSummary.deviceName ?: "Android Device"})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-sync when online", style = MaterialTheme.typography.titleMedium)
                        Text("Automatically back up changes when connected", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = prefs.autoCloudSync,
                        onCheckedChange = onSetAutoCloudSync,
                        modifier = Modifier.testTag("settings_autosync_switch")
                    )
                }

                Spacer(Modifier.height(8.dp))
                Text("Conflict Resolution Policy", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ConflictPolicy.entries.forEach { policy ->
                        FilterChip(
                            selected = conflictPolicy == policy,
                            onClick = { onSetConflictPolicy(policy) },
                            label = { Text(policy.label) }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onBackupNow,
                        enabled = currentUser != null && cloudSyncState !is CloudSyncUiState.Syncing,
                        modifier = Modifier.weight(1f).testTag("settings_backup_now_button")
                    ) {
                        Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Backup")
                    }
                    OutlinedButton(
                        onClick = onRestoreNow,
                        enabled = currentUser != null && cloudSyncState !is CloudSyncUiState.Syncing,
                        modifier = Modifier.weight(1f).testTag("settings_restore_now_button")
                    ) {
                        Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Restore")
                    }
                }

                when (cloudSyncState) {
                    is CloudSyncUiState.Syncing -> {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text("Syncing with Firestore...", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    is CloudSyncUiState.Success -> {
                        Spacer(Modifier.height(8.dp))
                        Text(cloudSyncState.message, style = MaterialTheme.typography.labelMedium, color = ext.success)
                    }
                    is CloudSyncUiState.Error -> {
                        Spacer(Modifier.height(8.dp))
                        Text(cloudSyncState.message, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                    }
                    CloudSyncUiState.Idle -> Unit
                }
            }
        }

        // 3. Notifications & Reader
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader("Notifications & Reader", Icons.Rounded.Notifications)
                Spacer(Modifier.height(8.dp))
                SettingsToggleRow(
                    title = "Read Incoming Notifications Aloud",
                    subtitle = "Requires Android Notification Listener access",
                    checked = prefs.notificationReaderEnabled,
                    testTag = "settings_notif_reader_switch",
                    onCheckedChange = onSetNotificationReader
                )
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onNavigate("notifications_log") },
                        modifier = Modifier.weight(1f)
                    ) { Text("Notification Hub") }
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("System Access") }
                }
            }
        }

        // 4. Alarm & Clock Settings
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader("Alarm & Clock", Icons.Rounded.Alarm)
                Spacer(Modifier.height(8.dp))
                SettingsToggleRow(
                    title = "24-Hour Clock Format",
                    subtitle = "Applies to Dashboard, Alarms, Calendar & World Clock",
                    checked = prefs.use24HourClock,
                    testTag = "settings_24h_switch",
                    onCheckedChange = onSetClockFormat
                )
                Spacer(Modifier.height(6.dp))
                Text("Default Snooze Duration", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 15, 20).forEach { mins ->
                        FilterChip(
                            selected = prefs.defaultSnoozeMinutes == mins,
                            onClick = { onSetDefaultSnooze(mins) },
                            label = { Text("$mins min") }
                        )
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !exactAlarmAllowed) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Grant Exact Alarm Permission")
                    }
                }
            }
        }

        // 5. Colour & Design Dashboard Shortcut
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_design_studio_card"),
                onClick = { onNavigate("design") }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Rounded.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("Colour & Design Dashboard", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${prefs.design.themePreset.label} • ${prefs.design.backgroundStyle.label}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Open Design Studio")
                }
            }
        }

        // 6. Privacy & Security
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader("Privacy & Security", Icons.Rounded.Security)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("App Lock (PIN & Biometrics)", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (prefs.appLockEnabled) "Protected with salted SHA-256 PIN" else "Disabled",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (prefs.appLockEnabled) {
                            OutlinedButton(onClick = onLockNow) {
                                Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Lock")
                            }
                        }
                        Button(
                            onClick = { showPinDialog = true },
                            modifier = Modifier.testTag("settings_pin_config_button")
                        ) {
                            Text(if (prefs.appLockEnabled) "Manage" else "Set PIN")
                        }
                    }
                }

                if (prefs.appLockEnabled && biometricSupported) {
                    Spacer(Modifier.height(6.dp))
                    SettingsToggleRow(
                        title = "Unlock with Biometrics",
                        subtitle = "Fingerprint or Face unlock on supported devices",
                        checked = prefs.biometricEnabled,
                        testTag = "settings_biometric_switch",
                        onCheckedChange = { onConfigurePin(true, null, it) }
                    )
                }

                Spacer(Modifier.height(10.dp))
                Text("Permission Status", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusPill(if (micGranted) "Mic: Allowed" else "Mic: Off", if (micGranted) ext.success else MaterialTheme.colorScheme.error)
                    StatusPill(if (locGranted) "Location: Allowed" else "Location: Off", if (locGranted) ext.success else MaterialTheme.colorScheme.error)
                    StatusPill(if (notifGranted) "Alerts: Allowed" else "Alerts: Off", if (notifGranted) ext.success else MaterialTheme.colorScheme.error)
                    StatusPill(if (exactAlarmAllowed) "Exact Alarm: OK" else "Exact Alarm: Restricted", if (exactAlarmAllowed) ext.success else ext.warning)
                }

                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", context.packageName, null)
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("App Permissions") }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val json = onExportJson()
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/json"
                                    putExtra(Intent.EXTRA_SUBJECT, "RR Reminder Data Export")
                                    putExtra(Intent.EXTRA_TEXT, json)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Export RR Reminder Data"))
                                exportStatus = "Prepared JSON export (${json.length} chars)"
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("settings_export_json_button")
                    ) {
                        Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Export JSON")
                    }
                }
                if (exportStatus != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(exportStatus!!, style = MaterialTheme.typography.labelMedium, color = ext.success)
                }
            }
        }

        // 7. Offline Mode
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader("Offline Mode & Connectivity", Icons.Rounded.CloudOff)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill(
                        if (isOnline) "Network: Online" else "Network: Offline",
                        if (isOnline) ext.success else ext.warning
                    )
                    StatusPill(
                        if (prefs.offlineModePreferred) "Offline Mode Active" else "Live Connected Mode",
                        if (prefs.offlineModePreferred) ext.warning else MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(8.dp))
                SettingsToggleRow(
                    title = "Force Offline Mode",
                    subtitle = "Disables live weather & cloud calls while keeping local tools working",
                    checked = prefs.offlineModePreferred,
                    testTag = "settings_offline_switch",
                    onCheckedChange = onSetOfflineMode
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Works Offline: Alarms, Timers, Stopwatch, Pomodoro, World Clock, Tasks, Goals, Habits, Calendar, Birthdays, Notes & Voice Notes, Calculator, Unit Converter, Battery Monitor, App Lock.\nRequires Internet: Live Weather Forecast, City Search, Speed Test, Cloud Backup.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 8. Data Management
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader("Data Management", Icons.Rounded.DeleteForever)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (currentUser != null) {
                        OutlinedButton(
                            onClick = { showConfirmDeleteCloud = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) { Text("Delete Cloud Data") }
                    }
                    OutlinedButton(
                        onClick = { showConfirmDeleteLocal = true },
                        modifier = Modifier.weight(1f).testTag("settings_delete_local_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Erase Local Data") }
                }
            }
        }

        // 9. About RR Reminder
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader("About RR Reminder", Icons.Rounded.Info)
                Spacer(Modifier.height(6.dp))
                Text("RR Reminder • v1.0", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Your Time. Your Tasks. Your Personal Companion.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Created by Ridam Raj",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Powered by Firebase Cloud Firestore, Open-Meteo Live Weather & Air Quality APIs, Jetpack Compose Design Studio, and Android Room local persistence.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }

    if (showPinDialog) {
        PinSetupDialog(
            currentlyEnabled = prefs.appLockEnabled,
            biometricSupported = biometricSupported,
            currentBiometric = prefs.biometricEnabled,
            onDismiss = { showPinDialog = false },
            onSave = { pin, biometric ->
                onConfigurePin(true, pin, biometric)
                showPinDialog = false
            },
            onDisable = {
                onConfigurePin(false, null, false)
                showPinDialog = false
            }
        )
    }

    if (showConfirmDeleteLocal) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteLocal = false },
            icon = { Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Erase All Local Data?") },
            text = { Text("This deletes all reminders, alarms, calendar events, tasks, birthdays, notes, goals, habits, and resets preferences on this device.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteLocalData()
                        showConfirmDeleteLocal = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Erase Everything") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeleteLocal = false }) { Text("Cancel") }
            }
        )
    }

    if (showConfirmDeleteCloud) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteCloud = false },
            icon = { Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Cloud Backup?") },
            text = { Text("This permanently deletes your cloud backup documents from Firebase Firestore. Local data on this phone will stay intact.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCloudData()
                        showConfirmDeleteCloud = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete Cloud Data") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeleteCloud = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun PinSetupDialog(
    currentlyEnabled: Boolean,
    biometricSupported: Boolean,
    currentBiometric: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, Boolean) -> Unit,
    onDisable: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf(currentBiometric) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (currentlyEnabled) "Manage App Lock PIN" else "Set App Lock PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin = it },
                    label = { Text("New PIN (4–6 digits)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth().testTag("pin_setup_input")
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) confirmPin = it },
                    label = { Text("Confirm PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth().testTag("pin_confirm_input")
                )
                if (biometricSupported) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Also allow Biometric unlock", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = bio, onCheckedChange = { bio = it })
                    }
                }
                if (error != null) {
                    Text(error!!, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        pin.length < 4 -> error = "PIN must be at least 4 digits."
                        pin != confirmPin -> error = "PINs do not match."
                        else -> onSave(pin, bio)
                    }
                },
                modifier = Modifier.testTag("pin_save_button")
            ) { Text("Save PIN") }
        },
        dismissButton = {
            Row {
                if (currentlyEnabled) {
                    TextButton(onClick = onDisable) {
                        Text("Disable Lock", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
