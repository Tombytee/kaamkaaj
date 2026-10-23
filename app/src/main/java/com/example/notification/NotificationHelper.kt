package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.receiver.NotificationActionReceiver

object NotificationHelper {
    const val CHANNEL_DOCUMENTS = "kaamkaaj_documents_v5"
    const val CHANNEL_PAYMENTS = "kaamkaaj_payments_v5"
    const val CHANNEL_WARRANTIES = "kaamkaaj_warranties_v5"
    const val CHANNEL_REMINDERS = "kaamkaaj_reminders_v5"
    const val CHANNEL_GREETINGS = "kaamkaaj_greetings_v5"

    private const val GROUP_KAAMKAAJ_ALERTS = "group_kaamkaaj_alerts"

    const val COLOR_GOLD = 0xFFD4AF37.toInt()
    const val COLOR_PAYMENT = 0xFF10B981.toInt()
    const val COLOR_WARRANTY = 0xFF0EA5E9.toInt()
    const val COLOR_REMINDER = 0xFFA855F7.toInt()
    const val COLOR_GREETING = 0xFFF59E0B.toInt()

    fun getChimeSoundUri(context: Context): Uri {
        return Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/raw/kaamkaaj_chime")
    }

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            // Clean up legacy channel IDs so system registers fresh channels with sound
            listOf(
                "kaamkaaj_documents", "kaamkaaj_documents_v2", "kaamkaaj_documents_v3", "kaamkaaj_documents_v4",
                "kaamkaaj_payments", "kaamkaaj_payments_v2", "kaamkaaj_payments_v3", "kaamkaaj_payments_v4",
                "kaamkaaj_warranties", "kaamkaaj_warranties_v2", "kaamkaaj_warranties_v3", "kaamkaaj_warranties_v4",
                "kaamkaaj_reminders", "kaamkaaj_reminders_v2", "kaamkaaj_reminders_v3", "kaamkaaj_reminders_v4",
                "kaamkaaj_greetings", "kaamkaaj_greetings_v2", "kaamkaaj_greetings_v3", "kaamkaaj_greetings_v4"
            ).forEach { oldChannelId ->
                try {
                    notificationManager.deleteNotificationChannel(oldChannelId)
                } catch (_: Exception) {}
            }

            // Channel Group for tidy system settings organization
            val group = NotificationChannelGroup(GROUP_KAAMKAAJ_ALERTS, "KaamKaaj Notifications")
            notificationManager.createNotificationChannelGroup(group)

            val soundUri = getChimeSoundUri(context)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            // 1. Documents Channel
            val docChannel = NotificationChannel(
                CHANNEL_DOCUMENTS,
                "Document Expiry Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Timely alerts for expiring IDs, passports, insurance & essential documents"
                setGroup(GROUP_KAAMKAAJ_ALERTS)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 180, 100, 240)
                enableLights(true)
                lightColor = COLOR_GOLD
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                setSound(soundUri, audioAttributes)
            }

            // 2. Payments Channel
            val paymentChannel = NotificationChannel(
                CHANNEL_PAYMENTS,
                "Payment Due Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent alerts for upcoming bills, utility dues, loans & subscriptions"
                setGroup(GROUP_KAAMKAAJ_ALERTS)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 100, 250, 100, 250)
                enableLights(true)
                lightColor = COLOR_PAYMENT
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                setSound(soundUri, audioAttributes)
            }

            // 3. Warranties Channel
            val warrantyChannel = NotificationChannel(
                CHANNEL_WARRANTIES,
                "Warranty Expiry Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Expiry reminders for appliance, electronics & gadget warranties"
                setGroup(GROUP_KAAMKAAJ_ALERTS)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 160, 120, 160)
                enableLights(true)
                lightColor = COLOR_WARRANTY
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                setSound(soundUri, audioAttributes)
            }

            // 4. Reminders Channel
            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Personal Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Scheduled alerts for personal tasks, events, and recurring habits"
                setGroup(GROUP_KAAMKAAJ_ALERTS)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 220, 100, 220)
                enableLights(true)
                lightColor = COLOR_REMINDER
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                setSound(soundUri, audioAttributes)
            }

            // 5. Greetings Channel
            val greetingChannel = NotificationChannel(
                CHANNEL_GREETINGS,
                "Daily Greetings",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Friendly daily morning, afternoon, and night greetings from Subham"
                setGroup(GROUP_KAAMKAAJ_ALERTS)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 140, 90, 140)
                enableLights(true)
                lightColor = COLOR_GREETING
                setShowBadge(false)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                setSound(soundUri, audioAttributes)
            }

            notificationManager.createNotificationChannels(
                listOf(docChannel, paymentChannel, warrantyChannel, reminderChannel, greetingChannel)
            )
        }
    }

    private fun getChannelSubText(channelId: String): String {
        return when (channelId) {
            CHANNEL_DOCUMENTS -> "📄 Document Expiry"
            CHANNEL_PAYMENTS -> "💳 Payment Due"
            CHANNEL_WARRANTIES -> "🛡️ Warranty Alert"
            CHANNEL_REMINDERS -> "⏰ Task Reminder"
            CHANNEL_GREETINGS -> "✨ Daily Greeting"
            else -> "KaamKaaj"
        }
    }

    private fun getChannelColor(channelId: String): Int {
        return when (channelId) {
            CHANNEL_DOCUMENTS -> COLOR_GOLD
            CHANNEL_PAYMENTS -> COLOR_PAYMENT
            CHANNEL_WARRANTIES -> COLOR_WARRANTY
            CHANNEL_REMINDERS -> COLOR_REMINDER
            CHANNEL_GREETINGS -> COLOR_GREETING
            else -> COLOR_GOLD
        }
    }

    private fun getChannelSmallIcon(channelId: String): Int {
        return when (channelId) {
            CHANNEL_DOCUMENTS -> R.drawable.ic_notif_document
            CHANNEL_PAYMENTS -> R.drawable.ic_notif_payment
            CHANNEL_WARRANTIES -> R.drawable.ic_notif_warranty
            CHANNEL_REMINDERS -> R.drawable.ic_notif_reminder
            CHANNEL_GREETINGS -> R.drawable.ic_notification_stat
            else -> R.drawable.ic_notification_stat
        }
    }

    private fun getChannelCategory(channelId: String): String {
        return when (channelId) {
            CHANNEL_DOCUMENTS -> NotificationCompat.CATEGORY_EVENT
            CHANNEL_PAYMENTS -> NotificationCompat.CATEGORY_REMINDER
            CHANNEL_WARRANTIES -> NotificationCompat.CATEGORY_STATUS
            CHANNEL_REMINDERS -> NotificationCompat.CATEGORY_REMINDER
            CHANNEL_GREETINGS -> NotificationCompat.CATEGORY_STATUS
            else -> NotificationCompat.CATEGORY_REMINDER
        }
    }

    fun showNotification(
        context: Context,
        notificationId: Int,
        channelId: String,
        title: String,
        message: String,
        targetTab: String = "home",
        itemId: Long = -1L,
        itemType: String = ""
    ) {
        // Ensure channels are created
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EXTRA_TARGET_TAB", targetTab)
            putExtra("EXTRA_ITEM_ID", itemId)
            putExtra("EXTRA_ITEM_TYPE", itemType)
        }

        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)

        val mainPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            pendingIntentFlags
        )

        val largeIcon = try {
            BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
        } catch (_: Exception) {
            null
        }

        val subText = getChannelSubText(channelId)
        val accentColor = getChannelColor(channelId)
        val smallIconRes = getChannelSmallIcon(channelId)
        val category = getChannelCategory(channelId)
        val soundUri = getChimeSoundUri(context)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(smallIconRes)
            .setLargeIcon(largeIcon)
            .setColor(accentColor)
            .setContentTitle(title)
            .setContentText(message)
            .setSubText(subText)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message)
                    .setBigContentTitle(title)
                    .setSummaryText(subText)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(category)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(mainPendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE or NotificationCompat.DEFAULT_LIGHTS)
            .setSound(soundUri)

        // Context-aware actionable buttons (without redundant "Open in KaamKaaj" / "View" buttons)
        when {
            (itemType == AlarmScheduler.TYPE_PAYMENT || channelId == CHANNEL_PAYMENTS) && itemId > 0 -> {
                val markPaidIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                    action = NotificationActionReceiver.ACTION_MARK_PAYMENT_PAID
                    putExtra(NotificationActionReceiver.EXTRA_ID, itemId)
                    putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                }
                val markPaidPending = PendingIntent.getBroadcast(
                    context,
                    notificationId * 10 + 1,
                    markPaidIntent,
                    pendingIntentFlags
                )

                val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                    action = NotificationActionReceiver.ACTION_SNOOZE
                    putExtra(NotificationActionReceiver.EXTRA_ID, itemId)
                    putExtra(NotificationActionReceiver.EXTRA_TYPE, AlarmScheduler.TYPE_PAYMENT)
                    putExtra(NotificationActionReceiver.EXTRA_TITLE, title)
                    putExtra(NotificationActionReceiver.EXTRA_MESSAGE, message)
                    putExtra(NotificationActionReceiver.EXTRA_TARGET_TAB, "payments")
                    putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                    putExtra(NotificationActionReceiver.EXTRA_SNOOZE_MILLIS, 60 * 60 * 1000L) // 1 Hour
                }
                val snoozePending = PendingIntent.getBroadcast(
                    context,
                    notificationId * 10 + 2,
                    snoozeIntent,
                    pendingIntentFlags
                )

                builder.addAction(R.drawable.ic_action_check, "Mark Paid", markPaidPending)
                builder.addAction(R.drawable.ic_action_snooze, "Snooze 1h", snoozePending)
            }

            (itemType == AlarmScheduler.TYPE_REMINDER || channelId == CHANNEL_REMINDERS) && itemId > 0 -> {
                val markDoneIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                    action = NotificationActionReceiver.ACTION_MARK_REMINDER_DONE
                    putExtra(NotificationActionReceiver.EXTRA_ID, itemId)
                    putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                }
                val markDonePending = PendingIntent.getBroadcast(
                    context,
                    notificationId * 10 + 1,
                    markDoneIntent,
                    pendingIntentFlags
                )

                val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                    action = NotificationActionReceiver.ACTION_SNOOZE
                    putExtra(NotificationActionReceiver.EXTRA_ID, itemId)
                    putExtra(NotificationActionReceiver.EXTRA_TYPE, AlarmScheduler.TYPE_REMINDER)
                    putExtra(NotificationActionReceiver.EXTRA_TITLE, title)
                    putExtra(NotificationActionReceiver.EXTRA_MESSAGE, message)
                    putExtra(NotificationActionReceiver.EXTRA_TARGET_TAB, "reminders")
                    putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                    putExtra(NotificationActionReceiver.EXTRA_SNOOZE_MILLIS, 60 * 60 * 1000L) // 1 Hour
                }
                val snoozePending = PendingIntent.getBroadcast(
                    context,
                    notificationId * 10 + 2,
                    snoozeIntent,
                    pendingIntentFlags
                )

                val tomorrowIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                    action = NotificationActionReceiver.ACTION_SNOOZE
                    putExtra(NotificationActionReceiver.EXTRA_ID, itemId)
                    putExtra(NotificationActionReceiver.EXTRA_TYPE, AlarmScheduler.TYPE_REMINDER)
                    putExtra(NotificationActionReceiver.EXTRA_TITLE, title)
                    putExtra(NotificationActionReceiver.EXTRA_MESSAGE, message)
                    putExtra(NotificationActionReceiver.EXTRA_TARGET_TAB, "reminders")
                    putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                    putExtra(NotificationActionReceiver.EXTRA_SNOOZE_MILLIS, 24 * 60 * 60 * 1000L) // 24 Hours / Tomorrow
                }
                val tomorrowPending = PendingIntent.getBroadcast(
                    context,
                    notificationId * 10 + 3,
                    tomorrowIntent,
                    pendingIntentFlags
                )

                builder.addAction(R.drawable.ic_action_check, "Mark Done", markDonePending)
                builder.addAction(R.drawable.ic_action_snooze, "Snooze 1h", snoozePending)
                builder.addAction(R.drawable.ic_action_snooze, "Tomorrow", tomorrowPending)
            }

            (itemType == AlarmScheduler.TYPE_DOCUMENT || channelId == CHANNEL_DOCUMENTS) && itemId > 0 -> {
                val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                    action = NotificationActionReceiver.ACTION_SNOOZE
                    putExtra(NotificationActionReceiver.EXTRA_ID, itemId)
                    putExtra(NotificationActionReceiver.EXTRA_TYPE, AlarmScheduler.TYPE_DOCUMENT)
                    putExtra(NotificationActionReceiver.EXTRA_TITLE, title)
                    putExtra(NotificationActionReceiver.EXTRA_MESSAGE, message)
                    putExtra(NotificationActionReceiver.EXTRA_TARGET_TAB, "documents")
                    putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                    putExtra(NotificationActionReceiver.EXTRA_SNOOZE_MILLIS, 24 * 60 * 60 * 1000L) // 1 Day
                }
                val snoozePending = PendingIntent.getBroadcast(
                    context,
                    notificationId * 10 + 2,
                    snoozeIntent,
                    pendingIntentFlags
                )

                builder.addAction(R.drawable.ic_action_snooze, "Remind Tomorrow", snoozePending)
            }

            (itemType == AlarmScheduler.TYPE_WARRANTY || channelId == CHANNEL_WARRANTIES) && itemId > 0 -> {
                val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                    action = NotificationActionReceiver.ACTION_SNOOZE
                    putExtra(NotificationActionReceiver.EXTRA_ID, itemId)
                    putExtra(NotificationActionReceiver.EXTRA_TYPE, AlarmScheduler.TYPE_WARRANTY)
                    putExtra(NotificationActionReceiver.EXTRA_TITLE, title)
                    putExtra(NotificationActionReceiver.EXTRA_MESSAGE, message)
                    putExtra(NotificationActionReceiver.EXTRA_TARGET_TAB, "warranties")
                    putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                    putExtra(NotificationActionReceiver.EXTRA_SNOOZE_MILLIS, 24 * 60 * 60 * 1000L) // 1 Day
                }
                val snoozePending = PendingIntent.getBroadcast(
                    context,
                    notificationId * 10 + 2,
                    snoozeIntent,
                    pendingIntentFlags
                )

                builder.addAction(R.drawable.ic_action_snooze, "Remind Tomorrow", snoozePending)
            }
        }

        try {
            val manager = NotificationManagerCompat.from(context)
            if (manager.areNotificationsEnabled()) {
                manager.notify(notificationId, builder.build())
            }
        } catch (_: SecurityException) {
            // Handled when notification permission is not granted on API 33+
        }
    }
}
