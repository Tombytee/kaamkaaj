package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

enum class SearchFilter(val label: String) {
    ALL("All"),
    DOCUMENTS("Documents"),
    PAYMENTS("Payments"),
    WARRANTIES("Warranties"),
    REMINDERS("Reminders")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: KaamKaajViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf(SearchFilter.ALL) }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(GlassSurfaceDark)
                            .border(1.dp, GlassBorderDark, RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = TextMutedDark,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextField(
                                value = searchQuery,
                                onValueChange = { viewModel.updateSearchQuery(it) },
                                placeholder = {
                                    Text(
                                        "Search across all records...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextMutedDark
                                    )
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                                ),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            AnimatedVisibility(
                                visible = searchQuery.isNotBlank(),
                                enter = fadeIn(tween(160)) + scaleIn(tween(160)),
                                exit = fadeOut(tween(140)) + scaleOut(tween(140))
                            ) {
                                IconButton(
                                    onClick = { viewModel.updateSearchQuery("") },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = TextMutedDark,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(SearchFilter.entries) { filter ->
                    val isSelected = selectedFilter == filter
                    val count = when (filter) {
                        SearchFilter.ALL -> searchResults.totalCount
                        SearchFilter.DOCUMENTS -> searchResults.documents.size
                        SearchFilter.PAYMENTS -> searchResults.payments.size
                        SearchFilter.WARRANTIES -> searchResults.warranties.size
                        SearchFilter.REMINDERS -> searchResults.reminders.size
                    }
                    val chipBg by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else GlassSurfaceDark,
                        animationSpec = tween(200),
                        label = "search_chip_bg"
                    )
                    val chipBorder by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f) else GlassBorderDark,
                        animationSpec = tween(200),
                        label = "search_chip_border"
                    )
                    val chipText by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(200),
                        label = "search_chip_text"
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(chipBg)
                            .border(1.dp, chipBorder, RoundedCornerShape(100.dp))
                            .bouncyClickable(pressedScale = 0.94f) { selectedFilter = filter }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "${filter.label} ($count)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp
                            ),
                            color = chipText
                        )
                    }
                }
            }

            if (searchQuery.isBlank()) {
                CommonEmptyState(
                    icon = Icons.Outlined.Search,
                    title = "Search Everything",
                    description = "Type keywords, names, bill categories, product brands, or IDs to instantly search your KaamKaaj database.",
                    modifier = Modifier.fillMaxSize()
                )
            } else if (searchResults.totalCount == 0) {
                CommonEmptyState(
                    icon = Icons.Outlined.SearchOff,
                    title = "No Results Found",
                    description = "No documents, payments, warranties, or reminders match \"$searchQuery\".",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Documents Section
                    if ((selectedFilter == SearchFilter.ALL || selectedFilter == SearchFilter.DOCUMENTS) && searchResults.documents.isNotEmpty()) {
                        item {
                            Text(
                                text = "DOCUMENTS (${searchResults.documents.size})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.6.sp
                                ),
                                color = TextMutedDark
                            )
                        }
                        items(searchResults.documents, key = { "doc_${it.id}" }) { doc ->
                            Box(
                                modifier = Modifier
                                    .animateItem()
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(GlassSurfaceDark)
                                    .border(1.dp, GlassBorderDark, RoundedCornerShape(16.dp))
                                    .glassCardClickable { selectedDoc = doc }
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = doc.name,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = if (doc.documentNumber.isNotBlank()) "ID: ${doc.documentNumber} • ${doc.category}" else doc.category,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextMutedDark
                                        )
                                    }
                                    ExpiryStatusBadge(expiryDateMillis = doc.expiryDateMillis)
                                }
                            }
                        }
                    }

                    // Payments Section
                    if ((selectedFilter == SearchFilter.ALL || selectedFilter == SearchFilter.PAYMENTS) && searchResults.payments.isNotEmpty()) {
                        item {
                            Text(
                                text = "PAYMENTS (${searchResults.payments.size})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.6.sp
                                ),
                                color = TextMutedDark
                            )
                        }
                        items(searchResults.payments, key = { "pay_${it.id}" }) { payment ->
                            Box(
                                modifier = Modifier
                                    .animateItem()
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(GlassSurfaceDark)
                                    .border(1.dp, GlassBorderDark, RoundedCornerShape(16.dp))
                                    .glassCardClickable { selectedPayment = payment }
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = payment.name,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = "Due: ${DateUtils.formatDate(payment.dueDateMillis)} • ₹${String.format("%.2f", payment.amount)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextMutedDark
                                        )
                                    }
                                    PaymentStatusBadge(isPaid = payment.isPaid, dueDateMillis = payment.dueDateMillis)
                                }
                            }
                        }
                    }

                    // Warranties Section
                    if ((selectedFilter == SearchFilter.ALL || selectedFilter == SearchFilter.WARRANTIES) && searchResults.warranties.isNotEmpty()) {
                        item {
                            Text(
                                text = "WARRANTIES (${searchResults.warranties.size})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.6.sp
                                ),
                                color = TextMutedDark
                            )
                        }
                        items(searchResults.warranties, key = { "war_${it.id}" }) { warranty ->
                            Box(
                                modifier = Modifier
                                    .animateItem()
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(GlassSurfaceDark)
                                    .border(1.dp, GlassBorderDark, RoundedCornerShape(16.dp))
                                    .glassCardClickable { selectedWarranty = warranty }
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = warranty.productName,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = "${warranty.brand} • Expires ${DateUtils.formatDate(warranty.expiryDateMillis)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextMutedDark
                                        )
                                    }
                                    WarrantyStatusBadge(expiryDateMillis = warranty.expiryDateMillis)
                                }
                            }
                        }
                    }

                    // Reminders Section
                    if ((selectedFilter == SearchFilter.ALL || selectedFilter == SearchFilter.REMINDERS) && searchResults.reminders.isNotEmpty()) {
                        item {
                            Text(
                                text = "REMINDERS (${searchResults.reminders.size})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.6.sp
                                ),
                                color = TextMutedDark
                            )
                        }
                        items(searchResults.reminders, key = { "rem_${it.id}" }) { reminder ->
                            Box(
                                modifier = Modifier
                                    .animateItem()
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(GlassSurfaceDark)
                                    .border(1.dp, GlassBorderDark, RoundedCornerShape(16.dp))
                                    .glassCardClickable { selectedReminder = reminder }
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = reminder.isCompleted,
                                        onCheckedChange = { viewModel.toggleReminderCompleted(reminder) },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = MaterialTheme.colorScheme.primary,
                                            uncheckedColor = TextMutedDark
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = reminder.title,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = DateUtils.formatDateTime(reminder.dueDateTimeMillis),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextMutedDark
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail sheets
    selectedDoc?.let { doc ->
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

    selectedPayment?.let { payment ->
        PaymentDetailsSheet(
            payment = payment,
            onDismiss = { selectedPayment = null },
            onTogglePaid = { viewModel.togglePaymentPaid(payment) },
            onEdit = {
                selectedPayment = null
                editingPayment = payment
            },
            onDelete = {
                viewModel.deletePayment(payment)
                selectedPayment = null
            },
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
            onToggleCompleted = { viewModel.toggleReminderCompleted(reminder) },
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
            }
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

    // Edit dialogs
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
