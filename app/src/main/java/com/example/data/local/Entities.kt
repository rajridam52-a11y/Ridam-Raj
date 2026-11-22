package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val notes: String = "",
    val triggerAtMillis: Long,
    val repeatType: String = "NONE", // NONE, DAILY, WEEKLY, MONTHLY
    val category: String = "General",
    val locationName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusMeters: Int = 150,
    val isCompleted: Boolean = false,
    val priority: String = "MEDIUM", // LOW, MEDIUM, HIGH
    val soundName: String = "Default",
    val snoozeMinutes: Int = 10,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val startMillis: Long,
    val endMillis: Long,
    val category: String = "Appointments", // Appointments, Study, Important, Birthday, Personal
    val isAllDay: Boolean = false,
    val recurrence: String = "NONE",
    val reminderMinutesBefore: Int = 15,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val priority: String = "MEDIUM", // LOW, MEDIUM, HIGH
    val category: String = "Personal",
    val dueDateMillis: Long? = null,
    val isCompleted: Boolean = false,
    val completedAtMillis: Long? = null,
    val recurrence: String = "NONE",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val hour: Int,
    val minute: Int,
    val label: String = "Alarm",
    val repeatDaysMask: Int = 0, // bit 0=Sun .. bit 6=Sat
    val enabled: Boolean = true,
    val vibrate: Boolean = true,
    val soundType: String = "Cosmic Chime",
    val snoozeMinutes: Int = 10,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "birthdays")
data class BirthdayEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val month: Int, // 1..12
    val day: Int,   // 1..31
    val birthYear: Int? = null,
    val notes: String = "",
    val photoUri: String? = null,
    val advanceDays: Int = 1,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val category: String = "Quick Note",
    val isPinned: Boolean = false,
    val isVoiceNote: Boolean = false,
    val audioPath: String? = null,
    val durationSeconds: Int = 0,
    val transcription: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val targetCount: Int = 1,
    val currentCount: Int = 0,
    val unit: String = "steps",
    val deadlineMillis: Long? = null,
    val category: String = "Daily",
    val isCompleted: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String = "Wellness",
    val targetPerWeek: Int = 7,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val historyDatesCsv: String = "", // yyyy-MM-dd comma-separated
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "world_cities")
data class WorldCityEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val cityName: String,
    val country: String,
    val timeZoneId: String,
    val isFavorite: Boolean = false,
    val sortOrder: Int = 0
)

@Entity(tableName = "notification_logs")
data class NotificationLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val body: String,
    val category: String,
    val sourceApp: String = "RR Reminder",
    val timestampMillis: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@Entity(tableName = "speed_tests")
data class SpeedTestResultEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val downloadMbps: Double,
    val uploadMbps: Double,
    val latencyMs: Long,
    val timestampMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "calc_history")
data class CalcHistoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val expression: String,
    val result: String,
    val timestampMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "pomodoro_sessions")
data class PomodoroSessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val label: String,
    val durationMinutes: Int,
    val sessionType: String, // FOCUS, SHORT_BREAK, LONG_BREAK
    val completedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val role: String, // "user" or "assistant"
    val text: String,
    val actionSummary: String? = null,
    val timestampMillis: Long = System.currentTimeMillis()
)
