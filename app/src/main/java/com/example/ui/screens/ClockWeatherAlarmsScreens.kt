package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.local.AlarmEntity
import com.example.data.local.PomodoroSessionEntity
import com.example.data.local.WorldCityEntity
import com.example.data.remote.GeocodingResultDto
import com.example.data.remote.IndiaRegion
import com.example.data.remote.IndiaWeatherDirectory
import com.example.ui.theme.GlassCard
import com.example.ui.theme.GlassSegmentedTabs
import com.example.ui.theme.LocalRrColors
import com.example.ui.theme.NeonGradientButton
import com.example.ui.theme.PremiumEmptyState
import com.example.ui.theme.PremiumLoadingCard
import com.example.ui.theme.RadialProgressRing
import com.example.ui.theme.StatusPill
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Public
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.viewmodel.AllIndiaWeatherUiState
import com.example.ui.viewmodel.CountdownTimerItem
import com.example.ui.viewmodel.PomodoroPhase
import com.example.ui.viewmodel.PomodoroState
import com.example.ui.viewmodel.StopwatchState
import com.example.ui.viewmodel.WeatherUiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun SubScreenTopBar(title: String, subtitle: String, onBack: () -> Unit, trailing: (@Composable () -> Unit)? = null) {
    val ext = LocalRrColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    ext.glassHighlight.copy(alpha = 0.24f),
                                    ext.glassSurface
                                )
                            )
                        )
                        .border(1.dp, ext.glassBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(ext.glowPrimary, ext.glowSecondary)))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        subtitle.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(title, style = MaterialTheme.typography.headlineMedium)
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun WorldClockScreen(
    nowMillis: Long,
    cities: List<WorldCityEntity>,
    use24Hour: Boolean,
    searchResults: List<GeocodingResultDto>,
    onSearchCity: (String) -> Unit,
    onAddCity: (GeocodingResultDto) -> Unit,
    onToggleFavorite: (WorldCityEntity) -> Unit,
    onDeleteCity: (String) -> Unit,
    onToggle24Hour: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var query by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SubScreenTopBar("World Clock", "Global Timezones", onBack) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("24h", style = MaterialTheme.typography.labelMedium)
                    Switch(checked = use24Hour, onCheckedChange = onToggle24Hour)
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        onSearchCity(it)
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search city or country...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true
                )
            }
            if (query.length >= 2 && searchResults.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 10.dp) {
                    searchResults.take(5).forEach { res ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${res.name}, ${res.country} (${res.timezone ?: "UTC"})", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            TextButton(onClick = { onAddCity(res); query = "" }) { Text("Add") }
                        }
                    }
                }
            }
        }

        if (cities.isEmpty()) {
            item {
                PremiumEmptyState(
                    icon = Icons.Rounded.Public,
                    title = "No World Clocks Added",
                    subtitle = "Search any city above to track live local time and day/night indicators."
                )
            }
        }

        items(cities, key = { it.id }) { city ->
            val tz = TimeZone.getTimeZone(city.timeZoneId)
            val cal = Calendar.getInstance(tz).apply { timeInMillis = nowMillis }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val isDay = hour in 6..18
            val timeStr = SimpleDateFormat(if (use24Hour) "HH:mm:ss" else "h:mm:ss a", Locale.getDefault()).apply {
                timeZone = tz
            }.format(Date(nowMillis))
            val dateStr = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).apply {
                timeZone = tz
            }.format(Date(nowMillis))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isDay) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay,
                                contentDescription = if (isDay) "Day" else "Night",
                                tint = if (isDay) LocalRrColors.current.warning else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(city.cityName, style = MaterialTheme.typography.titleLarge)
                        }
                        Text("${city.country} • $dateStr • ${city.timeZoneId}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(timeStr, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        Row {
                            IconButton(onClick = { onToggleFavorite(city) }) {
                                Icon(
                                    if (city.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                    contentDescription = "Favorite",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { onDeleteCity(city.id) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WeatherScreen(
    weatherState: WeatherUiState,
    allIndiaWeatherState: AllIndiaWeatherUiState,
    searchResults: List<GeocodingResultDto>,
    onSearchCity: (String) -> Unit,
    onSelectCity: (String, Double, Double) -> Unit,
    onUseGpsLocation: () -> Unit,
    onRefresh: () -> Unit,
    onRefreshAllIndia: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val ext = LocalRrColors.current
    var weatherTab by remember { mutableIntStateOf(0) } // 0=City Forecast, 1=All India Live
    var query by remember { mutableStateOf("") }
    var selectedRegion by remember { mutableStateOf(IndiaRegion.ALL) }
    var indiaFilter by remember { mutableStateOf("") }

    val locLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onUseGpsLocation()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SubScreenTopBar("Live Weather", "All India & Global • Open-Meteo", onBack) {
                Row {
                    IconButton(onClick = {
                        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        if (granted) onUseGpsLocation()
                        else locLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    }) {
                        Icon(Icons.Rounded.MyLocation, contentDescription = "Use device location")
                    }
                    IconButton(onClick = {
                        onRefresh()
                        onRefreshAllIndia()
                    }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh weather")
                    }
                }
            }
        }

        item {
            GlassSegmentedTabs(
                tabs = listOf("City Forecast", "All India Live"),
                selectedIndex = weatherTab,
                onSelect = {
                    weatherTab = it
                    if (it == 1 && allIndiaWeatherState is AllIndiaWeatherUiState.Error) {
                        onRefreshAllIndia()
                    }
                }
            )
        }

        if (weatherTab == 0) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        onSearchCity(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search Indian state, city, or global...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true
                )
                if (query.length >= 2 && searchResults.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 10.dp) {
                        searchResults.take(6).forEach { c ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val placeLabel = listOfNotNull(c.name.takeIf { it.isNotBlank() }, c.admin1, c.country.takeIf { it.isNotBlank() }).joinToString(", ")
                                Text(placeLabel, modifier = Modifier.weight(1f))
                                TextButton(onClick = {
                                    onSelectCity(c.name, c.latitude, c.longitude)
                                    query = ""
                                }) { Text("Select") }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val quickIndia = IndiaWeatherDirectory.cities.take(10)
                    items(quickIndia, key = { it.name }) { city ->
                        val activeCity = (weatherState as? WeatherUiState.Success)?.snapshot?.cityName == city.name
                        FilterChip(
                            selected = activeCity,
                            onClick = { onSelectCity(city.name, city.latitude, city.longitude) },
                            label = { Text(city.name) }
                        )
                    }
                }
            }

            when (weatherState) {
                is WeatherUiState.Loading -> {
                    item {
                        PremiumLoadingCard(message = "Loading live weather forecast...")
                    }
                }
                is WeatherUiState.Error -> {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text(weatherState.message, color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(8.dp))
                            NeonGradientButton(text = "Retry", onClick = onRefresh)
                        }
                    }
                }
                is WeatherUiState.Success -> {
                    val w = weatherState.snapshot
                    if (w.alertBanner != null) {
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth(), accentStripeColor = ext.warning) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Warning, contentDescription = "Alert", tint = ext.warning)
                                    Spacer(Modifier.width(8.dp))
                                    Text(w.alertBanner, style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    }
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth(), accentStripeColor = ext.glowPrimary) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(w.cityName, style = MaterialTheme.typography.headlineMedium)
                                    Text(w.conditionLabel, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                    if (w.isFromCache) {
                                        Spacer(Modifier.height(4.dp))
                                        StatusPill(text = "Cached Offline Snapshot", color = ext.warning)
                                    }
                                }
                                Text(
                                    "${w.tempCelsius.toInt()}°C",
                                    style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                WeatherMetricPill("Feels", "${w.feelsLikeCelsius.toInt()}°C", Modifier.weight(1f))
                                WeatherMetricPill("Humidity", "${w.humidityPercent}%", Modifier.weight(1f))
                                WeatherMetricPill("Wind", "${w.windSpeedKmh.toInt()} km/h", Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                WeatherMetricPill("Rain", "${w.rainProbabilityPercent}%", Modifier.weight(1f))
                                WeatherMetricPill("UV", "${w.uvIndex.toInt()}", Modifier.weight(1f))
                                WeatherMetricPill("AQI", "${w.usAqi}", Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Sunrise ${w.sunrise} • Sunset ${w.sunset}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    item {
                        Text("Hourly Forecast", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(w.hourly) { h ->
                                GlassCard(contentPadding = 12.dp) {
                                    Text(h.timeLabel, style = MaterialTheme.typography.labelMedium)
                                    Spacer(Modifier.height(4.dp))
                                    Text("${h.tempCelsius.toInt()}°C", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                    Text("${h.rainProbability}% rain", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    item { Text("7-Day Forecast", style = MaterialTheme.typography.titleLarge) }
                    items(w.daily) { d ->
                        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 12.dp) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(d.dateLabel, style = MaterialTheme.typography.titleMedium)
                                Text("Rain ${d.rainProbability}% • UV ${d.uvMax.toInt()}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${d.minTempC.toInt()}° / ${d.maxTempC.toInt()}°", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        } else {
            item {
                OutlinedTextField(
                    value = indiaFilter,
                    onValueChange = { indiaFilter = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Filter Indian state or city (e.g. Maharashtra, Kochi)...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    IndiaRegion.entries.forEach { reg ->
                        FilterChip(
                            selected = selectedRegion == reg,
                            onClick = { selectedRegion = reg },
                            label = { Text(reg.label) }
                        )
                    }
                }
            }

            when (allIndiaWeatherState) {
                is AllIndiaWeatherUiState.Loading -> {
                    item {
                        PremiumLoadingCard(message = "Fetching live weather across all Indian states & UTs...")
                    }
                }
                is AllIndiaWeatherUiState.Error -> {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text(allIndiaWeatherState.message, color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(8.dp))
                            NeonGradientButton(text = "Retry All India", onClick = onRefreshAllIndia)
                        }
                    }
                }
                is AllIndiaWeatherUiState.Success -> {
                    val filteredIndia = allIndiaWeatherState.cities.filter { item ->
                        val matchRegion = selectedRegion == IndiaRegion.ALL || item.city.region == selectedRegion
                        val q = indiaFilter.trim()
                        val matchText = q.isBlank() ||
                            item.city.name.contains(q, ignoreCase = true) ||
                            item.city.stateOrUt.contains(q, ignoreCase = true)
                        matchRegion && matchText
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Live India Forecast (${filteredIndia.size} Stations)", style = MaterialTheme.typography.titleLarge)
                            StatusPill(
                                text = if (allIndiaWeatherState.cities.any { it.isFromCache }) "Cached" else "Live IST",
                                color = if (allIndiaWeatherState.cities.any { it.isFromCache }) ext.warning else ext.success
                            )
                        }
                    }
                    if (filteredIndia.isEmpty()) {
                        item {
                            PremiumEmptyState(
                                icon = Icons.Rounded.Search,
                                title = "No Matching Indian City",
                                subtitle = "Try clearing the filter or switching to All India."
                            )
                        }
                    } else {
                        items(filteredIndia, key = { "${it.city.name}_${it.city.stateOrUt}" }) { entry ->
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = 14.dp,
                                accentStripeColor = if (entry.rainProbabilityPercent >= 60) ext.glowSecondary else ext.glowPrimary,
                                onClick = {
                                    onSelectCity(entry.city.name, entry.city.latitude, entry.city.longitude)
                                    weatherTab = 0
                                }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(entry.city.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                            StatusPill(text = entry.city.region.label, color = MaterialTheme.colorScheme.secondary)
                                        }
                                        Text(entry.city.stateOrUt, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "${entry.conditionLabel} • Rain ${entry.rainProbabilityPercent}% • Wind ${entry.windSpeedKmh.toInt()} km/h • Hum ${entry.humidityPercent}%",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            "${entry.tempCelsius.toInt()}°C",
                                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            "${entry.minTempC.toInt()}° / ${entry.maxTempC.toInt()}°",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlarmsScreen(
    alarms: List<AlarmEntity>,
    use24Hour: Boolean,
    defaultSnooze: Int,
    onSaveAlarm: (AlarmEntity) -> Unit,
    onDeleteAlarm: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var showAdd by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SubScreenTopBar("Advanced Alarms", "AlarmManager Scheduled", onBack) {
                Button(onClick = { showAdd = true }, modifier = Modifier.testTag("alarm_add_button")) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Text(" Add")
                }
            }
        }

        if (alarms.isEmpty()) {
            item {
                PremiumEmptyState(
                    icon = Icons.Rounded.Alarm,
                    title = "No Alarms Scheduled",
                    subtitle = "Create a recurring wake-up or custom sound alarm using Android's exact scheduler.",
                    actionLabel = "Add Alarm",
                    onAction = { showAdd = true }
                )
            }
        }

        items(alarms, key = { it.id }) { alarm ->
            val displayTime = if (use24Hour) {
                String.format(Locale.US, "%02d:%02d", alarm.hour, alarm.minute)
            } else {
                val h12 = if (alarm.hour % 12 == 0) 12 else alarm.hour % 12
                val amPm = if (alarm.hour < 12) "AM" else "PM"
                String.format(Locale.US, "%d:%02d %s", h12, alarm.minute, amPm)
            }
            val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            val activeDays = dayNames.filterIndexed { idx, _ -> (alarm.repeatDaysMask and (1 shl idx)) != 0 }

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                accentStripeColor = if (alarm.enabled) MaterialTheme.colorScheme.primary else null
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(displayTime, style = MaterialTheme.typography.displayMedium, color = if (alarm.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(alarm.label, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = (if (activeDays.isEmpty()) "One-time" else activeDays.joinToString(", ")) +
                                " • Snooze ${alarm.snoozeMinutes}m • ${alarm.soundType}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Switch(
                            checked = alarm.enabled,
                            onCheckedChange = { onSaveAlarm(alarm.copy(enabled = it)) }
                        )
                        IconButton(onClick = { onDeleteAlarm(alarm.id) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete alarm", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var hourStr by remember { mutableStateOf("07") }
        var minStr by remember { mutableStateOf("00") }
        var label by remember { mutableStateOf("Morning Alarm") }
        var daysMask by remember { mutableIntStateOf(0) }
        var vibrate by remember { mutableStateOf(true) }
        var soundType by remember { mutableStateOf("Cosmic Chime") }

        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Create Alarm") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = hourStr, onValueChange = { hourStr = it.filter { c -> c.isDigit() }.take(2) }, label = { Text("Hour (0-23)") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = minStr, onValueChange = { minStr = it.filter { c -> c.isDigit() }.take(2) }, label = { Text("Minute") }, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Label") }, singleLine = true)
                    Text("Repeat Days", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("S", "M", "T", "W", "T", "F", "S").forEachIndexed { idx, d ->
                            val selected = (daysMask and (1 shl idx)) != 0
                            FilterChip(
                                selected = selected,
                                onClick = { daysMask = daysMask xor (1 shl idx) },
                                label = { Text(d) }
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Vibrate")
                        Switch(checked = vibrate, onCheckedChange = { vibrate = it })
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Cosmic Chime", "Aurora Bell", "Gentle Pulse").forEach { s ->
                            FilterChip(selected = soundType == s, onClick = { soundType = s }, label = { Text(s) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onSaveAlarm(
                        AlarmEntity(
                            hour = (hourStr.toIntOrNull() ?: 7).coerceIn(0, 23),
                            minute = (minStr.toIntOrNull() ?: 0).coerceIn(0, 59),
                            label = label.ifBlank { "Alarm" },
                            repeatDaysMask = daysMask,
                            enabled = true,
                            vibrate = vibrate,
                            soundType = soundType,
                            snoozeMinutes = defaultSnooze
                        )
                    )
                    showAdd = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun ClockTimerPomodoroScreen(
    stopwatch: StopwatchState,
    timers: List<CountdownTimerItem>,
    pomodoro: PomodoroState,
    pomodoroSessions: List<PomodoroSessionEntity>,
    onToggleStopwatch: () -> Unit,
    onResetStopwatch: () -> Unit,
    onLapStopwatch: () -> Unit,
    onAddTimer: (String, Int) -> Unit,
    onToggleTimer: (String) -> Unit,
    onResetTimer: (String) -> Unit,
    onDeleteTimer: (String) -> Unit,
    onTogglePomodoro: () -> Unit,
    onResetPomodoro: () -> Unit,
    onSelectPomodoroPhase: (PomodoroPhase) -> Unit,
    onUpdatePomodoroDurations: (Int, Int, Int) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var tab by remember { mutableIntStateOf(0) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SubScreenTopBar("Time Studio", "Stopwatch • Timers • Pomodoro", onBack) }
        item {
            GlassSegmentedTabs(
                tabs = listOf("Stopwatch", "Timers", "Pomodoro"),
                selectedIndex = tab,
                onSelect = { tab = it }
            )
        }

        when (tab) {
            0 -> {
                item {
                    val totalSec = stopwatch.elapsedMillis / 1000
                    val cs = (stopwatch.elapsedMillis % 1000) / 10
                    val formatted = String.format(Locale.US, "%02d:%02d.%02d", totalSec / 60, totalSec % 60, cs)
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(formatted, style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                NeonGradientButton(
                                    text = if (stopwatch.isRunning) "Pause" else if (stopwatch.elapsedMillis > 0) "Resume" else "Start",
                                    onClick = onToggleStopwatch
                                )
                                OutlinedButton(onClick = onLapStopwatch, enabled = stopwatch.elapsedMillis > 0) { Text("Lap") }
                                OutlinedButton(onClick = onResetStopwatch) { Text("Reset") }
                            }
                        }
                    }
                }
                items(stopwatch.laps.indices.toList()) { idx ->
                    val lapMs = stopwatch.laps[idx]
                    val s = lapMs / 1000
                    val cs = (lapMs % 1000) / 10
                    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 12.dp) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Lap ${stopwatch.laps.size - idx}")
                            Text(String.format(Locale.US, "%02d:%02d.%02d", s / 60, s % 60, cs), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            1 -> {
                item {
                    var label by remember { mutableStateOf("Focus Timer") }
                    var mins by remember { mutableStateOf("5") }
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Create Countdown Timer", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Label") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = mins, onValueChange = { mins = it.filter { c -> c.isDigit() }.take(3) }, label = { Text("Mins") }, modifier = Modifier.width(96.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { onAddTimer(label, (mins.toIntOrNull() ?: 5) * 60) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Start Timer")
                        }
                    }
                }
                items(timers, key = { it.id }) { t ->
                    val mm = t.remainingSeconds / 60
                    val ss = t.remainingSeconds % 60
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(t.label, style = MaterialTheme.typography.titleMedium)
                                Text(String.format(Locale.US, "%02d:%02d", mm, ss), style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            Row {
                                IconButton(onClick = { onToggleTimer(t.id) }) {
                                    Icon(if (t.isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = "Toggle timer")
                                }
                                IconButton(onClick = { onResetTimer(t.id) }) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = "Reset timer")
                                }
                                IconButton(onClick = { onDeleteTimer(t.id) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete timer", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                item {
                    val mm = pomodoro.remainingSeconds / 60
                    val ss = pomodoro.remainingSeconds % 60
                    val totalPhaseSeconds = when (pomodoro.phase) {
                        PomodoroPhase.FOCUS -> pomodoro.focusMinutes * 60
                        PomodoroPhase.SHORT_BREAK -> pomodoro.shortBreakMinutes * 60
                        PomodoroPhase.LONG_BREAK -> pomodoro.longBreakMinutes * 60
                    }.coerceAtLeast(1)
                    val pct = pomodoro.remainingSeconds.toFloat() / totalPhaseSeconds
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                PomodoroPhase.entries.forEach { ph ->
                                    FilterChip(
                                        selected = pomodoro.phase == ph,
                                        onClick = { onSelectPomodoroPhase(ph) },
                                        label = { Text(ph.label.substringBefore(" ")) }
                                    )
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            RadialProgressRing(
                                progress = pct,
                                size = 180.dp,
                                strokeWidth = 10.dp
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        String.format(Locale.US, "%02d:%02d", mm, ss),
                                        style = MaterialTheme.typography.displayMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(pomodoro.phase.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                NeonGradientButton(
                                    text = if (pomodoro.isRunning) "Pause" else "Start",
                                    onClick = onTogglePomodoro
                                )
                                OutlinedButton(onClick = onResetPomodoro) { Text("Reset") }
                            }
                        }
                    }
                }
                item {
                    var fStr by remember(pomodoro.focusMinutes) { mutableStateOf(pomodoro.focusMinutes.toString()) }
                    var sbStr by remember(pomodoro.shortBreakMinutes) { mutableStateOf(pomodoro.shortBreakMinutes.toString()) }
                    var lbStr by remember(pomodoro.longBreakMinutes) { mutableStateOf(pomodoro.longBreakMinutes.toString()) }
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Custom Session Durations (minutes)", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = fStr, onValueChange = { fStr = it.filter { c -> c.isDigit() } }, label = { Text("Focus") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = sbStr, onValueChange = { sbStr = it.filter { c -> c.isDigit() } }, label = { Text("Short") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = lbStr, onValueChange = { lbStr = it.filter { c -> c.isDigit() } }, label = { Text("Long") }, modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = {
                            onUpdatePomodoroDurations(
                                fStr.toIntOrNull() ?: 25,
                                sbStr.toIntOrNull() ?: 5,
                                lbStr.toIntOrNull() ?: 15
                            )
                        }) { Text("Update Durations") }
                    }
                }
                item {
                    val todayFocusMins = pomodoroSessions.filter { it.sessionType == "FOCUS" }.sumOf { it.durationMinutes }
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Daily Focus Stats: $todayFocusMins mins across ${pomodoroSessions.size} sessions", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherMetricPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val ext = LocalRrColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(ext.glassSurface.copy(alpha = 0.65f))
            .border(0.75.dp, ext.glassBorder.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
    }
}
