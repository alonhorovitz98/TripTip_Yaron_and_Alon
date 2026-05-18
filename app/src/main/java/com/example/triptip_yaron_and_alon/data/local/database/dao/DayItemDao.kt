package com.example.triptip_yaron_and_alon.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.triptip_yaron_and_alon.data.local.database.entities.DayItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DayItemDao {
    @Query("SELECT * FROM simple_items WHERE dayId = :dayId ORDER BY sortOrder ASC, id ASC")
    fun getItemsByDay(dayId: String): Flow<List<DayItemEntity>>

    @Query("SELECT * FROM simple_items WHERE dayId IN (SELECT id FROM simple_days WHERE tripId = :tripId)")
    fun getItemsForTrip(tripId: String): Flow<List<DayItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: DayItemEntity)

    @Query("DELETE FROM simple_items WHERE id = :itemId")
    suspend fun deleteById(itemId: String)
}
