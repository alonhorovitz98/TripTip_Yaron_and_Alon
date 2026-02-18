package com.example.triptip_yaron_and_alon.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.triptip_yaron_and_alon.data.local.database.dao.*
import com.example.triptip_yaron_and_alon.data.local.database.entities.*

@Database(
    entities = [
        PostEntity::class,
        UserEntity::class,
        TripEntity::class,
        TripDayEntity::class,
        TripItemEntity::class,
        // New entities for social features
        CommentEntity::class,
        NotificationEntity::class,
        SearchHistoryEntity::class
    ],
    version = 2, // Incremented for new entities
    exportSchema = false
)
abstract class TripTipDatabase : RoomDatabase() {
    
    abstract fun postDao(): PostDao
    abstract fun userDao(): UserDao
    abstract fun tripDao(): TripDao
    abstract fun tripDayDao(): TripDayDao
    abstract fun tripItemDao(): TripItemDao
    // New DAOs for social features
    abstract fun commentDao(): CommentDao
    abstract fun notificationDao(): NotificationDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    
    companion object {
        @Volatile
        private var INSTANCE: TripTipDatabase? = null
        
        fun getDatabase(context: Context): TripTipDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TripTipDatabase::class.java,
                    "triptip_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

