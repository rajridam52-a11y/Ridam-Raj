package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ui.theme.AuroraMagenta
import com.example.ui.theme.CosmicViolet
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GlassCard
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.ScreenOverlineHeader
import com.example.ui.theme.StatusPill
import com.example.ui.theme.SuccessMint
import com.example.ui.theme.WarningAmber

private data class ToolTile(
    val route: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val tint: Color
)

@Composable
fun ToolsHubScreen(
    alarmCount: Int,
    taskCount: Int,
    reminderCount: Int,
    noteCount: Int,
    birthdayCount: Int,
    goalCount: Int,
    onNavigate: (String) -> Unit
) {
    val featured = listOf(
        ToolTile("alarms", "Alarms", "$alarmCount active", Icons.Rounded.Alarm, CosmicViolet),
        ToolTile("timer", "Time Studio", "Pomodoro • Laps", Icons.Rounded.Timer, ElectricCyan)
    )
    val tools = listOf(
        ToolTile("world_clock", "World Clock", "Global zones", Icons.Rounded.Public, NeonBlue),
        ToolTile("weather", "Live Weather", "7-day • AQI", Icons.Rounded.WbSunny, WarningAmber),
        ToolTile("tasks", "Task Manager", "$taskCount pending", Icons.Rounded.CheckCircle, SuccessMint),
        ToolTile("goals", "Goals & Habits", "$goalCount goals", Icons.Rounded.Flag, SuccessMint),
        ToolTile("location_reminders", "Location Alerts", "$reminderCount active", Icons.Rounded.LocationOn, NeonBlue),
        ToolTile("birthdays", "Birthdays", "$birthdayCount saved", Icons.Rounded.Cake, AuroraMagenta),
        ToolTile("notes", "Notes & Voice", "$noteCount saved", Icons.Rounded.EditNote, WarningAmber),
        ToolTile("productivity", "Productivity", "Calc • Units • Net", Icons.Rounded.Calculate, ElectricCyan),
        ToolTile("notifications_log", "Notification Hub", "Reader & Log", Icons.Rounded.Notifications, AuroraMagenta),
        ToolTile("design_studio", "Design Studio", "Themes & Colors", Icons.Rounded.Palette, CosmicViolet)
    )

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 114.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ScreenOverlineHeader(
                overline = "ALL-IN-ONE SUITE",
                title = "More Features",
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                StatusPill(text = "${featured.size + tools.size} Tools", color = MaterialTheme.colorScheme.primary)
            }
        }

        // Featured Duo Bento Row
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                featured.forEach { tile ->
                    GlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tool_tile_${tile.route}"),
                        cornerRadius = 22.dp,
                        contentPadding = 14.dp,
                        accentStripeColor = tile.tint,
                        onClick = { onNavigate(tile.route) }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(tile.tint.copy(alpha = 0.30f), tile.tint.copy(alpha = 0.12f))
                                        )
                                    )
                                    .border(1.dp, tile.tint.copy(alpha = 0.60f), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(tile.icon, contentDescription = tile.title, tint = tile.tint, modifier = Modifier.size(22.dp))
                            }
                            Text(tile.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1)
                            Text(tile.subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                }
            }
        }

        items(tools, key = { it.route }) { tile ->
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp)
                    .testTag("tool_tile_${tile.route}"),
                cornerRadius = 20.dp,
                contentPadding = 12.dp,
                onClick = { onNavigate(tile.route) }
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(tile.tint.copy(alpha = 0.28f), tile.tint.copy(alpha = 0.10f))
                                )
                            )
                            .border(1.dp, tile.tint.copy(alpha = 0.55f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = tile.icon,
                            contentDescription = tile.title,
                            tint = tile.tint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        tile.title,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        tile.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
