package com.example.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val dateTimeFormatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

    @Volatile
    private var cachedTodayMidnight: Long = 0L
    @Volatile
    private var lastMidnightCheckTime: Long = 0L
    @Volatile
    private var cachedTzOffset: Long = 0L

    private fun getTodayMidnight(): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        if (now - lastMidnightCheckTime > 30_000L || cachedTodayMidnight == 0L) {
            val tz = TimeZone.getDefault().getOffset(now).toLong()
            cachedTzOffset = tz
            cachedTodayMidnight = ((now + tz) / 86_400_000L) * 86_400_000L - tz
            lastMidnightCheckTime = now
        }
        return cachedTodayMidnight to cachedTzOffset
    }

    @Synchronized
    fun formatDate(millis: Long?): String {
        if (millis == null) return "No date"
        return dateFormatter.format(Date(millis))
    }

    @Synchronized
    fun formatDateTime(millis: Long?): String {
        if (millis == null) return "No date"
        return dateTimeFormatter.format(Date(millis))
    }

    @Synchronized
    fun formatTime(millis: Long?): String {
        if (millis == null) return ""
        return timeFormatter.format(Date(millis))
    }

    fun getDaysDifference(targetMillis: Long): Long {
        val (todayMidnight, tzOffset) = getTodayMidnight()
        val targetMidnight = ((targetMillis + tzOffset) / 86_400_000L) * 86_400_000L - tzOffset
        val diff = targetMidnight - todayMidnight
        return diff / 86_400_000L
    }

    fun calculateWarrantyExpiry(purchaseDateMillis: Long, months: Int): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = purchaseDateMillis
            add(Calendar.MONTH, months)
        }
        return calendar.timeInMillis
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePickerDialog(
    initialDateMillis: Long = System.currentTimeMillis(),
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillis
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { onDateSelected(it) }
                    onDismiss()
                }
            ) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTimePickerDialog(
    initialHour: Int = 10,
    initialMinute: Int = 0,
    onTimeSelected: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onTimeSelected(timePickerState.hour, timePickerState.minute)
                    onDismiss()
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        text = {
            TimePicker(state = timePickerState)
        }
    )
}
