package com.example.triptip_yaron_and_alon.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.triptip_yaron_and_alon.data.local.database.entities.TripEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query("SELECT * FROM simple_trips WHERE userId = :userId ORDER BY createdAt DESC")
    fun getTripsByUser(userId: String): Flow<List<TripEntity>>

    @Query("SELECT * FROM simple_trips WHERE id = :tripId LIMIT 1")
    fun getTripById(tripId: String): Flow<TripEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trip: TripEntity)

    @Query("DELETE FROM simple_trips WHERE id = :tripId")
    suspend fun deleteById(tripId: String)
}
