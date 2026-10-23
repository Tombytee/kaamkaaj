package com.example.data.repository

import android.content.Context
import com.example.data.local.KaamKaajDatabase
import com.example.data.model.DocumentItem
import com.example.data.model.PaymentItem
import com.example.data.model.ReminderItem
import com.example.data.model.WarrantyItem
import com.example.notification.AlarmScheduler
import kotlinx.coroutines.flow.Flow

class KaamKaajRepository(
    private val context: Context,
    private val database: KaamKaajDatabase
) {
    private val documentDao = database.documentDao()
    private val paymentDao = database.paymentDao()
    private val warrantyDao = database.warrantyDao()
    private val reminderDao = database.reminderDao()

    // --- Documents ---
    val allDocuments: Flow<List<DocumentItem>> = documentDao.getAllDocuments()

    fun getExpiringSoonDocuments(daysThreshold: Int = 30): Flow<List<DocumentItem>> {
        val now = System.currentTimeMillis()
        val threshold = now + (daysThreshold.toLong() * 24 * 60 * 60 * 1000)
        return documentDao.getExpiringSoonDocuments(now, threshold)
    }

    fun searchDocuments(query: String): Flow<List<DocumentItem>> = documentDao.searchDocuments(query)

    suspend fun saveDocument(document: DocumentItem) {
        val id = if (document.id == 0L) {
            documentDao.insertDocument(document)
        } else {
            documentDao.updateDocument(document)
            document.id
        }

        // Cancel old alarm
        AlarmScheduler.cancelAlarm(context, AlarmScheduler.TYPE_DOCUMENT, id)

        // Schedule new alarm if configured
        if (document.expiryDateMillis != null && document.reminderDaysBefore >= 0) {
            val triggerTime = AlarmScheduler.calculateDateTriggerTime(
                targetDateMillis = document.expiryDateMillis,
                daysBefore = document.reminderDaysBefore
            )
            if (triggerTime > System.currentTimeMillis()) {
                val daysText = if (document.reminderDaysBefore == 0) "today" else "in ${document.reminderDaysBefore} days"
                AlarmScheduler.scheduleAlarm(
                    context = context,
                    type = AlarmScheduler.TYPE_DOCUMENT,
                    id = id,
                    title = "📄 Document Expiring: ${document.name}",
                    message = "${document.name} (${document.category}) is expiring $daysText.",
                    triggerAtMillis = triggerTime,
                    targetTab = "documents"
                )
            }
        }
    }

    suspend fun deleteDocument(document: DocumentItem) {
        AlarmScheduler.cancelAlarm(context, AlarmScheduler.TYPE_DOCUMENT, document.id)
        document.attachments.forEach { att ->
            com.example.data.storage.AttachmentStorageManager.deleteAttachmentFile(att.filePath)
        }
        documentDao.deleteDocument(document)
    }

    // --- Payments ---
    val allPayments: Flow<List<PaymentItem>> = paymentDao.getAllPayments()

    fun getUpcomingPayments(daysThreshold: Int = 7): Flow<List<PaymentItem>> {
        val now = System.currentTimeMillis()
        val threshold = now + (daysThreshold.toLong() * 24 * 60 * 60 * 1000)
        return paymentDao.getUpcomingPayments(threshold)
    }

    fun searchPayments(query: String): Flow<List<PaymentItem>> = paymentDao.searchPayments(query)

    suspend fun savePayment(payment: PaymentItem) {
        val id = if (payment.id == 0L) {
            paymentDao.insertPayment(payment)
        } else {
            paymentDao.updatePayment(payment)
            payment.id
        }

        // Cancel old alarm
        AlarmScheduler.cancelAlarm(context, AlarmScheduler.TYPE_PAYMENT, id)

        // Schedule new alarm if not paid
        if (!payment.isPaid && payment.reminderDaysBefore >= 0) {
            val triggerTime = AlarmScheduler.calculateDateTriggerTime(
                targetDateMillis = payment.dueDateMillis,
                daysBefore = payment.reminderDaysBefore
            )
            if (triggerTime > System.currentTimeMillis()) {
                val daysText = if (payment.reminderDaysBefore == 0) "due today" else "due in ${payment.reminderDaysBefore} days"
                AlarmScheduler.scheduleAlarm(
                    context = context,
                    type = AlarmScheduler.TYPE_PAYMENT,
                    id = id,
                    title = "💳 Payment Due: ${payment.name}",
                    message = "${payment.name} amount ₹${payment.amount} is $daysText (${payment.category}).",
                    triggerAtMillis = triggerTime,
                    targetTab = "payments"
                )
            }
        }
    }

    suspend fun togglePaymentPaid(payment: PaymentItem) {
        val updated = payment.copy(isPaid = !payment.isPaid)
        savePayment(updated)
    }

    suspend fun deletePayment(payment: PaymentItem) {
        AlarmScheduler.cancelAlarm(context, AlarmScheduler.TYPE_PAYMENT, payment.id)
        payment.attachments.forEach { att ->
            com.example.data.storage.AttachmentStorageManager.deleteAttachmentFile(att.filePath)
        }
        paymentDao.deletePayment(payment)
    }

    // --- Warranties ---
    val allWarranties: Flow<List<WarrantyItem>> = warrantyDao.getAllWarranties()

    fun getExpiringSoonWarranties(daysThreshold: Int = 30): Flow<List<WarrantyItem>> {
        val now = System.currentTimeMillis()
        val threshold = now + (daysThreshold.toLong() * 24 * 60 * 60 * 1000)
        return warrantyDao.getExpiringSoonWarranties(now, threshold)
    }

    fun searchWarranties(query: String): Flow<List<WarrantyItem>> = warrantyDao.searchWarranties(query)

    suspend fun saveWarranty(warranty: WarrantyItem) {
        val id = if (warranty.id == 0L) {
            warrantyDao.insertWarranty(warranty)
        } else {
            warrantyDao.updateWarranty(warranty)
            warranty.id
        }

        // Cancel old alarm
        AlarmScheduler.cancelAlarm(context, AlarmScheduler.TYPE_WARRANTY, id)

        // Schedule new alarm
        if (warranty.reminderDaysBefore >= 0) {
            val triggerTime = AlarmScheduler.calculateDateTriggerTime(
                targetDateMillis = warranty.expiryDateMillis,
                daysBefore = warranty.reminderDaysBefore
            )
            if (triggerTime > System.currentTimeMillis()) {
                val daysText = if (warranty.reminderDaysBefore == 0) "today" else "in ${warranty.reminderDaysBefore} days"
                AlarmScheduler.scheduleAlarm(
                    context = context,
                    type = AlarmScheduler.TYPE_WARRANTY,
                    id = id,
                    title = "🛡️ Warranty Expiring: ${warranty.productName}",
                    message = "Warranty for ${warranty.productName} (${warranty.brand}) expires $daysText.",
                    triggerAtMillis = triggerTime,
                    targetTab = "warranties"
                )
            }
        }
    }

    suspend fun deleteWarranty(warranty: WarrantyItem) {
        AlarmScheduler.cancelAlarm(context, AlarmScheduler.TYPE_WARRANTY, warranty.id)
        warrantyDao.deleteWarranty(warranty)
    }

    // --- Reminders ---
    val allReminders: Flow<List<ReminderItem>> = reminderDao.getAllReminders()

    fun getUpcomingReminders(daysThreshold: Int = 3): Flow<List<ReminderItem>> {
        val now = System.currentTimeMillis()
        val threshold = now + (daysThreshold.toLong() * 24 * 60 * 60 * 1000)
        return reminderDao.getUpcomingReminders(threshold)
    }

    fun searchReminders(query: String): Flow<List<ReminderItem>> = reminderDao.searchReminders(query)

    suspend fun saveReminder(reminder: ReminderItem) {
        val id = if (reminder.id == 0L) {
            reminderDao.insertReminder(reminder)
        } else {
            reminderDao.updateReminder(reminder)
            reminder.id
        }

        // Cancel old alarm
        AlarmScheduler.cancelAlarm(context, AlarmScheduler.TYPE_REMINDER, id)

        // Schedule new alarm if not completed and in future
        if (!reminder.isCompleted && reminder.dueDateTimeMillis > System.currentTimeMillis()) {
            AlarmScheduler.scheduleAlarm(
                context = context,
                type = AlarmScheduler.TYPE_REMINDER,
                id = id,
                title = "⏰ Reminder: ${reminder.title}",
                message = reminder.description.ifBlank { "You scheduled this reminder." },
                triggerAtMillis = reminder.dueDateTimeMillis,
                repeatOption = reminder.repeatOption,
                targetTab = "reminders"
            )
        }
    }

    suspend fun toggleReminderCompleted(reminder: ReminderItem) {
        if (!reminder.isCompleted) {
            // Completing the reminder
            if (reminder.repeatOption != "NONE" && reminder.repeatOption.isNotBlank()) {
                // Repeating reminder: advance schedule to next recurrence and keep active for next alert
                val nextDue = com.example.notification.ReminderSchedulerUtils.calculateNextOccurrence(
                    baseMillis = reminder.dueDateTimeMillis,
                    repeatOption = reminder.repeatOption
                )
                val updated = reminder.copy(
                    dueDateTimeMillis = nextDue,
                    isCompleted = false,
                    completedAtMillis = System.currentTimeMillis()
                )
                saveReminder(updated)
            } else {
                // One-time reminder: mark completed with timestamp
                val updated = reminder.copy(
                    isCompleted = true,
                    completedAtMillis = System.currentTimeMillis()
                )
                saveReminder(updated)
            }
        } else {
            // Re-activating a completed reminder
            val updated = reminder.copy(
                isCompleted = false,
                completedAtMillis = null
            )
            saveReminder(updated)
        }
    }

    suspend fun rescheduleReminder(reminder: ReminderItem, newDueDateTimeMillis: Long) {
        val updated = reminder.copy(
            dueDateTimeMillis = newDueDateTimeMillis,
            isCompleted = false,
            completedAtMillis = null
        )
        saveReminder(updated)
    }

    suspend fun deleteReminder(reminder: ReminderItem) {
        AlarmScheduler.cancelAlarm(context, AlarmScheduler.TYPE_REMINDER, reminder.id)
        reminderDao.deleteReminder(reminder)
    }

    // --- Clear All ---
    suspend fun clearAllData() {
        val docs = documentDao.getAllDocumentsList()
        val pays = paymentDao.getAllPaymentsList()
        val wars = warrantyDao.getAllWarrantiesList()
        val rems = reminderDao.getAllRemindersList()

        docs.forEach { AlarmScheduler.cancelAlarm(context, AlarmScheduler.TYPE_DOCUMENT, it.id) }
        pays.forEach { AlarmScheduler.cancelAlarm(context, AlarmScheduler.TYPE_PAYMENT, it.id) }
        wars.forEach { AlarmScheduler.cancelAlarm(context, AlarmScheduler.TYPE_WARRANTY, it.id) }
        rems.forEach { AlarmScheduler.cancelAlarm(context, AlarmScheduler.TYPE_REMINDER, it.id) }

        documentDao.clearAllDocuments()
        paymentDao.clearAllPayments()
        warrantyDao.clearAllWarranties()
        reminderDao.clearAllReminders()
    }

    suspend fun rescheduleAllNotifications() {
        AlarmScheduler.rescheduleAllAlarms(context)
    }
}
