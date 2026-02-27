package com.example.triptip_yaron_and_alon.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 5, // Added imageUrl to comments
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
        
        /**
         * Migration from version 2 to 3:
         * - Makes postId nullable (was NOT NULL in version 2)
         * - Adds placeId column (nullable)
         * - SQLite doesn't support changing column nullability with ALTER TABLE,
         *   so we need to recreate the table
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Step 1: Create new table with correct schema (postId and placeId nullable)
                database.execSQL("""
                    CREATE TABLE trip_items_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        dayId TEXT NOT NULL,
                        postId TEXT,
                        placeId TEXT,
                        `order` INTEGER NOT NULL,
                        notes TEXT,
                        cachedAt INTEGER NOT NULL,
                        FOREIGN KEY(dayId) REFERENCES trip_days(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                
                // Step 2: Copy data from old table to new table
                // Set placeId to NULL for existing rows (they don't have placeId yet)
                database.execSQL("""
                    INSERT INTO trip_items_new (id, dayId, postId, placeId, `order`, notes, cachedAt)
                    SELECT id, dayId, postId, NULL as placeId, `order`, notes, cachedAt
                    FROM trip_items
                """.trimIndent())
                
                // Step 3: Drop old table
                database.execSQL("DROP TABLE trip_items")
                
                // Step 4: Rename new table to original name
                database.execSQL("ALTER TABLE trip_items_new RENAME TO trip_items")
                
                // Step 5: Recreate indices
                database.execSQL("CREATE INDEX index_trip_items_dayId ON trip_items(dayId)")
                database.execSQL("CREATE INDEX index_trip_items_postId ON trip_items(postId)")
                database.execSQL("CREATE INDEX index_trip_items_placeId ON trip_items(placeId)")
            }
        }
        
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE posts ADD COLUMN likedBy TEXT NOT NULL DEFAULT ''")
            }
        }
        
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE comments ADD COLUMN imageUrl TEXT")
            }
        }
        
        fun getDatabase(context: Context): TripTipDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TripTipDatabase::class.java,
                    "triptip_database"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration() // Fallback if migration fails
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

