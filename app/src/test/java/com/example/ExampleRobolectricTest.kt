package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.service.AssistantState
import com.example.service.VoiceStateManager
import com.example.tools.AppControlManager
import com.example.tools.ToolRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34]) // Set to stable SDK 34 for high reliability in test environments
class ExampleRobolectricTest {

    @Test
    fun testAppNameIsNova() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("NOVA", appName)
    }

    @Test
    fun testVoiceStateManagerTransitions() {
        // Initial state is IDLE
        assertEquals(AssistantState.IDLE, VoiceStateManager.state.value)

        // Set to LISTENING
        VoiceStateManager.updateState(AssistantState.LISTENING)
        assertEquals(AssistantState.LISTENING, VoiceStateManager.state.value)

        // Set Muted and verify state changes to MUTED
        VoiceStateManager.setMuted(true)
        assertTrue(VoiceStateManager.isMuted.value)
        assertEquals(AssistantState.MUTED, VoiceStateManager.state.value)

        // Unmute and check transition back
        VoiceStateManager.setMuted(false)
        VoiceStateManager.updateState(AssistantState.SPEAKING)
        assertEquals(AssistantState.SPEAKING, VoiceStateManager.state.value)
    }

    @Test
    fun testVoiceStateManagerPromptAndResponse() {
        VoiceStateManager.setPrompt("Open YouTube")
        assertEquals("Open YouTube", VoiceStateManager.currentPrompt.value)

        VoiceStateManager.setResponse("Opening YouTube app.")
        assertEquals("Opening YouTube app.", VoiceStateManager.currentResponse.value)
    }

    @Test
    fun testToolRegistryDeclarations() {
        val tools = ToolRegistry.toolsList
        assertNotNull(tools)
        assertTrue(tools.isNotEmpty())

        val declarations = tools.first().functionDeclarations
        val names = declarations.map { it.name }

        assertTrue(names.contains("open_app"))
        assertTrue(names.contains("search_app"))
        assertTrue(names.contains("create_note"))
        assertTrue(names.contains("create_reminder"))
        assertTrue(names.contains("call_contact"))
    }

    @Test
    fun testAppControlManagerSettings() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appControlManager = AppControlManager(context)

        // Test settings launcher
        val settingsResult = appControlManager.openSettings()
        assertEquals("Opened Android system settings.", settingsResult)

        // Test general web search trigger response
        val searchResult = appControlManager.searchWeb("test query")
        assertTrue(searchResult.contains("Searching the web for \"test query\"."))
    }
}
