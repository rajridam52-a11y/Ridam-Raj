package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RingVolume
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.prefs.CustomRingtoneItem
import com.example.system.PreviewAudioState
import com.example.system.RingtoneAudioController
import com.example.system.RingingState
import com.example.ui.theme.GlassCard
import com.example.ui.theme.LocalRrColors
import com.example.ui.theme.NeonGradientButton
import com.example.ui.theme.PremiumEmptyState
import com.example.ui.theme.SectionHeader
import com.example.ui.theme.StatusPill

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RingtoneStudioScreen(
    defaultRingtoneName: String,
    defaultRingtoneUri: String?,
    savedRingtones: List<CustomRingtoneItem>,
    previewState: PreviewAudioState,
    ringingState: RingingState?,
    onImportSong: (Uri, Boolean) -> Unit,
    onAddSystemRingtone: (Uri?, Boolean) -> Unit,
    onSetDefaultRingtone: (String, String?) -> Unit,
    onDeleteRingtone: (CustomRingtoneItem) -> Unit,
    onPreviewRingtone: (String, String, String?) -> Unit,
    onStopPreview: () -> Unit,
    onTestRing: (String, String?) -> Unit,
    onStopRinging: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = {
        onStopPreview()
        onBack()
    })

    DisposableEffect(Unit) {
        onDispose { onStopPreview() }
    }

    val ext = LocalRrColors.current

    val pickSongLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onImportSong(uri, true)
        }
    }

    val pickSystemLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val pickedUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            if (pickedUri != null) {
                onAddSystemRingtone(pickedUri, true)
            }
        }
    }

    val defaultPreviewPlaying = previewState.isPlaying && previewState.activeId == "default_active"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SubScreenTopBar(
                title = "Song Ringtones",
                subtitle = "Any Song • Alarms, Timers & Alerts",
                onBack = {
                    onStopPreview()
                    onBack()
                }
            )
        }

        // Active Default Ringtone Hero Card
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
                    val iconTint = if ((ext.glowPrimary.luminance() + ext.glowSecondary.luminance()) / 2f > 0.62f) {
                        Color(0xFF0A0E17)
                    } else {
                        Color.White
                    }
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(ext.glowPrimary, ext.glowSecondary))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = "Active Ringtone",
                            tint = iconTint,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "ACTIVE DEFAULT RINGTONE",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = defaultRingtoneName,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (!defaultRingtoneUri.isNullOrBlank()) {
                                "Custom audio active for alarms, timers & reminders"
                            } else {
                                "Using built-in device alarm sound"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (defaultPreviewPlaying) {
                                onStopPreview()
                            } else {
                                onPreviewRingtone("default_active", defaultRingtoneName, defaultRingtoneUri)
                            }
                        },
                        modifier = Modifier.testTag("ringtone_preview_default_button")
                    ) {
                        Icon(
                            imageVector = if (defaultPreviewPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (defaultPreviewPlaying) "Stop Preview" else "Preview")
                    }

                    OutlinedButton(
                        onClick = {
                            if (ringingState?.isRinging == true) {
                                onStopRinging()
                            } else {
                                onTestRing(defaultRingtoneName, defaultRingtoneUri)
                            }
                        },
                        modifier = Modifier.testTag("ringtone_test_ring_button")
                    ) {
                        Icon(
                            imageVector = if (ringingState?.isRinging == true) Icons.Rounded.Stop else Icons.Rounded.RingVolume,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (ringingState?.isRinging == true) "Stop Ring" else "Test Ring")
                    }

                    if (!defaultRingtoneUri.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = { onSetDefaultRingtone("System Default Alarm", null) }
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Reset")
                        }
                    }
                }

                if (!previewState.error.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        previewState.error,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Pick Any Song / System Picker Card
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader("Add Custom Song", Icons.Rounded.LibraryMusic)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Choose any MP3, M4A, WAV, FLAC, or OGG song from your device storage, Downloads, or Drive. Songs are saved privately for offline alarm playback with zero extra permissions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                NeonGradientButton(
                    text = "Pick Any Song From Device",
                    icon = Icons.Rounded.AudioFile,
                    onClick = { pickSongLauncher.launch(arrayOf("audio/*")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ringtone_pick_song_button")
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select System Ringtone")
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                            if (!defaultRingtoneUri.isNullOrBlank()) {
                                runCatching { Uri.parse(defaultRingtoneUri) }.getOrNull()?.let {
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, it)
                                }
                            }
                        }
                        pickSystemLauncher.launch(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ringtone_pick_system_button")
                ) {
                    Icon(Icons.Rounded.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Browse Phone System Ringtones")
                }
            }
        }

        // Imported Songs Library Header
        item {
            SectionHeader(
                title = "My Imported Songs (${savedRingtones.size})",
                icon = Icons.Rounded.MusicNote
            )
        }

        if (savedRingtones.isEmpty()) {
            item {
                PremiumEmptyState(
                    icon = Icons.Rounded.MusicNote,
                    title = "No Songs Imported Yet",
                    subtitle = "Tap 'Pick Any Song From Device' above to use your favorite music track as an alarm or reminder ringtone.",
                    actionLabel = "Pick Song",
                    onAction = { pickSongLauncher.launch(arrayOf("audio/*")) }
                )
            }
        } else {
            items(savedRingtones, key = { it.id }) { item ->
                val isDefault = defaultRingtoneUri == item.uriString
                val isPlaying = previewState.isPlaying && previewState.activeId == item.id
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = 14.dp,
                    accentStripeColor = if (isDefault) ext.glowPrimary else null
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    (if (isDefault) ext.glowPrimary else ext.glowSecondary).copy(alpha = 0.16f)
                                )
                                .border(
                                    1.dp,
                                    (if (isDefault) ext.glowPrimary else ext.glassBorder).copy(alpha = 0.55f),
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (item.sourceType == "SYSTEM") Icons.Rounded.NotificationsActive else Icons.Rounded.MusicNote,
                                contentDescription = item.name,
                                tint = if (isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val metaParts = buildList {
                                if (item.artist.isNotBlank()) add(item.artist)
                                if (item.durationMs > 0L) add(RingtoneAudioController.formatDurationMs(item.durationMs))
                                add(if (item.sourceType == "SYSTEM") "System" else "Saved Song")
                            }
                            Text(
                                text = metaParts.joinToString(" • "),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isDefault) {
                                Spacer(Modifier.height(4.dp))
                                StatusPill(text = "Active Default", color = ext.success, icon = Icons.Rounded.CheckCircle)
                            }
                        }

                        IconButton(
                            onClick = {
                                if (isPlaying) onStopPreview()
                                else onPreviewRingtone(item.id, item.name, item.uriString)
                            }
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Stop preview" else "Preview ${item.name}",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(onClick = { onDeleteRingtone(item) }) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Remove ${item.name}",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    if (!isDefault) {
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { onSetDefaultRingtone(item.name, item.uriString) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Set as Default Ringtone")
                        }
                    }
                }
            }
        }

        // Built-in RR Chimes fallback section
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader("Built-In Alert Tones", Icons.Rounded.NotificationsActive)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RingtoneAudioController.builtInChimes.forEach { chime ->
                        val active = defaultRingtoneUri == null && defaultRingtoneName == chime
                        if (active) {
                            Button(onClick = { onSetDefaultRingtone(chime, null) }) {
                                Text("✓ $chime")
                            }
                        } else {
                            OutlinedButton(onClick = { onSetDefaultRingtone(chime, null) }) {
                                Text(chime)
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}
