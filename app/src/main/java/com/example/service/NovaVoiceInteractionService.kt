package com.example.service

import android.content.Intent
import android.service.voice.VoiceInteractionService
import android.util.Log

class NovaVoiceInteractionService : VoiceInteractionService() {
    private val tag = "NovaVoiceInteraction"

    override fun onCreate() {
        super.onCreate()
        Log.d(tag, "NovaVoiceInteractionService onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(tag, "NovaVoiceInteractionService onStartCommand")
        
        // Launch main activity to handle direct user voice assistant activation
        val mainIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("voice_interaction_trigger", true)
        }
        if (mainIntent != null) {
            startActivity(mainIntent)
        }
        return super.onStartCommand(intent, flags, startId)
    }
}
