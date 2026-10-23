package com.example.data.local

import androidx.room.*
import com.example.data.model.PaymentItem
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY isPaid ASC, dueDateMillis ASC")
    fun getAllPayments(): Flow<List<PaymentItem>>

    @Query("SELECT * FROM payments WHERE id = :id")
    suspend fun getPaymentById(id: Long): PaymentItem?

    @Query("SELECT * FROM payments WHERE isPaid = 0 AND dueDateMillis <= :upcomingThresholdMillis ORDER BY dueDateMillis ASC")
    fun getUpcomingPayments(upcomingThresholdMillis: Long): Flow<List<PaymentItem>>

    @Query("SELECT * FROM payments WHERE name LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%'")
    fun searchPayments(query: String): Flow<List<PaymentItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentItem): Long

    @Update
    suspend fun updatePayment(payment: PaymentItem)

    @Delete
    suspend fun deletePayment(payment: PaymentItem)

    @Query("DELETE FROM payments WHERE id = :id")
    suspend fun deletePaymentById(id: Long)

    @Query("UPDATE payments SET isPaid = :isPaid WHERE id = :id")
    suspend fun updatePaidStatus(id: Long, isPaid: Boolean)

    @Query("DELETE FROM payments")
    suspend fun clearAllPayments()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(payments: List<PaymentItem>)

    @Query("SELECT * FROM payments")
    suspend fun getAllPaymentsList(): List<PaymentItem>
}
