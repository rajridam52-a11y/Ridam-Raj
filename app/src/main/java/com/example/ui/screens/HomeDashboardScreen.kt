package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AddTask
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.AlarmEntity
import com.example.data.local.CalendarEventEntity
import com.example.data.local.GoalEntity
import com.example.data.local.ReminderEntity
import com.example.data.local.TaskEntity
import com.example.data.local.WorldCityEntity
import com.example.data.prefs.AppUserPreferences
import com.example.data.prefs.DashboardCardType
import com.example.data.prefs.DesignSettings
import com.example.data.prefs.ThemePreset
import com.example.system.BatteryStatusInfo
import com.example.ui.theme.GlassCard
import com.example.ui.theme.LocalRrColors
import com.example.ui.theme.NeonGradientButton
import com.example.ui.theme.PremiumLoadingCard
import com.example.ui.theme.RadialProgressRing
import com.example.ui.theme.ScreenOverlineHeader
import com.example.ui.theme.SectionHeader
import com.example.ui.theme.StatusPill
import com.example.ui.viewmodel.StopwatchState
import com.example.ui.viewmodel.WeatherUiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDashboardScreen(
    nowMillis: Long,
    prefs: AppUserPreferences,
    isOnline: Boolean,
    weatherState: WeatherUiState,
    reminders: List<ReminderEntity>,
    tasks: List<TaskEntity>,
    events: List<CalendarEventEntity>,
    goals: List<GoalEntity>,
    alarms: List<AlarmEntity>,
    worldCities: List<WorldCityEntity>,
    stopwatch: StopwatchState,
    battery: BatteryStatusInfo,
    onNavigate: (String) -> Unit,
    onToggleAlarm: (AlarmEntity) -> Unit,
    onIncrementGoal: (GoalEntity) -> Unit,
    onUpdateDesign: (DesignSettings) -> Unit
) {
    var showCustomizeSheet by remember { mutableStateOf(false) }
    val ext = LocalRrColors.current
    val design = prefs.design
    val cardPad = if (design.compactWidgets) 12.dp else 16.dp

    val visibleCards = remember(design.dashboardOrder, design.hiddenCards) {
        design.dashboardOrder
            .mapNotNull { id -> DashboardCardType.entries.firstOrNull { it.id == id } }
            .filterNot { design.hiddenCards.contains(it.id) }
    }

    val pendingTaskCount = remember(tasks) { tasks.count { !it.isCompleted } }
    val activeReminderCount = remember(reminders) { reminders.count { !it.isCompleted } }
    val activeAlarmCount = remember(alarms) { alarms.count { it.enabled } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header Bar with Quick Theme Switcher & Dashboard Customizer
        item {
            ScreenOverlineHeader(
                overline = "RR REMINDER",
                title = "Home Dashboard"
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!isOnline || prefs.offlineModePreferred) {
                        StatusPill(
                            text = "Offline",
                            color = ext.warning,
                            icon = Icons.Rounded.CloudOff
                        )
                    }
                    FilledTonalIconButton(
                        onClick = {
                            val entries = ThemePreset.entries
                            val nextIdx = (entries.indexOf(design.themePreset) + 1) % entries.size
                            val nextPreset = entries[nextIdx]
                            onUpdateDesign(
                                design.copy(
                                    themePreset = nextPreset,
                                    isDarkMode = nextPreset != ThemePreset.LIGHT_FROST,
                                    customAccentHex = nextPreset.defaultAccentHex,
                                    customSecondaryHex = nextPreset.defaultSecondaryHex
                                )
                            )
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .border(1.dp, ext.glassBorder.copy(alpha = 0.6f), CircleShape)
                            .testTag("dashboard_cycle_theme_button")
                    ) {
                        Icon(
                            Icons.Rounded.Palette,
                            contentDescription = "Cycle color theme"
                        )
                    }
                    FilledTonalIconButton(
                        onClick = { showCustomizeSheet = true },
                        modifier = Modifier
                            .size(48.dp)
                            .border(1.dp, ext.glassBorder.copy(alpha = 0.6f), CircleShape)
                            .testTag("dashboard_customize_button")
                    ) {
                        Icon(
                            Icons.Rounded.DashboardCustomize,
                            contentDescription = "Customize dashboard cards"
                        )
                    }
                }
            }
        }

        // Overview Statistics Bento Strip
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BentoStatTile(
                    label = "Reminders",
                    value = activeReminderCount.toString(),
                    tint = ext.warning,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("location_reminders") }
                )
                BentoStatTile(
                    label = "Tasks",
                    value = pendingTaskCount.toString(),
                    tint = ext.glowPrimary,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("tasks") }
                )
                BentoStatTile(
                    label = "Alarms",
                    value = activeAlarmCount.toString(),
                    tint = ext.glowSecondary,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("alarms") }
                )
            }
        }

        items(visibleCards, key = { it.id }) { cardType ->
            when (cardType) {
                DashboardCardType.CLOCK_GREETING -> {
                    val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
                    val hour = cal.get(Calendar.HOUR_OF_DAY)
                    val greeting = when {
                        hour < 12 -> "Good Morning"
                        hour < 17 -> "Good Afternoon"
                        else -> "Good Evening"
                    }
                    val timePattern = if (prefs.use24HourClock) "HH:mm" else "h:mm a"
                    val timeStr = SimpleDateFormat(timePattern, Locale.getDefault()).format(Date(nowMillis))
                    val dateStr = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(Date(nowMillis))

                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = cardPad,
                        accentStripeColor = ext.glowPrimary
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                StatusPill(
                                    text = dateStr,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = timeStr,
                                    style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "$greeting!",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Stay focused, stay ahead.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                ext.glowPrimary.copy(alpha = 0.92f),
                                                ext.glowSecondary.copy(alpha = 0.88f)
                                            )
                                        )
                                    )
                                    .border(2.dp, Color.White.copy(alpha = 0.42f), CircleShape)
                                    .clickable { onNavigate("design") },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.ColorLens,
                                    contentDescription = "Open Colour & Design Studio",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }

                DashboardCardType.WEATHER -> {
                    when (weatherState) {
                        is WeatherUiState.Loading -> {
                            PremiumLoadingCard(message = "Fetching live weather...")
                        }
                        is WeatherUiState.Error -> {
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = cardPad,
                                onClick = { onNavigate("weather") }
                            ) {
                                SectionHeader(title = "Weather", icon = Icons.Rounded.WbSunny)
                                Spacer(Modifier.height(6.dp))
                                Text(weatherState.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        is WeatherUiState.Success -> {
                            val w = weatherState.snapshot
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("home_weather_card"),
                                contentPadding = cardPad,
                                onClick = { onNavigate("weather") }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        GlowingSquareIcon(
                                            icon = Icons.Rounded.WbSunny,
                                            tint = ext.warning,
                                            contentDescription = "Weather"
                                        )
                                        Spacer(Modifier.width(14.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "${w.tempCelsius.toInt()}°C",
                                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                StatusPill(text = "AQI ${w.usAqi}", color = ext.success)
                                            }
                                            Text(
                                                "${w.conditionLabel} • ${w.cityName}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Open weather", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                DashboardCardType.NEXT_REMINDER -> {
                    val nextRem = reminders.firstOrNull { !it.isCompleted }
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = cardPad,
                        accentStripeColor = ext.warning,
                        onClick = { onNavigate("location_reminders") }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                GlowingSquareIcon(
                                    icon = Icons.Rounded.NotificationsActive,
                                    tint = ext.warning,
                                    contentDescription = "Reminder"
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Next Reminder", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                    if (nextRem == null) {
                                        Text("Tap to create a timed or GPS alert", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } else {
                                        val fmt = SimpleDateFormat("EEE • h:mm a", Locale.getDefault())
                                        Text(
                                            text = "${nextRem.title} (${fmt.format(Date(nextRem.triggerAtMillis))})",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Manage reminders", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                DashboardCardType.GOALS_PROGRESS -> {
                    val total = goals.size
                    val done = goals.count { it.isCompleted }
                    val pct = if (total == 0) 0f else done.toFloat() / total
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = cardPad,
                        onClick = { onNavigate("goals") }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                RadialProgressRing(
                                    progress = pct,
                                    size = 56.dp,
                                    strokeWidth = 6.dp,
                                    primaryColor = ext.success,
                                    secondaryColor = ext.glowPrimary
                                ) {
                                    Text(
                                        text = "${(pct * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = ext.success
                                    )
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Today's Goals", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "$done of $total completed",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            val active = goals.firstOrNull { !it.isCompleted }
                            if (active != null) {
                                TextButton(onClick = { onIncrementGoal(active) }) {
                                    Text("+1", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Icon(Icons.Rounded.Flag, contentDescription = "Goals", tint = ext.success)
                            }
                        }
                    }
                }

                DashboardCardType.QUICK_TOOLS -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickDockTile(
                            label = "Alarm",
                            icon = Icons.Rounded.Alarm,
                            tint = ext.glowSecondary,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate("alarms") }
                        )
                        QuickDockTile(
                            label = "Tasks",
                            icon = Icons.Rounded.AddTask,
                            tint = ext.success,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate("tasks") }
                        )
                        QuickDockTile(
                            label = "Timers",
                            icon = Icons.Rounded.Timer,
                            tint = ext.glowPrimary,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate("timer") }
                        )
                        QuickDockTile(
                            label = "World",
                            icon = Icons.Rounded.Public,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate("world_clock") }
                        )
                    }
                }

                DashboardCardType.TODAY_SCHEDULE -> {
                    val todayStart = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    val todayEnd = todayStart + 86_400_000L
                    val todays = events.filter { it.startMillis in todayStart..todayEnd }.take(3)
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = cardPad,
                        accentStripeColor = ext.glowPrimary,
                        onClick = { onNavigate("calendar") }
                    ) {
                        SectionHeader(title = "Today's Schedule", icon = Icons.Rounded.Event) {
                            StatusPill(text = "${todays.size} events", color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(8.dp))
                        if (todays.isEmpty()) {
                            Text(
                                "Clear schedule today — tap to plan an event.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            val fmt = SimpleDateFormat(if (prefs.use24HourClock) "HH:mm" else "h:mm a", Locale.getDefault())
                            todays.forEach { ev ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        ev.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(fmt.format(Date(ev.startMillis)), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                DashboardCardType.CALENDAR_PREVIEW -> {
                    val favCity = worldCities.firstOrNull()
                    val nextAlarm = alarms.firstOrNull { it.enabled }
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = cardPad,
                        onClick = { onNavigate("timer") }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Next Alarm", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    nextAlarm?.let { String.format(Locale.US, "%02d:%02d", it.hour, it.minute) } ?: "None",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    favCity?.cityName ?: "World Clock",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val cityTime = favCity?.let {
                                    SimpleDateFormat(if (prefs.use24HourClock) "HH:mm" else "h:mm a", Locale.getDefault()).apply {
                                        timeZone = TimeZone.getTimeZone(it.timeZoneId)
                                    }.format(Date(nowMillis))
                                } ?: "--:--"
                                Text(cityTime, style = MaterialTheme.typography.titleMedium)
                            }
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                Text("Stopwatch", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val sec = stopwatch.elapsedMillis / 1000
                                Text(String.format(Locale.US, "%02d:%02d", sec / 60, sec % 60), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                DashboardCardType.BATTERY_STATUS -> {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = cardPad,
                        onClick = { onNavigate("productivity") }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                GlowingSquareIcon(
                                    icon = if (battery.isCharging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryStd,
                                    tint = if (battery.levelPercent > 20) ext.success else ext.warning,
                                    contentDescription = "Battery"
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Battery ${battery.levelPercent}%", style = MaterialTheme.typography.titleMedium)
                                    Text("${battery.pluggedSource} • ${battery.healthLabel}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            StatusPill(text = "${battery.temperatureCelsius}°C", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        // Colour & Design Customization Banner Card
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_design_studio_card"),
                contentPadding = 16.dp,
                onClick = { onNavigate("design") }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        GlowingSquareIcon(
                            icon = Icons.Rounded.Palette,
                            tint = ext.glowPrimary,
                            contentDescription = "Design Dashboard"
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Colour & Design Dashboard", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${design.themePreset.label} • ${design.cardStyle.label} • ${design.fontStyle.label}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Open Design Dashboard", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
    }

    if (showCustomizeSheet) {
        ModalBottomSheet(onDismissRequest = { showCustomizeSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Customize Dashboard Cards", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = { onNavigate("design"); showCustomizeSheet = false }) {
                        Text("Design Studio")
                    }
                }
                design.dashboardOrder.forEachIndexed { index, cardId ->
                    val type = DashboardCardType.entries.firstOrNull { it.id == cardId } ?: return@forEachIndexed
                    val visible = !design.hiddenCards.contains(cardId)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(type.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        IconButton(
                            enabled = index > 0,
                            onClick = {
                                val mutable = design.dashboardOrder.toMutableList()
                                val item = mutable.removeAt(index)
                                mutable.add(index - 1, item)
                                onUpdateDesign(design.copy(dashboardOrder = mutable))
                            }
                        ) { Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move up") }
                        IconButton(
                            enabled = index < design.dashboardOrder.lastIndex,
                            onClick = {
                                val mutable = design.dashboardOrder.toMutableList()
                                val item = mutable.removeAt(index)
                                mutable.add(index + 1, item)
                                onUpdateDesign(design.copy(dashboardOrder = mutable))
                            }
                        ) { Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move down") }
                        IconButton(
                            onClick = {
                                val nextHidden = if (visible) design.hiddenCards + cardId else design.hiddenCards - cardId
                                onUpdateDesign(design.copy(hiddenCards = nextHidden))
                            }
                        ) {
                            Icon(
                                if (visible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                contentDescription = if (visible) "Hide card" else "Show card"
                            )
                        }
                    }
                }
                NeonGradientButton(
                    text = "Done",
                    onClick = { showCustomizeSheet = false },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun BentoStatTile(
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = modifier,
        cornerRadius = 18.dp,
        contentPadding = 12.dp,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = tint
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GlowingSquareIcon(
    icon: ImageVector,
    tint: Color,
    contentDescription: String
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    listOf(tint.copy(alpha = 0.28f), tint.copy(alpha = 0.12f))
                )
            )
            .border(1.dp, tint.copy(alpha = 0.55f), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun QuickDockTile(
    label: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = modifier,
        cornerRadius = 18.dp,
        contentPadding = 10.dp,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            GlowingSquareIcon(icon = icon, tint = tint, contentDescription = label)
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
