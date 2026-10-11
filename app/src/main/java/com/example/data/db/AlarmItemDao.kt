package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AlarmItem
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmItemDao {

    @Query("SELECT * FROM alarm_items ORDER BY hour ASC, minute ASC")
    fun getAllAlarms(): Flow<List<AlarmItem>>

    @Query("SELECT * FROM alarm_items WHERE id = :id")
    suspend fun getAlarmById(id: Long): AlarmItem?

    @Query("SELECT * FROM alarm_items WHERE isEnabled = 1")
    suspend fun getActiveAlarmsOnce(): List<AlarmItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alarm: AlarmItem): Long

    @Update
    suspend fun update(alarm: AlarmItem)

    @Delete
    suspend fun delete(alarm: AlarmItem)

    @Query("DELETE FROM alarm_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE alarm_items SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun setAlarmEnabled(id: Long, isEnabled: Boolean)

    @Query("DELETE FROM alarm_items")
    suspend fun clearAll()
}
