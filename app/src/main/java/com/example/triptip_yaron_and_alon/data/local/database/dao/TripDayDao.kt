package com.example.triptip_yaron_and_alon.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.triptip_yaron_and_alon.data.local.database.entities.TripDayEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDayDao {
    @Query("SELECT * FROM trip_days WHERE tripId = :tripId ORDER BY dayNumber ASC")
    fun getDaysByTrip(tripId: String): Flow<List<TripDayEntity>>
    
    @Query("SELECT * FROM trip_days WHERE id = :dayId LIMIT 1")
    fun getDayById(dayId: String): Flow<TripDayEntity?>
    
    @Query("SELECT * FROM trip_days ORDER BY dayNumber ASC")
    fun getAllDays(): Flow<List<TripDayEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(day: TripDayEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(days: List<TripDayEntity>)
    
    @Update
    suspend fun update(day: TripDayEntity)
    
    @Delete
    suspend fun delete(day: TripDayEntity)
    
    @Query("DELETE FROM trip_days WHERE id = :dayId")
    suspend fun deleteById(dayId: String)
    
    @Query("DELETE FROM trip_days WHERE tripId = :tripId")
    suspend fun deleteByTripId(tripId: String)
    
    @Query("DELETE FROM trip_days")
    suspend fun deleteAll()
}

