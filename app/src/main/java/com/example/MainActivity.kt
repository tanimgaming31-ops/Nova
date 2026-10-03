package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.data.database.AppDatabase
import com.example.presentation.ui.NovaApp
import com.example.presentation.viewmodel.NovaViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: NovaViewModel

    private val requestMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (recordGranted) {
            Toast.makeText(this, "Microphone access granted. NOVA is listening!", Toast.LENGTH_SHORT).show()
            viewModel.startAssistantService(this)
        } else {
            Toast.makeText(
                this,
                "Microphone access denied. Voice commands will not function without this permission.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Setup Local Database & ViewModel
        val database = AppDatabase.getDatabase(applicationContext)
        val viewModelFactory = NovaViewModel.Factory(database)
        viewModel = ViewModelProvider(this, viewModelFactory)[NovaViewModel::class.java]

        // Check & Request Permissions at Launch
        checkAndRequestPermissions()

        setContent {
            MyApplicationTheme {
                NovaApp(viewModel = viewModel)
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsNeeded = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missingPermissions = permissionsNeeded.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestMultiplePermissionsLauncher.launch(missingPermissions.toTypedArray())
        } else {
            // Already have permissions, make sure the listening service is started
            viewModel.startAssistantService(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
