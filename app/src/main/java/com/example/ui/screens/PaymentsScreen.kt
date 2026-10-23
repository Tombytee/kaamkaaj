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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PaymentItem
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.KaamKaajViewModel

enum class PaymentFilter(val label: String) {
    ALL("All"),
    PENDING("Pending"),
    PAID("Paid"),
    OVERDUE("Overdue")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentsScreen(
    viewModel: KaamKaajViewModel,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val payments by viewModel.allPayments.collectAsStateWithLifecycle()
    val defaultPaymentReminder by viewModel.defaultPaymentReminderDays.collectAsStateWithLifecycle()

    var activeFilter by remember { mutableStateOf(PaymentFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedPayment by remember { mutableStateOf<PaymentItem?>(null) }
    var editingPayment by remember { mutableStateOf<PaymentItem?>(null) }

    val pendingTotal = remember(payments) {
        payments.filter { !it.isPaid }.sumOf { it.amount }
    }

    val filteredPayments = remember(payments, activeFilter, searchQuery) {
        payments.filter { payment ->
            val matchesFilter = when (activeFilter) {
                PaymentFilter.ALL -> true
                PaymentFilter.PENDING -> !payment.isPaid
                PaymentFilter.PAID -> payment.isPaid
                PaymentFilter.OVERDUE -> !payment.isPaid && DateUtils.getDaysDifference(payment.dueDateMillis) < 0
            }
            val matchesQuery = searchQuery.isBlank() ||
                    payment.name.contains(searchQuery, ignoreCase = true) ||
                    payment.category.contains(searchQuery, ignoreCase = true) ||
                    payment.notes.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Bills & Payments",
                onSearchClick = onNavigateToSearch
            )
        },
        floatingActionButton = {
            LiquidGlassFab(
                onClick = { showAddDialog = true },
                contentDescription = "Add Payment",
                modifier = Modifier.padding(bottom = 72.dp)
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
            // Summary Card (Total Pending)
            if (payments.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(getCardBackground())
                        .border(1.dp, getCardBorder(), RoundedCornerShape(22.dp))
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TOTAL PENDING DUES",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.4.sp
                                ),
                                color = TextMutedDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "₹${String.format("%.2f", pendingTotal)}",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 28.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(StatusActive.copy(alpha = 0.12f))
                                .border(1.dp, StatusActive.copy(alpha = 0.30f), RoundedCornerShape(100.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${payments.count { !it.isPaid }} PENDING",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.sp
                                ),
                                color = StatusActive
                            )
                        }
                    }
                }
            }

            // Search bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(getCardBackground())
                    .border(1.dp, getCardBorder(), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 4.dp)
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
                    Spacer(modifier = Modifier.width(10.dp))
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                "Search bills, utility payments...",
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
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(28.dp)
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

            // Filter Tabs
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(PaymentFilter.entries) { filter ->
                    val isSelected = activeFilter == filter
                    val chipBg = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else GlassSurfaceDark
                    val chipBorder = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f) else GlassBorderDark
                    val chipText = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(chipBg)
                            .border(1.dp, chipBorder, RoundedCornerShape(100.dp))
                            .bouncyClickable(pressedScale = 0.94f) { activeFilter = filter }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = filter.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp
                            ),
                            color = chipText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (payments.isEmpty()) {
                CommonEmptyState(
                    icon = Icons.Outlined.Payments,
                    title = "No Payments or Bills",
                    description = "Keep track of electricity, rent, mobile recharges, credit card bills, EMI and subscriptions with automated due date reminders.",
                    actionLabel = "Add First Payment",
                    onActionClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxSize()
                )
            } else if (filteredPayments.isEmpty()) {
                CommonEmptyState(
                    icon = Icons.Outlined.SearchOff,
                    title = "No Matching Payments",
                    description = "No payments found for the selected filter.",
                    actionLabel = "Reset Filter",
                    onActionClick = {
                        searchQuery = ""
                        activeFilter = PaymentFilter.ALL
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredPayments, key = { it.id }) { payment ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(getCardBackground())
                                .border(1.dp, getCardBorder(), RoundedCornerShape(18.dp))
                                .glassCardClickable { selectedPayment = payment }
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Fast Toggle button with spring bounce
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .bouncyClickable(pressedScale = 0.85f) {
                                            viewModel.togglePaymentPaid(payment)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (payment.isPaid) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                        contentDescription = "Toggle Paid",
                                        tint = if (payment.isPaid) StatusActive else TextMutedDark,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = payment.name,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        CategoryChip(category = payment.category)
                                        if (payment.attachments.isNotEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.AttachFile,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text(
                                                        text = "${payment.attachments.size}",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 10.sp
                                                        ),
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "Due ${DateUtils.formatDate(payment.dueDateMillis)}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = TextMutedDark
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "₹${String.format("%.2f", payment.amount)}",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        ),
                                        color = if (payment.isPaid) TextMutedDark else MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    PaymentStatusBadge(isPaid = payment.isPaid, dueDateMillis = payment.dueDateMillis)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditPaymentDialog(
            defaultReminderDays = defaultPaymentReminder,
            onDismiss = { showAddDialog = false },
            onSave = { viewModel.savePayment(it) }
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

    editingPayment?.let { payment ->
        AddEditPaymentDialog(
            initialPayment = payment,
            defaultReminderDays = defaultPaymentReminder,
            onDismiss = { editingPayment = null },
            onSave = { viewModel.savePayment(it) }
        )
    }
}

