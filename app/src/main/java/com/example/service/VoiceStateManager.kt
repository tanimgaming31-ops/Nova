package com.example.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object VoiceStateManager {
    private val _state = MutableStateFlow(AssistantState.IDLE)
    val state: StateFlow<AssistantState> = _state.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _currentPrompt = MutableStateFlow("")
    val currentPrompt: StateFlow<String> = _currentPrompt.asStateFlow()

    private val _currentResponse = MutableStateFlow("")
    val currentResponse: StateFlow<String> = _currentResponse.asStateFlow()

    fun updateState(newState: AssistantState) {
        if (_isMuted.value && newState == AssistantState.LISTENING) {
            _state.value = AssistantState.MUTED
        } else {
            _state.value = newState
        }
    }

    fun setMuted(muted: Boolean) {
        _isMuted.value = muted
        if (muted) {
            _state.value = AssistantState.MUTED
        } else {
            _state.value = AssistantState.IDLE
        }
    }

    fun setPrompt(prompt: String) {
        _currentPrompt.value = prompt
    }

    fun setResponse(response: String) {
        _currentResponse.value = response
    }
}
