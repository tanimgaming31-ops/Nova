package com.example.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

class AppControlManager(private val context: Context) {

    private val appPackages = mapOf(
        "youtube" to "com.google.android.youtube",
        "tiktok" to "com.zhiliaoapp.musically",
        "whatsapp" to "com.whatsapp",
        "gmail" to "com.google.android.gm",
        "chrome" to "com.android.chrome",
        "settings" to "com.android.settings",
        "facebook" to "com.facebook.katana",
        "instagram" to "com.instagram.android",
        "spotify" to "com.spotify.music"
    )

    fun openApp(appName: String): String {
        val normalizedName = appName.lowercase().trim()
        val packageName = appPackages[normalizedName] ?: getPackageByName(normalizedName)

        if (packageName != null) {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return "Successfully opened $appName."
            }
        }

        // Fallback: If not installed, search Play Store or browser
        val playStoreUri = Uri.parse("market://search?q=$normalizedName")
        val playIntent = Intent(Intent.ACTION_VIEW, playStoreUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(playIntent)
            "$appName is not installed. Opened Play Store to download it."
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=open+$appName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            "$appName is not installed. Opened Google Search in web browser."
        }
    }

    fun searchApp(appName: String, query: String): String {
        val normalizedName = appName.lowercase().trim()
        val cleanQuery = Uri.encode(query)

        return when {
            normalizedName.contains("youtube") -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$cleanQuery")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                "Searching YouTube for \"$query\"."
            }
            normalizedName.contains("tiktok") -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.tiktok.com/search?q=$cleanQuery")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                "Searching TikTok for \"$query\"."
            }
            else -> {
                val searchUrl = "https://www.google.com/search?q=${normalizedName}+search+${cleanQuery}"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                "Searched $appName using browser with query \"$query\"."
            }
        }
    }

    fun openUrl(url: String): String {
        var formattedUrl = url.trim()
        if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
            formattedUrl = "https://$formattedUrl"
        }
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Opening link: $formattedUrl"
        } catch (e: Exception) {
            "Failed to open link: $formattedUrl. Error: ${e.message}"
        }
    }

    fun searchWeb(query: String): String {
        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(android.app.SearchManager.QUERY, query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Searching the web for \"$query\"."
        } catch (e: Exception) {
            openUrl("https://www.google.com/search?q=${Uri.encode(query)}")
        }
    }

    fun openSettings(): String {
        return try {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Opened Android system settings."
        } catch (e: Exception) {
            "Failed to open system settings."
        }
    }

    private fun getPackageByName(name: String): String? {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(0)
        for (appInfo in packages) {
            val label = pm.getApplicationLabel(appInfo).toString().lowercase()
            if (label == name || label.contains(name)) {
                return appInfo.packageName
            }
        }
        return null
    }
}
