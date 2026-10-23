package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupHelper
import com.example.data.model.DocumentItem
import com.example.data.model.PaymentItem
import com.example.data.model.ReminderItem
import com.example.data.model.WarrantyItem
import com.example.data.preferences.AppThemeMode
import com.example.data.preferences.UserPreferences
import com.example.data.repository.KaamKaajRepository
import com.example.security.AppLockManager
import com.example.security.BiometricAuthManager
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SearchResults(
    val documents: List<DocumentItem> = emptyList(),
    val payments: List<PaymentItem> = emptyList(),
    val warranties: List<WarrantyItem> = emptyList(),
    val reminders: List<ReminderItem> = emptyList()
) {
    val totalCount: Int get() = documents.size + payments.size + warranties.size + reminders.size
}

class KaamKaajViewModel(
    private val repository: KaamKaajRepository,
    private val userPreferences: UserPreferences,
    private val backupHelper: BackupHelper
) : ViewModel() {

    val themeMode: StateFlow<AppThemeMode> = userPreferences.themeMode
    val notificationsEnabled: StateFlow<Boolean> = userPreferences.notificationsEnabled
    val defaultDocReminderDays: StateFlow<Int> = userPreferences.defaultDocumentReminderDays
    val defaultPaymentReminderDays: StateFlow<Int> = userPreferences.defaultPaymentReminderDays
    val defaultWarrantyReminderDays: StateFlow<Int> = userPreferences.defaultWarrantyReminderDays
    val dailyGreetingsEnabled: StateFlow<Boolean> = userPreferences.dailyGreetingsEnabled

    // App Lock & Security
    val appLockEnabled: StateFlow<Boolean> = userPreferences.appLockEnabled
    val appLockTimeoutSeconds: StateFlow<Int> = userPreferences.appLockTimeoutSeconds
    val isAppLocked: StateFlow<Boolean> = AppLockManager.isLocked

    // Data streams (Cached in ViewModel memory for instant 0-latency tab transitions)
    val allDocuments: StateFlow<List<DocumentItem>> = repository.allDocuments
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val expiringDocuments: StateFlow<List<DocumentItem>> = repository.getExpiringSoonDocuments(30)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allPayments: StateFlow<List<PaymentItem>> = repository.allPayments
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val upcomingPayments: StateFlow<List<PaymentItem>> = repository.getUpcomingPayments(14)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allWarranties: StateFlow<List<WarrantyItem>> = repository.allWarranties
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val expiringWarranties: StateFlow<List<WarrantyItem>> = repository.getExpiringSoonWarranties(45)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allReminders: StateFlow<List<ReminderItem>> = repository.allReminders
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val upcomingReminders: StateFlow<List<ReminderItem>> = repository.getUpcomingReminders(7)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Search query & combined search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<SearchResults> = _searchQuery
        .debounce(200)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(SearchResults())
            } else {
                combine(
                    repository.searchDocuments(query),
                    repository.searchPayments(query),
                    repository.searchWarranties(query),
                    repository.searchReminders(query)
                ) { docs, pays, wars, rems ->
                    SearchResults(
                        documents = docs,
                        payments = pays,
                        warranties = wars,
                        reminders = rems
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchResults())

    // Backup & Restore Feedback Messages
    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // --- Document actions ---
    fun saveDocument(doc: DocumentItem) {
        viewModelScope.launch {
            repository.saveDocument(doc)
            _userMessage.emit("Document saved successfully")
        }
    }

    fun deleteDocument(doc: DocumentItem) {
        viewModelScope.launch {
            repository.deleteDocument(doc)
            _userMessage.emit("Document deleted")
        }
    }

    // --- Payment actions ---
    fun savePayment(payment: PaymentItem) {
        viewModelScope.launch {
            repository.savePayment(payment)
            _userMessage.emit("Payment saved")
        }
    }

    fun togglePaymentPaid(payment: PaymentItem) {
        viewModelScope.launch {
            repository.togglePaymentPaid(payment)
            _userMessage.emit(if (!payment.isPaid) "Marked as paid" else "Marked as pending")
        }
    }

    fun deletePayment(payment: PaymentItem) {
        viewModelScope.launch {
            repository.deletePayment(payment)
            _userMessage.emit("Payment deleted")
        }
    }

    // --- Warranty actions ---
    fun saveWarranty(warranty: WarrantyItem) {
        viewModelScope.launch {
            repository.saveWarranty(warranty)
            _userMessage.emit("Warranty saved")
        }
    }

    fun deleteWarranty(warranty: WarrantyItem) {
        viewModelScope.launch {
            repository.deleteWarranty(warranty)
            _userMessage.emit("Warranty deleted")
        }
    }

    // --- Reminder actions ---
    fun saveReminder(reminder: ReminderItem) {
        viewModelScope.launch {
            repository.saveReminder(reminder)
            _userMessage.emit("Reminder saved")
        }
    }

    fun toggleReminderCompleted(reminder: ReminderItem) {
        viewModelScope.launch {
            val wasCompleted = reminder.isCompleted
            repository.toggleReminderCompleted(reminder)
            _userMessage.emit(
                if (!wasCompleted) {
                    if (reminder.repeatOption != "NONE" && reminder.repeatOption.isNotBlank()) {
                        "Completed! Rescheduled for next ${com.example.notification.ReminderSchedulerUtils.getRepeatLabel(reminder.repeatOption)}"
                    } else {
                        "Reminder marked as completed"
                    }
                } else {
                    "Reminder restored to active"
                }
            )
        }
    }

    fun rescheduleReminder(reminder: ReminderItem, newDueDateTimeMillis: Long) {
        viewModelScope.launch {
            repository.rescheduleReminder(reminder, newDueDateTimeMillis)
            _userMessage.emit("Reminder rescheduled")
        }
    }

    fun deleteReminder(reminder: ReminderItem) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
            _userMessage.emit("Reminder deleted")
        }
    }

    // --- Preferences actions ---
    fun setThemeMode(mode: AppThemeMode) {
        userPreferences.setThemeMode(mode)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        userPreferences.setNotificationsEnabled(enabled)
        viewModelScope.launch {
            if (enabled) {
                repository.rescheduleAllNotifications()
                _userMessage.emit("Notifications enabled & reminders scheduled")
            } else {
                _userMessage.emit("Notifications disabled")
            }
        }
    }

    fun setDefaultDocReminderDays(days: Int) {
        userPreferences.setDefaultDocumentReminderDays(days)
    }

    fun setDefaultPaymentReminderDays(days: Int) {
        userPreferences.setDefaultPaymentReminderDays(days)
    }

    fun setDefaultWarrantyReminderDays(days: Int) {
        userPreferences.setDefaultWarrantyReminderDays(days)
    }

    fun setDailyGreetingsEnabled(enabled: Boolean, context: android.content.Context) {
        userPreferences.setDailyGreetingsEnabled(enabled)
        com.example.notification.AlarmScheduler.scheduleDailyGreetings(context)
        viewModelScope.launch {
            _userMessage.emit(
                if (enabled) "Daily greetings from Subham enabled 🌅"
                else "Daily greetings paused"
            )
        }
    }

    // --- App Lock & Biometrics ---
    fun unlockApp() {
        AppLockManager.unlock()
    }

    fun lockAppNow() {
        AppLockManager.lockNow()
    }

    fun setAppLockTimeout(seconds: Int) {
        userPreferences.setAppLockTimeoutSeconds(seconds)
    }

    fun toggleAppLockWithAuth(
        activity: FragmentActivity,
        enable: Boolean,
        onSuccess: () -> Unit = {},
        onError: ((String) -> Unit)? = null
    ) {
        if (!BiometricAuthManager.isAuthAvailable(activity)) {
            val errorMsg = "Please setup a Device Screen Lock (PIN, Pattern, or Fingerprint) in Android Settings first."
            viewModelScope.launch { _userMessage.emit(errorMsg) }
            onError?.invoke(errorMsg)
            return
        }

        val promptTitle = if (enable) "Enable App Lock" else "Disable App Lock"
        val promptSubtitle = if (enable) "Confirm your device credentials to activate App Lock" else "Authenticate to turn off App Lock"

        BiometricAuthManager.authenticate(
            activity = activity,
            title = promptTitle,
            subtitle = promptSubtitle,
            onSuccess = {
                userPreferences.setAppLockEnabled(enable)
                if (!enable) {
                    AppLockManager.unlock()
                }
                viewModelScope.launch {
                    _userMessage.emit(if (enable) "App Lock enabled successfully" else "App Lock disabled")
                }
                onSuccess()
            },
            onError = { _, errString ->
                val errorMsg = errString.toString().ifBlank { "Authentication cancelled" }
                viewModelScope.launch { _userMessage.emit(errorMsg) }
                onError?.invoke(errorMsg)
            },
            onFailed = {
                onError?.invoke("Authentication failed. Please try again.")
            }
        )
    }

    fun promptUnlock(
        activity: FragmentActivity,
        onSuccess: () -> Unit = {},
        onError: ((String) -> Unit)? = null
    ) {
        BiometricAuthManager.authenticate(
            activity = activity,
            title = "Unlock KaamKaaj",
            subtitle = "Authenticate to access your private records",
            onSuccess = {
                AppLockManager.unlock()
                onSuccess()
            },
            onError = { _, errString ->
                onError?.invoke(errString.toString().ifBlank { "Authentication cancelled" })
            },
            onFailed = {
                onError?.invoke("Authentication failed. Please try again.")
            }
        )
    }

    // --- Backup & Restore ---
    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            val result = backupHelper.exportDataToUri(uri)
            if (result.isSuccess) {
                val count = result.getOrDefault(0)
                _userMessage.emit("Successfully exported $count records to backup file")
            } else {
                _userMessage.emit("Export failed: ${result.exceptionOrNull()?.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            val result = backupHelper.importDataFromUri(uri)
            if (result.isSuccess) {
                val count = result.getOrDefault(0)
                _userMessage.emit("Successfully restored $count items from backup")
            } else {
                _userMessage.emit("Restore failed: ${result.exceptionOrNull()?.localizedMessage ?: "Invalid file format"}")
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _userMessage.emit("All data has been cleared")
        }
    }
}

class KaamKaajViewModelFactory(
    private val repository: KaamKaajRepository,
    private val userPreferences: UserPreferences,
    private val backupHelper: BackupHelper
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(KaamKaajViewModel::class.java)) {
            return KaamKaajViewModel(repository, userPreferences, backupHelper) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
