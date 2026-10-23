package com.example

import com.example.data.local.Converters
import com.example.data.model.BackupData
import com.example.data.model.DocumentAttachment
import com.example.data.model.PaymentItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.*
import org.junit.Test

class PaymentAttachmentsTest {

    @Test
    fun testPaymentItemWithAttachments() {
        val attachment = DocumentAttachment(
            id = "att_123",
            fileName = "electricity_bill_august.pdf",
            filePath = "/data/user/0/com.aistudio.kaamkaaj/app_attachments/electricity_bill_august.pdf",
            mimeType = "application/pdf",
            fileSizeBytes = 1024L
        )

        val payment = PaymentItem(
            name = "Electricity Bill",
            amount = 1450.50,
            dueDateMillis = 1700000000000L,
            category = "Utility",
            attachments = listOf(attachment)
        )

        assertEquals(1, payment.attachments.size)
        assertEquals("electricity_bill_august.pdf", payment.attachments[0].fileName)
        assertEquals("application/pdf", payment.attachments[0].mimeType)
    }

    @Test
    fun testConvertersForPaymentAttachments() {
        val converters = Converters()
        val attachments = listOf(
            DocumentAttachment(
                id = "1",
                fileName = "invoice.jpg",
                filePath = "/path/to/invoice.jpg",
                mimeType = "image/jpeg",
                fileSizeBytes = 2048L
            ),
            DocumentAttachment(
                id = "2",
                fileName = "receipt.pdf",
                filePath = "/path/to/receipt.pdf",
                mimeType = "application/pdf",
                fileSizeBytes = 4096L
            )
        )

        val json = converters.fromAttachmentList(attachments)
        assertNotNull(json)
        assertTrue(json.contains("invoice.jpg"))
        assertTrue(json.contains("receipt.pdf"))

        val deserialized = converters.toAttachmentList(json)
        assertEquals(2, deserialized.size)
        assertEquals("invoice.jpg", deserialized[0].fileName)
        assertEquals("receipt.pdf", deserialized[1].fileName)
    }

    @Test
    fun testBackupDataSerializationWithPaymentAttachments() {
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        val adapter = moshi.adapter(BackupData::class.java)

        val attachment = DocumentAttachment(
            id = "pay_att_1",
            fileName = "broadband_bill.pdf",
            filePath = "/storage/broadband_bill.pdf",
            mimeType = "application/pdf",
            fileSizeBytes = 5120L
        )

        val payment = PaymentItem(
            id = 42,
            name = "Airtel Fiber",
            amount = 999.0,
            dueDateMillis = 1710000000000L,
            frequency = "MONTHLY",
            category = "Utility",
            attachments = listOf(attachment)
        )

        val backup = BackupData(
            payments = listOf(payment)
        )

        val json = adapter.toJson(backup)
        assertNotNull(json)
        assertTrue(json.contains("broadband_bill.pdf"))

        val restoredBackup = adapter.fromJson(json)
        assertNotNull(restoredBackup)
        assertEquals(1, restoredBackup!!.payments.size)
        assertEquals(1, restoredBackup.payments[0].attachments.size)
        assertEquals("broadband_bill.pdf", restoredBackup.payments[0].attachments[0].fileName)
    }
}
