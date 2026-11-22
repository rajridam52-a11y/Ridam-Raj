package com.example.data.cloud

import android.content.Context
import android.os.Build
import com.example.R
import com.example.data.local.AlarmEntity
import com.example.data.local.AppDatabase
import com.example.data.local.BirthdayEntity
import com.example.data.local.CalendarEventEntity
import com.example.data.local.GoalEntity
import com.example.data.local.HabitEntity
import com.example.data.local.NoteEntity
import com.example.data.local.ReminderEntity
import com.example.data.local.TaskEntity
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

enum class ConflictPolicy(val label: String) {
    NEWEST_WINS("Newest Timestamp Wins"),
    PREFER_LOCAL("Overwrite Cloud with Local"),
    PREFER_CLOUD("Overwrite Local with Cloud")
}

data class BackupSyncResult(
    val uploadedCount: Int,
    val downloadedCount: Int,
    val timestampMillis: Long
)

class CloudBackupRepository(
    private val db: FirebaseFirestore,
    private val localDb: AppDatabase
) {
    constructor(context: Context, localDb: AppDatabase) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        localDb = localDb
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Please sign in with Google to use cloud backup.")
    }

    fun observeBackupSummary(userId: String): Flow<UserBackupSummary?> = flow {
        if (userId.isBlank() || auth.currentUser?.uid != userId) {
            emit(null)
            return@flow
        }
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(
                            UserBackupSummary::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun backupToCloud(conflictPolicy: ConflictPolicy = ConflictPolicy.NEWEST_WINS): Result<BackupSyncResult> {
        var currentPath = "users"
        return try {
            val uid = requireUserId()
            val userDoc = db.collection("users").document(uid)
            currentPath = userDoc.path

            val reminders = localDb.reminderDao().getAllOnce()
            val events = localDb.calendarEventDao().getAllOnce()
            val tasks = localDb.taskDao().getAllOnce()
            val alarms = localDb.alarmDao().getAllOnce()
            val birthdays = localDb.birthdayDao().getAllOnce()
            val notes = localDb.noteDao().getAllOnce()
            val goals = localDb.goalDao().getAllOnce()
            val habits = localDb.habitDao().getAllOnce()

            var uploaded = 0

            // Sync each subcollection cleanly respecting createdAt immutability & schema rules
            uploaded += upsertSubcollection(
                uid = uid,
                subcollection = "reminders",
                items = reminders.map { r ->
                    r.id to mapOf(
                        "title" to r.title.take(300).ifBlank { "Reminder" },
                        "notes" to r.notes.take(2000),
                        "triggerAtMillis" to r.triggerAtMillis.coerceAtLeast(0L),
                        "repeatType" to (r.repeatType.takeIf { it in listOf("NONE", "DAILY", "WEEKLY", "MONTHLY") } ?: "NONE"),
                        "category" to r.category.take(60).ifBlank { "General" },
                        "locationName" to r.locationName?.take(200),
                        "latitude" to r.latitude,
                        "longitude" to r.longitude,
                        "radiusMeters" to if (r.latitude != null) r.radiusMeters.coerceIn(50, 10000) else null,
                        "completed" to r.isCompleted,
                        "priority" to (r.priority.takeIf { it in listOf("LOW", "MEDIUM", "HIGH") } ?: "MEDIUM")
                    ).filterValues { it != null } as Map<String, Any>
                },
                localUpdatedAt = reminders.associate { it.id to it.updatedAtMillis },
                policy = conflictPolicy
            )

            uploaded += upsertSubcollection(
                uid = uid,
                subcollection = "events",
                items = events.map { e ->
                    val start = e.startMillis.coerceAtLeast(0L)
                    val end = e.endMillis.coerceAtLeast(start)
                    e.id to mapOf(
                        "title" to e.title.take(300).ifBlank { "Event" },
                        "description" to e.description.take(2000),
                        "startMillis" to start,
                        "endMillis" to end,
                        "category" to e.category.take(60).ifBlank { "Appointments" },
                        "allDay" to e.isAllDay,
                        "recurrence" to e.recurrence.take(40).ifBlank { "NONE" }
                    )
                },
                localUpdatedAt = events.associate { it.id to it.updatedAtMillis },
                policy = conflictPolicy
            )

            uploaded += upsertSubcollection(
                uid = uid,
                subcollection = "tasks",
                items = tasks.map { t ->
                    t.id to mapOf(
                        "title" to t.title.take(300).ifBlank { "Task" },
                        "description" to t.description.take(2000),
                        "priority" to (t.priority.takeIf { it in listOf("LOW", "MEDIUM", "HIGH") } ?: "MEDIUM"),
                        "category" to t.category.take(60).ifBlank { "Personal" },
                        "dueDateMillis" to t.dueDateMillis,
                        "completed" to t.isCompleted,
                        "recurrence" to t.recurrence.take(40).ifBlank { "NONE" }
                    ).filterValues { it != null } as Map<String, Any>
                },
                localUpdatedAt = tasks.associate { it.id to it.updatedAtMillis },
                policy = conflictPolicy
            )

            uploaded += upsertSubcollection(
                uid = uid,
                subcollection = "alarms",
                items = alarms.map { a ->
                    a.id to mapOf(
                        "hour" to a.hour.coerceIn(0, 23),
                        "minute" to a.minute.coerceIn(0, 59),
                        "label" to a.label.take(120),
                        "repeatDaysMask" to a.repeatDaysMask.coerceIn(0, 127),
                        "enabled" to a.enabled,
                        "vibrate" to a.vibrate,
                        "snoozeMinutes" to a.snoozeMinutes.coerceIn(1, 60)
                    )
                },
                localUpdatedAt = alarms.associate { it.id to it.updatedAtMillis },
                policy = conflictPolicy
            )

            uploaded += upsertSubcollection(
                uid = uid,
                subcollection = "birthdays",
                items = birthdays.map { b ->
                    b.id to mapOf(
                        "name" to b.name.take(150).ifBlank { "Friend" },
                        "month" to b.month.coerceIn(1, 12),
                        "day" to b.day.coerceIn(1, 31),
                        "birthYear" to b.birthYear?.coerceIn(1900, 2100),
                        "notes" to b.notes.take(1000),
                        "advanceDays" to b.advanceDays.coerceIn(0, 30)
                    ).filterValues { it != null } as Map<String, Any>
                },
                localUpdatedAt = birthdays.associate { it.id to it.updatedAtMillis },
                policy = conflictPolicy
            )

            uploaded += upsertSubcollection(
                uid = uid,
                subcollection = "notes",
                items = notes.map { n ->
                    n.id to mapOf(
                        "title" to n.title.take(300).ifBlank { "Note" },
                        "content" to n.content.take(20000),
                        "category" to n.category.take(60).ifBlank { "Quick Note" },
                        "pinned" to n.isPinned
                    )
                },
                localUpdatedAt = notes.associate { it.id to it.updatedAtMillis },
                policy = conflictPolicy
            )

            uploaded += upsertSubcollection(
                uid = uid,
                subcollection = "goals",
                items = goals.map { g ->
                    g.id to mapOf(
                        "title" to g.title.take(200).ifBlank { "Goal" },
                        "targetCount" to g.targetCount.coerceIn(1, 100000),
                        "currentCount" to g.currentCount.coerceIn(0, 100000),
                        "unit" to g.unit.take(40).ifBlank { "steps" },
                        "completed" to g.isCompleted
                    )
                },
                localUpdatedAt = goals.associate { it.id to it.updatedAtMillis },
                policy = conflictPolicy
            )

            uploaded += upsertSubcollection(
                uid = uid,
                subcollection = "habits",
                items = habits.map { h ->
                    h.id to mapOf(
                        "name" to h.name.take(150).ifBlank { "Habit" },
                        "category" to h.category.take(60).ifBlank { "Wellness" },
                        "streak" to h.streak.coerceIn(0, 50000),
                        "bestStreak" to h.bestStreak.coerceIn(0, 50000),
                        "historyDatesCsv" to h.historyDatesCsv.take(20000)
                    )
                },
                localUpdatedAt = habits.associate { it.id to it.updatedAtMillis },
                policy = conflictPolicy
            )

            val total = reminders.size + events.size + tasks.size + alarms.size +
                birthdays.size + notes.size + goals.size + habits.size

            val displayName = (auth.currentUser?.displayName ?: auth.currentUser?.email ?: "RR User").take(120)
            val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".take(120)

            val existingUserSnap = userDoc.get().await()
            if (existingUserSnap.exists()) {
                userDoc.update(
                    mapOf(
                        "displayName" to displayName,
                        "deviceName" to deviceName,
                        "totalItems" to total,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                userDoc.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to displayName,
                        "deviceName" to deviceName,
                        "totalItems" to total,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            Result.success(
                BackupSyncResult(
                    uploadedCount = uploaded,
                    downloadedCount = 0,
                    timestampMillis = System.currentTimeMillis()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, currentPath)
            Result.failure(e)
        }
    }

    private suspend fun upsertSubcollection(
        uid: String,
        subcollection: String,
        items: List<Pair<String, Map<String, Any>>>,
        localUpdatedAt: Map<String, Long>,
        policy: ConflictPolicy
    ): Int {
        if (items.isEmpty()) return 0
        val colRef = db.collection("users").document(uid).collection(subcollection)
        val existingSnap = colRef.get().await()
        val existingDocs = existingSnap.documents.associateBy { it.id }

        var count = 0
        for ((id, fields) in items) {
            val safeId = id.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
            val existingDoc = existingDocs[safeId]
            if (existingDoc != null) {
                val cloudMillis = existingDoc.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
                val localMillis = localUpdatedAt[id] ?: 0L
                val shouldPush = when (policy) {
                    ConflictPolicy.PREFER_LOCAL -> true
                    ConflictPolicy.PREFER_CLOUD -> false
                    ConflictPolicy.NEWEST_WINS -> localMillis >= cloudMillis
                }
                if (shouldPush) {
                    val updateMap = fields.toMutableMap()
                    updateMap["updatedAt"] = FieldValue.serverTimestamp()
                    colRef.document(safeId).update(updateMap).await()
                    count++
                }
            } else {
                val createMap = fields.toMutableMap()
                createMap["userId"] = uid
                createMap["createdAt"] = FieldValue.serverTimestamp()
                createMap["updatedAt"] = FieldValue.serverTimestamp()
                colRef.document(safeId).set(createMap).await()
                count++
            }
        }
        return count
    }

    suspend fun restoreFromCloud(conflictPolicy: ConflictPolicy = ConflictPolicy.PREFER_CLOUD): Result<BackupSyncResult> {
        var currentPath = "users"
        return try {
            val uid = requireUserId()
            val userDoc = db.collection("users").document(uid)
            currentPath = userDoc.path
            var downloaded = 0

            val remSnap = userDoc.collection("reminders").get().await()
            val localReminders = localDb.reminderDao().getAllOnce().associateBy { it.id }
            remSnap.toObjects(CloudReminder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE).forEach { cr ->
                val cloudTime = cr.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
                val localTime = localReminders[cr.id]?.updatedAtMillis ?: 0L
                if (conflictPolicy == ConflictPolicy.PREFER_CLOUD || cloudTime >= localTime) {
                    localDb.reminderDao().upsert(
                        ReminderEntity(
                            id = cr.id,
                            title = cr.title,
                            notes = cr.notes ?: "",
                            triggerAtMillis = cr.triggerAtMillis,
                            repeatType = cr.repeatType,
                            category = cr.category,
                            locationName = cr.locationName,
                            latitude = cr.latitude,
                            longitude = cr.longitude,
                            radiusMeters = (cr.radiusMeters ?: 150L).toInt(),
                            isCompleted = cr.completed,
                            priority = cr.priority,
                            createdAtMillis = cr.createdAt?.toDate()?.time ?: cloudTime,
                            updatedAtMillis = cloudTime
                        )
                    )
                    downloaded++
                }
            }

            val evtSnap = userDoc.collection("events").get().await()
            val localEvents = localDb.calendarEventDao().getAllOnce().associateBy { it.id }
            evtSnap.toObjects(CloudCalendarEvent::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE).forEach { ce ->
                val cloudTime = ce.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
                val localTime = localEvents[ce.id]?.updatedAtMillis ?: 0L
                if (conflictPolicy == ConflictPolicy.PREFER_CLOUD || cloudTime >= localTime) {
                    localDb.calendarEventDao().upsert(
                        CalendarEventEntity(
                            id = ce.id,
                            title = ce.title,
                            description = ce.description ?: "",
                            startMillis = ce.startMillis,
                            endMillis = ce.endMillis,
                            category = ce.category,
                            isAllDay = ce.allDay,
                            recurrence = ce.recurrence,
                            createdAtMillis = ce.createdAt?.toDate()?.time ?: cloudTime,
                            updatedAtMillis = cloudTime
                        )
                    )
                    downloaded++
                }
            }

            val taskSnap = userDoc.collection("tasks").get().await()
            val localTasks = localDb.taskDao().getAllOnce().associateBy { it.id }
            taskSnap.toObjects(CloudTask::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE).forEach { ct ->
                val cloudTime = ct.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
                val localTime = localTasks[ct.id]?.updatedAtMillis ?: 0L
                if (conflictPolicy == ConflictPolicy.PREFER_CLOUD || cloudTime >= localTime) {
                    localDb.taskDao().upsert(
                        TaskEntity(
                            id = ct.id,
                            title = ct.title,
                            description = ct.description ?: "",
                            priority = ct.priority,
                            category = ct.category,
                            dueDateMillis = ct.dueDateMillis,
                            isCompleted = ct.completed,
                            recurrence = ct.recurrence,
                            createdAtMillis = ct.createdAt?.toDate()?.time ?: cloudTime,
                            updatedAtMillis = cloudTime
                        )
                    )
                    downloaded++
                }
            }

            val alarmSnap = userDoc.collection("alarms").get().await()
            val localAlarms = localDb.alarmDao().getAllOnce().associateBy { it.id }
            alarmSnap.toObjects(CloudAlarm::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE).forEach { ca ->
                val cloudTime = ca.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
                val localTime = localAlarms[ca.id]?.updatedAtMillis ?: 0L
                if (conflictPolicy == ConflictPolicy.PREFER_CLOUD || cloudTime >= localTime) {
                    localDb.alarmDao().upsert(
                        AlarmEntity(
                            id = ca.id,
                            hour = ca.hour.toInt(),
                            minute = ca.minute.toInt(),
                            label = ca.label,
                            repeatDaysMask = ca.repeatDaysMask.toInt(),
                            enabled = ca.enabled,
                            vibrate = ca.vibrate,
                            snoozeMinutes = ca.snoozeMinutes.toInt(),
                            createdAtMillis = ca.createdAt?.toDate()?.time ?: cloudTime,
                            updatedAtMillis = cloudTime
                        )
                    )
                    downloaded++
                }
            }

            val bdaySnap = userDoc.collection("birthdays").get().await()
            bdaySnap.toObjects(CloudBirthday::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE).forEach { cb ->
                val cloudTime = cb.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
                localDb.birthdayDao().upsert(
                    BirthdayEntity(
                        id = cb.id,
                        name = cb.name,
                        month = cb.month.toInt(),
                        day = cb.day.toInt(),
                        birthYear = cb.birthYear?.toInt(),
                        notes = cb.notes ?: "",
                        advanceDays = cb.advanceDays.toInt(),
                        createdAtMillis = cb.createdAt?.toDate()?.time ?: cloudTime,
                        updatedAtMillis = cloudTime
                    )
                )
                downloaded++
            }

            val noteSnap = userDoc.collection("notes").get().await()
            noteSnap.toObjects(CloudNote::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE).forEach { cn ->
                val cloudTime = cn.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
                localDb.noteDao().upsert(
                    NoteEntity(
                        id = cn.id,
                        title = cn.title,
                        content = cn.content,
                        category = cn.category,
                        isPinned = cn.pinned,
                        createdAtMillis = cn.createdAt?.toDate()?.time ?: cloudTime,
                        updatedAtMillis = cloudTime
                    )
                )
                downloaded++
            }

            val goalSnap = userDoc.collection("goals").get().await()
            goalSnap.toObjects(CloudGoal::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE).forEach { cg ->
                val cloudTime = cg.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
                localDb.goalDao().upsert(
                    GoalEntity(
                        id = cg.id,
                        title = cg.title,
                        targetCount = cg.targetCount.toInt(),
                        currentCount = cg.currentCount.toInt(),
                        unit = cg.unit,
                        isCompleted = cg.completed,
                        createdAtMillis = cg.createdAt?.toDate()?.time ?: cloudTime,
                        updatedAtMillis = cloudTime
                    )
                )
                downloaded++
            }

            val habitSnap = userDoc.collection("habits").get().await()
            habitSnap.toObjects(CloudHabit::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE).forEach { ch ->
                val cloudTime = ch.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
                localDb.habitDao().upsert(
                    HabitEntity(
                        id = ch.id,
                        name = ch.name,
                        category = ch.category,
                        streak = ch.streak.toInt(),
                        bestStreak = ch.bestStreak.toInt(),
                        historyDatesCsv = ch.historyDatesCsv,
                        createdAtMillis = ch.createdAt?.toDate()?.time ?: cloudTime,
                        updatedAtMillis = cloudTime
                    )
                )
                downloaded++
            }

            Result.success(
                BackupSyncResult(
                    uploadedCount = 0,
                    downloadedCount = downloaded,
                    timestampMillis = System.currentTimeMillis()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, currentPath)
            Result.failure(e)
        }
    }

    suspend fun deleteAllCloudData(): Result<Unit> {
        var currentPath = "users"
        return try {
            val uid = requireUserId()
            val userDoc = db.collection("users").document(uid)
            currentPath = userDoc.path
            val subcollections = listOf(
                "reminders", "events", "tasks", "alarms",
                "birthdays", "notes", "goals", "habits"
            )
            for (sub in subcollections) {
                val docs = userDoc.collection(sub).get().await()
                for (doc in docs.documents) {
                    doc.reference.delete().await()
                }
            }
            userDoc.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, currentPath)
            Result.failure(e)
        }
    }
}
