package com.example.ui.screens

import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.ui.text.font.FontWeight
import com.example.data.local.CalcHistoryEntity
import com.example.data.local.NotificationLogEntity
import com.example.data.local.SpeedTestResultEntity
import com.example.system.BatteryStatusInfo
import com.example.system.RrNotificationListenerService
import com.example.system.ScientificCalculatorEngine
import com.example.system.UnitConverterEngine
import com.example.ui.theme.GlassCard
import com.example.ui.theme.GlassSegmentedTabs
import com.example.ui.theme.LocalRrColors
import com.example.ui.theme.NeonGradientButton
import com.example.ui.theme.PremiumEmptyState
import com.example.ui.theme.RadialProgressRing
import com.example.ui.theme.StatusPill
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductivitySuiteScreen(
    calcHistory: List<CalcHistoryEntity>,
    speedTests: List<SpeedTestResultEntity>,
    speedTestRunning: Boolean,
    speedTestError: String?,
    batteryStatus: BatteryStatusInfo,
    onSaveCalc: (String, String) -> Unit,
    onClearCalc: () -> Unit,
    onRunSpeedTest: () -> Unit,
    onRefreshBattery: () -> Unit = {},
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var tab by remember { mutableIntStateOf(0) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SubScreenTopBar("Productivity Suite", "Calc • Units • Speed • Battery", onBack) }
        item {
            GlassSegmentedTabs(
                tabs = listOf("Calc", "Units", "Speed", "Battery"),
                selectedIndex = tab,
                onSelect = { tab = it }
            )
        }

        when (tab) {
            0 -> {
                item {
                    var expr by remember { mutableStateOf("") }
                    var output by remember { mutableStateOf("0") }
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = expr,
                            onValueChange = { expr = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Expression (e.g. sqrt(144)+sin(30)*10)") }
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("= $output", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(10.dp))
                        val pad = listOf(
                            listOf("7", "8", "9", "÷", "sqrt("),
                            listOf("4", "5", "6", "×", "^"),
                            listOf("1", "2", "3", "−", "("),
                            listOf("0", ".", "π", "+", ")")
                        )
                        pad.forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                row.forEach { key ->
                                    OutlinedButton(onClick = { expr += key }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) {
                                        Text(key)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(onClick = { expr = ""; output = "0" }, modifier = Modifier.weight(1f)) { Text("Clear") }
                            NeonGradientButton(
                                text = "Calculate =",
                                onClick = {
                                    ScientificCalculatorEngine.evaluate(expr)
                                        .onSuccess { v ->
                                            val resStr = if (v % 1.0 == 0.0) v.toLong().toString() else String.format(Locale.US, "%.6f", v).trimEnd('0').trimEnd('.')
                                            output = resStr
                                            onSaveCalc(expr, resStr)
                                        }
                                        .onFailure { output = it.localizedMessage ?: "Error" }
                                },
                                modifier = Modifier.weight(1.2f)
                            )
                        }
                    }
                }
                if (calcHistory.isNotEmpty()) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("History", style = MaterialTheme.typography.titleMedium)
                            IconButton(onClick = onClearCalc) { Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear history") }
                        }
                    }
                    items(calcHistory, key = { it.id }) { h ->
                        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 10.dp) {
                            Text("${h.expression} = ${h.result}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            1 -> {
                item {
                    var category by remember { mutableStateOf(UnitConverterEngine.Category.LENGTH) }
                    var inputStr by remember { mutableStateOf("100") }
                    var fromUnit by remember(category) { mutableStateOf(category.units.first()) }
                    var toUnit by remember(category) { mutableStateOf(category.units.getOrElse(1) { category.units.first() }) }
                    val value = inputStr.toDoubleOrNull() ?: 0.0
                    val converted = UnitConverterEngine.convert(category, value, fromUnit, toUnit)

                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            UnitConverterEngine.Category.entries.forEach { c ->
                                FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c.label) })
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(value = inputStr, onValueChange = { inputStr = it }, label = { Text("Value") }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        Text("From", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            category.units.forEach { u -> FilterChip(selected = fromUnit == u, onClick = { fromUnit = u }, label = { Text(u) }) }
                        }
                        Text("To", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            category.units.forEach { u -> FilterChip(selected = toUnit == u, onClick = { toUnit = u }, label = { Text(u) }) }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "${String.format(Locale.US, "%.4f", converted).trimEnd('0').trimEnd('.')} $toUnit",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            2 -> {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Live Internet Speed Test", style = MaterialTheme.typography.titleLarge)
                        }
                        Spacer(Modifier.height(8.dp))
                        if (speedTestError != null) {
                            Text(speedTestError, color = MaterialTheme.colorScheme.error)
                        }
                        NeonGradientButton(
                            text = if (speedTestRunning) "Testing throughput..." else "Start Speed Test",
                            enabled = !speedTestRunning,
                            onClick = onRunSpeedTest,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                items(speedTests, key = { it.id }) { st ->
                    val fmt = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 12.dp, accentStripeColor = MaterialTheme.colorScheme.primary) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Down: ${String.format(Locale.US, "%.1f", st.downloadMbps)} Mbps • Up: ${String.format(Locale.US, "%.1f", st.uploadMbps)} Mbps", style = MaterialTheme.typography.titleMedium)
                                Text("Latency ${st.latencyMs} ms • ${fmt.format(Date(st.timestampMillis))}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            3 -> {
                item {
                    val ext = LocalRrColors.current
                    val pct = (batteryStatus.levelPercent / 100f).coerceIn(0f, 1f)
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.BatteryChargingFull, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text("Device Battery Monitor", style = MaterialTheme.typography.titleLarge)
                            }
                            OutlinedButton(onClick = onRefreshBattery) {
                                Text("Refresh")
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RadialProgressRing(
                                progress = pct,
                                size = 128.dp,
                                strokeWidth = 10.dp,
                                primaryColor = if (batteryStatus.levelPercent > 20) ext.success else ext.warning,
                                secondaryColor = ext.glowPrimary
                            ) {
                                Text(
                                    "${batteryStatus.levelPercent}%",
                                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            StatusPill(
                                text = if (batteryStatus.isCharging) "Charging (${batteryStatus.pluggedSource})" else "Discharging",
                                color = if (batteryStatus.isCharging) ext.success else MaterialTheme.colorScheme.primary
                            )
                            val currentLabel = if (batteryStatus.currentMa != 0) " • ${batteryStatus.currentMa} mA" else ""
                            Text(
                                "Health: ${batteryStatus.healthLabel} • ${batteryStatus.temperatureCelsius}°C • ${batteryStatus.voltageMv} mV • ${batteryStatus.technology}$currentLabel",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationHubScreen(
    logs: List<NotificationLogEntity>,
    notificationReaderEnabled: Boolean,
    onToggleNotificationReader: (Boolean) -> Unit,
    onMarkAllRead: () -> Unit,
    onClearLogs: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val listenerEnabledInSystem = remember(context, notificationReaderEnabled) {
        val enabledListeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners").orEmpty()
        val cn = ComponentName(context, RrNotificationListenerService::class.java)
        enabledListeners.contains(cn.flattenToString())
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SubScreenTopBar("Notification Hub", "History & Voice Reader", onBack) {
                Row {
                    IconButton(onClick = onMarkAllRead) { Icon(Icons.Rounded.DoneAll, contentDescription = "Mark all read") }
                    IconButton(onClick = onClearLogs) { Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear logs") }
                }
            }
        }
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Notification Voice Reader", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Optionally reads incoming notifications aloud using Android's NotificationListenerService. Contents are never read or collected unless you explicitly grant system permission and enable the switch below.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Read Notifications Aloud")
                    Switch(checked = notificationReaderEnabled, onCheckedChange = onToggleNotificationReader)
                }
                if (!listenerEnabledInSystem) {
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(onClick = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }) {
                        Text("Grant System Notification Listener Access")
                    }
                }
            }
        }
        if (logs.isEmpty()) {
            item {
                PremiumEmptyState(
                    icon = Icons.Rounded.Notifications,
                    title = "Notification Log Empty",
                    subtitle = "Captured notifications and voice-reader activity will appear here once enabled."
                )
            }
        }
        items(logs, key = { it.id }) { log ->
            val fmt = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 12.dp,
                accentStripeColor = if (!log.isRead) MaterialTheme.colorScheme.primary else null
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatusPill(text = log.category)
                    Text(fmt.format(Date(log.timestampMillis)), style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.height(4.dp))
                Text(log.title, style = MaterialTheme.typography.titleMedium)
                Text(log.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
