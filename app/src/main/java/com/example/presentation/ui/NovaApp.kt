package com.example.presentation.ui

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.NoteEntity
import com.example.data.database.ReminderEntity
import com.example.data.database.ConversationLogEntity
import com.example.presentation.viewmodel.NovaViewModel
import com.example.service.AssistantState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Futuristic Dark Palette
val DeepSpace = Color(0xFF0A0D14)
val DarkNebula = Color(0xFF121622)
val NeonCyan = Color(0xFF00FFF0)
val NeonPurple = Color(0xFFBD00FF)
val NeonPink = Color(0xFFFF007A)
val SolidGreen = Color(0xFF00FF66)
val SoftGray = Color(0xFF9EA3B0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovaApp(viewModel: NovaViewModel) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }

    val currentState by viewModel.currentState.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val prompt by viewModel.prompt.collectAsState()
    val response by viewModel.response.collectAsState()

    val notes by viewModel.notes.collectAsState()
    val reminders by viewModel.reminders.collectAsState()
    val conversationHistory by viewModel.conversationHistory.collectAsState()

    // Start assistant service automatically if idle
    LaunchedEffect(Unit) {
        viewModel.startAssistantService(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "NOVA AI",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 20.sp,
                            modifier = Modifier.testTag("app_title")
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when (currentState) {
                                AssistantState.LISTENING -> NeonCyan.copy(alpha = 0.15f)
                                AssistantState.THINKING -> NeonPurple.copy(alpha = 0.15f)
                                AssistantState.SPEAKING -> NeonPink.copy(alpha = 0.15f)
                                AssistantState.SLEEPING -> SoftGray.copy(alpha = 0.15f)
                                AssistantState.MUTED -> Color.Red.copy(alpha = 0.15f)
                                else -> Color.White.copy(alpha = 0.1f)
                            },
                            border = BorderStroke(
                                1.dp,
                                when (currentState) {
                                    AssistantState.LISTENING -> NeonCyan
                                    AssistantState.THINKING -> NeonPurple
                                    AssistantState.SPEAKING -> NeonPink
                                    AssistantState.SLEEPING -> SoftGray
                                    AssistantState.MUTED -> Color.Red
                                    else -> Color.Gray
                                }
                            )
                        ) {
                            Text(
                                currentState.name,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepSpace)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkNebula,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Mic, contentDescription = "Assistant") },
                    label = { Text("Assistant") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonCyan,
                        unselectedIconColor = SoftGray,
                        selectedTextColor = NeonCyan,
                        indicatorColor = NeonCyan.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Notes, contentDescription = "Data Lists") },
                    label = { Text("Notes") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonCyan,
                        unselectedIconColor = SoftGray,
                        selectedTextColor = NeonCyan,
                        indicatorColor = NeonCyan.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Control Center") },
                    label = { Text("Control") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonCyan,
                        unselectedIconColor = SoftGray,
                        selectedTextColor = NeonCyan,
                        indicatorColor = NeonCyan.copy(alpha = 0.1f)
                    )
                )
            }
        },
        containerColor = DeepSpace
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                0 -> AssistantTab(
                    currentState = currentState,
                    isMuted = isMuted,
                    prompt = prompt,
                    response = response,
                    conversationHistory = conversationHistory,
                    onToggleMute = { viewModel.toggleMute(context) },
                    onToggleSleep = { viewModel.toggleSleep(context) },
                    onClearHistory = { viewModel.clearHistory() }
                )
                1 -> NotesTab(
                    notes = notes,
                    reminders = reminders,
                    onDeleteNote = { viewModel.deleteNote(it) },
                    onDeleteReminder = { viewModel.deleteReminder(it) },
                    onToggleReminder = { id, done -> viewModel.updateReminderStatus(id, done) },
                    onAddNote = { title, content -> viewModel.addNoteManually(title, content) },
                    onAddReminder = { text, time -> viewModel.addReminderManually(text, time) }
                )
                2 -> ControlCenterTab(
                    currentState = currentState,
                    isMuted = isMuted,
                    onToggleMute = { viewModel.toggleMute(context) },
                    onToggleSleep = { viewModel.toggleSleep(context) },
                    onStartService = { viewModel.startAssistantService(context) },
                    onStopService = { viewModel.stopAssistantService(context) }
                )
            }
        }
    }
}

// --- TAB 1: CENTRAL NOVA ASSISTANT TAB ---
@Composable
fun AssistantTab(
    currentState: AssistantState,
    isMuted: Boolean,
    prompt: String,
    response: String,
    conversationHistory: List<ConversationLogEntity>,
    onToggleMute: () -> Unit,
    onToggleSleep: () -> Unit,
    onClearHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Developer Credit Watermark
        Text(
            "Made by Tanim • Developed by Tanim",
            color = NeonCyan.copy(alpha = 0.4f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Animated AI Orb Core
        Box(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            NovaOrb(state = currentState)
        }

        // Live Dialogue / Transcription Feed Panel
        Card(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkNebula),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Live Transcript",
                        color = NeonCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(
                        onClick = onClearHistory,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = "Clear Chat history",
                            tint = SoftGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = Color.White.copy(alpha = 0.08f)
                )

                if (conversationHistory.isEmpty() && prompt.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Say \"Hey Nova\" to start a continuous hands-free voice dialogue.",
                            color = SoftGray,
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        reverseLayout = true
                    ) {
                        // Show active ongoing interaction first at the bottom of standard list
                        if (response.isNotEmpty()) {
                            item {
                                ChatBubble(sender = "nova", text = response)
                            }
                        }
                        if (prompt.isNotEmpty()) {
                            item {
                                ChatBubble(sender = "user", text = prompt)
                            }
                        }
                        // Sort by newest first because list is reversed
                        items(conversationHistory.reversed()) { log ->
                            ChatBubble(sender = log.sender, text = log.text)
                        }
                    }
                }
            }
        }

        // Control HUD Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleMute,
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        if (isMuted) Color.Red.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                        CircleShape
                    )
                    .border(
                        1.dp,
                        if (isMuted) Color.Red else Color.White.copy(alpha = 0.1f),
                        CircleShape
                    )
                    .testTag("mute_button")
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Mute / Unmute Toggle",
                    tint = if (isMuted) Color.Red else Color.White
                )
            }

            Surface(
                onClick = onToggleSleep,
                shape = RoundedCornerShape(24.dp),
                color = if (currentState == AssistantState.SLEEPING) SoftGray.copy(alpha = 0.2f) else NeonCyan.copy(alpha = 0.1f),
                border = BorderStroke(
                    1.dp,
                    if (currentState == AssistantState.SLEEPING) SoftGray else NeonCyan
                ),
                modifier = Modifier.height(48.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (currentState == AssistantState.SLEEPING) Icons.Default.NightlightRound else Icons.Default.Sensors,
                        contentDescription = "Status trigger",
                        tint = if (currentState == AssistantState.SLEEPING) SoftGray else NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (currentState == AssistantState.SLEEPING) "AWAKEN NOVA" else "SLEEP NOVA",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Beautiful Custom Chat Bubbles
@Composable
fun ChatBubble(sender: String, text: String) {
    val isUser = sender == "user"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Text(
                text = if (isUser) "YOU" else "NOVA",
                fontSize = 9.sp,
                color = if (isUser) NeonCyan else NeonPurple,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
            Surface(
                color = if (isUser) NeonCyan.copy(alpha = 0.12f) else NeonPurple.copy(alpha = 0.12f),
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                border = BorderStroke(
                    1.dp,
                    if (isUser) NeonCyan.copy(alpha = 0.3f) else NeonPurple.copy(alpha = 0.3f)
                )
            ) {
                Text(
                    text = text,
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

// --- DETAILED ANIMATED GRADIENT PULSING NOVA ORB ---
@Composable
fun NovaOrb(state: AssistantState) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")

    // Breathing pulse scale animation
    val scaleFactor by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AssistantState.LISTENING -> 800
                    AssistantState.THINKING -> 500
                    AssistantState.SPEAKING -> 700
                    AssistantState.SLEEPING -> 2000
                    else -> 1500
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Outer glow rotating rotation degrees
    val rotationDegrees by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AssistantState.THINKING -> 1500
                    AssistantState.SPEAKING -> 2500
                    else -> 5000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val colorStart = when (state) {
        AssistantState.LISTENING -> NeonCyan
        AssistantState.THINKING -> NeonPurple
        AssistantState.SPEAKING -> NeonPink
        AssistantState.SLEEPING -> Color(0xFF0F2625)
        AssistantState.ERROR -> Color.Red
        AssistantState.MUTED -> Color.Red
        else -> NeonCyan.copy(alpha = 0.6f)
    }

    val colorEnd = when (state) {
        AssistantState.LISTENING -> NeonPurple
        AssistantState.THINKING -> NeonPink
        AssistantState.SPEAKING -> NeonCyan
        AssistantState.SLEEPING -> Color(0xFF071413)
        AssistantState.ERROR -> Color(0xFF550000)
        AssistantState.MUTED -> Color(0xFF550000)
        else -> NeonPurple.copy(alpha = 0.6f)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(240.dp)
    ) {
        // Outer pulsing ring 2
        Box(
            modifier = Modifier
                .size(220.dp)
                .scale(scaleFactor * 1.15f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(colorStart.copy(alpha = 0.12f), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        // Outer pulsing ring 1
        Box(
            modifier = Modifier
                .size(170.dp)
                .scale(scaleFactor)
                .background(
                    Brush.radialGradient(
                        colors = listOf(colorStart.copy(alpha = 0.25f), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        // Main Animated Gradient Core Orb
        Box(
            modifier = Modifier
                .size(110.dp)
                .scale(scaleFactor)
                .background(
                    Brush.linearGradient(
                        colors = listOf(colorStart, colorEnd)
                    ),
                    CircleShape
                )
                .border(2.dp, Color.White.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (state) {
                    AssistantState.LISTENING -> Icons.Default.KeyboardVoice
                    AssistantState.THINKING -> Icons.Default.HourglassEmpty
                    AssistantState.SPEAKING -> Icons.Default.VolumeUp
                    AssistantState.SLEEPING -> Icons.Default.PowerSettingsNew
                    AssistantState.MUTED -> Icons.Default.MicOff
                    else -> Icons.Default.GraphicEq
                },
                contentDescription = "Orb Status Icon",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

// --- TAB 2: DATA LISTS FOR SAVED NOTES & REMINDERS ---
@Composable
fun NotesTab(
    notes: List<NoteEntity>,
    reminders: List<ReminderEntity>,
    onDeleteNote: (NoteEntity) -> Unit,
    onDeleteReminder: (ReminderEntity) -> Unit,
    onToggleReminder: (id: Int, done: Boolean) -> Unit,
    onAddNote: (String, String) -> Unit,
    onAddReminder: (String, String) -> Unit
) {
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var showAddReminderDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Sub-sections header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "SAVED VOICE DATA",
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                fontSize = 14.sp
            )
            Row {
                Button(
                    onClick = { showAddNoteDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Note", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Note", fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    onClick = { showAddReminderDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Reminder", modifier = Modifier.size(16.dp), tint = Color.Black)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reminder", fontSize = 11.sp, color = Color.Black)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // Reminders Section
            item {
                Text(
                    "Reminders",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            if (reminders.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkNebula)
                    ) {
                        Text(
                            "No active reminders. Ask \"Remind me at 8 PM to call Mom\" to add one.",
                            color = SoftGray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(reminders) { reminder ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("reminder_item_${reminder.id}"),
                        colors = CardDefaults.cardColors(containerColor = DarkNebula)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Checkbox(
                                    checked = reminder.isCompleted,
                                    onCheckedChange = { onToggleReminder(reminder.id, it) },
                                    colors = CheckboxDefaults.colors(checkedColor = NeonCyan)
                                )
                                Column {
                                    Text(
                                        reminder.text,
                                        color = if (reminder.isCompleted) SoftGray else Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AccessTime, "Time", tint = NeonCyan, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(reminder.time, color = NeonCyan, fontSize = 11.sp)
                                    }
                                }
                            }
                            IconButton(onClick = { onDeleteReminder(reminder) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Reminder", tint = Color.Red.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }

            // Notes Section
            item {
                Text(
                    "Notes",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }

            if (notes.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkNebula)
                    ) {
                        Text(
                            "No saved notes. Ask \"Create note containing meeting schedule\" to add one.",
                            color = SoftGray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(notes) { note ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("note_item_${note.id}"),
                        colors = CardDefaults.cardColors(containerColor = DarkNebula)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    note.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { onDeleteNote(note) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete Note",
                                        tint = Color.Red.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                note.content,
                                color = SoftGray,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val dateStr = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(Date(note.timestamp))
                            Text(
                                dateStr,
                                color = SoftGray.copy(alpha = 0.5f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Note Manually Dialog
    if (showAddNoteDialog) {
        var noteTitle by remember { mutableStateOf("") }
        var noteContent by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            title = { Text("Add Note", color = Color.White) },
            text = {
                Column {
                    OutlinedTextField(
                        value = noteTitle,
                        onValueChange = { noteTitle = it },
                        label = { Text("Title") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonPurple,
                            unfocusedLabelColor = SoftGray,
                            focusedLabelColor = NeonPurple,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = noteContent,
                        onValueChange = { noteContent = it },
                        label = { Text("Content") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonPurple,
                            unfocusedLabelColor = SoftGray,
                            focusedLabelColor = NeonPurple,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (noteTitle.isNotBlank()) {
                        onAddNote(noteTitle, noteContent)
                        showAddNoteDialog = false
                    }
                }) {
                    Text("Save", color = NeonPurple)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddNoteDialog = false }) {
                    Text("Cancel", color = SoftGray)
                }
            },
            containerColor = DarkNebula
        )
    }

    // Add Reminder Manually Dialog
    if (showAddReminderDialog) {
        var remText by remember { mutableStateOf("") }
        var remTime by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddReminderDialog = false },
            title = { Text("Add Reminder", color = Color.White) },
            text = {
                Column {
                    OutlinedTextField(
                        value = remText,
                        onValueChange = { remText = it },
                        label = { Text("Reminder Task") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedLabelColor = SoftGray,
                            focusedLabelColor = NeonCyan,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = remTime,
                        onValueChange = { remTime = it },
                        label = { Text("Time (e.g. 8 PM)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedLabelColor = SoftGray,
                            focusedLabelColor = NeonCyan,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (remText.isNotBlank()) {
                        onAddReminder(remText, if (remTime.isNotBlank()) remTime else "soon")
                        showAddReminderDialog = false
                    }
                }) {
                    Text("Save", color = NeonCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddReminderDialog = false }) {
                    Text("Cancel", color = SoftGray)
                }
            },
            containerColor = DarkNebula
        )
    }
}

// --- TAB 3: CONTROL CENTER TAB ---
@Composable
fun ControlCenterTab(
    currentState: AssistantState,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onToggleSleep: () -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                "SYSTEM CONTROL CENTER",
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Developer Info Panel
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = DarkNebula),
                border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "NOVA Voice Assistant",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Text(
                        "Version 1.0 (Release-Production Ready)",
                        color = SoftGray,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Developer Credit:",
                        fontWeight = FontWeight.SemiBold,
                        color = NeonCyan,
                        fontSize = 13.sp
                    )
                    Text(
                        "Developed by Tanim",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Made by Tanim",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Commands List Guide Panel
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = DarkNebula)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Continuous Voice Commands Guide",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val commandsList = listOf(
                        "\"Hey Nova\" -> Triggers voice wake",
                        "\"Open YouTube\" -> Opens YouTube app",
                        "\"Search YouTube for Free Fire\" -> Searches inside YT",
                        "\"Call Tanim\" -> Queries Contacts and calls phone",
                        "\"Create note shopping list\" -> Persists note to database",
                        "\"Remind me at 8 PM to read\" -> Saves reminder in list",
                        "\"Go to sleep\" -> Safe low-battery standby mode"
                    )
                    commandsList.forEach { cmd ->
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, "bullet", tint = NeonCyan, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(cmd, color = SoftGray, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Action controls
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onToggleMute,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMuted) Color.Red else Color.White.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(if (isMuted) Icons.Default.MicOff else Icons.Default.Mic, "Mute")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isMuted) "Unmute Mic" else "Mute Mic")
                }

                Button(
                    onClick = onToggleSleep,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentState == AssistantState.SLEEPING) SoftGray else NeonPurple
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(if (currentState == AssistantState.SLEEPING) Icons.Default.WbSunny else Icons.Default.Nightlight, "Sleep")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (currentState == AssistantState.SLEEPING) "Wake Up" else "Sleep Mode")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStartService,
                    colors = ButtonDefaults.buttonColors(containerColor = SolidGreen),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, "Start Service", tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Start Service", color = Color.Black)
                }

                Button(
                    onClick = onStopService,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Stop, "Stop Service")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Stop Service")
                }
            }
        }
    }
}
