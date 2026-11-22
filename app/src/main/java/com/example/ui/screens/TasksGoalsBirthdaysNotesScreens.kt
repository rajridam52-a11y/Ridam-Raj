package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.local.BirthdayEntity
import com.example.data.local.GoalEntity
import com.example.data.local.HabitEntity
import com.example.data.local.NoteEntity
import com.example.data.local.ReminderEntity
import com.example.data.local.TaskEntity
import com.example.data.prefs.CustomRingtoneItem
import com.example.data.remote.GeocodingResultDto
import com.example.system.VoiceNoteRecorder
import com.example.ui.theme.GlassCard
import com.example.ui.theme.GlassSegmentedTabs
import com.example.ui.theme.LocalRrColors
import com.example.ui.theme.PremiumEmptyState
import com.example.ui.theme.RadialProgressRing
import com.example.ui.theme.StatusPill
import com.example.ui.theme.WaveformVisualizerBar
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.ui.text.font.FontWeight
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TasksScreen(
    tasks: List<TaskEntity>,
    onSaveTask: (TaskEntity) -> Unit,
    onToggleTask: (TaskEntity) -> Unit,
    onDeleteTask: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var filterTab by remember { mutableIntStateOf(0) } // 0=Pending, 1=Completed
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TaskEntity?>(null) }

    val filtered = remember(tasks, filterTab) {
        if (filterTab == 0) tasks.filter { !it.isCompleted } else tasks.filter { it.isCompleted }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SubScreenTopBar("Smart Tasks", "Priorities & Recurring", onBack) {
                Button(onClick = { editing = null; showAdd = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Text(" Task")
                }
            }
        }
        item {
            GlassSegmentedTabs(
                tabs = listOf(
                    "Pending (${tasks.count { !it.isCompleted }})",
                    "Completed (${tasks.count { it.isCompleted }})"
                ),
                selectedIndex = filterTab,
                onSelect = { filterTab = it }
            )
        }
        if (filtered.isEmpty()) {
            item {
                PremiumEmptyState(
                    icon = Icons.Rounded.CheckCircle,
                    title = if (filterTab == 0) "All Caught Up" else "No Completed Tasks Yet",
                    subtitle = if (filterTab == 0) "You have no pending tasks. Add one to organize your day." else "Completed tasks will appear here.",
                    actionLabel = if (filterTab == 0) "Add Task" else null,
                    onAction = if (filterTab == 0) { { editing = null; showAdd = true } } else null
                )
            }
        }
        items(filtered, key = { it.id }) { task ->
            val ext = LocalRrColors.current
            val stripe = when (task.priority) {
                "HIGH" -> MaterialTheme.colorScheme.error
                "MEDIUM" -> ext.warning
                else -> ext.success
            }
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 12.dp,
                accentStripeColor = stripe
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = task.isCompleted, onCheckedChange = { onToggleTask(task) })
                    Column(modifier = Modifier.weight(1f)) {
                        Text(task.title, style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatusPill(text = task.priority)
                            StatusPill(text = task.category, color = MaterialTheme.colorScheme.secondary)
                        }
                        if (task.description.isNotBlank()) {
                            Text(task.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = { editing = task; showAdd = true }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit task")
                    }
                    IconButton(onClick = { onDeleteTask(task.id) }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete task", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (showAdd) {
        var title by remember { mutableStateOf(editing?.title ?: "") }
        var desc by remember { mutableStateOf(editing?.description ?: "") }
        var priority by remember { mutableStateOf(editing?.priority ?: "MEDIUM") }
        var category by remember { mutableStateOf(editing?.category ?: "Personal") }
        var recurrence by remember { mutableStateOf(editing?.recurrence ?: "NONE") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text(if (editing == null) "New Task" else "Edit Task") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true)
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Details") }, maxLines = 2)
                    Text("Priority", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("LOW", "MEDIUM", "HIGH").forEach { p ->
                            FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(p) })
                        }
                    }
                    Text("Category", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Personal", "Work", "Study", "Health").forEach { c ->
                            FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c) })
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("NONE", "DAILY", "WEEKLY").forEach { r ->
                            FilterChip(selected = recurrence == r, onClick = { recurrence = r }, label = { Text(r) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(enabled = title.isNotBlank(), onClick = {
                    onSaveTask(
                        (editing ?: TaskEntity(title = title.trim())).copy(
                            title = title.trim(),
                            description = desc.trim(),
                            priority = priority,
                            category = category,
                            recurrence = recurrence
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
fun GoalsHabitsScreen(
    goals: List<GoalEntity>,
    habits: List<HabitEntity>,
    onSaveGoal: (GoalEntity) -> Unit,
    onIncrementGoal: (GoalEntity, Int) -> Unit,
    onDeleteGoal: (String) -> Unit,
    onSaveHabit: (HabitEntity) -> Unit,
    onCheckInHabit: (HabitEntity) -> Unit,
    onDeleteHabit: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var tab by remember { mutableIntStateOf(0) }
    var showAddGoal by remember { mutableStateOf(false) }
    var showAddHabit by remember { mutableStateOf(false) }
    val todayKey = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SubScreenTopBar("Goals & Habits", "Daily Momentum", onBack) {
                Button(onClick = { if (tab == 0) showAddGoal = true else showAddHabit = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Text(if (tab == 0) " Goal" else " Habit")
                }
            }
        }
        item {
            GlassSegmentedTabs(
                tabs = listOf("Daily Goals", "Habit Tracker"),
                selectedIndex = tab,
                onSelect = { tab = it }
            )
        }

        if (tab == 0) {
            if (goals.isEmpty()) {
                item {
                    PremiumEmptyState(
                        icon = Icons.Rounded.CheckCircle,
                        title = "No Daily Goals",
                        subtitle = "Set measurable daily targets to track your focus and progress.",
                        actionLabel = "Add Goal",
                        onAction = { showAddGoal = true }
                    )
                }
            }
            items(goals, key = { it.id }) { goal ->
                val pct = (goal.currentCount.toFloat() / goal.targetCount.coerceAtLeast(1)).coerceIn(0f, 1f)
                val ext = LocalRrColors.current
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            RadialProgressRing(
                                progress = pct,
                                size = 50.dp,
                                strokeWidth = 5.dp,
                                primaryColor = ext.success,
                                secondaryColor = ext.glowPrimary
                            ) {
                                Text(
                                    "${(pct * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ext.success
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(goal.title, style = MaterialTheme.typography.titleMedium)
                                Text("${goal.currentCount} / ${goal.targetCount} ${goal.unit}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Row {
                            OutlinedButton(onClick = { onIncrementGoal(goal, 1) }) { Text("+1") }
                            IconButton(onClick = { onDeleteGoal(goal.id) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Delete goal", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        } else {
            if (habits.isEmpty()) {
                item {
                    PremiumEmptyState(
                        icon = Icons.Rounded.LocalFireDepartment,
                        title = "No Habits Tracked",
                        subtitle = "Build daily streaks and track consistency across wellness and productivity.",
                        actionLabel = "Add Habit",
                        onAction = { showAddHabit = true }
                    )
                }
            }
            items(habits, key = { it.id }) { habit ->
                val checkedToday = habit.historyDatesCsv.split(",").contains(todayKey)
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(habit.name, style = MaterialTheme.typography.titleMedium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.LocalFireDepartment, contentDescription = "Streak", tint = LocalRrColors.current.warning, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Streak: ${habit.streak}d • Best: ${habit.bestStreak}d • ${habit.category}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onCheckInHabit(habit) }) {
                                Icon(
                                    if (checkedToday) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                    contentDescription = "Check in today",
                                    tint = if (checkedToday) LocalRrColors.current.success else MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { onDeleteHabit(habit.id) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Delete habit", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddGoal) {
        var title by remember { mutableStateOf("") }
        var target by remember { mutableStateOf("5") }
        var unit by remember { mutableStateOf("steps") }
        AlertDialog(
            onDismissRequest = { showAddGoal = false },
            title = { Text("New Daily Goal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Goal Title") })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = target, onValueChange = { target = it.filter { c -> c.isDigit() } }, label = { Text("Target") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Unit") }, modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {
                Button(enabled = title.isNotBlank(), onClick = {
                    onSaveGoal(GoalEntity(title = title.trim(), targetCount = (target.toIntOrNull() ?: 1).coerceAtLeast(1), unit = unit.ifBlank { "units" }))
                    showAddGoal = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddGoal = false }) { Text("Cancel") } }
        )
    }

    if (showAddHabit) {
        var name by remember { mutableStateOf("") }
        var cat by remember { mutableStateOf("Wellness") }
        AlertDialog(
            onDismissRequest = { showAddHabit = false },
            title = { Text("New Habit") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Habit Name") })
                    OutlinedTextField(value = cat, onValueChange = { cat = it }, label = { Text("Category") })
                }
            },
            confirmButton = {
                Button(enabled = name.isNotBlank(), onClick = {
                    onSaveHabit(HabitEntity(name = name.trim(), category = cat.ifBlank { "Wellness" }))
                    showAddHabit = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddHabit = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun BirthdaysScreen(
    birthdays: List<BirthdayEntity>,
    onSaveBirthday: (BirthdayEntity) -> Unit,
    onDeleteBirthday: (String) -> Unit,
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
            SubScreenTopBar("Birthday Manager", "Annual Reminders & Countdowns", onBack) {
                Button(onClick = { showAdd = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Text(" Birthday")
                }
            }
        }
        if (birthdays.isEmpty()) {
            item {
                PremiumEmptyState(
                    icon = Icons.Rounded.Cake,
                    title = "No Birthdays Saved",
                    subtitle = "Save upcoming birthdays with optional photos and advance gift reminders.",
                    actionLabel = "Add Birthday",
                    onAction = { showAdd = true }
                )
            }
        }
        items(birthdays, key = { it.id }) { b ->
            val daysLeft = daysUntilBirthday(b.month, b.day)
            GlassCard(modifier = Modifier.fillMaxWidth(), accentStripeColor = MaterialTheme.colorScheme.secondary) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (!b.photoUri.isNullOrBlank()) {
                        AsyncImage(
                            model = Uri.parse(b.photoUri),
                            contentDescription = "${b.name} photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(48.dp).clip(CircleShape)
                        )
                    } else {
                        Icon(Icons.Rounded.Cake, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(40.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(b.name, style = MaterialTheme.typography.titleMedium)
                        Text("${b.month}/${b.day}" + (b.birthYear?.let { " ($it)" } ?: "") + " • Advance ${b.advanceDays}d", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (b.notes.isNotBlank()) Text(b.notes, style = MaterialTheme.typography.bodyMedium)
                    }
                    StatusPill(text = if (daysLeft == 0) "Today!" else "${daysLeft}d")
                    IconButton(onClick = { onDeleteBirthday(b.id) }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete birthday", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        var month by remember { mutableStateOf("12") }
        var day by remember { mutableStateOf("15") }
        var year by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        var photoUri by remember { mutableStateOf<String?>(null) }

        val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            photoUri = uri?.toString()
        }

        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Add Birthday") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Person's Name") })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = month, onValueChange = { month = it.filter { c -> c.isDigit() }.take(2) }, label = { Text("Month") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = day, onValueChange = { day = it.filter { c -> c.isDigit() }.take(2) }, label = { Text("Day") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = year, onValueChange = { year = it.filter { c -> c.isDigit() }.take(4) }, label = { Text("Year") }, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Gift ideas / Notes") })
                    OutlinedButton(onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Icon(Icons.Rounded.PhotoCamera, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (photoUri == null) "Optional Photo" else "Photo Selected")
                    }
                }
            },
            confirmButton = {
                Button(enabled = name.isNotBlank(), onClick = {
                    onSaveBirthday(
                        BirthdayEntity(
                            name = name.trim(),
                            month = (month.toIntOrNull() ?: 1).coerceIn(1, 12),
                            day = (day.toIntOrNull() ?: 1).coerceIn(1, 31),
                            birthYear = year.toIntOrNull()?.takeIf { it in 1900..2100 },
                            notes = notes.trim(),
                            photoUri = photoUri
                        )
                    )
                    showAdd = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
        )
    }
}

private fun daysUntilBirthday(month: Int, day: Int): Int {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val next = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.MONTH, (month - 1).coerceIn(0, 11))
        val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
        set(Calendar.DAY_OF_MONTH, day.coerceIn(1, maxDay))
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    if (next.before(today)) next.add(Calendar.YEAR, 1)
    return ((next.timeInMillis - today.timeInMillis) / 86_400_000L).toInt().coerceAtLeast(0)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotesVoiceScreen(
    notes: List<NoteEntity>,
    voiceRecorder: VoiceNoteRecorder,
    onSaveNote: (NoteEntity) -> Unit,
    onDeleteNote: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    DisposableEffect(Unit) {
        onDispose { voiceRecorder.stopPlayback() }
    }
    val context = LocalContext.current
    var search by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var playingId by remember { mutableStateOf<String?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var recordError by remember { mutableStateOf<String?>(null) }

    val micPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            voiceRecorder.startRecording().onSuccess { isRecording = true }.onFailure { recordError = it.localizedMessage }
        } else {
            recordError = "Microphone permission is required for voice notes."
        }
    }

    val filtered = remember(notes, search) {
        notes.filter {
            search.isBlank() || it.title.contains(search, ignoreCase = true) || it.content.contains(search, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SubScreenTopBar("Notes & Voice Notes", "Pinned • Categorized • Audio", onBack) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = {
                        if (isRecording) {
                            val (path, dur) = voiceRecorder.stopRecording()
                            isRecording = false
                            if (path != null) {
                                onSaveNote(
                                    NoteEntity(
                                        title = "Voice Note (${dur}s)",
                                        content = "Audio recording (${dur}s)",
                                        category = "Voice",
                                        isVoiceNote = true,
                                        audioPath = path,
                                        durationSeconds = dur
                                    )
                                )
                            }
                        } else {
                            val ok = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                            if (ok) {
                                voiceRecorder.startRecording().onSuccess { isRecording = true }.onFailure { recordError = it.localizedMessage }
                            } else {
                                micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    }) {
                        Icon(if (isRecording) Icons.Rounded.Stop else Icons.Rounded.Mic, contentDescription = "Record voice note")
                        Text(if (isRecording) " Stop" else " Rec")
                    }
                    Button(onClick = { showAdd = true }) { Text("+ Note") }
                }
            }
        }
        if (isRecording) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth(), accentStripeColor = MaterialTheme.colorScheme.error) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatusPill(text = "REC", color = MaterialTheme.colorScheme.error, icon = Icons.Rounded.Mic)
                        WaveformVisualizerBar(isActive = true, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        if (recordError != null) {
            item { Text(recordError!!, color = MaterialTheme.colorScheme.error) }
        }
        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search notes...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true
            )
        }
        if (filtered.isEmpty()) {
            item {
                PremiumEmptyState(
                    icon = Icons.Rounded.Mic,
                    title = "No Notes Yet",
                    subtitle = "Create a quick text note or tap Rec to capture an audio voice note.",
                    actionLabel = "New Note",
                    onAction = { showAdd = true }
                )
            }
        }
        items(filtered, key = { it.id }) { note ->
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                accentStripeColor = if (note.isPinned) MaterialTheme.colorScheme.primary else null
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        StatusPill(text = note.category)
                        Spacer(Modifier.height(4.dp))
                        Text(note.title, style = MaterialTheme.typography.titleMedium)
                        Text(note.content, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row {
                        if (note.isVoiceNote && note.audioPath != null) {
                            IconButton(onClick = {
                                if (playingId == note.id) {
                                    voiceRecorder.stopPlayback()
                                    playingId = null
                                } else {
                                    playingId = note.id
                                    voiceRecorder.playAudio(note.audioPath, onCompletion = { playingId = null }, onError = { playingId = null })
                                }
                            }) {
                                Icon(if (playingId == note.id) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, contentDescription = "Play audio")
                            }
                        }
                        IconButton(onClick = { onSaveNote(note.copy(isPinned = !note.isPinned)) }) {
                            Icon(Icons.Rounded.PushPin, contentDescription = "Pin", tint = if (note.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { onDeleteNote(note.id) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete note", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var title by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }
        var cat by remember { mutableStateOf("Quick Note") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("New Note") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
                    OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Content") }, minLines = 3)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Quick Note", "Work", "Study", "Ideas").forEach { c ->
                            FilterChip(selected = cat == c, onClick = { cat = c }, label = { Text(c) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(enabled = title.isNotBlank() || content.isNotBlank(), onClick = {
                    onSaveNote(NoteEntity(title = title.ifBlank { "Note" }, content = content.trim(), category = cat))
                    showAdd = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LocationRemindersScreen(
    reminders: List<ReminderEntity>,
    searchResults: List<GeocodingResultDto>,
    defaultRingtoneName: String,
    defaultRingtoneUri: String?,
    savedRingtones: List<CustomRingtoneItem>,
    onImportSongForReminder: (Uri, (CustomRingtoneItem) -> Unit) -> Unit,
    onPreviewRingtone: (String, String, String?) -> Unit,
    onStopPreview: () -> Unit,
    onSearchPlace: (String) -> Unit,
    onSaveReminder: (ReminderEntity) -> Unit,
    onDeleteReminder: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = {
        onStopPreview()
        onBack()
    })
    val context = LocalContext.current
    var showAdd by remember { mutableStateOf(false) }

    val locPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SubScreenTopBar(
                title = "Smart Reminders",
                subtitle = "Time & Geofence Alerts",
                onBack = {
                    onStopPreview()
                    onBack()
                }
            ) {
                Button(onClick = { showAdd = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Text(" Add")
                }
            }
        }
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Location Geofencing & Battery Info", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Location-based reminders use Android's low-power GeofencingClient. Grant Fine Location permission when adding a place-triggered reminder.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = { locPermLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }) {
                    Icon(Icons.Rounded.LocationOn, contentDescription = null)
                    Text(" Check / Grant Location Permission")
                }
            }
        }
        if (reminders.isEmpty()) {
            item {
                PremiumEmptyState(
                    icon = Icons.Rounded.NotificationsActive,
                    title = "No Active Reminders",
                    subtitle = "Schedule a timed alert or pick a place to trigger a GPS geofence reminder.",
                    actionLabel = "Create Reminder",
                    onAction = { showAdd = true }
                )
            }
        }
        items(reminders, key = { it.id }) { rem ->
            val fmt = SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault())
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 12.dp,
                accentStripeColor = if (!rem.isCompleted) MaterialTheme.colorScheme.primary else null
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = rem.isCompleted, onCheckedChange = { onSaveReminder(rem.copy(isCompleted = it)) })
                    Column(modifier = Modifier.weight(1f)) {
                        Text(rem.title, style = MaterialTheme.typography.titleMedium)
                        Text(fmt.format(Date(rem.triggerAtMillis)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        if (!rem.locationName.isNullOrBlank()) {
                            Text("Location: ${rem.locationName} (${rem.radiusMeters}m)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                        }
                        if (!rem.soundUri.isNullOrBlank() || rem.soundName != "Default") {
                            Text("Ringtone: ♪ ${rem.soundName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = { onDeleteReminder(rem.id) }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete reminder", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (showAdd) {
        var title by remember { mutableStateOf("") }
        var minsFromNow by remember { mutableStateOf("30") }
        var placeQuery by remember { mutableStateOf("") }
        var selectedPlace by remember { mutableStateOf<GeocodingResultDto?>(null) }
        var soundName by remember { mutableStateOf(defaultRingtoneName) }
        var soundUri by remember { mutableStateOf(defaultRingtoneUri) }
        var isPreviewing by remember { mutableStateOf(false) }

        val pickSongLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                onImportSongForReminder(uri) { imported ->
                    soundName = imported.name
                    soundUri = imported.uriString
                }
            }
        }

        AlertDialog(
            onDismissRequest = {
                onStopPreview()
                showAdd = false
            },
            title = { Text("Create Reminder") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Reminder Title") })
                    OutlinedTextField(value = minsFromNow, onValueChange = { minsFromNow = it.filter { c -> c.isDigit() } }, label = { Text("Trigger in (minutes)") })
                    OutlinedTextField(
                        value = placeQuery,
                        onValueChange = { placeQuery = it; onSearchPlace(it) },
                        label = { Text("Optional Location (e.g., Delhi, Office)") }
                    )
                    if (placeQuery.length >= 2 && searchResults.isNotEmpty() && selectedPlace == null) {
                        searchResults.take(3).forEach { p ->
                            TextButton(onClick = { selectedPlace = p; placeQuery = "${p.name}, ${p.country}" }) {
                                Text("Select: ${p.name}, ${p.country}")
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Alert Ringtone", style = MaterialTheme.typography.labelLarge)
                            Text("♪ $soundName", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = {
                            if (isPreviewing) {
                                onStopPreview()
                                isPreviewing = false
                            } else {
                                onPreviewRingtone("rem_dialog_preview", soundName, soundUri)
                                isPreviewing = true
                            }
                        }) {
                            Icon(
                                imageVector = if (isPreviewing) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPreviewing) "Stop preview" else "Preview ringtone",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { pickSongLauncher.launch(arrayOf("audio/*")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.AudioFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Pick Any Song as Ringtone")
                    }
                    if (savedRingtones.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = soundUri == null && soundName == "Default",
                                onClick = { soundName = "Default"; soundUri = null },
                                label = { Text("Default") }
                            )
                            savedRingtones.forEach { song ->
                                FilterChip(
                                    selected = soundUri == song.uriString,
                                    onClick = { soundName = song.name; soundUri = song.uriString },
                                    label = { Text("♪ ${song.name}") }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(enabled = title.isNotBlank(), onClick = {
                    onStopPreview()
                    val mins = (minsFromNow.toIntOrNull() ?: 30).coerceAtLeast(1)
                    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    if (selectedPlace != null && !granted) {
                        locPermLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                    onSaveReminder(
                        ReminderEntity(
                            title = title.trim(),
                            triggerAtMillis = System.currentTimeMillis() + mins * 60_000L,
                            locationName = selectedPlace?.name ?: placeQuery.takeIf { it.isNotBlank() },
                            latitude = selectedPlace?.latitude,
                            longitude = selectedPlace?.longitude,
                            soundName = soundName,
                            soundUri = soundUri
                        )
                    )
                    showAdd = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = {
                    onStopPreview()
                    showAdd = false
                }) { Text("Cancel") }
            }
        )
    }
}
