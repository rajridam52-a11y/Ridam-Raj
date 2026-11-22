package com.example.ui.viewmodel

import com.example.data.cloud.BackupSyncResult
import com.example.data.cloud.ConflictPolicy
import com.example.data.cloud.UserBackupSummary
import com.example.data.remote.IndiaCityWeatherSummary
import com.example.data.remote.WeatherSnapshot
import java.util.UUID

sealed interface WeatherUiState {
    data object Loading : WeatherUiState
    data class Success(val snapshot: WeatherSnapshot) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
}

sealed interface AllIndiaWeatherUiState {
    data object Loading : AllIndiaWeatherUiState
    data class Success(val cities: List<IndiaCityWeatherSummary>) : AllIndiaWeatherUiState
    data class Error(val message: String) : AllIndiaWeatherUiState
}

sealed interface CloudSyncUiState {
    data object Idle : CloudSyncUiState
    data object Syncing : CloudSyncUiState
    data class Success(val message: String, val result: BackupSyncResult? = null) : CloudSyncUiState
    data class Error(val message: String) : CloudSyncUiState
}

data class CountdownTimerItem(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val totalSeconds: Int,
    val remainingSeconds: Int,
    val isRunning: Boolean
)

enum class PomodoroPhase(val label: String, val defaultMinutes: Int) {
    FOCUS("Focus Session", 25),
    SHORT_BREAK("Short Break", 5),
    LONG_BREAK("Long Break", 15)
}

data class PomodoroState(
    val phase: PomodoroPhase = PomodoroPhase.FOCUS,
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val remainingSeconds: Int = 25 * 60,
    val isRunning: Boolean = false
)

data class StopwatchState(
    val elapsedMillis: Long = 0L,
    val isRunning: Boolean = false,
    val laps: List<Long> = emptyList()
)

data class CloudAccountInfo(
    val uid: String,
    val displayName: String,
    val email: String,
    val summary: UserBackupSummary? = null,
    val conflictPolicy: ConflictPolicy = ConflictPolicy.NEWEST_WINS
)
