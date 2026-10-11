package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ReviewLog
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewLogDao {

    @Query("SELECT * FROM review_logs ORDER BY reviewedAtEpochMs DESC")
    fun getAllLogs(): Flow<List<ReviewLog>>

    @Query("SELECT * FROM review_logs WHERE learningItemId = :itemId ORDER BY reviewedAtEpochMs DESC")
    fun getLogsForItem(itemId: Long): Flow<List<ReviewLog>>

    @Query("SELECT COUNT(*) FROM review_logs WHERE reviewedAtEpochMs >= :startOfDayEpochMs")
    fun getCountReviewedToday(startOfDayEpochMs: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: ReviewLog): Long

    @Query("DELETE FROM review_logs")
    suspend fun clearAll()
}
