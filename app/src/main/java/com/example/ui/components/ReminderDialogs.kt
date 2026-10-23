package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ReminderItem
import com.example.notification.ReminderSchedulerUtils
import com.example.ui.theme.*
import java.util.Calendar

val REMINDER_REPEAT_OPTIONS = ReminderSchedulerUtils.REPEAT_OPTIONS

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditReminderDialog(
    initialReminder: ReminderItem? = null,
    onDismiss: () -> Unit,
    onSave: (ReminderItem) -> Unit
) {
    var title by remember { mutableStateOf(initialReminder?.title ?: "") }
    var description by remember { mutableStateOf(initialReminder?.description ?: "") }
    var dueDateTimeMillis by remember {
        mutableStateOf(
            initialReminder?.dueDateTimeMillis ?: (System.currentTimeMillis() + 60 * 60 * 1000)
        )
    }
    var repeatOption by remember { mutableStateOf(initialReminder?.repeatOption ?: "NONE") }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var repeatExpanded by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }

    AppFormDialog(
        onDismiss = onDismiss,
        title = if (initialReminder == null) "Add Reminder" else "Edit Reminder",
        subtitle = "Schedule timely notifications for appointments, tasks, and follow-ups",
        categoryBadgeText = "REMINDERS & TASKS",
        icon = Icons.Default.NotificationsActive,
        iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
        iconContentColor = MaterialTheme.colorScheme.tertiary,
        confirmButtonText = "SAVE REMINDER",
        onConfirm = {
            if (title.isBlank()) {
                hasError = true
                return@AppFormDialog
            }
            val item = (initialReminder ?: ReminderItem(title = "", dueDateTimeMillis = 0)).copy(
                title = title.trim(),
                description = description.trim(),
                dueDateTimeMillis = dueDateTimeMillis,
                repeatOption = repeatOption
            )
            onSave(item)
            onDismiss()
        }
    ) {
        FormSectionHeader(title = "Reminder Details")

        OutlinedTextField(
            value = title,
            onValueChange = {
                title = it
                if (it.isNotBlank()) hasError = false
            },
            label = { Text("Reminder Title *") },
            placeholder = { Text("e.g. Doctor appointment, Car service, Call bank") },
            isError = hasError && title.isBlank(),
            supportingText = if (hasError && title.isBlank()) {
                { Text("Title is required") }
            } else null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description / Details (Optional)") },
            placeholder = { Text("Add instructions, address or checklist...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            minLines = 2,
            maxLines = 4
        )

        FormSectionHeader(title = "Schedule & Frequency")

        // Quick Presets
        Text(
            text = "Quick Presets",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = TextMutedDark
            )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickPresetChip(label = "+1 Hour", icon = Icons.Outlined.HourglassTop) {
                dueDateTimeMillis = ReminderSchedulerUtils.getPresetInHours(1)
            }
            QuickPresetChip(label = "Tonight (8 PM)", icon = Icons.Outlined.Bedtime) {
                dueDateTimeMillis = ReminderSchedulerUtils.getPresetTonight()
            }
            QuickPresetChip(label = "Tomorrow 9 AM", icon = Icons.Outlined.WbSunny) {
                dueDateTimeMillis = ReminderSchedulerUtils.getPresetTomorrowMorning()
            }
            QuickPresetChip(label = "Tomorrow 6 PM", icon = Icons.Outlined.NightsStay) {
                dueDateTimeMillis = ReminderSchedulerUtils.getPresetTomorrowEvening()
            }
            QuickPresetChip(label = "This Weekend", icon = Icons.Outlined.Weekend) {
                dueDateTimeMillis = ReminderSchedulerUtils.getPresetThisWeekend()
            }
            QuickPresetChip(label = "Next Week", icon = Icons.Outlined.CalendarMonth) {
                dueDateTimeMillis = ReminderSchedulerUtils.getPresetNextWeek()
            }
        }

        // Date Picker Row
        OutlinedTextField(
            value = DateUtils.formatDate(dueDateTimeMillis),
            onValueChange = {},
            readOnly = true,
            label = { Text("Date") },
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.CalendarToday, contentDescription = "Select Date")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePicker = true },
            shape = RoundedCornerShape(16.dp)
        )

        // Time Picker Row
        OutlinedTextField(
            value = DateUtils.formatTime(dueDateTimeMillis),
            onValueChange = {},
            readOnly = true,
            label = { Text("Time") },
            trailingIcon = {
                IconButton(onClick = { showTimePicker = true }) {
                    Icon(Icons.Default.Schedule, contentDescription = "Select Time")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showTimePicker = true },
            shape = RoundedCornerShape(16.dp)
        )

        // Repeat dropdown
        ExposedDropdownMenuBox(
            expanded = repeatExpanded,
            onExpandedChange = { repeatExpanded = !repeatExpanded }
        ) {
            OutlinedTextField(
                value = ReminderSchedulerUtils.getRepeatLabel(repeatOption),
                onValueChange = {},
                readOnly = true,
                label = { Text("Repeat Frequency") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = repeatExpanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )
            ExposedDropdownMenu(
                expanded = repeatExpanded,
                onDismissRequest = { repeatExpanded = false }
            ) {
                REMINDER_REPEAT_OPTIONS.forEach { (repKey, repLabel) ->
                    DropdownMenuItem(
                        text = { Text(repLabel) },
                        onClick = {
                            repeatOption = repKey
                            repeatExpanded = false
                        }
                    )
                }
            }
        }

        // Notification summary badge
        val repeatText = if (repeatOption != "NONE") " • Repeats ${ReminderSchedulerUtils.getRepeatLabel(repeatOption)}" else ""
        ReminderStatusCard(
            enabled = true,
            infoText = "Alert set for ${DateUtils.formatTime(dueDateTimeMillis)} on ${DateUtils.formatDate(dueDateTimeMillis)}$repeatText"
        )
    }

    if (showDatePicker) {
        AppDatePickerDialog(
            initialDateMillis = dueDateTimeMillis,
            onDateSelected = { selectedDate ->
                val cal = Calendar.getInstance().apply {
                    timeInMillis = dueDateTimeMillis
                }
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                val minute = cal.get(Calendar.MINUTE)

                val newCal = Calendar.getInstance().apply {
                    timeInMillis = selectedDate
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                }
                dueDateTimeMillis = newCal.timeInMillis
            },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showTimePicker) {
        val cal = remember(dueDateTimeMillis) {
            Calendar.getInstance().apply { timeInMillis = dueDateTimeMillis }
        }
        AppTimePickerDialog(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            onTimeSelected = { hour, minute ->
                val newCal = Calendar.getInstance().apply {
                    timeInMillis = dueDateTimeMillis
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                }
                dueDateTimeMillis = newCal.timeInMillis
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

@Composable
private fun QuickPresetChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(GlassSurfaceDark)
            .border(1.dp, GlassBorderDark, RoundedCornerShape(100.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickRescheduleSheet(
    reminder: ReminderItem,
    onDismiss: () -> Unit,
    onReschedule: (Long) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var customDateMillis by remember { mutableStateOf(reminder.dueDateTimeMillis) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Quick Reschedule",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = reminder.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMutedDark,
                        maxLines = 1
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.Update,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            HorizontalDivider(color = GlassBorderDark)

            Text(
                text = "Choose when to be reminded:",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondaryDark
                )
            )

            val options = listOf(
                Triple("In 1 Hour", Icons.Outlined.HourglassTop) {
                    onReschedule(ReminderSchedulerUtils.getPresetInHours(1))
                    onDismiss()
                },
                Triple("In 3 Hours", Icons.Outlined.HourglassBottom) {
                    onReschedule(ReminderSchedulerUtils.getPresetInHours(3))
                    onDismiss()
                },
                Triple("Tonight (8:00 PM)", Icons.Outlined.Bedtime) {
                    onReschedule(ReminderSchedulerUtils.getPresetTonight())
                    onDismiss()
                },
                Triple("Tomorrow Morning (9:00 AM)", Icons.Outlined.WbSunny) {
                    onReschedule(ReminderSchedulerUtils.getPresetTomorrowMorning())
                    onDismiss()
                },
                Triple("Tomorrow Evening (6:00 PM)", Icons.Outlined.NightsStay) {
                    onReschedule(ReminderSchedulerUtils.getPresetTomorrowEvening())
                    onDismiss()
                },
                Triple("This Weekend (Sat 10:00 AM)", Icons.Outlined.Weekend) {
                    onReschedule(ReminderSchedulerUtils.getPresetThisWeekend())
                    onDismiss()
                },
                Triple("Next Week (Mon 9:00 AM)", Icons.Outlined.CalendarMonth) {
                    onReschedule(ReminderSchedulerUtils.getPresetNextWeek())
                    onDismiss()
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(GlassSurfaceDark)
                    .border(1.dp, GlassBorderDark, RoundedCornerShape(16.dp))
            ) {
                options.forEachIndexed { index, (label, icon, action) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { action() }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = TextMutedDark,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (index < options.lastIndex) {
                        HorizontalDivider(color = GlassBorderDark.copy(alpha = 0.5f))
                    }
                }
            }

            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderDark)
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Pick Custom Date & Time...",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }

    if (showDatePicker) {
        AppDatePickerDialog(
            initialDateMillis = customDateMillis,
            onDateSelected = { selectedDate ->
                val cal = Calendar.getInstance().apply { timeInMillis = customDateMillis }
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                val minute = cal.get(Calendar.MINUTE)
                val newCal = Calendar.getInstance().apply {
                    timeInMillis = selectedDate
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                }
                customDateMillis = newCal.timeInMillis
                showDatePicker = false
                showTimePicker = true
            },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showTimePicker) {
        val cal = remember(customDateMillis) {
            Calendar.getInstance().apply { timeInMillis = customDateMillis }
        }
        AppTimePickerDialog(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            onTimeSelected = { hour, minute ->
                val newCal = Calendar.getInstance().apply {
                    timeInMillis = customDateMillis
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                }
                showTimePicker = false
                onReschedule(newCal.timeInMillis)
                onDismiss()
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderDetailsSheet(
    reminder: ReminderItem,
    onDismiss: () -> Unit,
    onToggleCompleted: () -> Unit,
    onReschedule: () -> Unit = {},
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
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                ReminderStatusBadge(
                    isCompleted = reminder.isCompleted,
                    dueDateTimeMillis = reminder.dueDateTimeMillis
                )
            }

            HorizontalDivider(color = GlassBorderDark)

            DetailRow(
                label = "Scheduled Date & Time",
                value = "${DateUtils.formatDate(reminder.dueDateTimeMillis)} at ${DateUtils.formatTime(reminder.dueDateTimeMillis)}"
            )

            DetailRow(
                label = "Repeat Frequency",
                value = ReminderSchedulerUtils.getRepeatLabel(reminder.repeatOption)
            )

            if (reminder.isCompleted && reminder.completedAtMillis != null) {
                DetailRow(
                    label = "Completion History",
                    value = "Completed on ${DateUtils.formatDateTime(reminder.completedAtMillis)}"
                )
            }

            // Reminder Notification Status
            ReminderStatusCard(
                enabled = !reminder.isCompleted,
                infoText = if (reminder.isCompleted) {
                    "Reminder marked as completed"
                } else {
                    "Alert scheduled for ${DateUtils.formatDate(reminder.dueDateTimeMillis)} at ${DateUtils.formatTime(reminder.dueDateTimeMillis)}"
                }
            )

            if (reminder.description.isNotBlank()) {
                DetailRow(label = "Description / Details", value = reminder.description)
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action row: Mark Done / Pending & Quick Reschedule
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        onToggleCompleted()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (reminder.isCompleted) Color(0xFF1E293B) else StatusActive,
                        contentColor = if (reminder.isCompleted) TextSecondaryDark else Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (reminder.isCompleted) Icons.Default.Replay else Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (reminder.isCompleted) "RE-ACTIVATE" else "MARK DONE",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                }

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onReschedule()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.Update, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "RESCHEDULE",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
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
                    "Delete Reminder?",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
                    color = StatusExpired
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete \"${reminder.title}\"? This action cannot be undone.",
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
fun ReminderDetailBottomSheet(
    reminder: ReminderItem,
    onDismiss: () -> Unit,
    onToggleCompleted: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) = ReminderDetailsSheet(
    reminder = reminder,
    onDismiss = onDismiss,
    onToggleCompleted = onToggleCompleted,
    onEdit = onEdit,
    onDelete = onDelete
)
