package com.example.data.local

import androidx.room.*
import com.example.data.model.WarrantyItem
import kotlinx.coroutines.flow.Flow

@Dao
interface WarrantyDao {
    @Query("SELECT * FROM warranties ORDER BY expiryDateMillis ASC, productName ASC")
    fun getAllWarranties(): Flow<List<WarrantyItem>>

    @Query("SELECT * FROM warranties WHERE id = :id")
    suspend fun getWarrantyById(id: Long): WarrantyItem?

    @Query("SELECT * FROM warranties WHERE expiryDateMillis >= :nowMillis AND expiryDateMillis <= :upcomingThresholdMillis ORDER BY expiryDateMillis ASC")
    fun getExpiringSoonWarranties(nowMillis: Long, upcomingThresholdMillis: Long): Flow<List<WarrantyItem>>

    @Query("SELECT * FROM warranties WHERE productName LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%'")
    fun searchWarranties(query: String): Flow<List<WarrantyItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWarranty(warranty: WarrantyItem): Long

    @Update
    suspend fun updateWarranty(warranty: WarrantyItem)

    @Delete
    suspend fun deleteWarranty(warranty: WarrantyItem)

    @Query("DELETE FROM warranties WHERE id = :id")
    suspend fun deleteWarrantyById(id: Long)

    @Query("DELETE FROM warranties")
    suspend fun clearAllWarranties()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(warranties: List<WarrantyItem>)

    @Query("SELECT * FROM warranties")
    suspend fun getAllWarrantiesList(): List<WarrantyItem>
}
