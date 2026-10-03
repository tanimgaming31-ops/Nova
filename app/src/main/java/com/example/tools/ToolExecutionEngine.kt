package com.example.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.database.NoteDao
import com.example.data.database.NoteEntity
import com.example.data.database.ReminderDao
import com.example.data.database.ReminderEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ToolExecutionEngine(
    private val context: Context,
    private val appControlManager: AppControlManager,
    private val contactManager: ContactManager,
    private val noteDao: NoteDao,
    private val reminderDao: ReminderDao
) {

    suspend fun executeTool(name: String, args: Map<String, String>?): String = withContext(Dispatchers.IO) {
        val arguments = args ?: emptyMap()
        try {
            when (name) {
                "open_app" -> {
                    val appName = arguments["app_name"] ?: return@withContext "Error: Missing argument 'app_name'."
                    appControlManager.openApp(appName)
                }

                "search_app" -> {
                    val appName = arguments["app_name"] ?: return@withContext "Error: Missing argument 'app_name'."
                    val query = arguments["query"] ?: return@withContext "Error: Missing argument 'query'."
                    appControlManager.searchApp(appName, query)
                }

                "open_url" -> {
                    val url = arguments["url"] ?: return@withContext "Error: Missing argument 'url'."
                    appControlManager.openUrl(url)
                }

                "search_web" -> {
                    val query = arguments["query"] ?: return@withContext "Error: Missing argument 'query'."
                    appControlManager.searchWeb(query)
                }

                "search_contact" -> {
                    val searchName = arguments["name"] ?: return@withContext "Error: Missing argument 'name'."
                    val contacts = contactManager.searchContacts(searchName)
                    if (contacts.isEmpty()) {
                        "No contacts found matching \"$searchName\". Ensure you have granted Contacts permission in settings."
                    } else {
                        val formatted = contacts.joinToString { "${it.name} (${it.phoneNumber})" }
                        "Found ${contacts.size} contacts: $formatted"
                    }
                }

                "call_contact" -> {
                    val searchName = arguments["name"] ?: return@withContext "Error: Missing argument 'name'."
                    val contacts = contactManager.searchContacts(searchName)
                    when {
                        contacts.isEmpty() -> {
                            "Could not find a contact named \"$searchName\". If you haven't given contact access, please grant the permission."
                        }
                        contacts.size == 1 -> {
                            val contact = contacts.first()
                            contactManager.callPhoneNumber(contact.phoneNumber)
                        }
                        else -> {
                            val list = contacts.joinToString { it.name }
                            "I found multiple contacts matching \"$searchName\": $list. Which one would you like me to call?"
                        }
                    }
                }

                "send_whatsapp_message" -> {
                    val contactName = arguments["contact_name"] ?: return@withContext "Error: Missing argument 'contact_name'."
                    val message = arguments["message"] ?: return@withContext "Error: Missing argument 'message'."
                    
                    val contacts = contactManager.searchContacts(contactName)
                    if (contacts.isNotEmpty()) {
                        val contact = contacts.first()
                        val cleanPhone = contact.phoneNumber.replace(Regex("[^0-9+]"), "")
                        val whatsappUri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(message)}")
                        val intent = Intent(Intent.ACTION_VIEW, whatsappUri).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                        "Opening WhatsApp chat with ${contact.name} and pre-filling your message."
                    } else {
                        // Fallback: Compose generally
                        val whatsappUri = Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(message)}")
                        val intent = Intent(Intent.ACTION_VIEW, whatsappUri).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                        "Contact not found. Opened general WhatsApp share with your message."
                    }
                }

                "create_note" -> {
                    val title = arguments["title"] ?: "New Note"
                    val content = arguments["content"] ?: ""
                    noteDao.insertNote(NoteEntity(title = title, content = content))
                    "Saved note: \"$title\"."
                }

                "create_reminder" -> {
                    val text = arguments["text"] ?: return@withContext "Error: Missing argument 'text'."
                    val time = arguments["time"] ?: "soon"
                    reminderDao.insertReminder(ReminderEntity(text = text, time = time))
                    "Set reminder: \"$text\" for $time."
                }

                "open_settings" -> {
                    appControlManager.openSettings()
                }

                else -> "Error: Unsupported tool \"$name\"."
            }
        } catch (e: Exception) {
            "Failed to execute tool \"$name\". Error: ${e.message}"
        }
    }
}
