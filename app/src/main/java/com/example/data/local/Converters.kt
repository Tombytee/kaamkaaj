package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.DocumentAttachment
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val attachmentListType = Types.newParameterizedType(
        List::class.java,
        DocumentAttachment::class.java
    )
    private val attachmentListAdapter = moshi.adapter<List<DocumentAttachment>>(attachmentListType)

    @TypeConverter
    fun fromAttachmentList(attachments: List<DocumentAttachment>?): String {
        return if (attachments.isNullOrEmpty()) {
            "[]"
        } else {
            try {
                attachmentListAdapter.toJson(attachments)
            } catch (e: Exception) {
                "[]"
            }
        }
    }

    @TypeConverter
    fun toAttachmentList(json: String?): List<DocumentAttachment> {
        return if (json.isNullOrBlank() || json == "[]") {
            emptyList()
        } else {
            try {
                attachmentListAdapter.fromJson(json) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
