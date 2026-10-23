package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.data.local.KaamKaajDatabase
import com.example.notification.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MARK_PAYMENT_PAID = "com.example.kaamkaaj.ACTION_MARK_PAYMENT_PAID"
        const val ACTION_MARK_REMINDER_DONE = "com.example.kaamkaaj.ACTION_MARK_REMINDER_DONE"
        const val ACTION_SNOOZE = "com.example.kaamkaaj.ACTION_SNOOZE"

        const val EXTRA_ID = "EXTRA_ID"
        const val EXTRA_TYPE = "EXTRA_TYPE"
        const val EXTRA_TITLE = "EXTRA_TITLE"
        const val EXTRA_MESSAGE = "EXTRA_MESSAGE"
        const val EXTRA_TARGET_TAB = "EXTRA_TARGET_TAB"
        const val EXTRA_NOTIFICATION_ID = "EXTRA_NOTIFICATION_ID"
        const val EXTRA_SNOOZE_MILLIS = "EXTRA_SNOOZE_MILLIS"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val id = intent.getLongExtra(EXTRA_ID, -1L)

        // Dismiss the active notification immediately
        if (notificationId != -1) {
            try {
                NotificationManagerCompat.from(context).cancel(notificationId)
            } catch (_: Exception) {}
        }

        when (action) {
            ACTION_MARK_PAYMENT_PAID -> {
                if (id > 0) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = KaamKaajDatabase.getDatabase(context)
                            db.paymentDao().updatePaidStatus(id, true)
                            AlarmScheduler.cancelAlarm(context, AlarmScheduler.TYPE_PAYMENT, id)
                        } catch (_: Exception) {
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }

            ACTION_MARK_REMINDER_DONE -> {
                if (id > 0) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = KaamKaajDatabase.getDatabase(context)
                            val reminderDao = db.reminderDao()
                            val reminder = reminderDao.getReminderById(id)
                            if (reminder != null) {
                                if (reminder.repeatOption != "NONE" && reminder.repeatOption.isNotBlank()) {
                                    // For recurring reminders: advance due date to next occurrence and schedule next alarm
                                    val nextDueMillis = com.example.notification.ReminderSchedulerUtils.calculateNextOccurrence(
                                        baseMillis = reminder.dueDateTimeMillis,
                                        repeatOption = reminder.repeatOption
                                    )
                                    val updated = reminder.copy(
                                        dueDateTimeMillis = nextDueMillis,
                                        isCompleted = false,
                                        completedAtMillis = System.currentTimeMillis()
                                    )
                                    reminderDao.updateReminder(updated)

                                    AlarmScheduler.scheduleAlarm(
                                        context = context,
                                        type = AlarmScheduler.TYPE_REMINDER,
                                        id = reminder.id,
                                        title = "⏰ Reminder: ${reminder.title}",
                                        message = reminder.description.ifBlank { "Scheduled reminder" },
                                        triggerAtMillis = nextDueMillis,
                                        repeatOption = reminder.repeatOption,
                                        targetTab = "reminders"
                                    )
                                } else {
                                    // One-time reminder: mark completed with history timestamp
                                    reminderDao.updateCompletedStatus(id, true, System.currentTimeMillis())
                                    AlarmScheduler.cancelAlarm(context, AlarmScheduler.TYPE_REMINDER, id)
                                }
                            }
                        } catch (_: Exception) {
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }

            ACTION_SNOOZE -> {
                val type = intent.getStringExtra(EXTRA_TYPE) ?: AlarmScheduler.TYPE_REMINDER
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "KaamKaaj Reminder"
                val message = intent.getStringExtra(EXTRA_MESSAGE) ?: ""
                val targetTab = intent.getStringExtra(EXTRA_TARGET_TAB) ?: "home"
                val snoozeMillis = intent.getLongExtra(EXTRA_SNOOZE_MILLIS, 60 * 60 * 1000L) // Default 1 hour

                if (id > 0) {
                    val triggerAtMillis = System.currentTimeMillis() + snoozeMillis
                    AlarmScheduler.scheduleAlarm(
                        context = context,
                        type = type,
                        id = id,
                        title = title,
                        message = message,
                        triggerAtMillis = triggerAtMillis,
                        repeatOption = "NONE",
                        targetTab = targetTab
                    )
                }
            }
        }
    }
}
