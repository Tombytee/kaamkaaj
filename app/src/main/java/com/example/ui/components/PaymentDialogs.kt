package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DocumentAttachment
import com.example.data.model.PaymentItem
import com.example.ui.theme.*

val PAYMENT_CATEGORIES = listOf(
    "Utility", "Rent", "Subscription", "Insurance",
    "Loan", "Credit Card", "Tax", "Education", "Other"
)

val PAYMENT_FREQUENCIES = listOf(
    "ONE_TIME" to "One-Time",
    "WEEKLY" to "Weekly",
    "MONTHLY" to "Monthly",
    "QUARTERLY" to "Quarterly",
    "YEARLY" to "Yearly"
)

val PAYMENT_REMINDER_OPTIONS = listOf(
    0 to "No reminder",
    1 to "1 day before",
    3 to "3 days before",
    7 to "7 days before"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPaymentDialog(
    initialPayment: PaymentItem? = null,
    defaultReminderDays: Int = 3,
    onDismiss: () -> Unit,
    onSave: (PaymentItem) -> Unit
) {
    var name by remember { mutableStateOf(initialPayment?.name ?: "") }
    var amountStr by remember { mutableStateOf(initialPayment?.amount?.toString() ?: "") }
    var dueDateMillis by remember { mutableStateOf(initialPayment?.dueDateMillis ?: System.currentTimeMillis()) }
    var frequency by remember { mutableStateOf(initialPayment?.frequency ?: "ONE_TIME") }
    var category by remember { mutableStateOf(initialPayment?.category ?: PAYMENT_CATEGORIES[0]) }
    var notes by remember { mutableStateOf(initialPayment?.notes ?: "") }
    var attachments by remember { mutableStateOf(initialPayment?.attachments ?: emptyList<DocumentAttachment>()) }
    var reminderDaysBefore by remember {
        mutableIntStateOf(
            if ((initialPayment?.reminderDaysBefore ?: defaultReminderDays) >= 0)
                (initialPayment?.reminderDaysBefore ?: defaultReminderDays)
            else defaultReminderDays
        )
    }
    var reminderEnabled by remember {
        mutableStateOf(initialPayment?.let { it.reminderDaysBefore >= 0 } ?: true)
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var frequencyExpanded by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }

    AppFormDialog(
        onDismiss = onDismiss,
        title = if (initialPayment == null) "Add Payment / Bill" else "Edit Payment",
        subtitle = "Track recurring bills, due dates, and payment alerts",
        categoryBadgeText = "BILLS & PAYMENTS",
        icon = Icons.Default.AccountBalanceWallet,
        iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        iconContentColor = MaterialTheme.colorScheme.secondary,
        confirmButtonText = "SAVE PAYMENT",
        onConfirm = {
            val amount = amountStr.toDoubleOrNull()
            if (name.isBlank() || amount == null || amount <= 0) {
                hasError = true
                return@AppFormDialog
            }
            val finalReminderDays = if (reminderEnabled) reminderDaysBefore else -1
            val item = (initialPayment ?: PaymentItem(name = "", amount = 0.0, dueDateMillis = 0)).copy(
                name = name.trim(),
                amount = amount,
                dueDateMillis = dueDateMillis,
                frequency = frequency,
                category = category,
                reminderDaysBefore = finalReminderDays,
                notes = notes.trim(),
                attachments = attachments
            )
            onSave(item)
            onDismiss()
        }
    ) {
        FormSectionHeader(title = "Payment Details")

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                if (it.isNotBlank()) hasError = false
            },
            label = { Text("Payment Name *") },
            placeholder = { Text("e.g. Electricity Bill, Internet, Netflix") },
            isError = hasError && name.isBlank(),
            supportingText = if (hasError && name.isBlank()) {
                { Text("Payment name is required") }
            } else null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = amountStr,
            onValueChange = {
                amountStr = it
                if (it.isNotBlank()) hasError = false
            },
            label = { Text("Amount *") },
            placeholder = { Text("e.g. 1499.00") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            prefix = { Text("₹ ") },
            isError = hasError && (amountStr.toDoubleOrNull() == null || amountStr.toDouble() <= 0),
            supportingText = if (hasError && (amountStr.toDoubleOrNull() == null || amountStr.toDouble() <= 0)) {
                { Text("Enter a valid amount") }
            } else null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        // Category dropdown
        ExposedDropdownMenuBox(
            expanded = categoryExpanded,
            onExpandedChange = { categoryExpanded = !categoryExpanded }
        ) {
            OutlinedTextField(
                value = category,
                onValueChange = {},
                readOnly = true,
                label = { Text("Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )
            ExposedDropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false }
            ) {
                PAYMENT_CATEGORIES.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat) },
                        onClick = {
                            category = cat
                            categoryExpanded = false
                        }
                    )
                }
            }
        }

        FormSectionHeader(title = "Schedule & Frequency")

        // Due Date Trigger
        OutlinedTextField(
            value = DateUtils.formatDate(dueDateMillis),
            onValueChange = {},
            readOnly = true,
            label = { Text("Due Date") },
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.CalendarToday, contentDescription = "Select Due Date")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePicker = true },
            shape = RoundedCornerShape(16.dp)
        )

        // Frequency Dropdown
        ExposedDropdownMenuBox(
            expanded = frequencyExpanded,
            onExpandedChange = { frequencyExpanded = !frequencyExpanded }
        ) {
            OutlinedTextField(
                value = PAYMENT_FREQUENCIES.find { it.first == frequency }?.second ?: frequency,
                onValueChange = {},
                readOnly = true,
                label = { Text("Billing Frequency") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = frequencyExpanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )
            ExposedDropdownMenu(
                expanded = frequencyExpanded,
                onDismissRequest = { frequencyExpanded = false }
            ) {
                PAYMENT_FREQUENCIES.forEach { (freqKey, freqLabel) ->
                    DropdownMenuItem(
                        text = { Text(freqLabel) },
                        onClick = {
                            frequency = freqKey
                            frequencyExpanded = false
                        }
                    )
                }
            }
        }

        FormSectionHeader(title = "Notifications & Reminders")

        // Reminder Notification Section
        ReminderNotificationControl(
            enabled = reminderEnabled,
            onEnabledChange = { reminderEnabled = it },
            selectedDaysBefore = reminderDaysBefore,
            onDaysSelected = { reminderDaysBefore = it },
            options = PAYMENT_REMINDER_OPTIONS,
            itemTypeLabel = "bill is due"
        )

        FormSectionHeader(title = "Attachments & Notes")

        // Bill / Receipt / Invoice Scans & Attachments Section
        AttachmentSection(
            attachments = attachments,
            onAttachmentsChange = { attachments = it },
            documentName = name,
            isEditable = true
        )

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (Optional)") },
            placeholder = { Text("Consumer number, biller details, or autopay note...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            minLines = 2,
            maxLines = 4
        )
    }

    if (showDatePicker) {
        AppDatePickerDialog(
            initialDateMillis = dueDateMillis,
            onDateSelected = { dueDateMillis = it },
            onDismiss = { showDatePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentDetailsSheet(
    payment: PaymentItem,
    onDismiss: () -> Unit,
    onTogglePaid: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onUpdateAttachments: ((List<DocumentAttachment>) -> Unit)? = null
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    CategoryChip(category = payment.category)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = payment.name,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Normal
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                PaymentStatusBadge(isPaid = payment.isPaid, dueDateMillis = payment.dueDateMillis)
            }

            Text(
                text = "₹${String.format("%.2f", payment.amount)}",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary
            )

            HorizontalDivider(color = GlassBorderDark)

            DetailRow(label = "Due Date", value = DateUtils.formatDate(payment.dueDateMillis))

            DetailRow(
                label = "Frequency",
                value = PAYMENT_FREQUENCIES.find { it.first == payment.frequency }?.second ?: payment.frequency
            )

            // Attached Scans & Files / Bill Invoices
            AttachmentSection(
                attachments = payment.attachments,
                onAttachmentsChange = { updatedList ->
                    onUpdateAttachments?.invoke(updatedList)
                },
                documentName = payment.name,
                isEditable = onUpdateAttachments != null
            )

            // Reminder Notification Status
            val hasNotification = payment.reminderDaysBefore >= 0 && !payment.isPaid
            ReminderStatusCard(
                enabled = hasNotification,
                infoText = if (payment.isPaid) {
                    "Payment completed (no pending alerts)"
                } else if (hasNotification) {
                    if (payment.reminderDaysBefore == 0) "Alert scheduled on due date at 9:00 AM"
                    else "Alert scheduled ${payment.reminderDaysBefore} days before due date at 9:00 AM"
                } else {
                    "No notifications will be sent for this bill"
                }
            )

            if (payment.notes.isNotBlank()) {
                DetailRow(label = "Notes", value = payment.notes)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    onTogglePaid()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (payment.isPaid) Color(0xFF1E293B) else StatusActive,
                    contentColor = if (payment.isPaid) TextSecondaryDark else Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (payment.isPaid) Icons.Default.Close else Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (payment.isPaid) "MARK AS UNPAID" else "MARK AS PAID",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = StatusExpired
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StatusExpired.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("DELETE", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }

                Button(
                    onClick = {
                        onDismiss()
                        onEdit()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("EDIT", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(22.dp),
            title = {
                Text(
                    "Delete Payment?",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
                    color = StatusExpired
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete \"${payment.name}\"? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDismiss()
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusExpired)
                ) {
                    Text("DELETE", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("CANCEL", color = TextMutedDark)
                }
            }
        )
    }
}

// Alias for backwards compatibility
@Composable
fun PaymentDetailBottomSheet(
    payment: PaymentItem,
    onDismiss: () -> Unit,
    onTogglePaid: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onUpdateAttachments: ((List<DocumentAttachment>) -> Unit)? = null
) = PaymentDetailsSheet(payment, onDismiss, onTogglePaid, onEdit, onDelete, onUpdateAttachments)
