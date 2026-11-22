package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ReminderEntity::class,
        CalendarEventEntity::class,
        TaskEntity::class,
        AlarmEntity::class,
        BirthdayEntity::class,
        NoteEntity::class,
        GoalEntity::class,
        HabitEntity::class,
        WorldCityEntity::class,
        NotificationLogEntity::class,
        SpeedTestResultEntity::class,
        CalcHistoryEntity::class,
        PomodoroSessionEntity::class,
        ChatMessageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun calendarEventDao(): CalendarEventDao
    abstract fun taskDao(): TaskDao
    abstract fun alarmDao(): AlarmDao
    abstract fun birthdayDao(): BirthdayDao
    abstract fun noteDao(): NoteDao
    abstract fun goalDao(): GoalDao
    abstract fun habitDao(): HabitDao
    abstract fun worldCityDao(): WorldCityDao
    abstract fun notificationLogDao(): NotificationLogDao
    abstract fun utilityDao(): UtilityDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rr_reminder_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
