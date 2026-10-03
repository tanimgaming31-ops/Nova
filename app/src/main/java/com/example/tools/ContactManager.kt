package com.example.tools

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract

class ContactManager(private val context: Context) {

    data class ContactInfo(val name: String, val phoneNumber: String)

    fun searchContacts(query: String): List<ContactInfo> {
        val results = mutableListOf<ContactInfo>()
        val pm = context.packageManager
        val hasContactsPermission = pm.checkPermission(
            android.Manifest.permission.READ_CONTACTS,
            context.packageName
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasContactsPermission) {
            return emptyList()
        }

        val contentResolver = context.contentResolver
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$query%")

        val cursor: Cursor? = contentResolver.query(uri, projection, selection, selectionArgs, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {
                if (nameIndex >= 0 && numberIndex >= 0) {
                    val name = it.getString(nameIndex)
                    val number = it.getString(numberIndex)
                    results.add(ContactInfo(name, number))
                }
            }
        }
        return results.distinctBy { it.phoneNumber }
    }

    fun callPhoneNumber(number: String, useDirectCall: Boolean = false): String {
        return try {
            val intentAction = if (useDirectCall) Intent.ACTION_CALL else Intent.ACTION_DIAL
            val intent = Intent(intentAction, Uri.parse("tel:$number")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            if (useDirectCall) "Initiated direct call to $number." else "Opened dialer with number $number."
        } catch (e: Exception) {
            // Fallback to ACTION_DIAL if direct call fails or permission is missing
            try {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                "Opened system dialer for $number."
            } catch (ex: Exception) {
                "Failed to place call. Error: ${ex.message}"
            }
        }
    }
}
