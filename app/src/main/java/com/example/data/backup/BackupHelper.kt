package com.example.data.backup

import android.content.Context
import android.net.Uri
import com.example.data.local.KaamKaajDatabase
import com.example.data.model.BackupData
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class BackupHelper(
    private val context: Context,
    private val database: KaamKaajDatabase
) {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val adapter = moshi.adapter(BackupData::class.java).indent("  ")

    suspend fun exportDataToUri(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val docs = database.documentDao().getAllDocumentsList()
            val payments = database.paymentDao().getAllPaymentsList()
            val warranties = database.warrantyDao().getAllWarrantiesList()
            val reminders = database.reminderDao().getAllRemindersList()

            val backup = BackupData(
                version = 1,
                exportDateMillis = System.currentTimeMillis(),
                documents = docs,
                payments = payments,
                warranties = warranties,
                reminders = reminders
            )

            val json = adapter.toJson(backup)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(json)
                }
            } ?: return@withContext Result.failure(Exception("Unable to open output stream"))

            val totalItems = docs.size + payments.size + warranties.size + reminders.size
            Result.success(totalItems)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importDataFromUri(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val content = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        content.append(line)
                        line = reader.readLine()
                    }
                }
            } ?: return@withContext Result.failure(Exception("Unable to open input stream"))

            val backup = adapter.fromJson(content.toString())
                ?: return@withContext Result.failure(Exception("Invalid backup file format"))

            // Insert restored items
            if (backup.documents.isNotEmpty()) {
                database.documentDao().insertAll(backup.documents)
            }
            if (backup.payments.isNotEmpty()) {
                database.paymentDao().insertAll(backup.payments)
            }
            if (backup.warranties.isNotEmpty()) {
                database.warrantyDao().insertAll(backup.warranties)
            }
            if (backup.reminders.isNotEmpty()) {
                database.reminderDao().insertAll(backup.reminders)
            }

            // Reschedule all notification alarms for newly imported items
            com.example.notification.AlarmScheduler.rescheduleAllAlarms(context)

            val totalRestored = backup.documents.size + backup.payments.size + backup.warranties.size + backup.reminders.size
            Result.success(totalRestored)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
