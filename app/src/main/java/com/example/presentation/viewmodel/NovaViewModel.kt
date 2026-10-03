package com.example.presentation.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.NoteEntity
import com.example.data.database.ReminderEntity
import com.example.data.database.ConversationLogEntity
import com.example.service.AssistantState
import com.example.service.BackgroundAudioService
import com.example.service.VoiceStateManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NovaViewModel(private val database: AppDatabase) : ViewModel() {

    val currentState: StateFlow<AssistantState> = VoiceStateManager.state
    val isMuted: StateFlow<Boolean> = VoiceStateManager.isMuted
    val prompt: StateFlow<String> = VoiceStateManager.currentPrompt
    val response: StateFlow<String> = VoiceStateManager.currentResponse

    val notes: StateFlow<List<NoteEntity>> = database.noteDao().getAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reminders: StateFlow<List<ReminderEntity>> = database.reminderDao().getAllReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val conversationHistory: StateFlow<List<ConversationLogEntity>> = database.conversationLogDao().getFullChatHistoryFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun startAssistantService(context: Context) {
        val hasMicPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasMicPermission) {
            android.util.Log.w("NovaViewModel", "Cannot start assistant service: RECORD_AUDIO permission not granted yet.")
            return
        }

        val intent = Intent(context, BackgroundAudioService::class.java).apply {
            action = BackgroundAudioService.ACTION_START_VOICE
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun toggleMute(context: Context) {
        val intent = Intent(context, BackgroundAudioService::class.java).apply {
            action = BackgroundAudioService.ACTION_MUTE_TOGGLE
        }
        context.startService(intent)
    }

    fun toggleSleep(context: Context) {
        val intent = Intent(context, BackgroundAudioService::class.java).apply {
            action = BackgroundAudioService.ACTION_SLEEP_TOGGLE
        }
        context.startService(intent)
    }

    fun stopAssistantService(context: Context) {
        val intent = Intent(context, BackgroundAudioService::class.java).apply {
            action = BackgroundAudioService.ACTION_STOP_VOICE
        }
        context.startService(intent)
    }

    fun clearHistory() {
        viewModelScope.launch {
            database.conversationLogDao().clearHistory()
            VoiceStateManager.setPrompt("")
            VoiceStateManager.setResponse("")
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            database.noteDao().deleteNote(note)
        }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            database.reminderDao().deleteReminder(reminder)
        }
    }

    fun updateReminderStatus(id: Int, isCompleted: Boolean) {
        viewModelScope.launch {
            database.reminderDao().updateReminderStatus(id, isCompleted)
        }
    }

    fun addNoteManually(title: String, content: String) {
        viewModelScope.launch {
            database.noteDao().insertNote(NoteEntity(title = title, content = content))
        }
    }

    fun addReminderManually(text: String, time: String) {
        viewModelScope.launch {
            database.reminderDao().insertReminder(ReminderEntity(text = text, time = time))
        }
    }

    // Factory pattern to inject database
    class Factory(private val database: AppDatabase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(NovaViewModel::class.java)) {
                return NovaViewModel(database) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
