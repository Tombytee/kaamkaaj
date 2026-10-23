package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DocumentAttachment(
    val id: String = java.util.UUID.randomUUID().toString(),
    val fileName: String,
    val filePath: String,
    val mimeType: String = "image/jpeg",
    val fileSizeBytes: Long = 0L,
    val addedAtMillis: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "documents")
data class DocumentItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String, // Identity, Vehicle, Property, Education, Health, Financial, Employment, Other
    val documentNumber: String = "",
    val issueDateMillis: Long? = null,
    val expiryDateMillis: Long? = null,
    val reminderDaysBefore: Int = 30, // 0 = None, 1, 7, 15, 30
    val notes: String = "",
    val attachments: List<DocumentAttachment> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "payments")
data class PaymentItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val amount: Double,
    val dueDateMillis: Long,
    val frequency: String = "ONE_TIME", // ONE_TIME, WEEKLY, MONTHLY, QUARTERLY, YEARLY
    val category: String = "Utility", // Utility, Rent, Subscription, Insurance, Loan, Credit Card, Tax, Other
    val notes: String = "",
    val isPaid: Boolean = false,
    val reminderDaysBefore: Int = 3, // 0 = None, 1, 3, 7
    val attachments: List<DocumentAttachment> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "warranties")
data class WarrantyItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productName: String,
    val brand: String = "",
    val purchaseDateMillis: Long,
    val durationMonths: Int,
    val expiryDateMillis: Long,
    val purchasePrice: Double? = null,
    val notes: String = "",
    val reminderDaysBefore: Int = 15, // 0 = None, 7, 15, 30
    val createdAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "reminders")
data class ReminderItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val dueDateTimeMillis: Long,
    val repeatOption: String = "NONE", // NONE, DAILY, WEEKDAYS, WEEKENDS, WEEKLY, BIWEEKLY, MONTHLY, YEARLY
    val isCompleted: Boolean = false,
    val completedAtMillis: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class BackupData(
    val version: Int = 1,
    val exportDateMillis: Long = System.currentTimeMillis(),
    val documents: List<DocumentItem> = emptyList(),
    val payments: List<PaymentItem> = emptyList(),
    val warranties: List<WarrantyItem> = emptyList(),
    val reminders: List<ReminderItem> = emptyList()
)
