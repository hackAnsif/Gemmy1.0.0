package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.LearningItem
import kotlinx.coroutines.flow.Flow

@Dao
interface LearningItemDao {

    @Query("SELECT * FROM learning_items ORDER BY createdAtEpochMs DESC")
    fun getAllLearningItems(): Flow<List<LearningItem>>

    @Query("SELECT * FROM learning_items WHERE id = :id")
    suspend fun getLearningItemById(id: Long): LearningItem?

    @Query("SELECT * FROM learning_items WHERE nextReviewEpochMs <= :currentTimeEpochMs ORDER BY nextReviewEpochMs ASC")
    fun getItemsDueForReview(currentTimeEpochMs: Long): Flow<List<LearningItem>>

    @Query("SELECT * FROM learning_items WHERE nextReviewEpochMs <= :currentTimeEpochMs ORDER BY nextReviewEpochMs ASC")
    suspend fun getItemsDueForReviewOnce(currentTimeEpochMs: Long): List<LearningItem>

    @Query("SELECT * FROM learning_items WHERE status = 'REMEMBERED'")
    fun getRememberedItems(): Flow<List<LearningItem>>

    @Query("SELECT COUNT(*) FROM learning_items")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM learning_items WHERE status = 'REMEMBERED'")
    fun getRememberedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM learning_items WHERE nextReviewEpochMs <= :currentTimeEpochMs")
    fun getDueCount(currentTimeEpochMs: Long): Flow<Int>

    @Query("SELECT * FROM learning_items WHERE title LIKE '%' || :query || '%' OR meaning LIKE '%' || :query || '%' OR example LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%'")
    fun searchLearningItems(query: String): Flow<List<LearningItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: LearningItem): Long

    @Update
    suspend fun update(item: LearningItem)

    @Delete
    suspend fun delete(item: LearningItem)

    @Query("DELETE FROM learning_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM learning_items")
    suspend fun clearAll()
}
