package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.example.data.model.WarrantyItem
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.KaamKaajViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarrantiesScreen(
    viewModel: KaamKaajViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val warranties by viewModel.allWarranties.collectAsStateWithLifecycle()
    val defaultWarrantyReminder by viewModel.defaultWarrantyReminderDays.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedWarranty by remember { mutableStateOf<WarrantyItem?>(null) }
    var editingWarranty by remember { mutableStateOf<WarrantyItem?>(null) }

    val filteredWarranties = remember(warranties, searchQuery) {
        if (searchQuery.isBlank()) {
            warranties
        } else {
            warranties.filter {
                it.productName.contains(searchQuery, ignoreCase = true) ||
                        it.brand.contains(searchQuery, ignoreCase = true) ||
                        it.notes.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Warranties",
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                onSearchClick = onNavigateToSearch
            )
        },
        floatingActionButton = {
            LiquidGlassFab(
                onClick = { showAddDialog = true },
                contentDescription = "Add Warranty"
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
            // Search Input
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
                                "Search warranties, product brands...",
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

            Spacer(modifier = Modifier.height(4.dp))

            if (warranties.isEmpty()) {
                CommonEmptyState(
                    icon = Icons.Outlined.VerifiedUser,
                    title = "No Warranties Tracked",
                    description = "Keep track of electronics, appliances, and vehicle warranties with calculated expiry dates and proactive reminder alerts.",
                    actionLabel = "Add First Warranty",
                    onActionClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxSize()
                )
            } else if (filteredWarranties.isEmpty()) {
                CommonEmptyState(
                    icon = Icons.Outlined.SearchOff,
                    title = "No Matching Warranties",
                    description = "No warranties match \"$searchQuery\".",
                    actionLabel = "Clear Search",
                    onActionClick = { searchQuery = "" },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredWarranties, key = { it.id }) { warranty ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(getCardBackground())
                                .border(1.dp, getCardBorder(), RoundedCornerShape(18.dp))
                                .glassCardClickable { selectedWarranty = warranty }
                                .padding(14.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (warranty.brand.isNotBlank()) {
                                        CategoryChip(category = warranty.brand)
                                    } else {
                                        Spacer(modifier = Modifier.width(1.dp))
                                    }
                                    WarrantyStatusBadge(expiryDateMillis = warranty.expiryDateMillis)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = warranty.productName,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Expires ${DateUtils.formatDate(warranty.expiryDateMillis)} (${warranty.durationMonths}m)",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = TextMutedDark
                                    )

                                    if (warranty.purchasePrice != null && warranty.purchasePrice > 0) {
                                        Text(
                                            text = "₹${String.format("%.2f", warranty.purchasePrice)}",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp
                                            ),
                                            color = MaterialTheme.colorScheme.primary
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

    if (showAddDialog) {
        AddEditWarrantyDialog(
            defaultReminderDays = defaultWarrantyReminder,
            onDismiss = { showAddDialog = false },
            onSave = { viewModel.saveWarranty(it) }
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

    editingWarranty?.let { warranty ->
        AddEditWarrantyDialog(
            initialWarranty = warranty,
            defaultReminderDays = defaultWarrantyReminder,
            onDismiss = { editingWarranty = null },
            onSave = { viewModel.saveWarranty(it) }
        )
    }
}

