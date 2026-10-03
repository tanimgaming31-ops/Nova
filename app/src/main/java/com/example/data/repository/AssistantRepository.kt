package com.example.data.repository

import android.util.Log
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.GenerateContentResponse
import com.example.data.api.Part
import com.example.data.api.FunctionResponse
import com.example.data.api.RetrofitClient
import com.example.data.database.ConversationLogDao
import com.example.data.database.ConversationLogEntity
import com.example.tools.ToolExecutionEngine
import com.example.tools.ToolRegistry
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AssistantRepository(
    private val conversationLogDao: ConversationLogDao,
    private val toolExecutionEngine: ToolExecutionEngine
) {
    private val tag = "AssistantRepository"

    // System instruction to give NOVA its witty, confident, playful, and intelligent personality.
    private val systemInstruction = Content(
        parts = listOf(
            Part(
                text = "You are NOVA, a premium, futuristic, highly intelligent, witty, and playful voice assistant. " +
                        "You are confident, friendly, and slightly teasing when appropriate. " +
                        "Your normal voice responses MUST be very concise, natural, and conversational (e.g. \"Sure, opening YouTube\"). " +
                        "Avoid being robotic, and never output excessive repetitive phrases. " +
                        "Do not make romantic or sexual remarks. " +
                        "Your developer is Tanim. You were developed by Tanim and made by Tanim. " +
                        "Do not replace the name Tanim with any other name. " +
                        "If the user asks you to perform an action, check if you have a matching tool available in your function list and call it."
            )
        )
    )

    suspend fun getConversationalResponse(userInput: String): String = withContext(Dispatchers.IO) {
        // 1. Persist the user's input to database log
        conversationLogDao.insertLog(ConversationLogEntity(sender = "user", text = userInput))

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            val err = "API Key is missing or invalid. Please configure GEMINI_API_KEY in the Secrets panel."
            conversationLogDao.insertLog(ConversationLogEntity(sender = "nova", text = err))
            return@withContext err
        }

        try {
            // 2. Build the message history
            val rawLogs = conversationLogDao.getFullChatHistory()
            val chatContents = mutableListOf<Content>()

            // We only keep the last 12 turns for context efficiency
            val logsToProcess = if (rawLogs.size > 12) rawLogs.takeLast(12) else rawLogs

            for (log in logsToProcess) {
                chatContents.add(
                    Content(
                        parts = listOf(Part(text = log.text))
                    )
                )
            }

            // 3. Make the initial request to Gemini
            val initialRequest = GenerateContentRequest(
                contents = chatContents,
                tools = ToolRegistry.toolsList,
                systemInstruction = systemInstruction
            )

            val initialResponse = RetrofitClient.service.generateContent(apiKey, initialRequest)
            val candidate = initialResponse.candidates?.firstOrNull()
            val firstPart = candidate?.content?.parts?.firstOrNull()

            // 4. Check if Gemini returned a function call request
            if (firstPart?.functionCall != null) {
                val functionCall = firstPart.functionCall
                val toolName = functionCall.name
                val toolArgs = functionCall.args
                Log.d(tag, "Gemini requested tool call: $toolName with args $toolArgs")

                // Execute the tool locally
                val toolExecutionResult = toolExecutionEngine.executeTool(toolName, toolArgs)
                Log.d(tag, "Tool execution result: $toolExecutionResult")

                // Add the function call request and response back to the dialog history
                val updatedContents = chatContents.toMutableList()
                updatedContents.add(Content(parts = listOf(firstPart)))
                updatedContents.add(
                    Content(
                        parts = listOf(
                            Part(
                                functionResponse = FunctionResponse(
                                    name = toolName,
                                    response = mapOf("result" to toolExecutionResult)
                                )
                            )
                        )
                    )
                )

                // Call Gemini again with the tool result so it can generate a natural voice summary
                val followUpRequest = GenerateContentRequest(
                    contents = updatedContents,
                    tools = ToolRegistry.toolsList,
                    systemInstruction = systemInstruction
                )

                val followUpResponse = RetrofitClient.service.generateContent(apiKey, followUpRequest)
                val finalAnswer = followUpResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "Action executed. $toolExecutionResult"

                conversationLogDao.insertLog(ConversationLogEntity(sender = "nova", text = finalAnswer))
                return@withContext finalAnswer
            } else {
                // Return direct text response
                val textResponse = firstPart?.text ?: "I'm not sure how to respond to that."
                conversationLogDao.insertLog(ConversationLogEntity(sender = "nova", text = textResponse))
                return@withContext textResponse
            }

        } catch (e: Exception) {
            Log.e(tag, "Gemini network/parsing exception", e)
            val errMsg = "I'm having trouble connecting right now."
            conversationLogDao.insertLog(ConversationLogEntity(sender = "nova", text = errMsg))
            return@withContext errMsg
        }
    }

    suspend fun clearHistory() {
        conversationLogDao.clearHistory()
    }
}
