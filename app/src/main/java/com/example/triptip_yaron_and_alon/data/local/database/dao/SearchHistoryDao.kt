package com.example.triptip_yaron_and_alon.data.local.database.dao

import androidx.room.*
import com.example.triptip_yaron_and_alon.data.local.database.entities.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for search history.
 * Manages user's recent search queries.
 */
@Dao
interface SearchHistoryDao {
    
    /**
     * Get recent search history, ordered by most recent first.
     * @param limit Maximum number of results (default 10)
     * @return Flow of recent searches
     */
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSearches(limit: Int = 10): Flow<List<SearchHistoryEntity>>
    
    /**
     * Get all search history.
     * @return Flow of all searches
     */
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC")
    fun getAllSearches(): Flow<List<SearchHistoryEntity>>
    
    /**
     * Search for a specific query in history.
     * @param query The search query
     * @return Flow of matching search history entry
     */
    @Query("SELECT * FROM search_history WHERE query = :query LIMIT 1")
    fun findByQuery(query: String): Flow<SearchHistoryEntity?>
    
    /**
     * Insert a new search query.
     * If query exists, it will be replaced with new timestamp.
     * @param searchHistory The search history entry
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(searchHistory: SearchHistoryEntity)
    
    /**
     * Delete a specific search history entry.
     * @param searchHistory The entry to delete
     */
    @Delete
    suspend fun delete(searchHistory: SearchHistoryEntity)
    
    /**
     * Delete a search by query string.
     * @param query The query to delete
     */
    @Query("DELETE FROM search_history WHERE query = :query")
    suspend fun deleteByQuery(query: String)
    
    /**
     * Clear all search history.
     */
    @Query("DELETE FROM search_history")
    suspend fun clearAll()
    
    /**
     * Delete old search history (older than threshold).
     * @param threshold Timestamp threshold
     */
    @Query("DELETE FROM search_history WHERE timestamp < :threshold")
    suspend fun deleteOldSearches(threshold: Long)
}
