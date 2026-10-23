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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DocumentItem
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.KaamKaajViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    viewModel: KaamKaajViewModel,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val documents by viewModel.allDocuments.collectAsStateWithLifecycle()
    val defaultDocReminder by viewModel.defaultDocReminderDays.collectAsStateWithLifecycle()

    var selectedCategory by remember { mutableStateOf("All") }
    var filterQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedDoc by remember { mutableStateOf<DocumentItem?>(null) }
    var editingDoc by remember { mutableStateOf<DocumentItem?>(null) }

    val categories = remember { listOf("All") + DOCUMENT_CATEGORIES }

    val filteredDocs = remember(documents, selectedCategory, filterQuery) {
        documents.filter { doc ->
            val matchesCategory = (selectedCategory == "All" || doc.category.equals(selectedCategory, ignoreCase = true))
            val matchesQuery = filterQuery.isBlank() ||
                    doc.name.contains(filterQuery, ignoreCase = true) ||
                    doc.documentNumber.contains(filterQuery, ignoreCase = true) ||
                    doc.notes.contains(filterQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Documents",
                onSearchClick = onNavigateToSearch
            )
        },
        floatingActionButton = {
            LiquidGlassFab(
                onClick = { showAddDialog = true },
                contentDescription = "Add Document",
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
            // Search Input inside Screen
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
                        value = filterQuery,
                        onValueChange = { filterQuery = it },
                        placeholder = {
                            Text(
                                "Search documents, ID numbers...",
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
                        visible = filterQuery.isNotBlank(),
                        enter = fadeIn(tween(160)) + scaleIn(tween(160)),
                        exit = fadeOut(tween(140)) + scaleOut(tween(140))
                    ) {
                        IconButton(
                            onClick = { filterQuery = "" },
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

            // Category Filter Row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    val chipBg = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else GlassSurfaceDark
                    val chipBorder = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f) else GlassBorderDark
                    val chipText = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(chipBg)
                            .border(1.dp, chipBorder, RoundedCornerShape(100.dp))
                            .bouncyClickable(pressedScale = 0.94f) { selectedCategory = cat }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = cat,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp
                            ),
                            color = chipText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (documents.isEmpty()) {
                CommonEmptyState(
                    icon = Icons.Outlined.Description,
                    title = "No Documents Added Yet",
                    description = "Keep your passport, driver's license, insurance, certificates, and vehicle registrations securely tracked with automated expiry alerts.",
                    actionLabel = "Add First Document",
                    onActionClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxSize()
                )
            } else if (filteredDocs.isEmpty()) {
                CommonEmptyState(
                    icon = Icons.Outlined.SearchOff,
                    title = "No Matching Documents",
                    description = "No documents found for \"$filterQuery\" in \"$selectedCategory\" category.",
                    actionLabel = "Clear Filter",
                    onActionClick = {
                        filterQuery = ""
                        selectedCategory = "All"
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredDocs, key = { it.id }) { doc ->
                        val icon = getDocCategoryIcon(doc.category)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(getCardBackground())
                                .border(1.dp, getCardBorder(), RoundedCornerShape(18.dp))
                                .glassCardClickable { selectedDoc = doc }
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
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.20f), RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = doc.name,
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
                                            Text(
                                                text = if (doc.documentNumber.isNotBlank()) "ID: ${doc.documentNumber}" else doc.category,
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                                color = TextMutedDark
                                            )
                                            if (doc.attachments.isNotEmpty()) {
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
                                                            text = "${doc.attachments.size}",
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 10.sp
                                                            ),
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                ExpiryStatusBadge(expiryDateMillis = doc.expiryDateMillis)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditDocumentDialog(
            defaultReminderDays = defaultDocReminder,
            onDismiss = { showAddDialog = false },
            onSave = { viewModel.saveDocument(it) }
        )
    }

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

    editingDoc?.let { doc ->
        AddEditDocumentDialog(
            initialDocument = doc,
            defaultReminderDays = defaultDocReminder,
            onDismiss = { editingDoc = null },
            onSave = { viewModel.saveDocument(it) }
        )
    }
}

private fun getDocCategoryIcon(category: String): ImageVector {
    return when (category.lowercase()) {
        "identity / id card" -> Icons.Outlined.Badge
        "passport / visa" -> Icons.Outlined.Public
        "driving license" -> Icons.Outlined.DirectionsCar
        "vehicle rc & insurance" -> Icons.Outlined.TimeToLeave
        "health / insurance" -> Icons.Outlined.HealthAndSafety
        "financial / tax / pan" -> Icons.Outlined.AccountBalance
        "certificates / degrees" -> Icons.Outlined.School
        "property / rent agreement" -> Icons.Outlined.HomeWork
        else -> Icons.Outlined.Description
    }
}

