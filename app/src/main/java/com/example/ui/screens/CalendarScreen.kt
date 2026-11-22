package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Brush
import com.example.data.local.BirthdayEntity
import com.example.data.local.CalendarEventEntity
import com.example.ui.theme.GlassCard
import com.example.ui.theme.GlassSegmentedTabs
import com.example.ui.theme.LocalRrColors
import com.example.ui.theme.PremiumEmptyState
import com.example.ui.theme.ScreenOverlineHeader
import com.example.ui.theme.StatusPill
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val EventCategories = listOf("All", "Appointments", "Study", "Important", "Birthday", "Personal")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarScreen(
    events: List<CalendarEventEntity>,
    birthdays: List<BirthdayEntity>,
    use24Hour: Boolean,
    onSaveEvent: (CalendarEventEntity) -> Unit,
    onDeleteEvent: (String) -> Unit
) {
    var viewMode by remember { mutableIntStateOf(0) } // 0=Monthly, 1=Weekly, 2=Daily
    var selectedDateMillis by remember { mutableLongStateOf(startOfDayMillis(System.currentTimeMillis())) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var editingEvent by remember { mutableStateOf<CalendarEventEntity?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    val selectedCal = remember(selectedDateMillis) {
        Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
    }

    val filteredEvents = remember(events, searchQuery, selectedCategory, viewMode, selectedDateMillis) {
        val dayStart = startOfDayMillis(selectedDateMillis)
        val dayEnd = dayStart + 86_400_000L
        val weekStart = dayStart - (selectedCal.get(Calendar.DAY_OF_WEEK) - 1) * 86_400_000L
        val weekEnd = weekStart + 7 * 86_400_000L

        events.filter { ev ->
            val matchSearch = searchQuery.isBlank() ||
                ev.title.contains(searchQuery, ignoreCase = true) ||
                ev.description.contains(searchQuery, ignoreCase = true)
            val matchCat = selectedCategory == "All" || ev.category.equals(selectedCategory, ignoreCase = true)
            val matchTime = when (viewMode) {
                2 -> ev.startMillis in dayStart until dayEnd
                1 -> ev.startMillis in weekStart until weekEnd
                else -> {
                    val cal = Calendar.getInstance().apply { timeInMillis = ev.startMillis }
                    cal.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR) &&
                        cal.get(Calendar.MONTH) == selectedCal.get(Calendar.MONTH)
                }
            }
            matchSearch && matchCat && matchTime
        }
    }

    val matchingBirthdays = remember(birthdays, selectedCal, viewMode) {
        val m = selectedCal.get(Calendar.MONTH) + 1
        val d = selectedCal.get(Calendar.DAY_OF_MONTH)
        birthdays.filter { b ->
            if (viewMode == 2) b.month == m && b.day == d
            else b.month == m
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingEvent = null
                    showDialog = true
                },
                modifier = Modifier.testTag("calendar_add_fab")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add event")
            }
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ScreenOverlineHeader(
                    overline = "SMART CALENDAR",
                    title = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(selectedDateMillis))
                )
            }

            item {
                GlassSegmentedTabs(
                    tabs = listOf("Monthly", "Weekly", "Daily"),
                    selectedIndex = viewMode,
                    onSelect = { viewMode = it }
                )
            }

            // Month navigation + Grid (for Monthly/Weekly)
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            val c = Calendar.getInstance().apply {
                                timeInMillis = selectedDateMillis
                                add(if (viewMode == 0) Calendar.MONTH else Calendar.DAY_OF_YEAR, -1)
                            }
                            selectedDateMillis = startOfDayMillis(c.timeInMillis)
                        }) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous")
                        }
                        Text(
                            text = SimpleDateFormat(
                                if (viewMode == 0) "MMMM yyyy" else "EEE, MMM d",
                                Locale.getDefault()
                            ).format(Date(selectedDateMillis)),
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(onClick = {
                            val c = Calendar.getInstance().apply {
                                timeInMillis = selectedDateMillis
                                add(if (viewMode == 0) Calendar.MONTH else Calendar.DAY_OF_YEAR, 1)
                            }
                            selectedDateMillis = startOfDayMillis(c.timeInMillis)
                        }) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next")
                        }
                    }

                    if (viewMode == 0) {
                        Spacer(Modifier.height(8.dp))
                        MonthGrid(
                            selectedDateMillis = selectedDateMillis,
                            events = events,
                            onSelectDate = { selectedDateMillis = it }
                        )
                    }
                }
            }

            // Search & Categories
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search events...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp)
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EventCategories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }

            if (matchingBirthdays.isNotEmpty()) {
                items(matchingBirthdays, key = { "b_${it.id}" }) { bday ->
                    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 12.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Cake, contentDescription = "Birthday", tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(10.dp))
                            Text("${bday.name}'s Birthday (${bday.month}/${bday.day})", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }

            if (filteredEvents.isEmpty()) {
                item {
                    PremiumEmptyState(
                        icon = Icons.Rounded.Edit,
                        title = "No Events Scheduled",
                        subtitle = "Your calendar is clear for this period. Tap below to add an appointment or reminder.",
                        actionLabel = "Add Event",
                        onAction = { editingEvent = null; showDialog = true }
                    )
                }
            } else {
                items(filteredEvents, key = { it.id }) { ev ->
                    val timeFmt = SimpleDateFormat(if (use24Hour) "MMM d • HH:mm" else "MMM d • h:mm a", Locale.getDefault())
                    val stripeColor = when (ev.category) {
                        "Important" -> Color(0xFFF43F5E)
                        "Study" -> Color(0xFF10B981)
                        "Appointments" -> Color(0xFF00E5FF)
                        "Birthday" -> Color(0xFFD946EF)
                        else -> MaterialTheme.colorScheme.primary
                    }
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = 14.dp,
                        accentStripeColor = stripeColor
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                StatusPill(text = ev.category)
                                Spacer(Modifier.height(6.dp))
                                Text(ev.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = if (ev.isAllDay) "All day • ${SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(ev.startMillis))}"
                                    else timeFmt.format(Date(ev.startMillis)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (ev.description.isNotBlank()) {
                                    Text(ev.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Row {
                                IconButton(onClick = { editingEvent = ev; showDialog = true }) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "Edit event")
                                }
                                IconButton(onClick = { onDeleteEvent(ev.id) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete event", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(72.dp)) }
        }
    }

    if (showDialog) {
        EventEditorDialog(
            initial = editingEvent,
            defaultDateMillis = selectedDateMillis,
            onDismiss = { showDialog = false },
            onSave = { ev ->
                onSaveEvent(ev)
                showDialog = false
            }
        )
    }
}

@Composable
private fun MonthGrid(
    selectedDateMillis: Long,
    events: List<CalendarEventEntity>,
    onSelectDate: (Long) -> Unit
) {
    val cal = Calendar.getInstance().apply {
        timeInMillis = selectedDateMillis
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val month = cal.get(Calendar.MONTH)
    val year = cal.get(Calendar.YEAR)
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val selectedDay = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }.get(Calendar.DAY_OF_MONTH)

    val eventDays = remember(events, month, year) {
        events.mapNotNull {
            val c = Calendar.getInstance().apply { timeInMillis = it.startMillis }
            if (c.get(Calendar.MONTH) == month && c.get(Calendar.YEAR) == year) c.get(Calendar.DAY_OF_MONTH) else null
        }.toSet()
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { d ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(d, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        val totalCells = firstDayOfWeek + daysInMonth
        val rows = (totalCells + 6) / 7
        for (r in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val cellIdx = r * 7 + c
                    val dayNum = cellIdx - firstDayOfWeek + 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .then(
                                if (dayNum in 1..daysInMonth) {
                                    val isSelected = dayNum == selectedDay
                                    Modifier
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable {
                                            val target = Calendar.getInstance().apply {
                                                set(year, month, dayNum, 9, 0, 0)
                                                set(Calendar.MILLISECOND, 0)
                                            }
                                            onSelectDate(startOfDayMillis(target.timeInMillis))
                                        }
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (dayNum in 1..daysInMonth) {
                            val isSelected = dayNum == selectedDay
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = dayNum.toString(),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                if (eventDays.contains(dayNum)) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EventEditorDialog(
    initial: CalendarEventEntity?,
    defaultDateMillis: Long,
    onDismiss: () -> Unit,
    onSave: (CalendarEventEntity) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "Appointments") }
    var recurrence by remember { mutableStateOf(initial?.recurrence ?: "NONE") }
    var hourText by remember {
        val h = Calendar.getInstance().apply { timeInMillis = initial?.startMillis ?: (defaultDateMillis + 9 * 3600_000L) }.get(Calendar.HOUR_OF_DAY)
        mutableStateOf(h.toString())
    }
    var minText by remember {
        val m = Calendar.getInstance().apply { timeInMillis = initial?.startMillis ?: defaultDateMillis }.get(Calendar.MINUTE)
        mutableStateOf(m.toString())
    }
    var isAllDay by remember { mutableStateOf(initial?.isAllDay ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New Calendar Event" else "Edit Event") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Event Title") }, singleLine = true)
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Notes") }, maxLines = 2)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = hourText, onValueChange = { hourText = it.filter { ch -> ch.isDigit() }.take(2) }, label = { Text("Hour (0-23)") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = minText, onValueChange = { minText = it.filter { ch -> ch.isDigit() }.take(2) }, label = { Text("Min (0-59)") }, modifier = Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isAllDay, onCheckedChange = { isAllDay = it })
                    Text("All-day event")
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EventCategories.drop(1).forEach { cat ->
                        FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat) })
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("NONE", "DAILY", "WEEKLY", "MONTHLY").forEach { rec ->
                        FilterChip(selected = recurrence == rec, onClick = { recurrence = rec }, label = { Text(rec) })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank(),
                onClick = {
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = initial?.startMillis ?: defaultDateMillis
                        set(Calendar.HOUR_OF_DAY, (hourText.toIntOrNull() ?: 9).coerceIn(0, 23))
                        set(Calendar.MINUTE, (minText.toIntOrNull() ?: 0).coerceIn(0, 59))
                        set(Calendar.SECOND, 0)
                    }
                    val start = cal.timeInMillis
                    onSave(
                        (initial ?: CalendarEventEntity(title = title.trim(), startMillis = start, endMillis = start + 3_600_000L)).copy(
                            title = title.trim(),
                            description = description.trim(),
                            startMillis = start,
                            endMillis = start + 3_600_000L,
                            category = category,
                            isAllDay = isAllDay,
                            recurrence = recurrence
                        )
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun startOfDayMillis(millis: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}
