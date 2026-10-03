package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.database.AppDatabase
import com.example.data.repository.AssistantRepository
import com.example.tools.AppControlManager
import com.example.tools.ContactManager
import com.example.tools.ToolExecutionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Locale

class BackgroundAudioService : Service(), RecognitionListener, TextToSpeech.OnInitListener {

    private val tag = "BackgroundAudioService"
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    private lateinit var appDatabase: AppDatabase
    private lateinit var assistantRepository: AssistantRepository

    companion object {
        private const val NOTIFICATION_ID = 1111
        private const val CHANNEL_ID = "nova_assistant_channel"

        const val ACTION_START_VOICE = "com.example.service.ACTION_START_VOICE"
        const val ACTION_STOP_VOICE = "com.example.service.ACTION_STOP_VOICE"
        const val ACTION_MUTE_TOGGLE = "com.example.service.ACTION_MUTE_TOGGLE"
        const val ACTION_SLEEP_TOGGLE = "com.example.service.ACTION_SLEEP_TOGGLE"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(tag, "BackgroundAudioService onCreate")

        // Initialize Local Storage & Business Repositories
        appDatabase = AppDatabase.getDatabase(applicationContext)
        val appControlManager = AppControlManager(applicationContext)
        val contactManager = ContactManager(applicationContext)
        val toolExecutionEngine = ToolExecutionEngine(
            applicationContext,
            appControlManager,
            contactManager,
            appDatabase.noteDao(),
            appDatabase.reminderDao()
        )
        assistantRepository = AssistantRepository(
            appDatabase.conversationLogDao(),
            toolExecutionEngine
        )

        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                buildNotification(),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }

        // Initialize Speech Engines on Main Thread
        initializeSpeechRecognizer()
        textToSpeech = TextToSpeech(this, this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(tag, "onStartCommand with action: $action")

        when (action) {
            ACTION_START_VOICE -> {
                VoiceStateManager.setMuted(false)
                VoiceStateManager.updateState(AssistantState.IDLE)
                triggerVoiceListening()
            }
            ACTION_STOP_VOICE -> {
                stopSelf()
            }
            ACTION_MUTE_TOGGLE -> {
                val isMuted = VoiceStateManager.isMuted.value
                VoiceStateManager.setMuted(!isMuted)
                if (!isMuted) {
                    speechRecognizer?.stopListening()
                } else {
                    triggerVoiceListening()
                }
                updateNotification()
            }
            ACTION_SLEEP_TOGGLE -> {
                val currState = VoiceStateManager.state.value
                if (currState == AssistantState.SLEEPING) {
                    VoiceStateManager.updateState(AssistantState.IDLE)
                    speakOut("Nova is awake. How can I help you?")
                } else {
                    VoiceStateManager.updateState(AssistantState.SLEEPING)
                    speakOut("Going to sleep now.")
                }
                updateNotification()
            }
        }
        return START_STICKY
    }

    private fun initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                setRecognitionListener(this@BackgroundAudioService)
            }
        } else {
            Log.e(tag, "Speech recognizer is not available on this device.")
            VoiceStateManager.updateState(AssistantState.ERROR)
        }
    }

    private fun triggerVoiceListening() {
        if (VoiceStateManager.isMuted.value) {
            VoiceStateManager.updateState(AssistantState.MUTED)
            return
        }

        val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        serviceScope.launch {
            try {
                speechRecognizer?.startListening(speechIntent)
                Log.d(tag, "SpeechRecognizer started listening")
            } catch (e: Exception) {
                Log.e(tag, "Failed to start SpeechRecognizer", e)
                VoiceStateManager.updateState(AssistantState.ERROR)
            }
        }
    }

    private fun speakOut(text: String) {
        if (!isTtsInitialized || textToSpeech == null) return
        Log.d(tag, "Speaking out: $text")
        VoiceStateManager.setResponse(text)
        VoiceStateManager.updateState(AssistantState.SPEAKING)

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "nova_voice_response")
        }
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "nova_voice_response")
    }

    // --- TextToSpeech OnInit ---
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.let { tts ->
                val result = tts.setLanguage(Locale.getDefault())
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.US)
                }
                isTtsInitialized = true

                // Register Utterance Callback to automatically resume listening when Nova stops speaking
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        VoiceStateManager.updateState(AssistantState.SPEAKING)
                    }

                    override fun onDone(utteranceId: String?) {
                        // Re-trigger listening automatically
                        serviceScope.launch {
                            val state = VoiceStateManager.state.value
                            if (state != AssistantState.SLEEPING && state != AssistantState.MUTED) {
                                VoiceStateManager.updateState(AssistantState.LISTENING)
                                triggerVoiceListening()
                            } else if (state == AssistantState.SLEEPING) {
                                triggerVoiceListening() // Continue listening for wake word
                            }
                        }
                    }

                    override fun onError(utteranceId: String?) {
                        VoiceStateManager.updateState(AssistantState.ERROR)
                    }
                })
            }
        } else {
            Log.e(tag, "Failed to initialize TextToSpeech")
        }
    }

    // --- SpeechRecognizer Listener Callbacks ---
    override fun onReadyForSpeech(params: Bundle?) {
        Log.d(tag, "onReadyForSpeech")
        val state = VoiceStateManager.state.value
        if (state != AssistantState.SLEEPING && state != AssistantState.MUTED) {
            VoiceStateManager.updateState(AssistantState.LISTENING)
        }
    }

    override fun onBeginningOfSpeech() {
        Log.d(tag, "onBeginningOfSpeech")
        // Barge-in (Interruption) Support: If Nova is currently speaking and user begins speaking, stop NOVA immediately!
        if (textToSpeech?.isSpeaking == true) {
            Log.d(tag, "Interrupted Nova while speaking!")
            textToSpeech?.stop()
            VoiceStateManager.updateState(AssistantState.INTERRUPTED)
        }
    }

    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        Log.d(tag, "onEndOfSpeech")
        val state = VoiceStateManager.state.value
        if (state != AssistantState.SLEEPING && state != AssistantState.MUTED) {
            VoiceStateManager.updateState(AssistantState.THINKING)
        }
    }

    override fun onError(error: Int) {
        Log.d(tag, "SpeechRecognizer Error: $error")
        // Speech recognition timeouts or errors are common when silent.
        // We gracefully restart the recognizer to keep continuous loop.
        serviceScope.launch {
            val state = VoiceStateManager.state.value
            if (state != AssistantState.MUTED) {
                // Wait briefly and restart
                kotlinx.coroutines.delay(800)
                triggerVoiceListening()
            }
        }
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull() ?: return
        Log.d(tag, "SpeechRecognizer onResults: $text")

        val state = VoiceStateManager.state.value

        if (state == AssistantState.SLEEPING) {
            // Check for Wake Word: "Hey Nova" or "Nova"
            val phrase = text.lowercase()
            if (phrase.contains("hey nova") || phrase.contains("nova") || phrase.contains("wake up")) {
                VoiceStateManager.updateState(AssistantState.IDLE)
                speakOut("Yes? I am here.")
            } else {
                // Continue sleeping, restart listening
                triggerVoiceListening()
            }
            return
        }

        // Active conversation turn
        VoiceStateManager.setPrompt(text)
        VoiceStateManager.updateState(AssistantState.THINKING)

        // Process request using Gemini API
        serviceScope.launch {
            val responseText = assistantRepository.getConversationalResponse(text)
            
            // Check if user requested to sleep or close
            val normalizedResponse = text.lowercase().trim()
            if (normalizedResponse.contains("go to sleep") || normalizedResponse.contains("bye nova") || normalizedResponse.contains("sleep nova")) {
                VoiceStateManager.updateState(AssistantState.SLEEPING)
                speakOut("Okay, going to sleep. Say 'Hey Nova' to wake me up.")
            } else {
                speakOut(responseText)
            }
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val partial = matches?.firstOrNull() ?: return
        Log.d(tag, "SpeechRecognizer onPartialResults: $partial")
        VoiceStateManager.setPrompt(partial)
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    // --- Foreground Service Notification ---
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "NOVA Voice Assistant"
            val desc = "Background voice activity service for NOVA Assistant"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = desc
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Actions mapping
        val muteIntent = Intent(this, BackgroundAudioService::class.java).apply { action = ACTION_MUTE_TOGGLE }
        val pendingMute = PendingIntent.getService(this, 1, muteIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val sleepIntent = Intent(this, BackgroundAudioService::class.java).apply { action = ACTION_SLEEP_TOGGLE }
        val pendingSleep = PendingIntent.getService(this, 2, sleepIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val isMuted = VoiceStateManager.isMuted.value
        val state = VoiceStateManager.state.value

        val muteLabel = if (isMuted) "Unmute" else "Mute"
        val sleepLabel = if (state == AssistantState.SLEEPING) "Wake Up" else "Sleep"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NOVA AI Assistant Active")
            .setContentText("Listening and ready for voice commands.")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_pause, muteLabel, pendingMute)
            .addAction(android.R.drawable.ic_menu_compass, sleepLabel, pendingSleep)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(tag, "BackgroundAudioService onDestroy")
        serviceScope.cancel()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        speechRecognizer?.destroy()
        super.onDestroy()
    }
}
