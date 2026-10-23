package com.example.data.local

import androidx.room.*
import com.example.data.model.ReminderItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY isCompleted ASC, dueDateTimeMillis ASC")
    fun getAllReminders(): Flow<List<ReminderItem>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Long): ReminderItem?

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 AND dueDateTimeMillis <= :upcomingThresholdMillis ORDER BY dueDateTimeMillis ASC")
    fun getUpcomingReminders(upcomingThresholdMillis: Long): Flow<List<ReminderItem>>

    @Query("SELECT * FROM reminders WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchReminders(query: String): Flow<List<ReminderItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderItem): Long

    @Update
    suspend fun updateReminder(reminder: ReminderItem)

    @Delete
    suspend fun deleteReminder(reminder: ReminderItem)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Long)

    @Query("UPDATE reminders SET isCompleted = :isCompleted, completedAtMillis = :completedAtMillis WHERE id = :id")
    suspend fun updateCompletedStatus(id: Long, isCompleted: Boolean, completedAtMillis: Long? = null)

    @Query("UPDATE reminders SET dueDateTimeMillis = :newDueDateTimeMillis, isCompleted = 0, completedAtMillis = NULL WHERE id = :id")
    suspend fun rescheduleReminder(id: Long, newDueDateTimeMillis: Long)

    @Query("DELETE FROM reminders")
    suspend fun clearAllReminders()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reminders: List<ReminderItem>)

    @Query("SELECT * FROM reminders")
    suspend fun getAllRemindersList(): List<ReminderItem>
}
