package com.example.triptip_yaron_and_alon.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.triptip_yaron_and_alon.data.local.database.entities.TripItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripItemDao {
    @Query("SELECT * FROM trip_items WHERE dayId = :dayId ORDER BY `order` ASC")
    fun getItemsByDay(dayId: String): Flow<List<TripItemEntity>>
    
    @Query("SELECT * FROM trip_items WHERE id = :itemId LIMIT 1")
    fun getItemById(itemId: String): Flow<TripItemEntity?>
    
    @Query("SELECT * FROM trip_items WHERE postId = :postId")
    fun getItemsByPost(postId: String): Flow<List<TripItemEntity>>
    
    @Query("SELECT * FROM trip_items ORDER BY `order` ASC")
    fun getAllItems(): Flow<List<TripItemEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TripItemEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TripItemEntity>)
    
    @Update
    suspend fun update(item: TripItemEntity)
    
    @Delete
    suspend fun delete(item: TripItemEntity)
    
    @Query("DELETE FROM trip_items WHERE id = :itemId")
    suspend fun deleteById(itemId: String)
    
    @Query("DELETE FROM trip_items WHERE dayId = :dayId")
    suspend fun deleteByDayId(dayId: String)
    
    @Query("DELETE FROM trip_items")
    suspend fun deleteAll()
}

