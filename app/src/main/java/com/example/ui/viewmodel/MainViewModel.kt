package com.example.ui.viewmodel

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.cloud.CloudBackupRepository
import com.example.data.cloud.ConflictPolicy
import com.example.data.cloud.UserBackupSummary
import com.example.data.local.AlarmEntity
import com.example.data.local.AppDatabase
import com.example.data.local.BirthdayEntity
import com.example.data.local.CalcHistoryEntity
import com.example.data.local.CalendarEventEntity
import com.example.data.local.GoalEntity
import com.example.data.local.HabitEntity
import com.example.data.local.NoteEntity
import com.example.data.local.NotificationLogEntity
import com.example.data.local.PomodoroSessionEntity
import com.example.data.local.ReminderEntity
import com.example.data.local.SpeedTestResultEntity
import com.example.data.local.TaskEntity
import com.example.data.local.WorldCityEntity
import com.example.data.prefs.AppUserPreferences
import com.example.data.prefs.DesignSettings
import com.example.data.prefs.PreferencesManager
import com.example.data.remote.GeocodingResultDto
import com.example.data.remote.WeatherRepository
import com.example.security.SecurityManager
import com.example.system.BatteryMonitor
import com.example.system.BatteryStatusInfo
import com.example.system.NotificationChannels
import com.example.system.SpeedTestRunner
import com.example.system.SystemSchedulers
import com.example.system.VoiceNoteRecorder
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(
    private val appContext: Context,
    private val db: AppDatabase,
    private val prefsManager: PreferencesManager,
    private val weatherRepo: WeatherRepository,
    private val cloudRepo: CloudBackupRepository,
    val voiceRecorder: VoiceNoteRecorder
) : ViewModel() {

    val preferences: StateFlow<AppUserPreferences> = prefsManager.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppUserPreferences())

    val reminders = db.reminderDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val events = db.calendarEventDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val tasks = db.taskDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val alarms = db.alarmDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val birthdays = db.birthdayDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val notes = db.noteDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val goals = db.goalDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val habits = db.habitDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val worldCities = db.worldCityDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val notificationLogs = db.notificationLogDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val speedTests = db.utilityDao().observeSpeedTests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val calcHistory = db.utilityDao().observeCalcHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val pomodoroSessions = db.utilityDao().observePomodoroSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _nowMillis = MutableStateFlow(System.currentTimeMillis())
    val nowMillis: StateFlow<Long> = _nowMillis.asStateFlow()

    private val _isOnline = MutableStateFlow(checkConnectivity())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _batteryStatus = MutableStateFlow(BatteryMonitor.readStatus(appContext))
    val batteryStatus: StateFlow<BatteryStatusInfo> = _batteryStatus.asStateFlow()

    private val _weatherState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val weatherState: StateFlow<WeatherUiState> = _weatherState.asStateFlow()

    private val _allIndiaWeatherState = MutableStateFlow<AllIndiaWeatherUiState>(AllIndiaWeatherUiState.Loading)
    val allIndiaWeatherState: StateFlow<AllIndiaWeatherUiState> = _allIndiaWeatherState.asStateFlow()

    private val _citySearchResults = MutableStateFlow<List<GeocodingResultDto>>(emptyList())
    val citySearchResults: StateFlow<List<GeocodingResultDto>> = _citySearchResults.asStateFlow()

    private val _stopwatch = MutableStateFlow(StopwatchState())
    val stopwatch: StateFlow<StopwatchState> = _stopwatch.asStateFlow()

    private val _timers = MutableStateFlow<List<CountdownTimerItem>>(emptyList())
    val timers: StateFlow<List<CountdownTimerItem>> = _timers.asStateFlow()

    private val _pomodoro = MutableStateFlow(PomodoroState())
    val pomodoro: StateFlow<PomodoroState> = _pomodoro.asStateFlow()

    private val _speedTestRunning = MutableStateFlow(false)
    val speedTestRunning: StateFlow<Boolean> = _speedTestRunning.asStateFlow()

    private val _speedTestError = MutableStateFlow<String?>(null)
    val speedTestError: StateFlow<String?> = _speedTestError.asStateFlow()

    private val _cloudSyncState = MutableStateFlow<CloudSyncUiState>(CloudSyncUiState.Idle)
    val cloudSyncState: StateFlow<CloudSyncUiState> = _cloudSyncState.asStateFlow()

    private val _cloudSummary = MutableStateFlow<UserBackupSummary?>(null)
    val cloudSummary: StateFlow<UserBackupSummary?> = _cloudSummary.asStateFlow()

    private val _conflictPolicy = MutableStateFlow(ConflictPolicy.NEWEST_WINS)
    val conflictPolicy: StateFlow<ConflictPolicy> = _conflictPolicy.asStateFlow()

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    private var stopwatchBaseElapsed = 0L
    private var stopwatchStartRealtime = 0L
    private var cloudObserverJob: Job? = null
    private var lockCheckedInitial = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            val statusIntent = if (intent?.action == Intent.ACTION_BATTERY_CHANGED) intent else null
            _batteryStatus.value = BatteryMonitor.readStatus(appContext, statusIntent)
        }
    }

    init {
        NotificationChannels.ensureCreated(appContext)
        seedDefaultWorldCities()
        registerRealTimeBatteryReceiver()
        startClockAndTimersLoop()
        observeNetworkConnectivity()

        viewModelScope.launch {
            val prefs = prefsManager.preferencesFlow.first()
            if (!lockCheckedInitial) {
                lockCheckedInitial = true
                if (prefs.appLockEnabled && prefs.pinHash.isNotBlank()) {
                    _isLocked.value = true
                }
            }
            if (!prefs.offlineModePreferred) {
                refreshWeather(prefs.weatherCityName, prefs.weatherLat, prefs.weatherLon)
                refreshAllIndiaWeather(forceRefresh = false)
            } else {
                _weatherState.value = WeatherUiState.Error("Offline mode is enabled.")
                _allIndiaWeatherState.value = AllIndiaWeatherUiState.Error("Offline mode is enabled.")
            }
        }
    }

    private fun registerRealTimeBatteryReceiver() {
        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_BATTERY_CHANGED)
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
            }
            val sticky = ContextCompat.registerReceiver(
                appContext,
                batteryReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            _batteryStatus.value = BatteryMonitor.readStatus(appContext, sticky)
        } catch (_: Exception) {
            _batteryStatus.value = BatteryMonitor.readStatus(appContext)
        }
    }

    fun refreshBatteryStatus() {
        _batteryStatus.value = BatteryMonitor.readStatus(appContext)
    }

    private fun seedDefaultWorldCities() {
        viewModelScope.launch {
            if (db.worldCityDao().count() == 0) {
                db.worldCityDao().insertAll(
                    listOf(
                        WorldCityEntity(cityName = "New Delhi", country = "India", timeZoneId = "Asia/Kolkata", isFavorite = true, sortOrder = 0),
                        WorldCityEntity(cityName = "London", country = "United Kingdom", timeZoneId = "Europe/London", isFavorite = true, sortOrder = 1),
                        WorldCityEntity(cityName = "New York", country = "United States", timeZoneId = "America/New_York", isFavorite = false, sortOrder = 2),
                        WorldCityEntity(cityName = "Tokyo", country = "Japan", timeZoneId = "Asia/Tokyo", isFavorite = false, sortOrder = 3),
                        WorldCityEntity(cityName = "Dubai", country = "UAE", timeZoneId = "Asia/Dubai", isFavorite = false, sortOrder = 4),
                        WorldCityEntity(cityName = "Paris", country = "France", timeZoneId = "Europe/Paris", isFavorite = false, sortOrder = 5)
                    )
                )
            }
        }
    }

    private fun startClockAndTimersLoop() {
        viewModelScope.launch {
            var batteryCounter = 0
            while (true) {
                delay(1000L)
                _nowMillis.value = System.currentTimeMillis()
                batteryCounter++
                if (batteryCounter % 5 == 0) {
                    _batteryStatus.value = BatteryMonitor.readStatus(appContext)
                }

                // Update Countdown Timers
                val activeTimers = _timers.value
                if (activeTimers.any { it.isRunning }) {
                    val updated = activeTimers.map { t ->
                        if (!t.isRunning) t
                        else if (t.remainingSeconds <= 1) {
                            postTimerFinishedNotification(t.label)
                            t.copy(remainingSeconds = 0, isRunning = false)
                        } else {
                            t.copy(remainingSeconds = t.remainingSeconds - 1)
                        }
                    }
                    _timers.value = updated
                }

                // Update Pomodoro
                val p = _pomodoro.value
                if (p.isRunning) {
                    if (p.remainingSeconds <= 1) {
                        postTimerFinishedNotification("${p.phase.label} completed!")
                        val completedMinutes = when (p.phase) {
                            PomodoroPhase.FOCUS -> p.focusMinutes
                            PomodoroPhase.SHORT_BREAK -> p.shortBreakMinutes
                            PomodoroPhase.LONG_BREAK -> p.longBreakMinutes
                        }
                        db.utilityDao().insertPomodoroSession(
                            PomodoroSessionEntity(
                                label = p.phase.label,
                                durationMinutes = completedMinutes,
                                sessionType = p.phase.name
                            )
                        )
                        val nextPhase = if (p.phase == PomodoroPhase.FOCUS) PomodoroPhase.SHORT_BREAK else PomodoroPhase.FOCUS
                        val nextMins = if (nextPhase == PomodoroPhase.FOCUS) p.focusMinutes else p.shortBreakMinutes
                        _pomodoro.value = p.copy(
                            phase = nextPhase,
                            remainingSeconds = nextMins * 60,
                            isRunning = false
                        )
                    } else {
                        _pomodoro.value = p.copy(remainingSeconds = p.remainingSeconds - 1)
                    }
                }
            }
        }

        viewModelScope.launch {
            while (true) {
                if (_stopwatch.value.isRunning) {
                    val elapsed = stopwatchBaseElapsed + (SystemClock.elapsedRealtime() - stopwatchStartRealtime)
                    _stopwatch.update { it.copy(elapsedMillis = elapsed) }
                    delay(50L)
                } else {
                    delay(250L)
                }
            }
        }
    }

    private fun observeNetworkConnectivity() {
        try {
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isOnline.value = true
                    viewModelScope.launch {
                        val prefs = preferences.value
                        if (!prefs.offlineModePreferred) {
                            refreshWeather(prefs.weatherCityName, prefs.weatherLat, prefs.weatherLon)
                            refreshAllIndiaWeather(forceRefresh = false)
                            if (prefs.autoCloudSync && Firebase.auth.currentUser != null) {
                                backupToCloud(silent = true)
                            }
                        }
                    }
                }

                override fun onLost(network: Network) {
                    _isOnline.value = checkConnectivity()
                }
            })
        } catch (_: Exception) {
        }
    }

    private fun checkConnectivity(): Boolean {
        return try {
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            false
        }
    }

    // --- Auth & Cloud Backup Observation (Strictly Auth-Gated) ---
    fun onAuthenticatedUserChanged(uid: String?) {
        cloudObserverJob?.cancel()
        _cloudSummary.value = null
        _cloudSyncState.value = CloudSyncUiState.Idle
        if (uid.isNullOrBlank()) return

        cloudObserverJob = viewModelScope.launch {
            cloudRepo.observeBackupSummary(uid)
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    Log.w("MainVM", "Cloud summary stream error", fsErr ?: error)
                    _cloudSyncState.value = CloudSyncUiState.Error(
                        (fsErr ?: error).localizedMessage ?: "Failed to read backup summary."
                    )
                }
                .collect { summary ->
                    _cloudSummary.value = summary
                }
        }
    }

    fun setConflictPolicy(policy: ConflictPolicy) {
        _conflictPolicy.value = policy
    }

    fun backupToCloud(silent: Boolean = false) {
        if (Firebase.auth.currentUser == null) {
            if (!silent) _cloudSyncState.value = CloudSyncUiState.Error("Sign in with Google first to back up to the cloud.")
            return
        }
        if (preferences.value.offlineModePreferred || !_isOnline.value) {
            if (!silent) _cloudSyncState.value = CloudSyncUiState.Error("Internet connection required for cloud backup.")
            return
        }
        viewModelScope.launch {
            _cloudSyncState.value = CloudSyncUiState.Syncing
            val res = cloudRepo.backupToCloud(_conflictPolicy.value)
            res.onSuccess { syncRes ->
                prefsManager.setLastCloudBackup(syncRes.timestampMillis)
                _cloudSyncState.value = CloudSyncUiState.Success(
                    "Backed up ${syncRes.uploadedCount} items cleanly.",
                    syncRes
                )
            }.onFailure { err ->
                if (!silent) {
                    _cloudSyncState.value = CloudSyncUiState.Error(
                        err.localizedMessage ?: "Cloud backup failed."
                    )
                }
            }
        }
    }

    fun restoreFromCloud() {
        if (Firebase.auth.currentUser == null) {
            _cloudSyncState.value = CloudSyncUiState.Error("Sign in with Google first to restore cloud data.")
            return
        }
        viewModelScope.launch {
            _cloudSyncState.value = CloudSyncUiState.Syncing
            val res = cloudRepo.restoreFromCloud(_conflictPolicy.value)
            res.onSuccess { syncRes ->
                prefsManager.setLastCloudBackup(syncRes.timestampMillis)
                rescheduleAllActive()
                _cloudSyncState.value = CloudSyncUiState.Success(
                    "Restored ${syncRes.downloadedCount} items from cloud.",
                    syncRes
                )
            }.onFailure { err ->
                _cloudSyncState.value = CloudSyncUiState.Error(
                    err.localizedMessage ?: "Cloud restore failed."
                )
            }
        }
    }

    fun deleteAllCloudData() {
        if (Firebase.auth.currentUser == null) return
        viewModelScope.launch {
            _cloudSyncState.value = CloudSyncUiState.Syncing
            cloudRepo.deleteAllCloudData()
                .onSuccess {
                    prefsManager.setLastCloudBackup(0L)
                    _cloudSyncState.value = CloudSyncUiState.Success("Cloud backup deleted.")
                }
                .onFailure { err ->
                    _cloudSyncState.value = CloudSyncUiState.Error(
                        err.localizedMessage ?: "Failed to delete cloud data."
                    )
                }
        }
    }

    // --- Design Studio & Preferences ---
    fun updateDesign(design: DesignSettings, notify: Boolean = false) {
        viewModelScope.launch {
            prefsManager.updateDesign(design)
            if (notify) _snackbarEvents.tryEmit("Theme & design updated")
        }
    }

    fun resetDesignToDefault() {
        viewModelScope.launch {
            prefsManager.resetDesignToDefault()
            _snackbarEvents.tryEmit("Design reset to default")
        }
    }

    fun setClockFormat(use24h: Boolean) {
        viewModelScope.launch { prefsManager.setClockFormat(use24h) }
    }

    fun setDefaultSnooze(mins: Int) {
        viewModelScope.launch { prefsManager.setDefaultSnooze(mins) }
    }

    fun setNotificationReader(enabled: Boolean) {
        viewModelScope.launch { prefsManager.setNotificationReader(enabled) }
    }

    fun setOfflineModePreferred(offline: Boolean) {
        viewModelScope.launch {
            prefsManager.setOfflineModePreferred(offline)
            if (!offline && _isOnline.value) {
                val p = preferences.value
                refreshWeather(p.weatherCityName, p.weatherLat, p.weatherLon)
                refreshAllIndiaWeather(forceRefresh = false)
            }
        }
    }

    fun setAutoCloudSync(enabled: Boolean) {
        viewModelScope.launch { prefsManager.setAutoCloudSync(enabled) }
    }

    fun configurePinLock(enabled: Boolean, rawPin: String?, biometric: Boolean) {
        viewModelScope.launch {
            if (!enabled) {
                prefsManager.setPinLock(false, "", "", false)
                _isLocked.value = false
                _snackbarEvents.tryEmit("App Lock disabled")
            } else if (!rawPin.isNullOrBlank()) {
                val salt = SecurityManager.generateSalt()
                val hash = SecurityManager.hashPin(rawPin, salt)
                prefsManager.setPinLock(true, hash, salt, biometric)
                _snackbarEvents.tryEmit("App Lock enabled")
            } else {
                val cur = preferences.value
                prefsManager.setPinLock(true, cur.pinHash, cur.pinSalt, biometric)
            }
        }
    }

    fun unlockWithPin(pin: String): Boolean {
        val prefs = preferences.value
        val valid = SecurityManager.verifyPin(pin, prefs.pinSalt, prefs.pinHash)
        if (valid) _isLocked.value = false
        return valid
    }

    fun unlockBiometricSuccess() {
        _isLocked.value = false
    }

    fun lockNow() {
        val prefs = preferences.value
        if (prefs.appLockEnabled && prefs.pinHash.isNotBlank()) {
            _isLocked.value = true
        }
    }

    // --- Weather ---
    fun refreshWeather(cityName: String, lat: Double, lon: Double) {
        if (preferences.value.offlineModePreferred) {
            _weatherState.value = WeatherUiState.Error("Offline Mode active. Disable offline mode for live weather.")
            return
        }
        viewModelScope.launch {
            _weatherState.value = WeatherUiState.Loading
            prefsManager.setWeatherLocation(cityName, lat, lon)
            weatherRepo.fetchLiveWeather(cityName, lat, lon)
                .onSuccess { _weatherState.value = WeatherUiState.Success(it) }
                .onFailure { err ->
                    _weatherState.value = WeatherUiState.Error(
                        err.localizedMessage ?: "Unable to load live weather."
                    )
                }
        }
    }

    fun refreshAllIndiaWeather(forceRefresh: Boolean = false) {
        if (preferences.value.offlineModePreferred) {
            _allIndiaWeatherState.value = AllIndiaWeatherUiState.Error("Offline Mode active. Disable offline mode for live All-India weather.")
            return
        }
        viewModelScope.launch {
            if (forceRefresh || _allIndiaWeatherState.value !is AllIndiaWeatherUiState.Success) {
                _allIndiaWeatherState.value = AllIndiaWeatherUiState.Loading
            }
            weatherRepo.fetchAllIndiaWeather(forceRefresh = forceRefresh)
                .onSuccess { _allIndiaWeatherState.value = AllIndiaWeatherUiState.Success(it) }
                .onFailure { err ->
                    _allIndiaWeatherState.value = AllIndiaWeatherUiState.Error(
                        err.localizedMessage ?: "Unable to load All-India weather forecast."
                    )
                }
        }
    }

    fun refreshWeatherByDeviceLocation() {
        viewModelScope.launch {
            val coords = weatherRepo.getCurrentOrLastCoordinates()
            if (coords == null) {
                _snackbarEvents.tryEmit("GPS coordinates unavailable yet. Select a city manually or enable location.")
                return@launch
            }
            refreshWeather("Current Location", coords.first, coords.second)
        }
    }

    fun searchCities(query: String) {
        viewModelScope.launch {
            weatherRepo.searchCities(query)
                .onSuccess { _citySearchResults.value = it }
                .onFailure { _citySearchResults.value = emptyList() }
        }
    }

    // --- World Clock ---
    fun addWorldCity(city: GeocodingResultDto) {
        val tz = city.timezone ?: return
        viewModelScope.launch {
            db.worldCityDao().upsert(
                WorldCityEntity(
                    cityName = city.name,
                    country = city.country,
                    timeZoneId = tz,
                    isFavorite = false
                )
            )
            _snackbarEvents.tryEmit("Added ${city.name} to World Clock")
        }
    }

    fun toggleCityFavorite(city: WorldCityEntity) {
        viewModelScope.launch {
            db.worldCityDao().upsert(city.copy(isFavorite = !city.isFavorite))
        }
    }

    fun deleteWorldCity(id: String) {
        viewModelScope.launch { db.worldCityDao().deleteById(id) }
    }

    // --- Stopwatch, Timer, Pomodoro ---
    fun toggleStopwatch() {
        val current = _stopwatch.value
        if (current.isRunning) {
            stopwatchBaseElapsed = current.elapsedMillis
            _stopwatch.value = current.copy(isRunning = false)
        } else {
            stopwatchStartRealtime = SystemClock.elapsedRealtime()
            _stopwatch.value = current.copy(isRunning = true)
        }
    }

    fun resetStopwatch() {
        stopwatchBaseElapsed = 0L
        stopwatchStartRealtime = 0L
        _stopwatch.value = StopwatchState()
    }

    fun lapStopwatch() {
        val cur = _stopwatch.value
        if (cur.elapsedMillis > 0L) {
            _stopwatch.value = cur.copy(laps = listOf(cur.elapsedMillis) + cur.laps)
        }
    }

    fun addAndStartTimer(label: String, durationSeconds: Int) {
        if (durationSeconds <= 0) return
        val timer = CountdownTimerItem(
            label = label.ifBlank { "Timer" },
            totalSeconds = durationSeconds,
            remainingSeconds = durationSeconds,
            isRunning = true
        )
        _timers.update { it + timer }
    }

    fun toggleTimer(id: String) {
        _timers.update { list ->
            list.map { if (it.id == id && it.remainingSeconds > 0) it.copy(isRunning = !it.isRunning) else it }
        }
    }

    fun resetTimer(id: String) {
        _timers.update { list ->
            list.map { if (it.id == id) it.copy(remainingSeconds = it.totalSeconds, isRunning = false) else it }
        }
    }

    fun deleteTimer(id: String) {
        _timers.update { list -> list.filterNot { it.id == id } }
    }

    fun togglePomodoro() {
        _pomodoro.update { it.copy(isRunning = !it.isRunning) }
    }

    fun resetPomodoro() {
        _pomodoro.update { p ->
            val mins = when (p.phase) {
                PomodoroPhase.FOCUS -> p.focusMinutes
                PomodoroPhase.SHORT_BREAK -> p.shortBreakMinutes
                PomodoroPhase.LONG_BREAK -> p.longBreakMinutes
            }
            p.copy(remainingSeconds = mins * 60, isRunning = false)
        }
    }

    fun selectPomodoroPhase(phase: PomodoroPhase) {
        _pomodoro.update { p ->
            val mins = when (phase) {
                PomodoroPhase.FOCUS -> p.focusMinutes
                PomodoroPhase.SHORT_BREAK -> p.shortBreakMinutes
                PomodoroPhase.LONG_BREAK -> p.longBreakMinutes
            }
            p.copy(phase = phase, remainingSeconds = mins * 60, isRunning = false)
        }
    }

    fun updatePomodoroDurations(focus: Int, shortBreak: Int, longBreak: Int) {
        _pomodoro.update { p ->
            val f = focus.coerceIn(1, 120)
            val sb = shortBreak.coerceIn(1, 45)
            val lb = longBreak.coerceIn(1, 60)
            val activeMins = when (p.phase) {
                PomodoroPhase.FOCUS -> f
                PomodoroPhase.SHORT_BREAK -> sb
                PomodoroPhase.LONG_BREAK -> lb
            }
            p.copy(
                focusMinutes = f,
                shortBreakMinutes = sb,
                longBreakMinutes = lb,
                remainingSeconds = if (p.isRunning) p.remainingSeconds else activeMins * 60
            )
        }
    }

    private fun postTimerFinishedNotification(label: String) {
        NotificationChannels.ensureCreated(appContext)
        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notif = NotificationCompat.Builder(appContext, NotificationChannels.CHANNEL_TIMER)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("RR Timer Complete")
            .setContentText(label)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify(label.hashCode(), notif)
        viewModelScope.launch {
            db.notificationLogDao().insert(
                NotificationLogEntity(title = "Timer Complete", body = label, category = "TIMER")
            )
        }
    }

    // --- Alarms ---
    fun saveAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            val updated = alarm.copy(updatedAtMillis = System.currentTimeMillis())
            db.alarmDao().upsert(updated)
            SystemSchedulers.scheduleAlarm(appContext, updated)
        }
    }

    fun deleteAlarm(id: String) {
        viewModelScope.launch {
            SystemSchedulers.cancelAlarm(appContext, id)
            db.alarmDao().deleteById(id)
        }
    }

    // --- Reminders & Location Reminders ---
    fun saveReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            val updated = reminder.copy(updatedAtMillis = System.currentTimeMillis())
            db.reminderDao().upsert(updated)
            if (!updated.isCompleted && updated.triggerAtMillis > System.currentTimeMillis()) {
                SystemSchedulers.scheduleReminder(appContext, updated)
            } else {
                SystemSchedulers.cancelReminder(appContext, updated.id)
            }
            if (updated.latitude != null && updated.longitude != null && !updated.isCompleted) {
                SystemSchedulers.registerLocationGeofence(appContext, updated) { _, msg ->
                    _snackbarEvents.tryEmit(msg)
                }
            }
        }
    }

    fun deleteReminder(id: String) {
        viewModelScope.launch {
            SystemSchedulers.cancelReminder(appContext, id)
            db.reminderDao().deleteById(id)
        }
    }

    // --- Calendar Events ---
    fun saveEvent(event: CalendarEventEntity) {
        viewModelScope.launch {
            db.calendarEventDao().upsert(event.copy(updatedAtMillis = System.currentTimeMillis()))
        }
    }

    fun deleteEvent(id: String) {
        viewModelScope.launch { db.calendarEventDao().deleteById(id) }
    }

    // --- Tasks ---
    fun saveTask(task: TaskEntity) {
        viewModelScope.launch {
            db.taskDao().upsert(task.copy(updatedAtMillis = System.currentTimeMillis()))
        }
    }

    fun toggleTaskCompleted(task: TaskEntity) {
        val now = System.currentTimeMillis()
        saveTask(
            task.copy(
                isCompleted = !task.isCompleted,
                completedAtMillis = if (!task.isCompleted) now else null
            )
        )
    }

    fun deleteTask(id: String) {
        viewModelScope.launch { db.taskDao().deleteById(id) }
    }

    // --- Birthdays ---
    fun saveBirthday(birthday: BirthdayEntity) {
        viewModelScope.launch {
            db.birthdayDao().upsert(birthday.copy(updatedAtMillis = System.currentTimeMillis()))
        }
    }

    fun deleteBirthday(id: String) {
        viewModelScope.launch { db.birthdayDao().deleteById(id) }
    }

    // --- Notes & Voice Notes ---
    fun saveNote(note: NoteEntity) {
        viewModelScope.launch {
            db.noteDao().upsert(note.copy(updatedAtMillis = System.currentTimeMillis()))
        }
    }

    fun deleteNote(id: String) {
        viewModelScope.launch { db.noteDao().deleteById(id) }
    }

    // --- Goals & Habits ---
    fun saveGoal(goal: GoalEntity) {
        viewModelScope.launch {
            val done = goal.currentCount >= goal.targetCount
            db.goalDao().upsert(goal.copy(isCompleted = done, updatedAtMillis = System.currentTimeMillis()))
        }
    }

    fun incrementGoal(goal: GoalEntity, delta: Int = 1) {
        val next = (goal.currentCount + delta).coerceIn(0, goal.targetCount)
        saveGoal(goal.copy(currentCount = next))
    }

    fun deleteGoal(id: String) {
        viewModelScope.launch { db.goalDao().deleteById(id) }
    }

    fun saveHabit(habit: HabitEntity) {
        viewModelScope.launch {
            db.habitDao().upsert(habit.copy(updatedAtMillis = System.currentTimeMillis()))
        }
    }

    fun checkInHabitToday(habit: HabitEntity) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val dates = habit.historyDatesCsv.split(",").filter { it.isNotBlank() }.toMutableSet()
        val added = if (dates.contains(today)) {
            dates.remove(today)
            false
        } else {
            dates.add(today)
            true
        }
        val newStreak = if (added) habit.streak + 1 else (habit.streak - 1).coerceAtLeast(0)
        val best = maxOf(habit.bestStreak, newStreak)
        saveHabit(
            habit.copy(
                streak = newStreak,
                bestStreak = best,
                historyDatesCsv = dates.sorted().joinToString(",")
            )
        )
    }

    fun deleteHabit(id: String) {
        viewModelScope.launch { db.habitDao().deleteById(id) }
    }

    // --- Productivity Utilities ---
    fun saveCalcHistory(expr: String, result: String) {
        viewModelScope.launch {
            db.utilityDao().insertCalcHistory(CalcHistoryEntity(expression = expr, result = result))
        }
    }

    fun clearCalcHistory() {
        viewModelScope.launch { db.utilityDao().clearCalcHistory() }
    }

    fun runInternetSpeedTest() {
        if (!_isOnline.value || preferences.value.offlineModePreferred) {
            _speedTestError.value = "Internet connection is required to run a speed test."
            return
        }
        viewModelScope.launch {
            _speedTestRunning.value = true
            _speedTestError.value = null
            SpeedTestRunner.runTest()
                .onSuccess { m ->
                    db.utilityDao().insertSpeedTest(
                        SpeedTestResultEntity(
                            downloadMbps = m.downloadMbps,
                            uploadMbps = m.uploadMbps,
                            latencyMs = m.latencyMs
                        )
                    )
                }
                .onFailure { err ->
                    _speedTestError.value = err.localizedMessage ?: "Speed test failed."
                }
            _speedTestRunning.value = false
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch { db.notificationLogDao().markAllRead() }
    }

    fun clearNotificationLogs() {
        viewModelScope.launch { db.notificationLogDao().clearAll() }
    }

    // --- Data Export & Deletion ---
    suspend fun exportAllDataAsJson(): String {
        val root = JSONObject()
        root.put("exportedAt", System.currentTimeMillis())
        root.put("reminders", JSONArray(reminders.value.map { JSONObject().put("title", it.title).put("triggerAtMillis", it.triggerAtMillis).put("completed", it.isCompleted) }))
        root.put("events", JSONArray(events.value.map { JSONObject().put("title", it.title).put("startMillis", it.startMillis).put("category", it.category) }))
        root.put("tasks", JSONArray(tasks.value.map { JSONObject().put("title", it.title).put("priority", it.priority).put("completed", it.isCompleted) }))
        root.put("alarms", JSONArray(alarms.value.map { JSONObject().put("hour", it.hour).put("minute", it.minute).put("label", it.label) }))
        root.put("birthdays", JSONArray(birthdays.value.map { JSONObject().put("name", it.name).put("month", it.month).put("day", it.day) }))
        root.put("notes", JSONArray(notes.value.map { JSONObject().put("title", it.title).put("content", it.content) }))
        root.put("goals", JSONArray(goals.value.map { JSONObject().put("title", it.title).put("progress", "${it.currentCount}/${it.targetCount}") }))
        root.put("habits", JSONArray(habits.value.map { JSONObject().put("name", it.name).put("streak", it.streak) }))
        return root.toString(2)
    }

    fun deleteAllLocalData() {
        viewModelScope.launch {
            alarms.value.forEach { SystemSchedulers.cancelAlarm(appContext, it.id) }
            reminders.value.forEach { SystemSchedulers.cancelReminder(appContext, it.id) }
            db.reminderDao().clearAll()
            db.calendarEventDao().clearAll()
            db.taskDao().clearAll()
            db.alarmDao().clearAll()
            db.birthdayDao().clearAll()
            db.noteDao().clearAll()
            db.goalDao().clearAll()
            db.habitDao().clearAll()
            db.notificationLogDao().clearAll()
            db.utilityDao().clearCalcHistory()
            db.utilityDao().clearChatMessages()
            prefsManager.clearAllPreferences()
            _snackbarEvents.tryEmit("All local data cleared")
        }
    }

    private suspend fun rescheduleAllActive() {
        db.alarmDao().getAllOnce().filter { it.enabled }.forEach {
            SystemSchedulers.scheduleAlarm(appContext, it)
        }
        db.reminderDao().getAllOnce().filter { !it.isCompleted }.forEach {
            SystemSchedulers.scheduleReminder(appContext, it)
        }
    }

    override fun onCleared() {
        try {
            appContext.unregisterReceiver(batteryReceiver)
        } catch (_: Exception) {
        }
        voiceRecorder.stopRecording()
        voiceRecorder.stopPlayback()
        super.onCleared()
    }
}
