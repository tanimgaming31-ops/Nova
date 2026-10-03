package com.example.tools

import com.example.data.api.FunctionDeclaration
import com.example.data.api.Parameters
import com.example.data.api.Property
import com.example.data.api.Tool

object ToolRegistry {

    val toolsList = listOf(
        Tool(
            functionDeclarations = listOf(
                FunctionDeclaration(
                    name = "open_app",
                    description = "Opens a specified application (e.g. YouTube, TikTok, WhatsApp, Chrome, Gmail).",
                    parameters = Parameters(
                        properties = mapOf(
                            "app_name" to Property(type = "STRING", description = "The name of the app to open (e.g., youtube, tiktok, settings)")
                        ),
                        required = listOf("app_name")
                    )
                ),
                FunctionDeclaration(
                    name = "search_app",
                    description = "Search inside a specific app (e.g. search YouTube or TikTok for a search query).",
                    parameters = Parameters(
                        properties = mapOf(
                            "app_name" to Property(type = "STRING", description = "The name of the app (youtube or tiktok)"),
                            "query" to Property(type = "STRING", description = "The search query (e.g., Free Fire, Android tutorials)")
                        ),
                        required = listOf("app_name", "query")
                    )
                ),
                FunctionDeclaration(
                    name = "open_url",
                    description = "Opens a specific web URL in the browser.",
                    parameters = Parameters(
                        properties = mapOf(
                            "url" to Property(type = "STRING", description = "The absolute URL to open (e.g., google.com)")
                        ),
                        required = listOf("url")
                    )
                ),
                FunctionDeclaration(
                    name = "search_web",
                    description = "Searches the web for a query.",
                    parameters = Parameters(
                        properties = mapOf(
                            "query" to Property(type = "STRING", description = "The query to search google for")
                        ),
                        required = listOf("query")
                    )
                ),
                FunctionDeclaration(
                    name = "search_contact",
                    description = "Searches system contacts for a given name.",
                    parameters = Parameters(
                        properties = mapOf(
                            "name" to Property(type = "STRING", description = "The name of the contact to search for")
                        ),
                        required = listOf("name")
                    )
                ),
                FunctionDeclaration(
                    name = "call_contact",
                    description = "Place a voice phone call to a contact name.",
                    parameters = Parameters(
                        properties = mapOf(
                            "name" to Property(type = "STRING", description = "The contact name to call")
                        ),
                        required = listOf("name")
                    )
                ),
                FunctionDeclaration(
                    name = "send_whatsapp_message",
                    description = "Prepares and composes a WhatsApp message to a contact.",
                    parameters = Parameters(
                        properties = mapOf(
                            "contact_name" to Property(type = "STRING", description = "The name of the contact"),
                            "message" to Property(type = "STRING", description = "The body of the message to send")
                        ),
                        required = listOf("contact_name", "message")
                    )
                ),
                FunctionDeclaration(
                    name = "create_note",
                    description = "Saves a personal text note.",
                    parameters = Parameters(
                        properties = mapOf(
                            "title" to Property(type = "STRING", description = "The title of the note"),
                            "content" to Property(type = "STRING", description = "The content body of the note")
                        ),
                        required = listOf("title", "content")
                    )
                ),
                FunctionDeclaration(
                    name = "create_reminder",
                    description = "Creates a calendar reminder or task with a time.",
                    parameters = Parameters(
                        properties = mapOf(
                            "text" to Property(type = "STRING", description = "The task/reminder description"),
                            "time" to Property(type = "STRING", description = "The scheduled time (e.g., 8:00 PM, tomorrow, Saturday)")
                        ),
                        required = listOf("text", "time")
                    )
                ),
                FunctionDeclaration(
                    name = "open_settings",
                    description = "Opens the Android system settings screen.",
                    parameters = Parameters(
                        properties = emptyMap()
                    )
                )
            )
        )
    )
}
