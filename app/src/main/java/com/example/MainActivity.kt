package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.notification.NotificationHelper
import com.example.security.AppLockManager
import com.example.ui.navigation.KaamKaajApp
import com.example.ui.theme.KaamKaajTheme
import com.example.ui.viewmodel.KaamKaajViewModel
import com.example.ui.viewmodel.KaamKaajViewModelFactory

class MainActivity : FragmentActivity() {

    private val viewModel: KaamKaajViewModel by viewModels {
        val app = application as KaamKaajApplication
        KaamKaajViewModelFactory(app.repository, app.userPreferences, app.backupHelper)
    }

    private var pendingTargetRoute by mutableStateOf<String?>(null)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission result handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as KaamKaajApplication
        AppLockManager.initOnStart(app.userPreferences.appLockEnabled.value)

        // Request POST_NOTIFICATIONS on Android 13+
        checkAndRequestNotificationPermission()

        // Parse notification intent
        handleNotificationIntent(intent)

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

            KaamKaajTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    KaamKaajApp(
                        viewModel = viewModel,
                        targetRoute = pendingTargetRoute,
                        onRouteConsumed = { pendingTargetRoute = null }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val app = application as KaamKaajApplication
        AppLockManager.onAppForegrounded(
            appLockEnabled = app.userPreferences.appLockEnabled.value,
            timeoutSeconds = app.userPreferences.appLockTimeoutSeconds.value
        )
    }

    override fun onStop() {
        super.onStop()
        AppLockManager.onAppBackgrounded()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val targetTab = intent?.getStringExtra("EXTRA_TARGET_TAB")
        if (!targetTab.isNullOrBlank()) {
            pendingTargetRoute = targetTab
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
