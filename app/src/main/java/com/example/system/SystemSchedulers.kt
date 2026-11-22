package com.example.system

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import com.example.data.local.AlarmEntity
import com.example.data.local.ReminderEntity
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import java.util.Calendar

object NotificationChannels {
    const val CHANNEL_ALARM = "rr_alarms"
    const val CHANNEL_REMINDER = "rr_reminders"
    const val CHANNEL_TIMER = "rr_timers"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val alarmAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val channels = listOf(
            NotificationChannel(
                CHANNEL_ALARM,
                "Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Scheduled alarms from RR Reminder"
                enableVibration(true)
                setSound(alarmSound, alarmAttrs)
            },
            NotificationChannel(
                CHANNEL_REMINDER,
                "Reminders & Events",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Time and location reminders"
                enableVibration(true)
            },
            NotificationChannel(
                CHANNEL_TIMER,
                "Timers & Pomodoro",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Countdown timer and focus session alerts"
                enableVibration(true)
            }
        )
        channels.forEach { nm.createNotificationChannel(it) }
    }
}

object SystemSchedulers {
    private const val TAG = "SystemSchedulers"

    fun nextAlarmTriggerMillis(hour: Int, minute: Int, repeatDaysMask: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (repeatDaysMask == 0) {
            if (target.timeInMillis <= now.timeInMillis) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis
        }
        for (offset in 0..7) {
            val candidate = (target.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, offset)
            }
            val dayBit = candidate.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sun..6=Sat
            if ((repeatDaysMask and (1 shl dayBit)) != 0 &&
                candidate.timeInMillis > now.timeInMillis
            ) {
                return candidate.timeInMillis
            }
        }
        if (target.timeInMillis <= now.timeInMillis) target.add(Calendar.DAY_OF_YEAR, 1)
        return target.timeInMillis
    }

    fun scheduleAlarm(context: Context, alarm: AlarmEntity): Boolean {
        NotificationChannels.ensureCreated(context)
        if (!alarm.enabled) {
            cancelAlarm(context, alarm.id)
            return true
        }
        val triggerAt = nextAlarmTriggerMillis(alarm.hour, alarm.minute, alarm.repeatDaysMask)
        return scheduleExactOrFallback(
            context = context,
            requestCode = alarm.id.hashCode(),
            triggerAtMillis = triggerAt,
            intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra(AlarmReceiver.EXTRA_TYPE, "ALARM")
                putExtra(AlarmReceiver.EXTRA_ID, alarm.id)
                putExtra(AlarmReceiver.EXTRA_TITLE, alarm.label.ifBlank { "RR Alarm" })
                putExtra(AlarmReceiver.EXTRA_SNOOZE, alarm.snoozeMinutes)
                putExtra(AlarmReceiver.EXTRA_VIBRATE, alarm.vibrate)
                putExtra(AlarmReceiver.EXTRA_SOUND_NAME, alarm.soundType)
                putExtra(AlarmReceiver.EXTRA_SOUND_URI, alarm.soundUri)
            }
        )
    }

    fun cancelAlarm(context: Context, id: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            Intent(context, AlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(pi)
    }

    fun scheduleReminder(context: Context, reminder: ReminderEntity): Boolean {
        NotificationChannels.ensureCreated(context)
        if (reminder.isCompleted || reminder.triggerAtMillis <= System.currentTimeMillis()) {
            return false
        }
        return scheduleExactOrFallback(
            context = context,
            requestCode = reminder.id.hashCode(),
            triggerAtMillis = reminder.triggerAtMillis,
            intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra(AlarmReceiver.EXTRA_TYPE, "REMINDER")
                putExtra(AlarmReceiver.EXTRA_ID, reminder.id)
                putExtra(AlarmReceiver.EXTRA_TITLE, reminder.title)
                putExtra(AlarmReceiver.EXTRA_BODY, reminder.notes.ifBlank { reminder.category })
                putExtra(AlarmReceiver.EXTRA_SNOOZE, reminder.snoozeMinutes)
                putExtra(AlarmReceiver.EXTRA_VIBRATE, true)
                putExtra(AlarmReceiver.EXTRA_SOUND_NAME, reminder.soundName)
                putExtra(AlarmReceiver.EXTRA_SOUND_URI, reminder.soundUri)
            }
        )
    }

    fun cancelReminder(context: Context, id: String) {
        cancelAlarm(context, id)
    }

    private fun scheduleExactOrFallback(
        context: Context,
        requestCode: Int,
        triggerAtMillis: Long,
        intent: Intent
    ): Boolean {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            }
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm permission denied; using inexact fallback", e)
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm", e)
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun registerLocationGeofence(
        context: Context,
        reminder: ReminderEntity,
        onResult: (Boolean, String) -> Unit
    ) {
        val lat = reminder.latitude
        val lon = reminder.longitude
        if (lat == null || lon == null) {
            onResult(false, "Coordinates are required for geofencing.")
            return
        }
        try {
            val client = LocationServices.getGeofencingClient(context)
            val geofence = Geofence.Builder()
                .setRequestId(reminder.id)
                .setCircularRegion(lat, lon, reminder.radiusMeters.toFloat().coerceIn(50f, 10000f))
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()
            val req = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build()
            val pi = PendingIntent.getBroadcast(
                context,
                reminder.id.hashCode(),
                Intent(context, GeofenceReceiver::class.java).apply {
                    putExtra(GeofenceReceiver.EXTRA_REMINDER_TITLE, reminder.title)
                    putExtra(GeofenceReceiver.EXTRA_LOCATION_NAME, reminder.locationName ?: "Saved Place")
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            client.addGeofences(req, pi)
                .addOnSuccessListener { onResult(true, "Geofence active") }
                .addOnFailureListener { e ->
                    onResult(false, e.localizedMessage ?: "Geofence registration failed")
                }
        } catch (e: SecurityException) {
            onResult(false, "Location permission required to register geofences.")
        } catch (e: Exception) {
            onResult(false, e.localizedMessage ?: "Geofence unavailable on this device.")
        }
    }
}
