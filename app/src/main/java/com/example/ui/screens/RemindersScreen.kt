package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ReminderItem
import com.example.notification.ReminderSchedulerUtils
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.KaamKaajViewModel

enum class ReminderFilter(val label: String) {
    ACTIVE("Active"),
    COMPLETED("Completed"),
    ALL("All")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    viewModel: KaamKaajViewModel,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reminders by viewModel.allReminders.collectAsStateWithLifecycle()
    var activeFilter by remember { mutableStateOf(ReminderFilter.ACTIVE) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedReminder by remember { mutableStateOf<ReminderItem?>(null) }
    var editingReminder by remember { mutableStateOf<ReminderItem?>(null) }
    var reschedulingReminder by remember { mutableStateOf<ReminderItem?>(null) }

    val activeCount = remember(reminders) { reminders.count { !it.isCompleted } }
    val completedCount = remember(reminders) { reminders.count { it.isCompleted } }

    val filteredReminders = remember(reminders, activeFilter, searchQuery) {
        reminders.filter { reminder ->
            val matchesFilter = when (activeFilter) {
                ReminderFilter.ALL -> true
                ReminderFilter.ACTIVE -> !reminder.isCompleted
                ReminderFilter.COMPLETED -> reminder.isCompleted
            }
            val matchesQuery = searchQuery.isBlank() ||
                    reminder.title.contains(searchQuery, ignoreCase = true) ||
                    reminder.description.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Reminders & Alerts",
                onSearchClick = onNavigateToSearch
            )
        },
        floatingActionButton = {
            LiquidGlassFab(
                onClick = { showAddDialog = true },
                contentDescription = "Add Reminder",
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
                                "Search reminders, notes...",
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
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
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

            // Filter Tabs with Counts
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ReminderFilter.entries) { filter ->
                    val isSelected = activeFilter == filter
                    val chipBg = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else GlassSurfaceDark
                    val chipBorder = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f) else GlassBorderDark
                    val chipText = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

                    val countBadge = when (filter) {
                        ReminderFilter.ACTIVE -> " ($activeCount)"
                        ReminderFilter.COMPLETED -> " ($completedCount)"
                        ReminderFilter.ALL -> " (${reminders.size})"
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(chipBg)
                            .border(1.dp, chipBorder, RoundedCornerShape(100.dp))
                            .bouncyClickable(pressedScale = 0.94f) { activeFilter = filter }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "${filter.label}$countBadge",
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

            if (reminders.isEmpty()) {
                CommonEmptyState(
                    icon = Icons.Outlined.Alarm,
                    title = "No Reminders Scheduled",
                    description = "Add personal reminders for appointments, servicing, follow-ups, and recurring tasks with custom alerts.",
                    actionLabel = "Add First Reminder",
                    onActionClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxSize()
                )
            } else if (filteredReminders.isEmpty()) {
                val emptyDesc = when (activeFilter) {
                    ReminderFilter.COMPLETED -> "No completed reminders in history yet."
                    ReminderFilter.ACTIVE -> "All reminders completed! Enjoy your day or add a new one."
                    ReminderFilter.ALL -> "No reminders found for \"$searchQuery\"."
                }
                CommonEmptyState(
                    icon = if (activeFilter == ReminderFilter.COMPLETED) Icons.Outlined.TaskAlt else Icons.Outlined.SearchOff,
                    title = if (activeFilter == ReminderFilter.COMPLETED) "No Completed Reminders" else "No Matching Reminders",
                    description = emptyDesc,
                    actionLabel = if (searchQuery.isNotBlank()) "Clear Search" else "View Active Reminders",
                    onActionClick = {
                        searchQuery = ""
                        activeFilter = ReminderFilter.ACTIVE
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredReminders, key = { it.id }) { reminder ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(getCardBackground())
                                .border(1.dp, getCardBorder(), RoundedCornerShape(18.dp))
                                .glassCardClickable { selectedReminder = reminder }
                                .padding(12.dp)
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

                                Spacer(modifier = Modifier.width(6.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = reminder.title,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            textDecoration = if (reminder.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                        ),
                                        color = if (reminder.isCompleted) TextMutedDark else MaterialTheme.colorScheme.onBackground
                                    )

                                    if (reminder.description.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = reminder.description,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                            color = TextMutedDark,
                                            maxLines = 1
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (reminder.isCompleted && reminder.completedAtMillis != null) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Outlined.CheckCircle,
                                                    contentDescription = null,
                                                    tint = StatusActive,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Done: ${DateUtils.formatDateTime(reminder.completedAtMillis)}",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                    color = TextMutedDark
                                                )
                                            }
                                        } else {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Schedule,
                                                    contentDescription = null,
                                                    tint = TextMutedDark,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = DateUtils.formatDateTime(reminder.dueDateTimeMillis),
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                    color = TextMutedDark
                                                )
                                            }
                                        }

                                        if (reminder.repeatOption != "NONE") {
                                            BadgeTag(
                                                text = ReminderSchedulerUtils.getRepeatLabel(reminder.repeatOption),
                                                backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                                                textColor = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                // Quick Reschedule Action Button
                                IconButton(
                                    onClick = { reschedulingReminder = reminder },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Update,
                                        contentDescription = "Reschedule Reminder",
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditReminderDialog(
            onDismiss = { showAddDialog = false },
            onSave = { viewModel.saveReminder(it) }
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

    editingReminder?.let { reminder ->
        AddEditReminderDialog(
            initialReminder = reminder,
            onDismiss = { editingReminder = null },
            onSave = { viewModel.saveReminder(it) }
        )
    }
}
