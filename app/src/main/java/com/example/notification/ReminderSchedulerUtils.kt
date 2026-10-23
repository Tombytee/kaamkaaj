package com.example.notification

import java.util.Calendar

object ReminderSchedulerUtils {

    val REPEAT_OPTIONS = listOf(
        "NONE" to "Does not repeat",
        "DAILY" to "Daily",
        "WEEKDAYS" to "Every Weekday (Mon–Fri)",
        "WEEKENDS" to "Every Weekend (Sat–Sun)",
        "WEEKLY" to "Weekly",
        "BIWEEKLY" to "Every 2 Weeks",
        "MONTHLY" to "Monthly",
        "YEARLY" to "Yearly"
    )

    fun getRepeatLabel(repeatOption: String): String {
        return REPEAT_OPTIONS.find { it.first == repeatOption }?.second ?: when (repeatOption) {
            "NONE" -> "Does not repeat"
            "DAILY" -> "Daily"
            "WEEKDAYS" -> "Weekdays"
            "WEEKENDS" -> "Weekends"
            "WEEKLY" -> "Weekly"
            "BIWEEKLY" -> "Every 2 Weeks"
            "MONTHLY" -> "Monthly"
            "YEARLY" -> "Yearly"
            else -> repeatOption
        }
    }

    /**
     * Calculates the next occurrence timestamp in milliseconds for a repeating reminder.
     * Ensures the returned time is strictly in the future (> fromMillis) while preserving
     * the hour, minute, and second of the original schedule.
     */
    fun calculateNextOccurrence(
        baseMillis: Long,
        repeatOption: String,
        fromMillis: Long = System.currentTimeMillis()
    ): Long {
        if (repeatOption == "NONE" || repeatOption.isBlank()) {
            return baseMillis
        }

        val cal = Calendar.getInstance().apply {
            timeInMillis = baseMillis
        }

        var guardIterations = 0
        while (cal.timeInMillis <= fromMillis && guardIterations < 1000) {
            guardIterations++
            when (repeatOption) {
                "DAILY" -> {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
                "WEEKDAYS" -> {
                    do {
                        cal.add(Calendar.DAY_OF_YEAR, 1)
                    } while (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
                }
                "WEEKENDS" -> {
                    do {
                        cal.add(Calendar.DAY_OF_YEAR, 1)
                    } while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY && cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY)
                }
                "WEEKLY" -> {
                    cal.add(Calendar.WEEK_OF_YEAR, 1)
                }
                "BIWEEKLY" -> {
                    cal.add(Calendar.WEEK_OF_YEAR, 2)
                }
                "MONTHLY" -> {
                    cal.add(Calendar.MONTH, 1)
                }
                "YEARLY" -> {
                    cal.add(Calendar.YEAR, 1)
                }
                else -> {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
        }

        return cal.timeInMillis
    }

    /**
     * Preset for +N hours from current time.
     */
    fun getPresetInHours(hours: Int): Long {
        val cal = Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, hours)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    /**
     * Preset for Tonight (8:00 PM / 20:00). If already past 8:00 PM, schedules for tomorrow 8:00 PM.
     */
    fun getPresetTonight(): Long {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= now) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    /**
     * Preset for Tomorrow Morning (9:00 AM).
     */
    fun getPresetTomorrowMorning(): Long {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    /**
     * Preset for Tomorrow Evening (6:00 PM / 18:00).
     */
    fun getPresetTomorrowEvening(): Long {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 18)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    /**
     * Preset for Upcoming Weekend (Saturday 10:00 AM).
     */
    fun getPresetThisWeekend(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY || cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    /**
     * Preset for Next Week (Next Monday 9:00 AM).
     */
    fun getPresetNextWeek(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, 1)
        }
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY || cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }
}
