package com.example

import android.app.Application
import com.example.data.backup.BackupHelper
import com.example.data.local.KaamKaajDatabase
import com.example.data.preferences.UserPreferences
import com.example.data.repository.KaamKaajRepository
import com.example.notification.NotificationHelper

class KaamKaajApplication : Application() {
    val database: KaamKaajDatabase by lazy { KaamKaajDatabase.getDatabase(this) }
    val repository: KaamKaajRepository by lazy { KaamKaajRepository(this, database) }
    val userPreferences: UserPreferences by lazy { UserPreferences(this) }
    val backupHelper: BackupHelper by lazy { BackupHelper(this, database) }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
        com.example.notification.AlarmScheduler.scheduleDailyGreetings(this)
    }
}
