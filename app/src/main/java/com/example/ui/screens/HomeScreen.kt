package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DocumentItem
import com.example.data.model.PaymentItem
import com.example.data.model.ReminderItem
import com.example.data.model.WarrantyItem
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.KaamKaajViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: KaamKaajViewModel,
    onNavigateToTab: (String) -> Unit,
    onNavigateToWarranties: () -> Unit,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val documents by viewModel.allDocuments.collectAsStateWithLifecycle()
    val expiringDocs by viewModel.expiringDocuments.collectAsStateWithLifecycle()
    val payments by viewModel.allPayments.collectAsStateWithLifecycle()
    val upcomingPayments by viewModel.upcomingPayments.collectAsStateWithLifecycle()
    val warranties by viewModel.allWarranties.collectAsStateWithLifecycle()
    val expiringWarranties by viewModel.expiringWarranties.collectAsStateWithLifecycle()
    val reminders by viewModel.allReminders.collectAsStateWithLifecycle()
    val upcomingReminders by viewModel.upcomingReminders.collectAsStateWithLifecycle()

    // Dialog & sheet states
    var showAddDocDialog by remember { mutableStateOf(false) }
    var showAddPaymentDialog by remember { mutableStateOf(false) }
    var showAddWarrantyDialog by remember { mutableStateOf(false) }
    var showAddReminderDialog by remember { mutableStateOf(false) }
    var showQuickActionSheet by remember { mutableStateOf(false) }

    var selectedDoc by remember { mutableStateOf<DocumentItem?>(null) }
    var selectedPayment by remember { mutableStateOf<PaymentItem?>(null) }
    var selectedWarranty by remember { mutableStateOf<WarrantyItem?>(null) }
    var selectedReminder by remember { mutableStateOf<ReminderItem?>(null) }
    var reschedulingReminder by remember { mutableStateOf<ReminderItem?>(null) }

    var editingDoc by remember { mutableStateOf<DocumentItem?>(null) }
    var editingPayment by remember { mutableStateOf<PaymentItem?>(null) }
    var editingWarranty by remember { mutableStateOf<WarrantyItem?>(null) }
    var editingReminder by remember { mutableStateOf<ReminderItem?>(null) }

    val defaultDocReminder by viewModel.defaultDocReminderDays.collectAsStateWithLifecycle()
    val defaultPaymentReminder by viewModel.defaultPaymentReminderDays.collectAsStateWithLifecycle()
    val defaultWarrantyReminder by viewModel.defaultWarrantyReminderDays.collectAsStateWithLifecycle()

    // Memoized computations to prevent jank during composition and scroll
    val pendingRemindersCount = remember(reminders) { reminders.count { !it.isCompleted } }
    val unpaidPaymentsSum = remember(payments) {
        payments.filter { !it.isPaid }.sumOf { it.amount }
    }

    val totalPriorityCount = remember(expiringDocs.size, upcomingPayments.size, pendingRemindersCount, expiringWarranties.size) {
        expiringDocs.size + upcomingPayments.size + pendingRemindersCount + expiringWarranties.size
    }
    val isTotallyEmpty = remember(documents.isEmpty(), payments.isEmpty(), warranties.isEmpty(), reminders.isEmpty()) {
        documents.isEmpty() && payments.isEmpty() && warranties.isEmpty() && reminders.isEmpty()
    }

    val topExpiringDocs = remember(expiringDocs) { expiringDocs.take(3) }
    val topUpcomingPayments = remember(upcomingPayments) { upcomingPayments.take(3) }
    val topUpcomingReminders = remember(upcomingReminders) { upcomingReminders.take(3) }
    val topExpiringWarranties = remember(expiringWarranties) { expiringWarranties.take(3) }
    val urgentPillDocs = remember(expiringDocs) { expiringDocs.take(2) }
    val urgentPillPayments = remember(upcomingPayments) { upcomingPayments.take(2) }

    Scaffold(
        topBar = {
            HomeHeader(onSearchClick = onNavigateToSearch)
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Hero Glass Priority Card
            item(key = "hero_priority_card") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(getCardBackground())
                        .border(1.dp, getCardBorder(), RoundedCornerShape(26.dp))
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    text = String.format("%02d", totalPriorityCount),
                                    style = MaterialTheme.typography.displayLarge.copy(
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 40.sp,
                                        lineHeight = 40.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "PRIORITY TASKS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        letterSpacing = 1.6.sp
                                    ),
                                    color = TextMutedDark
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(
                                        if (totalPriorityCount == 0) StatusActive.copy(alpha = 0.12f)
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    )
                                    .border(
                                        1.dp,
                                        if (totalPriorityCount == 0) StatusActive.copy(alpha = 0.3f)
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                        RoundedCornerShape(100.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = if (totalPriorityCount == 0) "ALL CLEAR" else "$totalPriorityCount ACTION${if (totalPriorityCount > 1) "S" else ""}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        letterSpacing = 1.2.sp
                                    ),
                                    color = if (totalPriorityCount == 0) StatusActive else MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Horizontal urgent item pills if any exist
                        if (urgentPillDocs.isNotEmpty() || urgentPillPayments.isNotEmpty()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                urgentPillDocs.forEach { doc ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(Color(0x12FFFFFF))
                                            .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(18.dp))
                                            .bouncyClickable(pressedScale = 0.94f) { selectedDoc = doc }
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = "DOCUMENT",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp,
                                                    letterSpacing = 1.2.sp
                                                ),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = doc.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                        }
                                    }
                                }

                                urgentPillPayments.forEach { payment ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(Color(0x12FFFFFF))
                                            .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(18.dp))
                                            .bouncyClickable(pressedScale = 0.94f) { selectedPayment = payment }
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = "PAYMENT",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp,
                                                    letterSpacing = 1.2.sp
                                                ),
                                                color = StatusActive
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = payment.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Stat Overview Grid
            item(key = "stat_overview_grid") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "OVERVIEW",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 2.sp
                        ),
                        color = TextMutedDark
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "Documents",
                            count = "${documents.size}",
                            subtitle = if (expiringDocs.isNotEmpty()) "${expiringDocs.size} expiring" else "All active",
                            icon = Icons.Outlined.Description,
                            accentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToTab("documents") }
                        )
                        StatCard(
                            title = "Due Bills",
                            count = "₹${unpaidPaymentsSum.toInt()}",
                            subtitle = "${payments.count { !it.isPaid }} pending",
                            icon = Icons.Outlined.Payments,
                            accentColor = StatusActive,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToTab("payments") }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "Reminders",
                            count = "$pendingRemindersCount",
                            subtitle = "${upcomingReminders.size} this week",
                            icon = Icons.Outlined.Alarm,
                            accentColor = StatusWarning,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToTab("reminders") }
                        )
                        StatCard(
                            title = "Warranties",
                            count = "${warranties.size}",
                            subtitle = if (expiringWarranties.isNotEmpty()) "${expiringWarranties.size} ending soon" else "Tracked",
                            icon = Icons.Outlined.VerifiedUser,
                            accentColor = StatusPurple,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToWarranties
                        )
                    }
                }
            }

            // Quick Action Buttons
            item(key = "quick_actions_bar") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "QUICK ACTIONS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 2.sp
                        ),
                        color = TextMutedDark
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        QuickActionChip(
                            label = "+ Scan Document",
                            icon = Icons.Outlined.DocumentScanner,
                            onClick = { showAddDocDialog = true }
                        )
                        QuickActionChip(
                            label = "+ Add Bill",
                            icon = Icons.Outlined.Payments,
                            onClick = { showAddPaymentDialog = true }
                        )
                        QuickActionChip(
                            label = "+ Add Reminder",
                            icon = Icons.Outlined.Alarm,
                            onClick = { showAddReminderDialog = true }
                        )
                        QuickActionChip(
                            label = "+ Add Warranty",
                            icon = Icons.Outlined.VerifiedUser,
                            onClick = { showAddWarrantyDialog = true }
                        )
                    }
                }
            }

            // Section 1: Expiring Documents
            if (topExpiringDocs.isNotEmpty()) {
                item(key = "header_expiring_docs") {
                    SectionHeader(
                        title = "UPCOMING EXPIRIES",
                        count = expiringDocs.size,
                        onViewAll = { onNavigateToTab("documents") }
                    )
                }
                items(topExpiringDocs, key = { "doc_${it.id}" }, contentType = { "glass_item" }) { doc ->
                    GlassItemCard(
                        icon = Icons.Outlined.Description,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = doc.name,
                        subtitle = if (doc.documentNumber.isNotBlank()) doc.documentNumber else doc.category,
                        badge = { ExpiryStatusBadge(expiryDateMillis = doc.expiryDateMillis) },
                        onClick = { selectedDoc = doc }
                    )
                }
            }

            // Section 2: Due Payments
            if (topUpcomingPayments.isNotEmpty()) {
                item(key = "header_due_payments") {
                    SectionHeader(
                        title = "DUE PAYMENTS",
                        count = upcomingPayments.size,
                        onViewAll = { onNavigateToTab("payments") }
                    )
                }
                items(topUpcomingPayments, key = { "pay_${it.id}" }, contentType = { "glass_item" }) { payment ->
                    GlassItemCard(
                        icon = Icons.Outlined.Payments,
                        iconTint = StatusActive,
                        title = payment.name,
                        subtitle = "Due: ${DateUtils.formatDate(payment.dueDateMillis)} • ₹${payment.amount.toInt()}",
                        badge = { PaymentStatusBadge(isPaid = payment.isPaid, dueDateMillis = payment.dueDateMillis) },
                        onClick = { selectedPayment = payment }
                    )
                }
            }

            // Section 3: Upcoming Reminders
            if (topUpcomingReminders.isNotEmpty()) {
                item(key = "header_upcoming_reminders") {
                    SectionHeader(
                        title = "PENDING ALERTS",
                        count = upcomingReminders.size,
                        onViewAll = { onNavigateToTab("reminders") }
                    )
                }
                items(topUpcomingReminders, key = { "rem_${it.id}" }, contentType = { "glass_item" }) { reminder ->
                    GlassItemCard(
                        icon = Icons.Outlined.Alarm,
                        iconTint = StatusWarning,
                        title = reminder.title,
                        subtitle = DateUtils.formatDateTime(reminder.dueDateTimeMillis),
                        badge = {
                            Checkbox(
                                checked = reminder.isCompleted,
                                onCheckedChange = { viewModel.toggleReminderCompleted(reminder) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = TextMutedDark
                                )
                            )
                        },
                        onClick = { selectedReminder = reminder }
                    )
                }
            }

            // Section 4: Warranties Ending Soon
            if (topExpiringWarranties.isNotEmpty()) {
                item(key = "header_expiring_warranties") {
                    SectionHeader(
                        title = "WARRANTIES ENDING SOON",
                        count = expiringWarranties.size,
                        onViewAll = onNavigateToWarranties
                    )
                }
                items(topExpiringWarranties, key = { "war_${it.id}" }, contentType = { "glass_item" }) { warranty ->
                    GlassItemCard(
                        icon = Icons.Outlined.VerifiedUser,
                        iconTint = StatusPurple,
                        title = warranty.productName,
                        subtitle = if (warranty.brand.isNotBlank()) warranty.brand else "Warranty item",
                        badge = { WarrantyStatusBadge(expiryDateMillis = warranty.expiryDateMillis) },
                        onClick = { selectedWarranty = warranty }
                    )
                }
            }

            // Quick Action Footer Button
            item(key = "footer_quick_action_button") {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = { showQuickActionSheet = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = "QUICK ACTION",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 2.sp
                        )
                    )
                }
            }
        }
    }

    // Quick Action Bottom Sheet
    if (showQuickActionSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQuickActionSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "CREATE NEW ITEM",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = TextMutedDark
                )

                QuickActionSheetItem(
                    title = "Add Document",
                    subtitle = "Store ID, Passport, Driver License, Registration",
                    icon = Icons.Outlined.Description,
                    onClick = {
                        showQuickActionSheet = false
                        showAddDocDialog = true
                    }
                )
                QuickActionSheetItem(
                    title = "Add Bill / Payment",
                    subtitle = "Track utility bills, rent, insurance, subscriptions",
                    icon = Icons.Outlined.Payments,
                    onClick = {
                        showQuickActionSheet = false
                        showAddPaymentDialog = true
                    }
                )
                QuickActionSheetItem(
                    title = "Add Reminder",
                    subtitle = "Set customized alert with date and repeat cycle",
                    icon = Icons.Outlined.Alarm,
                    onClick = {
                        showQuickActionSheet = false
                        showAddReminderDialog = true
                    }
                )
                QuickActionSheetItem(
                    title = "Add Warranty",
                    subtitle = "Keep product warranty cards & purchase dates",
                    icon = Icons.Outlined.VerifiedUser,
                    onClick = {
                        showQuickActionSheet = false
                        showAddWarrantyDialog = true
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Add Dialogs
    if (showAddDocDialog) {
        AddEditDocumentDialog(
            defaultReminderDays = defaultDocReminder,
            onDismiss = { showAddDocDialog = false },
            onSave = { viewModel.saveDocument(it) }
        )
    }

    if (showAddPaymentDialog) {
        AddEditPaymentDialog(
            defaultReminderDays = defaultPaymentReminder,
            onDismiss = { showAddPaymentDialog = false },
            onSave = { viewModel.savePayment(it) }
        )
    }

    if (showAddWarrantyDialog) {
        AddEditWarrantyDialog(
            defaultReminderDays = defaultWarrantyReminder,
            onDismiss = { showAddWarrantyDialog = false },
            onSave = { viewModel.saveWarranty(it) }
        )
    }

    if (showAddReminderDialog) {
        AddEditReminderDialog(
            onDismiss = { showAddReminderDialog = false },
            onSave = { viewModel.saveReminder(it) }
        )
    }

    // Details sheets
    val activeDoc = remember(documents, selectedDoc) {
        if (selectedDoc != null) {
            documents.find { it.id == selectedDoc?.id } ?: selectedDoc
        } else null
    }

    activeDoc?.let { doc ->
        DocumentDetailsSheet(
            document = doc,
            onDismiss = { selectedDoc = null },
            onEdit = {
                selectedDoc = null
                editingDoc = doc
            },
            onDelete = {
                viewModel.deleteDocument(doc)
                selectedDoc = null
            },
            onUpdateAttachments = { updatedAttachments ->
                val updatedDoc = doc.copy(attachments = updatedAttachments)
                viewModel.saveDocument(updatedDoc)
                selectedDoc = updatedDoc
            }
        )
    }

    val activePayment = remember(payments, selectedPayment) {
        if (selectedPayment != null) {
            payments.find { it.id == selectedPayment?.id } ?: selectedPayment
        } else null
    }

    activePayment?.let { payment ->
        PaymentDetailsSheet(
            payment = payment,
            onDismiss = { selectedPayment = null },
            onEdit = {
                selectedPayment = null
                editingPayment = payment
            },
            onDelete = {
                viewModel.deletePayment(payment)
                selectedPayment = null
            },
            onTogglePaid = { viewModel.togglePaymentPaid(payment) },
            onUpdateAttachments = { updatedAttachments ->
                val updatedPayment = payment.copy(attachments = updatedAttachments)
                viewModel.savePayment(updatedPayment)
                selectedPayment = updatedPayment
            }
        )
    }

    selectedWarranty?.let { warranty ->
        WarrantyDetailsSheet(
            warranty = warranty,
            onDismiss = { selectedWarranty = null },
            onEdit = {
                selectedWarranty = null
                editingWarranty = warranty
            },
            onDelete = {
                viewModel.deleteWarranty(warranty)
                selectedWarranty = null
            }
        )
    }

    selectedReminder?.let { reminder ->
        ReminderDetailsSheet(
            reminder = reminder,
            onDismiss = { selectedReminder = null },
            onReschedule = {
                selectedReminder = null
                reschedulingReminder = reminder
            },
            onEdit = {
                selectedReminder = null
                editingReminder = reminder
            },
            onDelete = {
                viewModel.deleteReminder(reminder)
                selectedReminder = null
            },
            onToggleCompleted = { viewModel.toggleReminderCompleted(reminder) }
        )
    }

    reschedulingReminder?.let { reminder ->
        QuickRescheduleSheet(
            reminder = reminder,
            onDismiss = { reschedulingReminder = null },
            onReschedule = { newDueMillis ->
                viewModel.rescheduleReminder(reminder, newDueMillis)
                reschedulingReminder = null
            }
        )
    }

    // Edit Dialogs
    editingDoc?.let { doc ->
        AddEditDocumentDialog(
            initialDocument = doc,
            defaultReminderDays = defaultDocReminder,
            onDismiss = { editingDoc = null },
            onSave = { viewModel.saveDocument(it) }
        )
    }

    editingPayment?.let { payment ->
        AddEditPaymentDialog(
            initialPayment = payment,
            defaultReminderDays = defaultPaymentReminder,
            onDismiss = { editingPayment = null },
            onSave = { viewModel.savePayment(it) }
        )
    }

    editingWarranty?.let { warranty ->
        AddEditWarrantyDialog(
            initialWarranty = warranty,
            defaultReminderDays = defaultWarrantyReminder,
            onDismiss = { editingWarranty = null },
            onSave = { viewModel.saveWarranty(it) }
        )
    }

    editingReminder?.let { reminder ->
        AddEditReminderDialog(
            initialReminder = reminder,
            onDismiss = { editingReminder = null },
            onSave = { viewModel.saveReminder(it) }
        )
    }
}

@Composable
fun StatCard(
    title: String,
    count: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val cardModifier = if (onClick != null) {
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(GlassSurfaceDark)
            .border(1.dp, GlassBorderDark, RoundedCornerShape(20.dp))
            .glassCardClickable(pressedScale = 0.95f, onClick = onClick)
            .padding(14.dp)
    } else {
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(GlassSurfaceDark)
            .border(1.dp, GlassBorderDark, RoundedCornerShape(20.dp))
            .padding(14.dp)
    }

    Box(modifier = cardModifier) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = count,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 24.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = TextMutedDark
            )
        }
    }
}

@Composable
fun QuickActionChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(GlassSurfaceDark)
            .border(1.dp, GlassBorderDark, RoundedCornerShape(14.dp))
            .bouncyClickable(pressedScale = 0.94f, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
fun GlassItemCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    badge: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(GlassSurfaceDark)
            .border(1.dp, GlassBorderDark, RoundedCornerShape(18.dp))
            .glassCardClickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconTint.copy(alpha = 0.10f))
                        .border(1.dp, iconTint.copy(alpha = 0.20f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = TextMutedDark
                    )
                }
            }

            badge()
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    count: Int,
    onViewAll: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 2.sp
            ),
            color = TextMutedDark
        )

        Text(
            text = "See all",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .bouncyClickable(pressedScale = 0.92f, onClick = onViewAll)
                .padding(vertical = 4.dp, horizontal = 6.dp)
        )
    }
}

@Composable
fun QuickActionSheetItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassSurfaceDark)
            .border(1.dp, GlassBorderDark, RoundedCornerShape(16.dp))
            .glassCardClickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMutedDark
                )
            }
        }
    }
}
