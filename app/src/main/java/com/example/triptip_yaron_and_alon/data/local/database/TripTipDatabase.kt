package com.example.triptip_yaron_and_alon.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.triptip_yaron_and_alon.data.local.database.dao.PostDao
import com.example.triptip_yaron_and_alon.data.local.database.dao.TripDao
import com.example.triptip_yaron_and_alon.data.local.database.dao.TripDayDao
import com.example.triptip_yaron_and_alon.data.local.database.dao.TripItemDao
import com.example.triptip_yaron_and_alon.data.local.database.dao.UserDao
import com.example.triptip_yaron_and_alon.data.local.database.entities.PostEntity
import com.example.triptip_yaron_and_alon.data.local.database.entities.TripDayEntity
import com.example.triptip_yaron_and_alon.data.local.database.entities.TripEntity
import com.example.triptip_yaron_and_alon.data.local.database.entities.TripItemEntity
import com.example.triptip_yaron_and_alon.data.local.database.entities.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

@Database(
    entities = [
        PostEntity::class,
        UserEntity::class,
        TripEntity::class,
        TripDayEntity::class,
        TripItemEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class TripTipDatabase : RoomDatabase() {
    
    abstract fun postDao(): PostDao
    abstract fun userDao(): UserDao
    abstract fun tripDao(): TripDao
    abstract fun tripDayDao(): TripDayDao
    abstract fun tripItemDao(): TripItemDao
    
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
                    .setQueryExecutor(Dispatchers.IO.asExecutor())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

