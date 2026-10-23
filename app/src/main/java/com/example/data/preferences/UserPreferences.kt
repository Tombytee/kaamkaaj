package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("kaamkaaj_user_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(loadNotificationsEnabled())
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _defaultDocumentReminderDays = MutableStateFlow(loadDefaultDocumentReminderDays())
    val defaultDocumentReminderDays: StateFlow<Int> = _defaultDocumentReminderDays.asStateFlow()

    private val _defaultPaymentReminderDays = MutableStateFlow(loadDefaultPaymentReminderDays())
    val defaultPaymentReminderDays: StateFlow<Int> = _defaultPaymentReminderDays.asStateFlow()

    private val _defaultWarrantyReminderDays = MutableStateFlow(loadDefaultWarrantyReminderDays())
    val defaultWarrantyReminderDays: StateFlow<Int> = _defaultWarrantyReminderDays.asStateFlow()

    private val _dailyGreetingsEnabled = MutableStateFlow(loadDailyGreetingsEnabled())
    val dailyGreetingsEnabled: StateFlow<Boolean> = _dailyGreetingsEnabled.asStateFlow()

    private val _firstLaunchCompleted = MutableStateFlow(loadFirstLaunchCompleted())
    val firstLaunchCompleted: StateFlow<Boolean> = _firstLaunchCompleted.asStateFlow()

    private val _appLockEnabled = MutableStateFlow(loadAppLockEnabled())
    val appLockEnabled: StateFlow<Boolean> = _appLockEnabled.asStateFlow()

    private val _appLockTimeoutSeconds = MutableStateFlow(loadAppLockTimeoutSeconds())
    val appLockTimeoutSeconds: StateFlow<Int> = _appLockTimeoutSeconds.asStateFlow()

    private fun loadFirstLaunchCompleted(): Boolean {
        return prefs.getBoolean(KEY_FIRST_LAUNCH_COMPLETED, false)
    }

    fun setFirstLaunchCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH_COMPLETED, completed).apply()
        _firstLaunchCompleted.value = completed
    }

    private fun loadAppLockEnabled(): Boolean {
        return prefs.getBoolean(KEY_APP_LOCK_ENABLED, false)
    }

    fun setAppLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, enabled).apply()
        _appLockEnabled.value = enabled
    }

    private fun loadAppLockTimeoutSeconds(): Int {
        return prefs.getInt(KEY_APP_LOCK_TIMEOUT, 0)
    }

    fun setAppLockTimeoutSeconds(seconds: Int) {
        prefs.edit().putInt(KEY_APP_LOCK_TIMEOUT, seconds).apply()
        _appLockTimeoutSeconds.value = seconds
    }

    private fun loadThemeMode(): AppThemeMode {
        val name = prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name
        return try {
            AppThemeMode.valueOf(name)
        } catch (_: Exception) {
            AppThemeMode.SYSTEM
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    private fun loadNotificationsEnabled(): Boolean {
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
        _notificationsEnabled.value = enabled
    }

    private fun loadDefaultDocumentReminderDays(): Int {
        return prefs.getInt(KEY_DEFAULT_DOC_REMINDER, 30)
    }

    fun setDefaultDocumentReminderDays(days: Int) {
        prefs.edit().putInt(KEY_DEFAULT_DOC_REMINDER, days).apply()
        _defaultDocumentReminderDays.value = days
    }

    private fun loadDefaultPaymentReminderDays(): Int {
        return prefs.getInt(KEY_DEFAULT_PAYMENT_REMINDER, 3)
    }

    fun setDefaultPaymentReminderDays(days: Int) {
        prefs.edit().putInt(KEY_DEFAULT_PAYMENT_REMINDER, days).apply()
        _defaultPaymentReminderDays.value = days
    }

    private fun loadDefaultWarrantyReminderDays(): Int {
        return prefs.getInt(KEY_DEFAULT_WARRANTY_REMINDER, 15)
    }

    fun setDefaultWarrantyReminderDays(days: Int) {
        prefs.edit().putInt(KEY_DEFAULT_WARRANTY_REMINDER, days).apply()
        _defaultWarrantyReminderDays.value = days
    }

    private fun loadDailyGreetingsEnabled(): Boolean {
        return prefs.getBoolean(KEY_DAILY_GREETINGS_ENABLED, true)
    }

    fun setDailyGreetingsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DAILY_GREETINGS_ENABLED, enabled).apply()
        _dailyGreetingsEnabled.value = enabled
    }

    companion object {
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_NOTIFICATIONS_ENABLED = "key_notifications_enabled"
        private const val KEY_DAILY_GREETINGS_ENABLED = "key_daily_greetings_enabled"
        private const val KEY_DEFAULT_DOC_REMINDER = "key_default_doc_reminder"
        private const val KEY_DEFAULT_PAYMENT_REMINDER = "key_default_payment_reminder"
        private const val KEY_DEFAULT_WARRANTY_REMINDER = "key_default_warranty_reminder"
        private const val KEY_FIRST_LAUNCH_COMPLETED = "key_first_launch_completed"
        private const val KEY_APP_LOCK_ENABLED = "key_app_lock_enabled"
        private const val KEY_APP_LOCK_TIMEOUT = "key_app_lock_timeout"
    }
}
