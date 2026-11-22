package com.example.data.cloud

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserBackupSummary(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val deviceName: String? = null,
    val totalItems: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudReminder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val notes: String? = null,
    val triggerAtMillis: Long = 0L,
    val repeatType: String = "NONE",
    val category: String = "General",
    val locationName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusMeters: Long? = null,
    val completed: Boolean = false,
    val priority: String = "MEDIUM",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudCalendarEvent(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String? = null,
    val startMillis: Long = 0L,
    val endMillis: Long = 0L,
    val category: String = "Appointments",
    val allDay: Boolean = false,
    val recurrence: String = "NONE",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudTask(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String? = null,
    val priority: String = "MEDIUM",
    val category: String = "Personal",
    val dueDateMillis: Long? = null,
    val completed: Boolean = false,
    val recurrence: String = "NONE",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudAlarm(
    @DocumentId val id: String = "",
    val userId: String = "",
    val hour: Long = 7L,
    val minute: Long = 0L,
    val label: String = "Alarm",
    val repeatDaysMask: Long = 0L,
    val enabled: Boolean = true,
    val vibrate: Boolean = true,
    val snoozeMinutes: Long = 10L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudBirthday(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val month: Long = 1L,
    val day: Long = 1L,
    val birthYear: Long? = null,
    val notes: String? = null,
    val advanceDays: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudNote(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val category: String = "Quick Note",
    val pinned: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudGoal(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val targetCount: Long = 1L,
    val currentCount: Long = 0L,
    val unit: String = "steps",
    val completed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudHabit(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val category: String = "Wellness",
    val streak: Long = 0L,
    val bestStreak: Long = 0L,
    val historyDatesCsv: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
