package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.data.preferences.AppThemeMode
import com.example.security.AuthCapability
import com.example.security.BiometricAuthManager
import com.example.ui.components.AppTopBar
import com.example.ui.components.DeveloperDialog
import com.example.ui.components.DOCUMENT_REMINDER_OPTIONS
import com.example.ui.components.PAYMENT_REMINDER_OPTIONS
import com.example.ui.components.UpdateDialog
import com.example.ui.components.WARRANTY_REMINDER_OPTIONS
import com.example.ui.theme.*
import com.example.ui.viewmodel.KaamKaajViewModel
import com.example.update.AppUpdateManager
import com.example.update.CheckUpdateResult
import com.example.update.UpdateInfo
import kotlinx.coroutines.launch

val APP_LOCK_TIMEOUT_OPTIONS = listOf(
    0 to "Immediately upon leaving app",
    15 to "After 15 seconds in background",
    30 to "After 30 seconds in background",
    60 to "After 1 minute in background",
    300 to "After 5 minutes in background"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: KaamKaajViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToBackup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentTheme by viewModel.themeMode.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val dailyGreetingsEnabled by viewModel.dailyGreetingsEnabled.collectAsStateWithLifecycle()
    val defaultDocReminder by viewModel.defaultDocReminderDays.collectAsStateWithLifecycle()
    val defaultPaymentReminder by viewModel.defaultPaymentReminderDays.collectAsStateWithLifecycle()
    val defaultWarrantyReminder by viewModel.defaultWarrantyReminderDays.collectAsStateWithLifecycle()
    val appLockEnabled by viewModel.appLockEnabled.collectAsStateWithLifecycle()
    val appLockTimeout by viewModel.appLockTimeoutSeconds.collectAsStateWithLifecycle()

    var showClearDataConfirm by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showDocReminderDialog by remember { mutableStateOf(false) }
    var showPaymentReminderDialog by remember { mutableStateOf(false) }
    var showWarrantyReminderDialog by remember { mutableStateOf(false) }
    var showLockTimeoutDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateInfoState by remember { mutableStateOf<UpdateInfo?>(null) }
    var upToDateResult by remember { mutableStateOf<CheckUpdateResult.UpToDate?>(null) }
    var updateErrorMessage by remember { mutableStateOf<String?>(null) }

    // Permission launcher for Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.setNotificationsEnabled(isGranted)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Settings",
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme Section
            SettingsSectionHeader(title = "APPEARANCE")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(GlassSurfaceDark)
                    .border(1.dp, GlassBorderDark, RoundedCornerShape(18.dp))
            ) {
                SettingsTile(
                    title = "Theme Aesthetic",
                    subtitle = when (currentTheme) {
                        AppThemeMode.SYSTEM -> "System Default"
                        AppThemeMode.LIGHT -> "Light Mode"
                        AppThemeMode.DARK -> "Sophisticated Dark"
                    },
                    icon = Icons.Outlined.DarkMode,
                    onClick = { showThemeDialog = true }
                )
            }

            // App Security & Lock Section
            val authInfo = remember(context) { BiometricAuthManager.getAuthInfo(context) }

            SettingsSectionHeader(title = "APP SECURITY & PRIVACY")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(GlassSurfaceDark)
                    .border(1.dp, GlassBorderDark, RoundedCornerShape(18.dp))
            ) {
                Column {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (appLockEnabled) KaamKaajGold.copy(alpha = 0.15f)
                                            else TextMutedDark.copy(alpha = 0.15f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (appLockEnabled) Icons.Filled.Lock else Icons.Outlined.LockOpen,
                                        contentDescription = null,
                                        tint = if (appLockEnabled) KaamKaajGold else TextMutedDark,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "App Lock",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = if (appLockEnabled) "Status: ON" else "Status: OFF",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (appLockEnabled) KaamKaajGold else StatusExpired
                                    )
                                }
                            }

                            Switch(
                                checked = appLockEnabled,
                                onCheckedChange = { targetState ->
                                    val fragmentActivity = context as? FragmentActivity
                                    if (fragmentActivity != null) {
                                        viewModel.toggleAppLockWithAuth(
                                            activity = fragmentActivity,
                                            enable = targetState
                                        )
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = KaamKaajGold,
                                    uncheckedThumbColor = TextMutedDark,
                                    uncheckedTrackColor = GlassBorderDark
                                )
                            )
                        }

                        // Method description & device compatibility card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (appLockEnabled) KaamKaajGold.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    1.dp,
                                    if (appLockEnabled) KaamKaajGold.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (authInfo.capability == AuthCapability.BIOMETRIC_AND_CREDENTIAL) Icons.Filled.Fingerprint else Icons.Filled.Security,
                                        contentDescription = null,
                                        tint = if (appLockEnabled) KaamKaajGold else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Security Method: ${authInfo.title}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (appLockEnabled) KaamKaajGold else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = if (appLockEnabled) {
                                        authInfo.description
                                    } else {
                                        "Guard private documents, bills, and warranties with Biometrics or device screen lock when opening or returning to the app."
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = if (appLockEnabled) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (appLockEnabled) {
                        HorizontalDivider(color = GlassBorderDark, modifier = Modifier.padding(horizontal = 16.dp))

                        SettingsTile(
                            title = "Lock Frequency & Timeout",
                            subtitle = when (appLockTimeout) {
                                0 -> "Immediately upon leaving app"
                                15 -> "After 15 seconds in background"
                                30 -> "After 30 seconds in background"
                                60 -> "After 1 minute in background"
                                300 -> "After 5 minutes in background"
                                else -> "$appLockTimeout seconds"
                            },
                            icon = Icons.Outlined.Timer,
                            onClick = { showLockTimeoutDialog = true }
                        )

                        HorizontalDivider(color = GlassBorderDark, modifier = Modifier.padding(horizontal = 16.dp))

                        SettingsTile(
                            title = "Lock App Now",
                            subtitle = "Instantly lock vault to test device authentication",
                            icon = Icons.Outlined.Lock,
                            iconTint = KaamKaajGold,
                            onClick = { viewModel.lockAppNow() }
                        )
                    }
                }
            }

            // Notifications Section
            SettingsSectionHeader(title = "NOTIFICATIONS & REMINDERS")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(GlassSurfaceDark)
                    .border(1.dp, GlassBorderDark, RoundedCornerShape(18.dp))
            ) {
                Column {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (notificationsEnabled) StatusActive.copy(alpha = 0.15f)
                                            else TextMutedDark.copy(alpha = 0.15f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (notificationsEnabled) Icons.Filled.NotificationsActive else Icons.Outlined.NotificationsOff,
                                        contentDescription = null,
                                        tint = if (notificationsEnabled) StatusActive else TextMutedDark,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "Reminder Notifications",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = if (notificationsEnabled) "Status: ON" else "Status: OFF",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (notificationsEnabled) StatusActive else StatusExpired
                                    )
                                }
                            }

                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { enable ->
                                    if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        val hasPerm = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (!hasPerm) {
                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            viewModel.setNotificationsEnabled(true)
                                        }
                                    } else {
                                        viewModel.setNotificationsEnabled(enable)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = StatusActive,
                                    uncheckedThumbColor = TextMutedDark,
                                    uncheckedTrackColor = GlassBorderDark
                                )
                            )
                        }

                        // Short explanation
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (notificationsEnabled) StatusActive.copy(alpha = 0.08f)
                                    else StatusExpired.copy(alpha = 0.08f)
                                )
                                .padding(10.dp)
                        ) {
                            Text(
                                text = if (notificationsEnabled) {
                                    "Reminder notifications are ON. You'll receive timely alerts before documents, bills & warranties expire, plus your scheduled personal reminders."
                                } else {
                                    "Reminder notifications are OFF. Scheduled reminders and expiry alerts will not be delivered."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (notificationsEnabled) StatusActive else TextMutedDark
                            )
                        }
                    }

                    HorizontalDivider(color = GlassBorderDark, modifier = Modifier.padding(horizontal = 16.dp))

                    // Daily Greetings Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).padding(end = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(StatusActive.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.WbSunny,
                                    contentDescription = "Daily Greetings",
                                    tint = StatusActive,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Daily Greetings from Subham",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = if (dailyGreetingsEnabled) "Morning (8:30 AM), Afternoon (2:00 PM) & Night (9:30 PM)" else "Disabled",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMutedDark
                                )
                            }
                        }

                        Switch(
                            checked = dailyGreetingsEnabled,
                            onCheckedChange = { enable ->
                                viewModel.setDailyGreetingsEnabled(enable, context)
                            },
                            enabled = notificationsEnabled,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = StatusActive,
                                uncheckedThumbColor = TextMutedDark,
                                uncheckedTrackColor = GlassBorderDark
                            )
                        )
                    }

                    HorizontalDivider(color = GlassBorderDark, modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsTile(
                        title = "Default Document Reminder",
                        subtitle = if (notificationsEnabled) "$defaultDocReminder days before expiry" else "Notifications disabled",
                        icon = Icons.Outlined.Description,
                        onClick = { showDocReminderDialog = true }
                    )

                    HorizontalDivider(color = GlassBorderDark, modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsTile(
                        title = "Default Payment Due Reminder",
                        subtitle = if (notificationsEnabled) "$defaultPaymentReminder days before due date" else "Notifications disabled",
                        icon = Icons.Outlined.Payments,
                        onClick = { showPaymentReminderDialog = true }
                    )

                    HorizontalDivider(color = GlassBorderDark, modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsTile(
                        title = "Default Warranty Reminder",
                        subtitle = if (notificationsEnabled) "$defaultWarrantyReminder days before expiry" else "Notifications disabled",
                        icon = Icons.Outlined.VerifiedUser,
                        onClick = { showWarrantyReminderDialog = true }
                    )

                    HorizontalDivider(color = GlassBorderDark, modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsTile(
                        title = "Send Test Greeting Alert",
                        subtitle = "Preview personal greeting from Subham",
                        icon = Icons.Outlined.WbSunny,
                        iconTint = StatusWarning,
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val hasPerm = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED
                                if (!hasPerm) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                            com.example.notification.NotificationHelper.showNotification(
                                context = context,
                                notificationId = 9998,
                                channelId = com.example.notification.NotificationHelper.CHANNEL_GREETINGS,
                                title = "🌅 Good Morning!",
                                message = "Good morning from Subham! Have a productive and wonderful day ahead.",
                                targetTab = "home"
                            )
                        }
                    )

                    HorizontalDivider(color = GlassBorderDark, modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsTile(
                        title = "Send Test Reminder Alert",
                        subtitle = "Verify instant notification delivery",
                        icon = Icons.Outlined.Send,
                        iconTint = StatusActive,
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val hasPerm = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED
                                if (!hasPerm) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                            com.example.notification.NotificationHelper.showNotification(
                                context = context,
                                notificationId = 9999,
                                channelId = com.example.notification.NotificationHelper.CHANNEL_REMINDERS,
                                title = "⏰ KaamKaaj Test Alert",
                                message = "Notifications are active and configured perfectly!",
                                targetTab = "reminders"
                            )
                        }
                    )
                }
            }

            // Data & Backup
            SettingsSectionHeader(title = "DATA & STORAGE")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(GlassSurfaceDark)
                    .border(1.dp, GlassBorderDark, RoundedCornerShape(18.dp))
            ) {
                Column {
                    SettingsTile(
                        title = "Backup & Restore Data",
                        subtitle = "Export or restore encrypted JSON records",
                        icon = Icons.Outlined.CloudUpload,
                        onClick = onNavigateToBackup
                    )
                }
            }

            // Danger Zone
            SettingsSectionHeader(title = "DANGER ZONE")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(StatusExpired.copy(alpha = 0.08f))
                    .border(1.dp, StatusExpired.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
            ) {
                SettingsTile(
                    title = "Clear All Data",
                    subtitle = "Permanently wipe all documents, bills, and reminders",
                    icon = Icons.Outlined.DeleteForever,
                    iconTint = StatusExpired,
                    titleColor = StatusExpired,
                    onClick = { showClearDataConfirm = true }
                )
            }

            // About Section
            SettingsSectionHeader(title = "ABOUT & GUIDE")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(getCardBackground())
                    .border(1.dp, getCardBorder(), RoundedCornerShape(18.dp))
            ) {
                Column {
                    SettingsTile(
                        title = "Check for Updates",
                        subtitle = if (isCheckingUpdate) "Checking for newer version..." else "v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE}) • Tap to check",
                        icon = Icons.Outlined.SystemUpdate,
                        iconTint = KaamKaajGold,
                        trailing = {
                            if (isCheckingUpdate) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = KaamKaajGold
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                                    contentDescription = null,
                                    tint = getTextMuted(),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        },
                        onClick = {
                            if (!isCheckingUpdate) {
                                coroutineScope.launch {
                                    isCheckingUpdate = true
                                    when (val result = AppUpdateManager.checkUpdateDetailed()) {
                                        is CheckUpdateResult.UpdateAvailable -> {
                                            updateInfoState = result.info
                                        }
                                        is CheckUpdateResult.UpToDate -> {
                                            upToDateResult = result
                                        }
                                        is CheckUpdateResult.CheckFailed -> {
                                            updateErrorMessage = result.message
                                        }
                                    }
                                    isCheckingUpdate = false
                                }
                            }
                        }
                    )

                    HorizontalDivider(color = getCardBorder(), modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsTile(
                        title = "Developer",
                        subtitle = "Subham kumar prusty",
                        icon = Icons.Outlined.Code,
                        iconTint = Color(0xFF818CF8),
                        onClick = { showAboutDialog = true }
                    )
                }
            }
        }
    }

    // Theme Selection Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(22.dp),
            title = {
                Text(
                    "Choose Theme",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            text = {
                Column {
                    AppThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentTheme == mode,
                                onClick = {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = TextMutedDark
                                )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = when (mode) {
                                    AppThemeMode.SYSTEM -> "System Default"
                                    AppThemeMode.LIGHT -> "Light Mode"
                                    AppThemeMode.DARK -> "Sophisticated Dark"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("CLOSE", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }

    // Document Reminder Interval Dialog
    if (showDocReminderDialog) {
        AlertDialog(
            onDismissRequest = { showDocReminderDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(22.dp),
            title = {
                Text(
                    "Default Document Reminder",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            text = {
                Column {
                    DOCUMENT_REMINDER_OPTIONS.forEach { (days, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.setDefaultDocReminderDays(days)
                                    showDocReminderDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = defaultDocReminder == days,
                                onClick = {
                                    viewModel.setDefaultDocReminderDays(days)
                                    showDocReminderDialog = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = TextMutedDark
                                )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDocReminderDialog = false }) {
                    Text("CLOSE", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }

    // Payment Reminder Interval Dialog
    if (showPaymentReminderDialog) {
        AlertDialog(
            onDismissRequest = { showPaymentReminderDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(22.dp),
            title = {
                Text(
                    "Default Payment Reminder",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            text = {
                Column {
                    PAYMENT_REMINDER_OPTIONS.forEach { (days, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.setDefaultPaymentReminderDays(days)
                                    showPaymentReminderDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = defaultPaymentReminder == days,
                                onClick = {
                                    viewModel.setDefaultPaymentReminderDays(days)
                                    showPaymentReminderDialog = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = TextMutedDark
                                )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPaymentReminderDialog = false }) {
                    Text("CLOSE", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }

    // Warranty Reminder Interval Dialog
    if (showWarrantyReminderDialog) {
        AlertDialog(
            onDismissRequest = { showWarrantyReminderDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(22.dp),
            title = {
                Text(
                    "Default Warranty Reminder",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            text = {
                Column {
                    WARRANTY_REMINDER_OPTIONS.forEach { (days, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.setDefaultWarrantyReminderDays(days)
                                    showWarrantyReminderDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = defaultWarrantyReminder == days,
                                onClick = {
                                    viewModel.setDefaultWarrantyReminderDays(days)
                                    showWarrantyReminderDialog = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = TextMutedDark
                                )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showWarrantyReminderDialog = false }) {
                    Text("CLOSE", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }

    // App Lock Timeout Dialog
    if (showLockTimeoutDialog) {
        AlertDialog(
            onDismissRequest = { showLockTimeoutDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(22.dp),
            title = {
                Text(
                    "App Lock Timeout",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            text = {
                Column {
                    APP_LOCK_TIMEOUT_OPTIONS.forEach { (seconds, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.setAppLockTimeout(seconds)
                                    showLockTimeoutDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = appLockTimeout == seconds,
                                onClick = {
                                    viewModel.setAppLockTimeout(seconds)
                                    showLockTimeoutDialog = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = KaamKaajGold,
                                    unselectedColor = TextMutedDark
                                )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLockTimeoutDialog = false }) {
                    Text("CLOSE", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }

    // Clear Data Confirmation Dialog
    if (showClearDataConfirm) {
        AlertDialog(
            onDismissRequest = { showClearDataConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(22.dp),
            title = {
                Text(
                    "Clear All Data?",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
                    color = StatusExpired
                )
            },
            text = {
                Text(
                    "This will permanently delete all your stored documents, payments, warranties, and reminders from the local database. This action CANNOT be undone. Proceed?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearDataConfirm = false
                        viewModel.clearAllData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusExpired)
                ) {
                    Text("CLEAR EVERYTHING", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataConfirm = false }) {
                    Text("CANCEL", color = TextMutedDark)
                }
            }
        )
    }

    // Developer Dialog (Apple iOS 26 Liquid Glass-inspired)
    if (showAboutDialog) {
        DeveloperDialog(
            onDismiss = { showAboutDialog = false }
        )
    }

    // Up to Date Dialog
    if (upToDateResult != null) {
        AlertDialog(
            onDismissRequest = { upToDateResult = null },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shape = RoundedCornerShape(24.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(SecondaryTealDark.copy(alpha = 0.15f))
                        .border(1.dp, SecondaryTealDark.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = "Up to date",
                        tint = SecondaryTealDark,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "You're Up to Date",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "KaamKaaj ${upToDateResult?.currentVersion} is currently the newest version available.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { upToDateResult = null },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KaamKaajGold,
                        contentColor = Color(0xFF0F172A)
                    )
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Update Check Failed / Offline Dialog
    if (updateErrorMessage != null) {
        AlertDialog(
            onDismissRequest = { updateErrorMessage = null },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shape = RoundedCornerShape(24.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(StatusExpired.copy(alpha = 0.15f))
                        .border(1.dp, StatusExpired.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WifiOff,
                        contentDescription = "Connection Error",
                        tint = StatusExpired,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Update Check",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = updateErrorMessage ?: "Could not check for updates. Please try again later.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = { updateErrorMessage = null },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("OK")
                }
            }
        )
    }

    // New Update Available Dialog
    if (updateInfoState != null) {
        UpdateDialog(
            updateInfo = updateInfoState!!,
            onDismiss = { updateInfoState = null }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 2.sp
        ),
        color = TextMutedDark,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp)
    )
}

@Composable
fun SettingsTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    titleColor: Color = MaterialTheme.colorScheme.onBackground,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClickable(pressedScale = 0.98f, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = titleColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = getTextMuted()
            )
        }
        if (trailing != null) {
            trailing()
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = null,
                tint = getTextMuted(),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
