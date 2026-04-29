package com.example.triptip_yaron_and_alon.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.triptip_yaron_and_alon.data.local.database.entities.TripDayEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDayDao {
    @Query("SELECT * FROM simple_days WHERE tripId = :tripId ORDER BY dayOrder ASC")
    fun getDaysByTrip(tripId: String): Flow<List<TripDayEntity>>

    @Query("SELECT * FROM simple_days WHERE id = :dayId LIMIT 1")
    fun getDayById(dayId: String): Flow<TripDayEntity?>

    @Query("SELECT * FROM simple_days WHERE tripId = :tripId ORDER BY dayOrder ASC")
    suspend fun getDaysForTripOnce(tripId: String): List<TripDayEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(day: TripDayEntity)

    @Query("DELETE FROM simple_days WHERE id = :dayId")
    suspend fun deleteById(dayId: String)

    @Query("SELECT d.* FROM simple_days d INNER JOIN simple_trips t ON t.id = d.tripId WHERE t.userId = :userId")
    fun getDaysByUserId(userId: String): Flow<List<TripDayEntity>>
}
