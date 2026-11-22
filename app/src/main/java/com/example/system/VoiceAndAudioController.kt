package com.example.system

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

class VoiceNoteRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var currentFile: File? = null
    private var startTimeMillis: Long = 0L

    fun startRecording(): Result<File> {
        return try {
            stopRecording()
            val dir = File(context.filesDir, "voice_notes").apply { mkdirs() }
            val file = File(dir, "vn_${System.currentTimeMillis()}.m4a")
            val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(128000)
            r.setAudioSamplingRate(44100)
            r.setOutputFile(file.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            currentFile = file
            startTimeMillis = System.currentTimeMillis()
            Result.success(file)
        } catch (e: Exception) {
            Log.e("VoiceNoteRecorder", "Failed to start recording", e)
            Result.failure(e)
        }
    }

    fun stopRecording(): Pair<String?, Int> {
        val file = currentFile
        val elapsedSec = ((System.currentTimeMillis() - startTimeMillis) / 1000L).toInt().coerceAtLeast(1)
        try {
            recorder?.stop()
        } catch (_: Exception) {
        }
        try {
            recorder?.release()
        } catch (_: Exception) {
        }
        recorder = null
        currentFile = null
        return file?.absolutePath to elapsedSec
    }

    fun playAudio(path: String, onCompletion: () -> Unit, onError: (String) -> Unit) {
        stopPlayback()
        try {
            val mp = MediaPlayer().apply {
                setDataSource(path)
                prepare()
                setOnCompletionListener {
                    stopPlayback()
                    onCompletion()
                }
                start()
            }
            player = mp
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Unable to play recording")
        }
    }

    fun stopPlayback() {
        try {
            player?.stop()
        } catch (_: Exception) {
        }
        try {
            player?.release()
        } catch (_: Exception) {
        }
        player = null
    }
}
