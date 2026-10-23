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
import com.example.data.model.WarrantyItem
import com.example.ui.theme.*

val WARRANTY_DURATION_OPTIONS = listOf(
    6 to "6 Months",
    12 to "1 Year (12 Months)",
    24 to "2 Years (24 Months)",
    36 to "3 Years (36 Months)",
    60 to "5 Years (60 Months)"
)

val WARRANTY_REMINDER_OPTIONS = listOf(
    0 to "No reminder",
    7 to "7 days before",
    15 to "15 days before",
    30 to "30 days before"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditWarrantyDialog(
    initialWarranty: WarrantyItem? = null,
    defaultReminderDays: Int = 15,
    onDismiss: () -> Unit,
    onSave: (WarrantyItem) -> Unit
) {
    var productName by remember { mutableStateOf(initialWarranty?.productName ?: "") }
    var brand by remember { mutableStateOf(initialWarranty?.brand ?: "") }
    var purchaseDateMillis by remember { mutableStateOf(initialWarranty?.purchaseDateMillis ?: System.currentTimeMillis()) }
    var durationMonths by remember { mutableIntStateOf(initialWarranty?.durationMonths ?: 12) }
    var priceStr by remember { mutableStateOf(initialWarranty?.purchasePrice?.toString() ?: "") }
    var reminderDaysBefore by remember {
        mutableIntStateOf(
            if ((initialWarranty?.reminderDaysBefore ?: defaultReminderDays) >= 0)
                (initialWarranty?.reminderDaysBefore ?: defaultReminderDays)
            else defaultReminderDays
        )
    }
    var reminderEnabled by remember {
        mutableStateOf(initialWarranty?.let { it.reminderDaysBefore >= 0 } ?: true)
    }
    var notes by remember { mutableStateOf(initialWarranty?.notes ?: "") }

    var showDatePicker by remember { mutableStateOf(false) }
    var durationExpanded by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }

    // Calculated expiry date
    val expiryDateMillis = remember(purchaseDateMillis, durationMonths) {
        DateUtils.calculateWarrantyExpiry(purchaseDateMillis, durationMonths)
    }

    AppFormDialog(
        onDismiss = onDismiss,
        title = if (initialWarranty == null) "Add Product Warranty" else "Edit Warranty",
        subtitle = "Track product coverage, receipts, and expiration alerts",
        categoryBadgeText = "WARRANTIES & ASSETS",
        icon = Icons.Default.VerifiedUser,
        iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
        iconContentColor = MaterialTheme.colorScheme.primary,
        confirmButtonText = "SAVE WARRANTY",
        onConfirm = {
            if (productName.isBlank()) {
                hasError = true
                return@AppFormDialog
            }
            val finalReminderDays = if (reminderEnabled) reminderDaysBefore else -1
            val item = (initialWarranty ?: WarrantyItem(productName = "", brand = "", purchaseDateMillis = 0, durationMonths = 12, expiryDateMillis = 0)).copy(
                productName = productName.trim(),
                brand = brand.trim(),
                purchaseDateMillis = purchaseDateMillis,
                durationMonths = durationMonths,
                expiryDateMillis = expiryDateMillis,
                purchasePrice = priceStr.toDoubleOrNull(),
                reminderDaysBefore = finalReminderDays,
                notes = notes.trim()
            )
            onSave(item)
            onDismiss()
        }
    ) {
        FormSectionHeader(title = "Product Information")

        OutlinedTextField(
            value = productName,
            onValueChange = {
                productName = it
                if (it.isNotBlank()) hasError = false
            },
            label = { Text("Product / Appliance Name *") },
            placeholder = { Text("e.g. MacBook Pro, Samsung TV, Air Conditioner") },
            isError = hasError && productName.isBlank(),
            supportingText = if (hasError && productName.isBlank()) {
                { Text("Product name is required") }
            } else null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = brand,
            onValueChange = { brand = it },
            label = { Text("Brand / Manufacturer") },
            placeholder = { Text("e.g. Apple, Sony, LG, Dell") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        FormSectionHeader(title = "Warranty Period & Price")

        // Purchase Date
        OutlinedTextField(
            value = DateUtils.formatDate(purchaseDateMillis),
            onValueChange = {},
            readOnly = true,
            label = { Text("Purchase Date") },
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.CalendarToday, contentDescription = "Select Purchase Date")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePicker = true },
            shape = RoundedCornerShape(16.dp)
        )

        // Duration Dropdown
        ExposedDropdownMenuBox(
            expanded = durationExpanded,
            onExpandedChange = { durationExpanded = !durationExpanded }
        ) {
            OutlinedTextField(
                value = WARRANTY_DURATION_OPTIONS.find { it.first == durationMonths }?.second
                    ?: "$durationMonths Months",
                onValueChange = {},
                readOnly = true,
                label = { Text("Warranty Duration") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = durationExpanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )
            ExposedDropdownMenu(
                expanded = durationExpanded,
                onDismissRequest = { durationExpanded = false }
            ) {
                WARRANTY_DURATION_OPTIONS.forEach { (months, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            durationMonths = months
                            durationExpanded = false
                        }
                    )
                }
            }
        }

        // Calculated Expiry Display
        OutlinedTextField(
            value = DateUtils.formatDate(expiryDateMillis),
            onValueChange = {},
            readOnly = true,
            label = { Text("Calculated Expiry Date") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )

        OutlinedTextField(
            value = priceStr,
            onValueChange = { priceStr = it },
            label = { Text("Purchase Price (Optional)") },
            placeholder = { Text("e.g. 84999") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            prefix = { Text("₹ ") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        FormSectionHeader(title = "Notifications & Details")

        // Reminder Notification Section
        ReminderNotificationControl(
            enabled = reminderEnabled,
            onEnabledChange = { reminderEnabled = it },
            selectedDaysBefore = reminderDaysBefore,
            onDaysSelected = { reminderDaysBefore = it },
            options = WARRANTY_REMINDER_OPTIONS,
            itemTypeLabel = "warranty expires"
        )

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes & Serial Number (Optional)") },
            placeholder = { Text("Serial number, store name, invoice info...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            minLines = 2,
            maxLines = 4
        )
    }

    if (showDatePicker) {
        AppDatePickerDialog(
            initialDateMillis = purchaseDateMillis,
            onDateSelected = { purchaseDateMillis = it },
            onDismiss = { showDatePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarrantyDetailsSheet(
    warranty: WarrantyItem,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
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
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (warranty.brand.isNotBlank()) {
                        CategoryChip(category = warranty.brand)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text(
                        text = warranty.productName,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Normal
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                WarrantyStatusBadge(expiryDateMillis = warranty.expiryDateMillis)
            }

            HorizontalDivider(color = GlassBorderDark)

            DetailRow(label = "Purchase Date", value = DateUtils.formatDate(warranty.purchaseDateMillis))
            DetailRow(label = "Warranty Duration", value = "${warranty.durationMonths} Months (${warranty.durationMonths / 12} Years)")
            DetailRow(label = "Expiry Date", value = DateUtils.formatDate(warranty.expiryDateMillis))

            if (warranty.purchasePrice != null && warranty.purchasePrice > 0) {
                DetailRow(label = "Purchase Price", value = "₹${String.format("%.2f", warranty.purchasePrice)}")
            }

            // Reminder Notification Status
            val hasNotification = warranty.reminderDaysBefore >= 0
            ReminderStatusCard(
                enabled = hasNotification,
                infoText = if (hasNotification) {
                    if (warranty.reminderDaysBefore == 0) "Alert scheduled on expiry date at 9:00 AM"
                    else "Alert scheduled ${warranty.reminderDaysBefore} days before expiry at 9:00 AM"
                } else {
                    "No notifications will be sent for this warranty"
                }
            )

            if (warranty.notes.isNotBlank()) {
                DetailRow(label = "Notes / Serial Number", value = warranty.notes)
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
                    "Delete Warranty?",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
                    color = StatusExpired
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete \"${warranty.productName}\"? This action cannot be undone.",
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
fun WarrantyDetailBottomSheet(
    warranty: WarrantyItem,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) = WarrantyDetailsSheet(warranty, onDismiss, onEdit, onDelete)
