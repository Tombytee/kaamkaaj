package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.DocumentItem
import com.example.data.model.PaymentItem
import com.example.data.model.ReminderItem
import com.example.data.model.WarrantyItem

@Database(
    entities = [
        DocumentItem::class,
        PaymentItem::class,
        WarrantyItem::class,
        ReminderItem::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class KaamKaajDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun paymentDao(): PaymentDao
    abstract fun warrantyDao(): WarrantyDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: KaamKaajDatabase? = null

        fun getDatabase(context: Context): KaamKaajDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KaamKaajDatabase::class.java,
                    "kaamkaaj_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
