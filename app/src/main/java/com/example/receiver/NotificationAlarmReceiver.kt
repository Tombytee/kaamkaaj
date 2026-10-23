package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.KaamKaajDatabase
import com.example.data.preferences.UserPreferences
import com.example.notification.AlarmScheduler
import com.example.notification.NotificationHelper
import com.example.notification.ReminderSchedulerUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class NotificationAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val userPrefs = UserPreferences(context)
        if (!userPrefs.notificationsEnabled.value) {
            return
        }

        val type = intent.getStringExtra("EXTRA_TYPE") ?: return
        val id = intent.getLongExtra("EXTRA_ID", -1L)
        val rawTitle = intent.getStringExtra("EXTRA_TITLE") ?: "KaamKaaj Reminder"
        val rawMessage = intent.getStringExtra("EXTRA_MESSAGE") ?: "You have an upcoming event."
        val repeatOption = intent.getStringExtra("EXTRA_REPEAT") ?: "NONE"
        val explicitTargetTab = intent.getStringExtra("EXTRA_TARGET_TAB")

        val isGreeting = type in listOf(
            AlarmScheduler.TYPE_GREETING_MORNING,
            AlarmScheduler.TYPE_GREETING_AFTERNOON,
            AlarmScheduler.TYPE_GREETING_NIGHT
        )

        // If daily greetings are disabled, do not show greeting notification
        if (isGreeting && !userPrefs.dailyGreetingsEnabled.value) {
            return
        }

        val (channelId, defaultTargetTab) = when (type) {
            AlarmScheduler.TYPE_DOCUMENT -> NotificationHelper.CHANNEL_DOCUMENTS to "documents"
            AlarmScheduler.TYPE_PAYMENT -> NotificationHelper.CHANNEL_PAYMENTS to "payments"
            AlarmScheduler.TYPE_WARRANTY -> NotificationHelper.CHANNEL_WARRANTIES to "warranties"
            AlarmScheduler.TYPE_REMINDER -> NotificationHelper.CHANNEL_REMINDERS to "reminders"
            AlarmScheduler.TYPE_GREETING_MORNING,
            AlarmScheduler.TYPE_GREETING_AFTERNOON,
            AlarmScheduler.TYPE_GREETING_NIGHT -> NotificationHelper.CHANNEL_GREETINGS to "home"
            else -> NotificationHelper.CHANNEL_REMINDERS to "home"
        }

        val targetTab = explicitTargetTab ?: defaultTargetTab
        val notificationId = (id % 100000).toInt() + 1000

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = KaamKaajDatabase.getDatabase(context)

                // For database entities, check if the item is still active to avoid duplicate/stale notifications
                when (type) {
                    AlarmScheduler.TYPE_REMINDER -> {
                        val reminder = db.reminderDao().getReminderById(id)
                        if (reminder == null || reminder.isCompleted) {
                            // Reminder was deleted or already completed; do not alert
                            return@launch
                        }

                        NotificationHelper.showNotification(
                            context = context,
                            notificationId = notificationId,
                            channelId = channelId,
                            title = "⏰ Reminder: ${reminder.title}",
                            message = reminder.description.ifBlank { "You have a scheduled reminder." },
                            targetTab = targetTab,
                            itemId = id,
                            itemType = type
                        )

                        // Reschedule if repeating
                        if (reminder.repeatOption != "NONE" && reminder.repeatOption.isNotBlank()) {
                            val nextTriggerMillis = ReminderSchedulerUtils.calculateNextOccurrence(
                                baseMillis = reminder.dueDateTimeMillis,
                                repeatOption = reminder.repeatOption
                            )
                            val updated = reminder.copy(dueDateTimeMillis = nextTriggerMillis)
                            db.reminderDao().updateReminder(updated)

                            AlarmScheduler.scheduleAlarm(
                                context = context,
                                type = type,
                                id = id,
                                title = "⏰ Reminder: ${reminder.title}",
                                message = reminder.description.ifBlank { "You have a scheduled reminder." },
                                triggerAtMillis = nextTriggerMillis,
                                repeatOption = reminder.repeatOption,
                                targetTab = targetTab
                            )
                        }
                    }

                    AlarmScheduler.TYPE_PAYMENT -> {
                        val payment = db.paymentDao().getPaymentById(id)
                        if (payment == null || payment.isPaid) {
                            return@launch
                        }
                        NotificationHelper.showNotification(
                            context = context,
                            notificationId = notificationId,
                            channelId = channelId,
                            title = rawTitle,
                            message = rawMessage,
                            targetTab = targetTab,
                            itemId = id,
                            itemType = type
                        )
                    }

                    AlarmScheduler.TYPE_DOCUMENT -> {
                        val doc = db.documentDao().getDocumentById(id)
                        if (doc == null) return@launch
                        NotificationHelper.showNotification(
                            context = context,
                            notificationId = notificationId,
                            channelId = channelId,
                            title = rawTitle,
                            message = rawMessage,
                            targetTab = targetTab,
                            itemId = id,
                            itemType = type
                        )
                    }

                    AlarmScheduler.TYPE_WARRANTY -> {
                        val warranty = db.warrantyDao().getWarrantyById(id)
                        if (warranty == null) return@launch
                        NotificationHelper.showNotification(
                            context = context,
                            notificationId = notificationId,
                            channelId = channelId,
                            title = rawTitle,
                            message = rawMessage,
                            targetTab = targetTab,
                            itemId = id,
                            itemType = type
                        )
                    }

                    else -> {
                        // Daily greetings or other system notifications
                        NotificationHelper.showNotification(
                            context = context,
                            notificationId = notificationId,
                            channelId = channelId,
                            title = rawTitle,
                            message = rawMessage,
                            targetTab = targetTab,
                            itemId = id,
                            itemType = type
                        )

                        // Reschedule daily greeting for tomorrow at the same slot
                        if (isGreeting && userPrefs.dailyGreetingsEnabled.value) {
                            val calendar = Calendar.getInstance().apply {
                                timeInMillis = System.currentTimeMillis()
                                add(Calendar.DAY_OF_YEAR, 1)
                                val (hour, minute) = when (type) {
                                    AlarmScheduler.TYPE_GREETING_MORNING -> 8 to 30
                                    AlarmScheduler.TYPE_GREETING_AFTERNOON -> 14 to 0
                                    else -> 21 to 30
                                }
                                set(Calendar.HOUR_OF_DAY, hour)
                                set(Calendar.MINUTE, minute)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }

                            AlarmScheduler.scheduleAlarm(
                                context = context,
                                type = type,
                                id = id,
                                title = rawTitle,
                                message = rawMessage,
                                triggerAtMillis = calendar.timeInMillis,
                                repeatOption = "DAILY",
                                targetTab = "home"
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                // Safe error fallback
            } finally {
                pendingResult.finish()
            }
        }
    }
}
