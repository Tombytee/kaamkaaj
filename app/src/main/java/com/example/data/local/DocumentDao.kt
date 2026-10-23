package com.example.data.local

import androidx.room.*
import com.example.data.model.DocumentItem
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY expiryDateMillis ASC, name ASC")
    fun getAllDocuments(): Flow<List<DocumentItem>>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getDocumentById(id: Long): DocumentItem?

    @Query("SELECT * FROM documents WHERE expiryDateMillis IS NOT NULL AND expiryDateMillis >= :nowMillis AND expiryDateMillis <= :upcomingThresholdMillis ORDER BY expiryDateMillis ASC")
    fun getExpiringSoonDocuments(nowMillis: Long, upcomingThresholdMillis: Long): Flow<List<DocumentItem>>

    @Query("SELECT * FROM documents WHERE name LIKE '%' || :query || '%' OR documentNumber LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%'")
    fun searchDocuments(query: String): Flow<List<DocumentItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentItem): Long

    @Update
    suspend fun updateDocument(document: DocumentItem)

    @Delete
    suspend fun deleteDocument(document: DocumentItem)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocumentById(id: Long)

    @Query("DELETE FROM documents")
    suspend fun clearAllDocuments()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(documents: List<DocumentItem>)

    @Query("SELECT * FROM documents")
    suspend fun getAllDocumentsList(): List<DocumentItem>
}
