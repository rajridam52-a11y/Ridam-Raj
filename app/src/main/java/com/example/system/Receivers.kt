package com.example.system

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.NotificationLogEntity
import com.example.data.prefs.PreferencesManager
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationChannels.ensureCreated(context)
        val type = intent.getStringExtra(EXTRA_TYPE) ?: "REMINDER"
        val id = intent.getStringExtra(EXTRA_ID) ?: System.currentTimeMillis().toString()
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "RR Reminder"
        val body = intent.getStringExtra(EXTRA_BODY) ?: if (type == "ALARM") "Alarm ringing" else "Scheduled reminder"
        val snoozeMins = intent.getIntExtra(EXTRA_SNOOZE, 10)
        val vibrate = intent.getBooleanExtra(EXTRA_VIBRATE, true)
        val extraSoundUri = intent.getStringExtra(EXTRA_SOUND_URI)
        val extraSoundName = intent.getStringExtra(EXTRA_SOUND_NAME)

        val channelId = if (type == "ALARM") NotificationChannels.CHANNEL_ALARM else NotificationChannels.CHANNEL_REMINDER
        val isAlarm = type == "ALARM"

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = runCatching { PreferencesManager(context).preferencesFlow.first() }.getOrNull()
                val effectiveUri = extraSoundUri?.takeIf { it.isNotBlank() } ?: prefs?.defaultRingtoneUri
                val effectiveName = extraSoundName?.takeIf { it.isNotBlank() } ?: prefs?.defaultRingtoneName ?: "System Default"

                val customSongStarted = if (!effectiveUri.isNullOrBlank() || isAlarm) {
                    RingtoneAudioController.startRinging(
                        context = context,
                        uriString = effectiveUri,
                        songName = effectiveName,
                        id = id,
                        title = title,
                        isAlarm = isAlarm,
                        vibrate = vibrate
                    )
                } else {
                    false
                }

                val openIntent = PendingIntent.getActivity(
                    context,
                    id.hashCode(),
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val snoozeIntent = PendingIntent.getBroadcast(
                    context,
                    (id + "_snooze").hashCode(),
                    Intent(context, NotificationActionReceiver::class.java).apply {
                        action = NotificationActionReceiver.ACTION_SNOOZE
                        putExtra(EXTRA_ID, id)
                        putExtra(EXTRA_TITLE, title)
                        putExtra(EXTRA_TYPE, type)
                        putExtra(EXTRA_SNOOZE, snoozeMins)
                        putExtra(EXTRA_SOUND_URI, effectiveUri)
                        putExtra(EXTRA_SOUND_NAME, effectiveName)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val completeIntent = PendingIntent.getBroadcast(
                    context,
                    (id + "_done").hashCode(),
                    Intent(context, NotificationActionReceiver::class.java).apply {
                        action = NotificationActionReceiver.ACTION_COMPLETE
                        putExtra(EXTRA_ID, id)
                        putExtra(EXTRA_TYPE, type)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val dismissDeleteIntent = PendingIntent.getBroadcast(
                    context,
                    (id + "_dismiss_swipe").hashCode(),
                    Intent(context, NotificationActionReceiver::class.java).apply {
                        action = NotificationActionReceiver.ACTION_DISMISS_RINGING
                        putExtra(EXTRA_ID, id)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notifBuilder = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(title)
                    .setContentText(
                        if (!effectiveUri.isNullOrBlank()) "$body • ♪ $effectiveName" else body
                    )
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(if (isAlarm) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
                    .setAutoCancel(true)
                    .setContentIntent(openIntent)
                    .setDeleteIntent(dismissDeleteIntent)
                    .addAction(0, "Snooze ${snoozeMins}m", snoozeIntent)
                    .addAction(0, if (isAlarm) "Dismiss" else "Complete", completeIntent)

                if (customSongStarted) {
                    notifBuilder.setSilent(true)
                }

                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(id.hashCode(), notifBuilder.build())

                AppDatabase.getInstance(context).notificationLogDao().insert(
                    NotificationLogEntity(
                        title = title,
                        body = if (!effectiveUri.isNullOrBlank()) "$body (♪ $effectiveName)" else body,
                        category = type
                    )
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_TYPE = "extra_type"
        const val EXTRA_ID = "extra_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_BODY = "extra_body"
        const val EXTRA_SNOOZE = "extra_snooze"
        const val EXTRA_VIBRATE = "extra_vibrate"
        const val EXTRA_SOUND_URI = "extra_sound_uri"
        const val EXTRA_SOUND_NAME = "extra_sound_name"
    }
}

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        RingtoneAudioController.stopRinging()
        val id = intent.getStringExtra(AlarmReceiver.EXTRA_ID) ?: return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(id.hashCode())

        when (intent.action) {
            ACTION_SNOOZE -> {
                val mins = intent.getIntExtra(AlarmReceiver.EXTRA_SNOOZE, 10)
                val title = intent.getStringExtra(AlarmReceiver.EXTRA_TITLE) ?: "RR Reminder"
                val type = intent.getStringExtra(AlarmReceiver.EXTRA_TYPE) ?: "REMINDER"
                val soundUri = intent.getStringExtra(AlarmReceiver.EXTRA_SOUND_URI)
                val soundName = intent.getStringExtra(AlarmReceiver.EXTRA_SOUND_NAME)
                val triggerAt = System.currentTimeMillis() + mins * 60_000L
                val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val pi = PendingIntent.getBroadcast(
                    context,
                    (id + "_snoozed").hashCode(),
                    Intent(context, AlarmReceiver::class.java).apply {
                        putExtra(AlarmReceiver.EXTRA_ID, id)
                        putExtra(AlarmReceiver.EXTRA_TITLE, "$title (Snoozed)")
                        putExtra(AlarmReceiver.EXTRA_TYPE, type)
                        putExtra(AlarmReceiver.EXTRA_SNOOZE, mins)
                        putExtra(AlarmReceiver.EXTRA_SOUND_URI, soundUri)
                        putExtra(AlarmReceiver.EXTRA_SOUND_NAME, soundName)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                    } else {
                        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                    }
                } catch (_: SecurityException) {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                }
            }
            ACTION_COMPLETE -> {
                val type = intent.getStringExtra(AlarmReceiver.EXTRA_TYPE) ?: "REMINDER"
                if (type == "REMINDER") {
                    val pending = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val dao = AppDatabase.getInstance(context).reminderDao()
                            val existing = dao.getAllOnce().firstOrNull { it.id == id }
                            if (existing != null) {
                                dao.upsert(existing.copy(isCompleted = true, updatedAtMillis = System.currentTimeMillis()))
                            }
                        } finally {
                            pending.finish()
                        }
                    }
                }
            }
            ACTION_DISMISS_RINGING -> Unit
        }
    }

    companion object {
        const val ACTION_SNOOZE = "com.example.ACTION_SNOOZE"
        const val ACTION_COMPLETE = "com.example.ACTION_COMPLETE"
        const val ACTION_DISMISS_RINGING = "com.example.ACTION_DISMISS_RINGING"
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                db.alarmDao().getAllOnce().filter { it.enabled }.forEach { alarm ->
                    SystemSchedulers.scheduleAlarm(context, alarm)
                }
                db.reminderDao().getAllOnce().filter { !it.isCompleted }.forEach { rem ->
                    SystemSchedulers.scheduleReminder(context, rem)
                }
            } catch (e: Exception) {
                Log.e("BootReceiver", "Failed to reschedule on boot", e)
            } finally {
                pending.finish()
            }
        }
    }
}

class GeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) {
            Log.e("GeofenceReceiver", "Geofence error code: ${event.errorCode}")
            return
        }
        if (event.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            NotificationChannels.ensureCreated(context)
            val title = intent.getStringExtra(EXTRA_REMINDER_TITLE) ?: "Location Reminder"
            val place = intent.getStringExtra(EXTRA_LOCATION_NAME) ?: "Target location"
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notif = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_REMINDER)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText("You arrived at $place")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            nm.notify(title.hashCode(), notif)
        }
    }

    companion object {
        const val EXTRA_REMINDER_TITLE = "extra_rem_title"
        const val EXTRA_LOCATION_NAME = "extra_loc_name"
    }
}
