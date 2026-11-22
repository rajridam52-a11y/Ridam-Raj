package com.example.system

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import com.example.data.local.AppDatabase
import com.example.data.local.NotificationLogEntity
import com.example.data.prefs.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

class RrNotificationListenerService : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var job: Job? = null

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(applicationContext) { status ->
            ttsReady = (status == TextToSpeech.SUCCESS)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val pkg = sbn?.packageName ?: return
        if (pkg == packageName) return
        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence("android.title")?.toString().orEmpty()
        val text = extras.getCharSequence("android.text")?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return

        job?.cancel()
        job = scope.launch {
            val prefs = PreferencesManager(applicationContext).preferencesFlow.first()
            if (!prefs.notificationReaderEnabled) return@launch

            AppDatabase.getInstance(applicationContext).notificationLogDao().insert(
                NotificationLogEntity(
                    title = title.ifBlank { "Notification" },
                    body = text,
                    category = "EXTERNAL",
                    sourceApp = pkg
                )
            )

            if (ttsReady) {
                tts?.language = Locale.getDefault()
                tts?.speak("Notification: $title. $text", TextToSpeech.QUEUE_ADD, null, "notif_${sbn.id}")
            }
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        scope.cancel()
        super.onDestroy()
    }
}
