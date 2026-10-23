package com.example

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.KaamKaajDatabase
import com.example.data.model.PaymentItem
import com.example.data.model.ReminderItem
import com.example.data.preferences.UserPreferences
import com.example.notification.AlarmScheduler
import com.example.notification.NotificationHelper
import com.example.receiver.NotificationActionReceiver
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNotificationManager
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class NotificationSystemTest {

    @Test
    fun `notification channels are properly initialized with categories including greetings`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        NotificationHelper.createNotificationChannels(context)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channels = notificationManager.notificationChannels

        assertTrue("Expected at least 5 notification channels", channels.size >= 5)
        val channelIds = channels.map { it.id }
        assertTrue(channelIds.contains(NotificationHelper.CHANNEL_DOCUMENTS))
        assertTrue(channelIds.contains(NotificationHelper.CHANNEL_PAYMENTS))
        assertTrue(channelIds.contains(NotificationHelper.CHANNEL_WARRANTIES))
        assertTrue(channelIds.contains(NotificationHelper.CHANNEL_REMINDERS))
        assertTrue(channelIds.contains(NotificationHelper.CHANNEL_GREETINGS))

        val greetingsChannel = notificationManager.getNotificationChannel(NotificationHelper.CHANNEL_GREETINGS)
        assertNotNull(greetingsChannel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, greetingsChannel.importance)

        val paymentChannel = notificationManager.getNotificationChannel(NotificationHelper.CHANNEL_PAYMENTS)
        assertNotNull(paymentChannel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, paymentChannel.importance)
        assertTrue(paymentChannel.shouldVibrate())
    }

    @Test
    fun `showNotification builds and posts notification without redundant open in kaamkaaj button`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNm: ShadowNotificationManager = shadowOf(notificationManager)

        // 1. Test greeting notification - should have no action buttons, only content intent
        NotificationHelper.showNotification(
            context = context,
            notificationId = 9001,
            channelId = NotificationHelper.CHANNEL_GREETINGS,
            title = "🌅 Good Morning!",
            message = "Good morning from Subham! Have a productive day ahead.",
            targetTab = "home"
        )

        val notifications = shadowNm.allNotifications
        assertTrue("Expected at least 1 posted notification", notifications.isNotEmpty())

        val greetingNotification = notifications.find { it.channelId == NotificationHelper.CHANNEL_GREETINGS }
        assertNotNull(greetingNotification)
        assertNotNull("Notification should have content intent on tap", greetingNotification!!.contentIntent)
        assertTrue("Greeting should not have redundant action buttons", greetingNotification.actions == null || greetingNotification.actions.isEmpty())

        // 2. Test payment notification - should have inline action button 'Mark as Paid'
        NotificationHelper.showNotification(
            context = context,
            notificationId = 501,
            channelId = NotificationHelper.CHANNEL_PAYMENTS,
            title = "💳 Electricity Bill Due",
            message = "Electricity bill of ₹1,450 is due tomorrow.",
            targetTab = "payments",
            itemId = 42L,
            itemType = AlarmScheduler.TYPE_PAYMENT
        )

        val paymentNotification = shadowNm.allNotifications.find { it.channelId == NotificationHelper.CHANNEL_PAYMENTS }
        assertNotNull(paymentNotification)
        assertTrue(paymentNotification!!.actions.isNotEmpty())
        assertEquals("Mark Paid", paymentNotification.actions[0].title.toString())
    }

    @Test
    fun `notification action receiver marks payment as paid`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = KaamKaajDatabase.getDatabase(context)
        val paymentDao = db.paymentDao()

        val paymentId = paymentDao.insertPayment(
            PaymentItem(
                name = "Broadband Bill",
                amount = 999.0,
                dueDateMillis = System.currentTimeMillis() + 86400000L,
                category = "Utilities",
                isPaid = false
            )
        )

        val receiver = NotificationActionReceiver()
        val intent = Intent(NotificationActionReceiver.ACTION_MARK_PAYMENT_PAID).apply {
            putExtra(NotificationActionReceiver.EXTRA_ID, paymentId)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, 1001)
        }

        receiver.onReceive(context, intent)
        kotlinx.coroutines.delay(200)

        val updated = paymentDao.getPaymentById(paymentId)
        assertNotNull(updated)
        assertTrue("Payment should be marked as paid", updated!!.isPaid)
    }

    @Test
    fun `notification action receiver marks reminder as completed`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = KaamKaajDatabase.getDatabase(context)
        val reminderDao = db.reminderDao()

        val reminderId = reminderDao.insertReminder(
            ReminderItem(
                title = "Renew Vehicle Insurance",
                description = "Visit portal to renew",
                dueDateTimeMillis = System.currentTimeMillis() + 86400000L,
                isCompleted = false
            )
        )

        val receiver = NotificationActionReceiver()
        val intent = Intent(NotificationActionReceiver.ACTION_MARK_REMINDER_DONE).apply {
            putExtra(NotificationActionReceiver.EXTRA_ID, reminderId)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, 1002)
        }

        receiver.onReceive(context, intent)
        kotlinx.coroutines.delay(200)

        val updated = reminderDao.getReminderById(reminderId)
        assertNotNull(updated)
        assertTrue("Reminder should be marked as completed", updated!!.isCompleted)
    }

    @Test
    fun `daily greetings preferences toggle and schedule properly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val userPrefs = UserPreferences(context)

        userPrefs.setDailyGreetingsEnabled(true)
        assertTrue(userPrefs.dailyGreetingsEnabled.value)

        AlarmScheduler.scheduleDailyGreetings(context)

        userPrefs.setDailyGreetingsEnabled(false)
        AlarmScheduler.scheduleDailyGreetings(context)
        assertTrue(!userPrefs.dailyGreetingsEnabled.value)
    }

    @Test
    fun `calculateDateTriggerTime calculates correct reminder timestamps`() {
        val targetDateMillis = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000)
        val triggerTime = AlarmScheduler.calculateDateTriggerTime(
            targetDateMillis = targetDateMillis,
            daysBefore = 2,
            hourOfDay = 9,
            minute = 0
        )

        assertTrue("Trigger time should be in the future", triggerTime > System.currentTimeMillis())
        assertTrue("Trigger time should be before the target date", triggerTime < targetDateMillis)
    }

    @Test
    fun `getChimeSoundUri generates valid resource string format`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val soundUri = NotificationHelper.getChimeSoundUri(context)

        assertEquals("android.resource://${context.packageName}/raw/kaamkaaj_chime", soundUri.toString())
    }

    @Test
    fun `notification channels are configured with chime sound`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        NotificationHelper.createNotificationChannels(context)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val docChannel = notificationManager.getNotificationChannel(NotificationHelper.CHANNEL_DOCUMENTS)
        assertNotNull(docChannel)
        assertNotNull("Channel sound should be configured", docChannel.sound)
        assertEquals(NotificationHelper.getChimeSoundUri(context), docChannel.sound)
    }
}
