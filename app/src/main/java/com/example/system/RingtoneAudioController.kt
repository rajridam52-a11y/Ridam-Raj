package com.example.system

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import com.example.data.prefs.CustomRingtoneItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.Locale
import java.util.UUID

data class PreviewAudioState(
    val activeId: String? = null,
    val activeName: String = "",
    val isPlaying: Boolean = false,
    val error: String? = null
)

data class RingingState(
    val id: String,
    val title: String,
    val songName: String,
    val type: String,
    val isRinging: Boolean = true
)

object RingtoneAudioController {
    private const val TAG = "RingtoneAudioController"
    private const val MAX_SONG_BYTES = 50L * 1024L * 1024L // 50 MB safety cap

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private var autoStopRunnable: Runnable? = null

    private var previewPlayer: MediaPlayer? = null
    private var alarmPlayer: MediaPlayer? = null
    private var activeVibrator: Vibrator? = null

    private val _previewState = MutableStateFlow(PreviewAudioState())
    val previewState: StateFlow<PreviewAudioState> = _previewState.asStateFlow()

    private val _ringingState = MutableStateFlow<RingingState?>(null)
    val ringingState: StateFlow<RingingState?> = _ringingState.asStateFlow()

    val builtInChimes: List<String> = listOf(
        "System Default Alarm",
        "Cosmic Chime",
        "Aurora Bell",
        "Gentle Pulse"
    )

    fun formatDurationMs(durationMs: Long): String {
        if (durationMs <= 0L) return "Audio"
        val totalSec = (durationMs / 1000L).coerceAtLeast(1L)
        val mins = totalSec / 60L
        val secs = totalSec % 60L
        return String.format(Locale.US, "%d:%02d", mins, secs)
    }

    fun importSongFromUri(context: Context, uri: Uri): Result<CustomRingtoneItem> {
        return try {
            val resolver = context.contentResolver
            runCatching {
                resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            var displayName = ""
            var reportedSize = -1L
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIdx >= 0) displayName = cursor.getString(nameIdx).orEmpty()
                        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) reportedSize = cursor.getLong(sizeIdx)
                    }
                }

            if (reportedSize > MAX_SONG_BYTES) {
                return Result.failure(IllegalArgumentException("Song file is larger than 50 MB limit."))
            }

            val mimeType = resolver.getType(uri)
            val extFromMime = mimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
            val extFromName = displayName.substringAfterLast('.', "").takeIf { it.length in 2..5 }
            val ext = (extFromMime ?: extFromName ?: "mp3").lowercase(Locale.US)

            val id = "song_${UUID.randomUUID().toString().take(12)}"
            val ringtonesDir = File(context.filesDir, "ringtones").apply { mkdirs() }
            val destFile = File(ringtonesDir, "$id.$ext")

            var totalBytes = 0L
            resolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        totalBytes += read
                        if (totalBytes > MAX_SONG_BYTES) {
                            destFile.delete()
                            return Result.failure(IllegalArgumentException("Song file exceeds 50 MB limit."))
                        }
                        output.write(buffer, 0, read)
                    }
                }
            } ?: return Result.failure(IllegalStateException("Unable to read the selected audio file."))

            if (totalBytes <= 0L) {
                destFile.delete()
                return Result.failure(IllegalStateException("Selected audio file appears to be empty."))
            }

            var metaTitle = ""
            var metaArtist = ""
            var metaDurationMs = 0L
            val retriever = MediaMetadataRetriever()
            runCatching {
                retriever.setDataSource(destFile.absolutePath)
                metaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE).orEmpty().trim()
                metaArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST).orEmpty().trim()
                metaDurationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
            }
            runCatching { retriever.release() }

            val cleanFileTitle = displayName
                .substringBeforeLast('.')
                .replace('_', ' ')
                .trim()
                .ifBlank { "Custom Song" }

            val finalName = metaTitle.ifBlank { cleanFileTitle }.take(80)
            val item = CustomRingtoneItem(
                id = id,
                name = finalName,
                artist = metaArtist.take(60),
                uriString = Uri.fromFile(destFile).toString(),
                durationMs = metaDurationMs,
                sourceType = "SONG",
                addedAtMillis = System.currentTimeMillis()
            )
            Result.success(item)
        } catch (e: Exception) {
            Log.e(TAG, "Song import failed", e)
            Result.failure(e)
        }
    }

    fun resolveSystemRingtone(context: Context, uri: Uri?): CustomRingtoneItem? {
        if (uri == null) return null
        val ringtone = runCatching { RingtoneManager.getRingtone(context, uri) }.getOrNull()
        val title = runCatching { ringtone?.getTitle(context) }.getOrNull()
            ?.takeIf { it.isNotBlank() } ?: "System Ringtone"
        return CustomRingtoneItem(
            id = "sys_${uri.toString().hashCode().toUInt()}",
            name = title,
            artist = "System Tone",
            uriString = uri.toString(),
            durationMs = 0L,
            sourceType = "SYSTEM",
            addedAtMillis = System.currentTimeMillis()
        )
    }

    fun deleteLocalRingtoneFile(context: Context, uriString: String) {
        runCatching {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "file") {
                val path = uri.path ?: return
                val ringtonesDir = File(context.filesDir, "ringtones").canonicalPath
                val target = File(path)
                if (target.canonicalPath.startsWith(ringtonesDir) && target.exists()) {
                    target.delete()
                }
            }
        }
    }

    fun clearAllLocalRingtones(context: Context) {
        stopPreview()
        stopRinging()
        runCatching {
            File(context.filesDir, "ringtones").deleteRecursively()
        }
    }

    @Synchronized
    fun previewPlay(context: Context, id: String, name: String, uriString: String?) {
        val current = _previewState.value
        if (current.isPlaying && current.activeId == id) {
            stopPreview()
            return
        }
        stopPreview()
        stopRinging()

        val resolvedUri = resolvePlayableUri(context, uriString, isAlarm = true)
        if (resolvedUri == null) {
            _previewState.value = PreviewAudioState(
                activeId = null,
                activeName = "",
                isPlaying = false,
                error = "No audio URI available to preview."
            )
            return
        }

        try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(context.applicationContext, resolvedUri)
                setOnPreparedListener { player ->
                    player.start()
                    _previewState.value = PreviewAudioState(
                        activeId = id,
                        activeName = name,
                        isPlaying = true,
                        error = null
                    )
                }
                setOnCompletionListener {
                    stopPreview()
                }
                setOnErrorListener { _, _, _ ->
                    stopPreview()
                    _previewState.value = PreviewAudioState(error = "Unable to play \"$name\".")
                    true
                }
                prepareAsync()
            }
            previewPlayer = mp
        } catch (e: Exception) {
            Log.e(TAG, "Preview failed for $uriString", e)
            stopPreview()
            _previewState.value = PreviewAudioState(
                error = e.localizedMessage ?: "Unable to preview this audio."
            )
        }
    }

    @Synchronized
    fun stopPreview() {
        runCatching { previewPlayer?.stop() }
        runCatching { previewPlayer?.release() }
        previewPlayer = null
        _previewState.value = PreviewAudioState()
    }

    /**
     * Starts ringing the configured custom song or ringtone on Android's alarm audio stream.
     * Returns true if playback was started cleanly via MediaPlayer.
     */
    @Synchronized
    fun startRinging(
        context: Context,
        uriString: String?,
        songName: String,
        id: String,
        title: String,
        isAlarm: Boolean,
        vibrate: Boolean = true
    ): Boolean {
        stopPreview()
        stopRinging()

        val resolvedUri = resolvePlayableUri(context, uriString, isAlarm = isAlarm) ?: return false
        return try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(if (isAlarm) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(context.applicationContext, resolvedUri)
                isLooping = isAlarm
                setOnCompletionListener {
                    if (!isAlarm) {
                        stopRinging()
                    }
                }
                setOnErrorListener { _, _, _ ->
                    stopRinging()
                    true
                }
                prepare()
                start()
            }
            alarmPlayer = mp
            if (vibrate) {
                startVibration(context, repeat = isAlarm)
            }
            _ringingState.value = RingingState(
                id = id,
                title = title,
                songName = songName.ifBlank { "Custom Ringtone" },
                type = if (isAlarm) "ALARM" else "REMINDER",
                isRinging = true
            )

            // Safety cap: stop alarms after 3 minutes or reminders/timers after 25 seconds
            val capMs = if (isAlarm) 180_000L else 25_000L
            val runnable = Runnable { stopRinging() }
            autoStopRunnable = runnable
            mainHandler.postDelayed(runnable, capMs)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start ringing for $uriString", e)
            stopRinging()
            false
        }
    }

    @Synchronized
    fun stopRinging() {
        autoStopRunnable?.let { mainHandler.removeCallbacks(it) }
        autoStopRunnable = null
        runCatching { alarmPlayer?.stop() }
        runCatching { alarmPlayer?.release() }
        alarmPlayer = null
        runCatching { activeVibrator?.cancel() }
        activeVibrator = null
        _ringingState.value = null
    }

    private fun startVibration(context: Context, repeat: Boolean) {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return
            if (!vibrator.hasVibrator()) return
            activeVibrator = vibrator
            val pattern = longArrayOf(0L, 450L, 350L, 450L, 600L)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, if (repeat) 0 else -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, if (repeat) 0 else -1)
            }
        }
    }

    fun resolvePlayableUri(context: Context, uriString: String?, isAlarm: Boolean): Uri? {
        if (!uriString.isNullOrBlank()) {
            val parsed = runCatching { Uri.parse(uriString) }.getOrNull()
            if (parsed != null) {
                if (parsed.scheme == "file") {
                    val file = parsed.path?.let { File(it) }
                    if (file != null && file.exists() && file.length() > 0L) {
                        return parsed
                    }
                } else {
                    return parsed
                }
            }
        }
        val fallbackType = if (isAlarm) RingtoneManager.TYPE_ALARM else RingtoneManager.TYPE_NOTIFICATION
        return RingtoneManager.getDefaultUri(fallbackType)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
    }
}
