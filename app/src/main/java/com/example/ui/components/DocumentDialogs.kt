package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DocumentAttachment
import com.example.data.model.DocumentItem
import com.example.ui.theme.*

val DOCUMENT_CATEGORIES = listOf(
    "Identity / ID Card",
    "Passport / Visa",
    "Driving License",
    "Vehicle RC & Insurance",
    "Health / Insurance",
    "Financial / Tax / PAN",
    "Certificates / Degrees",
    "Property / Rent Agreement",
    "Other"
)

val DOCUMENT_REMINDER_OPTIONS = listOf(
    0 to "No reminder",
    1 to "1 day before",
    7 to "7 days before",
    15 to "15 days before",
    30 to "30 days before",
    60 to "60 days before",
    90 to "90 days before"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditDocumentDialog(
    initialDocument: DocumentItem? = null,
    defaultReminderDays: Int = 30,
    onDismiss: () -> Unit,
    onSave: (DocumentItem) -> Unit
) {
    var name by remember { mutableStateOf(initialDocument?.name ?: "") }
    var category by remember { mutableStateOf(initialDocument?.category ?: DOCUMENT_CATEGORIES[0]) }
    var documentNumber by remember { mutableStateOf(initialDocument?.documentNumber ?: "") }
    var issueDateMillis by remember { mutableStateOf(initialDocument?.issueDateMillis) }
    var expiryDateMillis by remember { mutableStateOf(initialDocument?.expiryDateMillis) }
    var reminderDaysBefore by remember {
        mutableIntStateOf(
            if ((initialDocument?.reminderDaysBefore ?: defaultReminderDays) >= 0)
                (initialDocument?.reminderDaysBefore ?: defaultReminderDays)
            else defaultReminderDays
        )
    }
    var reminderEnabled by remember {
        mutableStateOf(initialDocument?.let { it.reminderDaysBefore >= 0 } ?: true)
    }
    var notes by remember { mutableStateOf(initialDocument?.notes ?: "") }
    var attachments by remember { mutableStateOf(initialDocument?.attachments ?: emptyList<DocumentAttachment>()) }

    var showIssueDatePicker by remember { mutableStateOf(false) }
    var showExpiryDatePicker by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }

    AppFormDialog(
        onDismiss = onDismiss,
        title = if (initialDocument == null) "Add Document" else "Edit Document",
        subtitle = "Store official IDs, certificates, and renewal alerts",
        categoryBadgeText = "DOCUMENTS & RECORDS",
        icon = Icons.Default.FolderSpecial,
        iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
        iconContentColor = MaterialTheme.colorScheme.primary,
        confirmButtonText = "SAVE DOCUMENT",
        onConfirm = {
            if (name.isBlank()) {
                hasError = true
                return@AppFormDialog
            }
            val finalReminderDays = if (expiryDateMillis != null && reminderEnabled) {
                reminderDaysBefore
            } else {
                -1
            }
            val doc = (initialDocument ?: DocumentItem(name = "", category = "")).copy(
                name = name.trim(),
                category = category,
                documentNumber = documentNumber.trim(),
                issueDateMillis = issueDateMillis,
                expiryDateMillis = expiryDateMillis,
                reminderDaysBefore = finalReminderDays,
                notes = notes.trim(),
                attachments = attachments
            )
            onSave(doc)
            onDismiss()
        }
    ) {
        FormSectionHeader(title = "Document Details")

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                if (it.isNotBlank()) hasError = false
            },
            label = { Text("Document Name *") },
            placeholder = { Text("e.g. Passport, Driving License, National ID") },
            isError = hasError && name.isBlank(),
            supportingText = if (hasError && name.isBlank()) {
                { Text("Document name is required") }
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
                DOCUMENT_CATEGORIES.forEach { cat ->
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

        OutlinedTextField(
            value = documentNumber,
            onValueChange = { documentNumber = it },
            label = { Text("Document Number / ID") },
            placeholder = { Text("e.g. Z1234567, DL-042011001") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        FormSectionHeader(title = "Expiry & Reminders")

        // Issue Date Picker Trigger
        OutlinedTextField(
            value = DateUtils.formatDate(issueDateMillis),
            onValueChange = {},
            readOnly = true,
            label = { Text("Issue Date") },
            trailingIcon = {
                IconButton(onClick = { showIssueDatePicker = true }) {
                    Icon(Icons.Default.CalendarToday, contentDescription = "Select Issue Date")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showIssueDatePicker = true },
            shape = RoundedCornerShape(16.dp)
        )

        // Expiry Date Picker Trigger
        OutlinedTextField(
            value = DateUtils.formatDate(expiryDateMillis),
            onValueChange = {},
            readOnly = true,
            label = { Text("Expiry Date") },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (expiryDateMillis != null) {
                        IconButton(onClick = { expiryDateMillis = null }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Expiry")
                        }
                    }
                    IconButton(onClick = { showExpiryDatePicker = true }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Select Expiry Date")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showExpiryDatePicker = true },
            shape = RoundedCornerShape(16.dp)
        )

        // Reminder Notification Section
        if (expiryDateMillis != null) {
            ReminderNotificationControl(
                enabled = reminderEnabled,
                onEnabledChange = { reminderEnabled = it },
                selectedDaysBefore = reminderDaysBefore,
                onDaysSelected = { reminderDaysBefore = it },
                options = DOCUMENT_REMINDER_OPTIONS,
                itemTypeLabel = "document expires"
            )
        }

        FormSectionHeader(title = "Attachments & Notes")

        // Document Scans & Attachments Section
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
            placeholder = { Text("Add issuance authority, locker number, or extra notes...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            minLines = 2,
            maxLines = 4
        )
    }

    if (showIssueDatePicker) {
        AppDatePickerDialog(
            initialDateMillis = issueDateMillis ?: System.currentTimeMillis(),
            onDateSelected = { issueDateMillis = it },
            onDismiss = { showIssueDatePicker = false }
        )
    }

    if (showExpiryDatePicker) {
        AppDatePickerDialog(
            initialDateMillis = expiryDateMillis ?: (System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000),
            onDateSelected = { expiryDateMillis = it },
            onDismiss = { showExpiryDatePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailsSheet(
    document: DocumentItem,
    onDismiss: () -> Unit,
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
                    CategoryChip(category = document.category)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = document.name,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Normal
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                ExpiryStatusBadge(expiryDateMillis = document.expiryDateMillis)
            }

            HorizontalDivider(color = GlassBorderDark)

            if (document.documentNumber.isNotBlank()) {
                DetailRow(label = "Document ID / Number", value = document.documentNumber)
            }

            if (document.issueDateMillis != null) {
                DetailRow(label = "Issue Date", value = DateUtils.formatDate(document.issueDateMillis))
            }

            DetailRow(
                label = "Expiry Date",
                value = if (document.expiryDateMillis != null) DateUtils.formatDate(document.expiryDateMillis) else "No Expiry Date (Lifetime)"
            )

            // Attached Scans & Files
            AttachmentSection(
                attachments = document.attachments,
                onAttachmentsChange = { updatedList ->
                    onUpdateAttachments?.invoke(updatedList)
                },
                documentName = document.name,
                isEditable = onUpdateAttachments != null
            )

            // Reminder Notification Status
            val hasNotification = document.expiryDateMillis != null && document.reminderDaysBefore >= 0
            ReminderStatusCard(
                enabled = hasNotification,
                infoText = if (hasNotification) {
                    if (document.reminderDaysBefore == 0) "Alert scheduled on expiry date at 9:00 AM"
                    else "Alert scheduled ${document.reminderDaysBefore} days before expiry at 9:00 AM"
                } else if (document.expiryDateMillis == null) {
                    "No expiry date set (Lifetime document)"
                } else {
                    "No notifications will be sent for this document"
                }
            )

            if (document.notes.isNotBlank()) {
                DetailRow(label = "Notes", value = document.notes)
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                    "Delete Document?",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
                    color = StatusExpired
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete \"${document.name}\"? This action cannot be undone.",
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
fun DocumentDetailBottomSheet(
    document: DocumentItem,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) = DocumentDetailsSheet(document, onDismiss, onEdit, onDelete)

@Composable
fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp
            ),
            color = TextMutedDark
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
