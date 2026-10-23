package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.local.KaamKaajDatabase
import com.example.data.preferences.UserPreferences
import com.example.receiver.NotificationAlarmReceiver
import java.util.Calendar

object AlarmScheduler {
    const val TYPE_DOCUMENT = "DOCUMENT"
    const val TYPE_PAYMENT = "PAYMENT"
    const val TYPE_WARRANTY = "WARRANTY"
    const val TYPE_REMINDER = "REMINDER"
    const val TYPE_GREETING_MORNING = "GREETING_MORNING"
    const val TYPE_GREETING_AFTERNOON = "GREETING_AFTERNOON"
    const val TYPE_GREETING_NIGHT = "GREETING_NIGHT"

    const val ID_GREETING_MORNING = 9001L
    const val ID_GREETING_AFTERNOON = 9002L
    const val ID_GREETING_NIGHT = 9003L

    private fun getRequestCode(type: String, id: Long): Int {
        val prefix = when (type) {
            TYPE_DOCUMENT -> 1_000_000
            TYPE_PAYMENT -> 2_000_000
            TYPE_WARRANTY -> 3_000_000
            TYPE_REMINDER -> 4_000_000
            TYPE_GREETING_MORNING -> 6_000_000
            TYPE_GREETING_AFTERNOON -> 7_000_000
            TYPE_GREETING_NIGHT -> 8_000_000
            else -> 5_000_000
        }
        return prefix + (id % 900_000).toInt()
    }

    /**
     * Calculates the trigger timestamp in milliseconds for date-based events (Documents, Payments, Warranties)
     * by subtracting [daysBefore] from [targetDateMillis] and setting the alarm to [hourOfDay]:[minute] (default 9:00 AM).
     */
    fun calculateDateTriggerTime(
        targetDateMillis: Long,
        daysBefore: Int,
        hourOfDay: Int = 9,
        minute: Int = 0
    ): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = targetDateMillis
            add(Calendar.DAY_OF_YEAR, -daysBefore)
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val trigger = cal.timeInMillis
        val now = System.currentTimeMillis()

        // If calculated 9:00 AM on reminder date has already passed today,
        // but the actual expiry/due date is in the future, trigger in 1 minute
        if (trigger <= now) {
            val targetCal = Calendar.getInstance().apply {
                timeInMillis = targetDateMillis
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }
            if (targetCal.timeInMillis > now && daysBefore == 0) {
                return now + (60 * 1000) // 1 minute from now
            }
            return 0L
        }
        return trigger
    }

    fun scheduleAlarm(
        context: Context,
        type: String,
        id: Long,
        title: String,
        message: String,
        triggerAtMillis: Long,
        repeatOption: String = "NONE",
        targetTab: String = "home"
    ) {
        if (triggerAtMillis <= System.currentTimeMillis()) {
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val requestCode = getRequestCode(type, id)

        val intent = Intent(context, NotificationAlarmReceiver::class.java).apply {
            putExtra("EXTRA_TYPE", type)
            putExtra("EXTRA_ID", id)
            putExtra("EXTRA_TITLE", title)
            putExtra("EXTRA_MESSAGE", message)
            putExtra("EXTRA_REPEAT", repeatOption)
            putExtra("EXTRA_TARGET_TAB", targetTab)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } catch (_: Exception) {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        }
    }

    fun cancelAlarm(context: Context, type: String, id: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val requestCode = getRequestCode(type, id)

        val intent = Intent(context, NotificationAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    /**
     * Schedules the 3 daily greeting notifications (Morning, Afternoon, Night) locally.
     */
    fun scheduleDailyGreetings(context: Context) {
        val userPrefs = UserPreferences(context)
        if (!userPrefs.notificationsEnabled.value || !userPrefs.dailyGreetingsEnabled.value) {
            cancelAlarm(context, TYPE_GREETING_MORNING, ID_GREETING_MORNING)
            cancelAlarm(context, TYPE_GREETING_AFTERNOON, ID_GREETING_AFTERNOON)
            cancelAlarm(context, TYPE_GREETING_NIGHT, ID_GREETING_NIGHT)
            return
        }

        // 1. Morning greeting (08:30 AM)
        scheduleDailySlot(
            context = context,
            type = TYPE_GREETING_MORNING,
            id = ID_GREETING_MORNING,
            hour = 8,
            minute = 30,
            title = "🌅 Good Morning!",
            message = "Good morning from Subham! Have a productive and wonderful day ahead."
        )

        // 2. Afternoon greeting (02:00 PM / 14:00)
        scheduleDailySlot(
            context = context,
            type = TYPE_GREETING_AFTERNOON,
            id = ID_GREETING_AFTERNOON,
            hour = 14,
            minute = 0,
            title = "☀️ Good Afternoon!",
            message = "Good afternoon from Subham! Hope your day is going smoothly and well."
        )

        // 3. Night greeting (09:30 PM / 21:30)
        scheduleDailySlot(
            context = context,
            type = TYPE_GREETING_NIGHT,
            id = ID_GREETING_NIGHT,
            hour = 21,
            minute = 30,
            title = "🌙 Good Night!",
            message = "Good night from Subham! Rest well and have a peaceful night."
        )
    }

    private fun scheduleDailySlot(
        context: Context,
        type: String,
        id: Long,
        hour: Int,
        minute: Int,
        title: String,
        message: String
    ) {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (cal.timeInMillis <= now) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        scheduleAlarm(
            context = context,
            type = type,
            id = id,
            title = title,
            message = message,
            triggerAtMillis = cal.timeInMillis,
            repeatOption = "DAILY",
            targetTab = "home"
        )
    }

    suspend fun rescheduleAllAlarms(context: Context) {
        val db = KaamKaajDatabase.getDatabase(context)
        val now = System.currentTimeMillis()

        // 1. Documents
        val docs = db.documentDao().getAllDocumentsList()
        docs.forEach { doc ->
            if (doc.expiryDateMillis != null && doc.reminderDaysBefore >= 0) {
                val trigger = calculateDateTriggerTime(doc.expiryDateMillis, doc.reminderDaysBefore)
                if (trigger > now) {
                    val daysText = if (doc.reminderDaysBefore == 0) "today" else "in ${doc.reminderDaysBefore} days"
                    scheduleAlarm(
                        context = context,
                        type = TYPE_DOCUMENT,
                        id = doc.id,
                        title = "📄 Document Expiring: ${doc.name}",
                        message = "${doc.name} (${doc.category}) is expiring $daysText.",
                        triggerAtMillis = trigger,
                        targetTab = "documents"
                    )
                }
            }
        }

        // 2. Payments
        val payments = db.paymentDao().getAllPaymentsList()
        payments.forEach { payment ->
            if (!payment.isPaid && payment.reminderDaysBefore >= 0) {
                val trigger = calculateDateTriggerTime(payment.dueDateMillis, payment.reminderDaysBefore)
                if (trigger > now) {
                    val daysText = if (payment.reminderDaysBefore == 0) "due today" else "due in ${payment.reminderDaysBefore} days"
                    scheduleAlarm(
                        context = context,
                        type = TYPE_PAYMENT,
                        id = payment.id,
                        title = "💳 Payment Due: ${payment.name}",
                        message = "${payment.name} amount ₹${payment.amount} is $daysText (${payment.category}).",
                        triggerAtMillis = trigger,
                        targetTab = "payments"
                    )
                }
            }
        }

        // 3. Warranties
        val warranties = db.warrantyDao().getAllWarrantiesList()
        warranties.forEach { warranty ->
            if (warranty.reminderDaysBefore >= 0) {
                val trigger = calculateDateTriggerTime(warranty.expiryDateMillis, warranty.reminderDaysBefore)
                if (trigger > now) {
                    val daysText = if (warranty.reminderDaysBefore == 0) "today" else "in ${warranty.reminderDaysBefore} days"
                    scheduleAlarm(
                        context = context,
                        type = TYPE_WARRANTY,
                        id = warranty.id,
                        title = "🛡️ Warranty Expiring: ${warranty.productName}",
                        message = "Warranty for ${warranty.productName} (${warranty.brand}) expires $daysText.",
                        triggerAtMillis = trigger,
                        targetTab = "warranties"
                    )
                }
            }
        }

        // 4. Reminders
        val reminders = db.reminderDao().getAllRemindersList()
        reminders.forEach { reminder ->
            if (!reminder.isCompleted && reminder.dueDateTimeMillis > now) {
                scheduleAlarm(
                    context = context,
                    type = TYPE_REMINDER,
                    id = reminder.id,
                    title = "⏰ Reminder: ${reminder.title}",
                    message = reminder.description.ifBlank { "You have a scheduled task reminder." },
                    triggerAtMillis = reminder.dueDateTimeMillis,
                    repeatOption = reminder.repeatOption,
                    targetTab = "reminders"
                )
            }
        }

        // 5. Daily Greetings
        scheduleDailyGreetings(context)
    }
}
