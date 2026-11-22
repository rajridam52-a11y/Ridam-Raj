package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.cloud.CloudBackupRepository
import com.example.data.cloud.attemptAutoSignIn
import com.example.data.cloud.authStateFlow
import com.example.data.cloud.onGoogleSignInClicked
import com.example.data.cloud.signOutUser
import com.example.data.local.AppDatabase
import com.example.data.prefs.IconStyle
import com.example.data.prefs.PreferencesManager
import com.example.data.remote.WeatherRepository
import com.example.system.VoiceNoteRecorder
import com.example.ui.screens.AlarmsScreen
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.BirthdaysScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.ClockTimerPomodoroScreen
import com.example.ui.screens.DesignStudioScreen
import com.example.ui.screens.GoalsHabitsScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.LocationRemindersScreen
import com.example.ui.screens.NotesVoiceScreen
import com.example.ui.screens.NotificationHubScreen
import com.example.ui.screens.ProductivitySuiteScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.screens.ToolsHubScreen
import com.example.ui.screens.WeatherScreen
import com.example.ui.screens.WorldClockScreen
import com.example.ui.theme.CreatorSignatureFooter
import com.example.ui.theme.LocalRrColors
import com.example.ui.theme.NebulaBackground
import com.example.ui.theme.RrReminderTheme
import com.example.ui.viewmodel.MainViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

private data class TopNavItem(
    val route: String,
    val label: String,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector
)

private val TopNavDestinations = listOf(
    TopNavItem("home", "Home", Icons.Rounded.Dashboard, Icons.Outlined.Dashboard),
    TopNavItem("calendar", "Calendar", Icons.Rounded.CalendarMonth, Icons.Outlined.CalendarMonth),
    TopNavItem("tools", "Tools", Icons.Rounded.GridView, Icons.Outlined.GridView),
    TopNavItem("design", "Design", Icons.Rounded.Palette, Icons.Outlined.Palette),
    TopNavItem("settings", "Settings", Icons.Rounded.Settings, Icons.Outlined.Settings)
)

@Composable
fun RrReminderApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val viewModel: MainViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = AppDatabase.getInstance(app)
                val prefs = PreferencesManager(app)
                val weather = WeatherRepository(app)
                val cloud = CloudBackupRepository(app, db)
                val recorder = VoiceNoteRecorder(app)
                MainViewModel(app, db, prefs, weather, cloud, recorder)
            }
        }
    )

    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val isLocked by viewModel.isLocked.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val batteryStatus by viewModel.batteryStatus.collectAsStateWithLifecycle()
    val nowMillis by viewModel.nowMillis.collectAsStateWithLifecycle()

    val weatherState by viewModel.weatherState.collectAsStateWithLifecycle()
    val allIndiaWeatherState by viewModel.allIndiaWeatherState.collectAsStateWithLifecycle()
    val citySearchResults by viewModel.citySearchResults.collectAsStateWithLifecycle()

    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()
    val birthdays by viewModel.birthdays.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val worldCities by viewModel.worldCities.collectAsStateWithLifecycle()
    val notificationLogs by viewModel.notificationLogs.collectAsStateWithLifecycle()
    val speedTests by viewModel.speedTests.collectAsStateWithLifecycle()
    val calcHistory by viewModel.calcHistory.collectAsStateWithLifecycle()
    val pomodoroSessions by viewModel.pomodoroSessions.collectAsStateWithLifecycle()

    val stopwatch by viewModel.stopwatch.collectAsStateWithLifecycle()
    val timers by viewModel.timers.collectAsStateWithLifecycle()
    val pomodoro by viewModel.pomodoro.collectAsStateWithLifecycle()
    val speedTestRunning by viewModel.speedTestRunning.collectAsStateWithLifecycle()
    val speedTestError by viewModel.speedTestError.collectAsStateWithLifecycle()

    val cloudSyncState by viewModel.cloudSyncState.collectAsStateWithLifecycle()
    val cloudSummary by viewModel.cloudSummary.collectAsStateWithLifecycle()
    val conflictPolicy by viewModel.conflictPolicy.collectAsStateWithLifecycle()

    val credentialManager = remember(context) { CredentialManager.create(context) }
    val currentUser by Firebase.auth.authStateFlow().collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)
    var authLoading by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = { authError = null },
            onUnauthenticated = {},
            scope = scope
        )
    }

    LaunchedEffect(currentUser?.uid) {
        viewModel.onAuthenticatedUserChanged(currentUser?.uid)
    }

    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(850L)
        showSplash = false
    }

    RrReminderTheme(design = prefs.design) {
        if (isLocked && !showSplash) {
            AppLockScreen(
                biometricEnabled = prefs.biometricEnabled,
                onVerifyPin = { viewModel.unlockWithPin(it) },
                onBiometricSuccess = { viewModel.unlockBiometricSuccess() }
            )
            return@RrReminderTheme
        }

        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route ?: "home"
        val snackbarHostState = remember { SnackbarHostState() }

        val navigateTo: (String) -> Unit = { route ->
            val topRoutes = TopNavDestinations.map { it.route }
            if (route in topRoutes) {
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            } else {
                navController.navigate(route) {
                    launchSingleTop = true
                }
            }
        }

        LaunchedEffect(Unit) {
            viewModel.snackbarEvents.collect { msg ->
                snackbarHostState.showSnackbar(msg)
            }
        }

        NebulaBackground {
            val ext = LocalRrColors.current
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useRail = maxWidth >= 600.dp
                val showTopChrome = currentRoute in TopNavDestinations.map { it.route }

                Scaffold(
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        Surface(
                            color = Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            ext.glassSurface.copy(alpha = 0.88f),
                                            ext.gradientStart.copy(alpha = 0.96f)
                                        )
                                    )
                                )
                                .navigationBarsPadding()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (!useRail && showTopChrome) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(
                                                        Color.Transparent,
                                                        ext.glowPrimary.copy(alpha = 0.55f),
                                                        ext.glowSecondary.copy(alpha = 0.55f),
                                                        Color.Transparent
                                                    )
                                                )
                                            )
                                    )
                                    NavigationBar(
                                        containerColor = Color.Transparent,
                                        tonalElevation = 0.dp,
                                        windowInsets = WindowInsets(0)
                                    ) {
                                        TopNavDestinations.forEach { dest ->
                                            val selected = currentRoute == dest.route
                                            val icon = if (prefs.design.iconStyle == IconStyle.OUTLINED && !selected) {
                                                dest.outlinedIcon
                                            } else {
                                                dest.filledIcon
                                            }
                                            val iconScale by animateFloatAsState(
                                                targetValue = if (selected && prefs.design.animationsEnabled) 1.12f else 1f,
                                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                                label = "navIconScale"
                                            )
                                            NavigationBarItem(
                                                selected = selected,
                                                onClick = { navigateTo(dest.route) },
                                                icon = {
                                                    Icon(
                                                        icon,
                                                        contentDescription = dest.label,
                                                        modifier = Modifier.scale(iconScale)
                                                    )
                                                },
                                                label = {
                                                    Text(
                                                        dest.label,
                                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                                    )
                                                },
                                                colors = NavigationBarItemDefaults.colors(
                                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                                    indicatorColor = ext.glowPrimary.copy(alpha = 0.22f),
                                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                ),
                                                modifier = Modifier.testTag("nav_${dest.route}")
                                            )
                                        }
                                    }
                                }
                                CreatorSignatureFooter(
                                    modifier = Modifier.testTag("creator_signature_footer")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (useRail && showTopChrome) {
                            NavigationRail(
                                containerColor = ext.glassSurface.copy(alpha = 0.78f)
                            ) {
                                TopNavDestinations.forEach { dest ->
                                    val selected = currentRoute == dest.route
                                    val icon = if (prefs.design.iconStyle == IconStyle.OUTLINED && !selected) {
                                        dest.outlinedIcon
                                    } else {
                                        dest.filledIcon
                                    }
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = { navigateTo(dest.route) },
                                        icon = { Icon(icon, contentDescription = dest.label) },
                                        label = {
                                            Text(
                                                dest.label,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = ext.glowPrimary.copy(alpha = 0.18f),
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.testTag("rail_${dest.route}")
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                            val animDuration = if (prefs.design.animationsEnabled) {
                                (260f / prefs.design.animationSpeed.coerceIn(0.5f, 2.0f)).toInt()
                            } else 0
                            NavHost(
                                navController = navController,
                                startDestination = "home",
                                enterTransition = {
                                    fadeIn(tween(animDuration)) + slideInHorizontally(tween(animDuration)) { it / 14 }
                                },
                                exitTransition = {
                                    fadeOut(tween(animDuration / 2))
                                },
                                popEnterTransition = {
                                    fadeIn(tween(animDuration)) + slideInHorizontally(tween(animDuration)) { -it / 14 }
                                },
                                popExitTransition = {
                                    fadeOut(tween(animDuration / 2)) + slideOutHorizontally(tween(animDuration)) { it / 10 }
                                }
                            ) {
                                composable("home") {
                                    HomeDashboardScreen(
                                        nowMillis = nowMillis,
                                        prefs = prefs,
                                        isOnline = isOnline,
                                        weatherState = weatherState,
                                        reminders = reminders,
                                        tasks = tasks,
                                        events = events,
                                        goals = goals,
                                        alarms = alarms,
                                        worldCities = worldCities,
                                        stopwatch = stopwatch,
                                        battery = batteryStatus,
                                        onNavigate = navigateTo,
                                        onToggleAlarm = { viewModel.saveAlarm(it.copy(enabled = !it.enabled)) },
                                        onIncrementGoal = { viewModel.incrementGoal(it, 1) },
                                        onUpdateDesign = { viewModel.updateDesign(it, notify = false) }
                                    )
                                }

                                composable("calendar") {
                                    CalendarScreen(
                                        events = events,
                                        birthdays = birthdays,
                                        use24Hour = prefs.use24HourClock,
                                        onSaveEvent = viewModel::saveEvent,
                                        onDeleteEvent = viewModel::deleteEvent
                                    )
                                }

                                composable("tools") {
                                    ToolsHubScreen(
                                        alarmCount = alarms.count { it.enabled },
                                        taskCount = tasks.count { !it.isCompleted },
                                        reminderCount = reminders.count { !it.isCompleted },
                                        noteCount = notes.size,
                                        birthdayCount = birthdays.size,
                                        goalCount = goals.size,
                                        onNavigate = navigateTo
                                    )
                                }

                                composable("design") {
                                    DesignStudioScreen(
                                        currentDesign = prefs.design,
                                        onApplyDesign = { viewModel.updateDesign(it, notify = true) },
                                        onResetDefault = viewModel::resetDesignToDefault,
                                        onBack = null
                                    )
                                }

                                composable("design_studio") {
                                    DesignStudioScreen(
                                        currentDesign = prefs.design,
                                        onApplyDesign = { viewModel.updateDesign(it, notify = true) },
                                        onResetDefault = viewModel::resetDesignToDefault,
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                composable("settings") {
                                    SettingsScreen(
                                        prefs = prefs,
                                        isOnline = isOnline,
                                        currentUser = currentUser,
                                        authLoading = authLoading,
                                        authError = authError,
                                        cloudSyncState = cloudSyncState,
                                        cloudSummary = cloudSummary,
                                        conflictPolicy = conflictPolicy,
                                        onSignInGoogle = {
                                            authLoading = true
                                            authError = null
                                            onGoogleSignInClicked(
                                                context = context,
                                                credentialManager = credentialManager,
                                                onAuthSuccess = {
                                                    authLoading = false
                                                    authError = null
                                                },
                                                onAuthError = { msg ->
                                                    authLoading = false
                                                    authError = msg
                                                },
                                                onAuthCancelled = {
                                                    authLoading = false
                                                },
                                                scope = scope
                                            )
                                        },
                                        onSignOutGoogle = {
                                            signOutUser(
                                                credentialManager = credentialManager,
                                                scope = scope,
                                                onSignOutComplete = { authError = null }
                                            )
                                        },
                                        onSetConflictPolicy = viewModel::setConflictPolicy,
                                        onBackupNow = { viewModel.backupToCloud(silent = false) },
                                        onRestoreNow = viewModel::restoreFromCloud,
                                        onDeleteCloudData = viewModel::deleteAllCloudData,
                                        onSetClockFormat = viewModel::setClockFormat,
                                        onSetDefaultSnooze = viewModel::setDefaultSnooze,
                                        onSetNotificationReader = viewModel::setNotificationReader,
                                        onSetOfflineMode = viewModel::setOfflineModePreferred,
                                        onSetAutoCloudSync = viewModel::setAutoCloudSync,
                                        onConfigurePin = viewModel::configurePinLock,
                                        onLockNow = viewModel::lockNow,
                                        onExportJson = viewModel::exportAllDataAsJson,
                                        onDeleteLocalData = viewModel::deleteAllLocalData,
                                        onNavigate = navigateTo
                                    )
                                }

                                composable("world_clock") {
                                    WorldClockScreen(
                                        nowMillis = nowMillis,
                                        cities = worldCities,
                                        use24Hour = prefs.use24HourClock,
                                        searchResults = citySearchResults,
                                        onSearchCity = viewModel::searchCities,
                                        onAddCity = viewModel::addWorldCity,
                                        onToggleFavorite = viewModel::toggleCityFavorite,
                                        onDeleteCity = viewModel::deleteWorldCity,
                                        onToggle24Hour = viewModel::setClockFormat,
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                composable("weather") {
                                    WeatherScreen(
                                        weatherState = weatherState,
                                        allIndiaWeatherState = allIndiaWeatherState,
                                        searchResults = citySearchResults,
                                        onSearchCity = viewModel::searchCities,
                                        onSelectCity = viewModel::refreshWeather,
                                        onUseGpsLocation = viewModel::refreshWeatherByDeviceLocation,
                                        onRefresh = {
                                            viewModel.refreshWeather(
                                                prefs.weatherCityName,
                                                prefs.weatherLat,
                                                prefs.weatherLon
                                            )
                                        },
                                        onRefreshAllIndia = { viewModel.refreshAllIndiaWeather(forceRefresh = true) },
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                composable("alarms") {
                                    AlarmsScreen(
                                        alarms = alarms,
                                        use24Hour = prefs.use24HourClock,
                                        defaultSnooze = prefs.defaultSnoozeMinutes,
                                        onSaveAlarm = viewModel::saveAlarm,
                                        onDeleteAlarm = viewModel::deleteAlarm,
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                composable("timer") {
                                    ClockTimerPomodoroScreen(
                                        stopwatch = stopwatch,
                                        timers = timers,
                                        pomodoro = pomodoro,
                                        pomodoroSessions = pomodoroSessions,
                                        onToggleStopwatch = viewModel::toggleStopwatch,
                                        onResetStopwatch = viewModel::resetStopwatch,
                                        onLapStopwatch = viewModel::lapStopwatch,
                                        onAddTimer = viewModel::addAndStartTimer,
                                        onToggleTimer = viewModel::toggleTimer,
                                        onResetTimer = viewModel::resetTimer,
                                        onDeleteTimer = viewModel::deleteTimer,
                                        onTogglePomodoro = viewModel::togglePomodoro,
                                        onResetPomodoro = viewModel::resetPomodoro,
                                        onSelectPomodoroPhase = viewModel::selectPomodoroPhase,
                                        onUpdatePomodoroDurations = viewModel::updatePomodoroDurations,
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                composable("tasks") {
                                    TasksScreen(
                                        tasks = tasks,
                                        onSaveTask = viewModel::saveTask,
                                        onToggleTask = viewModel::toggleTaskCompleted,
                                        onDeleteTask = viewModel::deleteTask,
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                composable("goals") {
                                    GoalsHabitsScreen(
                                        goals = goals,
                                        habits = habits,
                                        onSaveGoal = viewModel::saveGoal,
                                        onIncrementGoal = viewModel::incrementGoal,
                                        onDeleteGoal = viewModel::deleteGoal,
                                        onSaveHabit = viewModel::saveHabit,
                                        onCheckInHabit = viewModel::checkInHabitToday,
                                        onDeleteHabit = viewModel::deleteHabit,
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                composable("birthdays") {
                                    BirthdaysScreen(
                                        birthdays = birthdays,
                                        onSaveBirthday = viewModel::saveBirthday,
                                        onDeleteBirthday = viewModel::deleteBirthday,
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                composable("notes") {
                                    NotesVoiceScreen(
                                        notes = notes,
                                        voiceRecorder = viewModel.voiceRecorder,
                                        onSaveNote = viewModel::saveNote,
                                        onDeleteNote = viewModel::deleteNote,
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                composable("location_reminders") {
                                    LocationRemindersScreen(
                                        reminders = reminders,
                                        searchResults = citySearchResults,
                                        onSearchPlace = viewModel::searchCities,
                                        onSaveReminder = viewModel::saveReminder,
                                        onDeleteReminder = viewModel::deleteReminder,
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                composable("productivity") {
                                    ProductivitySuiteScreen(
                                        calcHistory = calcHistory,
                                        speedTests = speedTests,
                                        speedTestRunning = speedTestRunning,
                                        speedTestError = speedTestError,
                                        batteryStatus = batteryStatus,
                                        onSaveCalc = viewModel::saveCalcHistory,
                                        onClearCalc = viewModel::clearCalcHistory,
                                        onRunSpeedTest = viewModel::runInternetSpeedTest,
                                        onRefreshBattery = viewModel::refreshBatteryStatus,
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                composable("notifications_log") {
                                    NotificationHubScreen(
                                        logs = notificationLogs,
                                        notificationReaderEnabled = prefs.notificationReaderEnabled,
                                        onToggleNotificationReader = viewModel::setNotificationReader,
                                        onMarkAllRead = viewModel::markAllNotificationsRead,
                                        onClearLogs = viewModel::clearNotificationLogs,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showSplash,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(320))
            ) {
                RrSplashOverlay(onDismiss = { showSplash = false })
            }
        }
    }
}

@Composable
private fun RrSplashOverlay(onDismiss: () -> Unit) {
    val ext = LocalRrColors.current
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val scale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.86f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "splashScale"
    )
    val badgeTextTint = if ((ext.glowPrimary.luminance() + ext.glowSecondary.luminance()) / 2f > 0.62f) {
        Color(0xFF0A0E17)
    } else {
        Color.White
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(ext.gradientStart, ext.gradientMid, ext.gradientEnd))
            )
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .scale(scale)
                .padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(94.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(listOf(ext.glowPrimary, ext.glowSecondary))
                    )
                    .border(2.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "RR",
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                    color = badgeTextTint
                )
            }
            Text(
                text = "RR REMINDER",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Your Time. Your Tasks.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Created by Ridam Raj",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
