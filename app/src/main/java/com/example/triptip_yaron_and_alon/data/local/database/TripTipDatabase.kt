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
        DayItemEntity::class,
        CommentEntity::class,
        NotificationEntity::class,
        SearchHistoryEntity::class
    ],
    version = 8,
    exportSchema = true
)
abstract class TripTipDatabase : RoomDatabase() {

    abstract fun postDao(): PostDao
    abstract fun userDao(): UserDao
    abstract fun tripDao(): TripDao
    abstract fun tripDayDao(): TripDayDao
    abstract fun dayItemDao(): DayItemDao
    abstract fun commentDao(): CommentDao
    abstract fun notificationDao(): NotificationDao
    abstract fun searchHistoryDao(): SearchHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: TripTipDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
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
                """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT INTO trip_items_new (id, dayId, postId, placeId, `order`, notes, cachedAt)
                    SELECT id, dayId, postId, NULL as placeId, `order`, notes, cachedAt
                    FROM trip_items
                """.trimIndent()
                )
                database.execSQL("DROP TABLE trip_items")
                database.execSQL("ALTER TABLE trip_items_new RENAME TO trip_items")
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

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE posts ADD COLUMN priceLevel INTEGER")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE trip_days ADD COLUMN description TEXT")
            }
        }

        /**
         * Replaces legacy [trips] / [trip_days] / [trip_items] with a minimal local-only schema.
         * Old trip data is dropped.
         */
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `trip_items`")
                db.execSQL("DROP TABLE IF EXISTS `trip_days`")
                db.execSQL("DROP TABLE IF EXISTS `trips`")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `simple_trips` (
                        `id` TEXT NOT NULL, `userId` TEXT NOT NULL, `name` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `simple_days` (
                        `id` TEXT NOT NULL, `tripId` TEXT NOT NULL, `dayOrder` INTEGER NOT NULL, `dateMillis` INTEGER,
                        PRIMARY KEY(`id`), FOREIGN KEY(`tripId`) REFERENCES `simple_trips`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_simple_days_tripId` ON `simple_days` (`tripId`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `simple_items` (
                        `id` TEXT NOT NULL, `dayId` TEXT NOT NULL, `type` TEXT NOT NULL, `value` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL,
                        PRIMARY KEY(`id`), FOREIGN KEY(`dayId`) REFERENCES `simple_days`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_simple_items_dayId` ON `simple_items` (`dayId`)")
            }
        }

        fun getDatabase(context: Context): TripTipDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TripTipDatabase::class.java,
                    "triptip_database"
                )
                    .addMigrations(
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
