package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AlarmItem
import com.example.data.model.LearningItem
import com.example.data.model.ReminderItem
import com.example.data.model.ReviewLog

@Database(
    entities = [
        LearningItem::class,
        ReminderItem::class,
        AlarmItem::class,
        ReviewLog::class
    ],
    version = 1,
    exportSchema = false
)
abstract class GemmyDatabase : RoomDatabase() {

    abstract fun learningItemDao(): LearningItemDao
    abstract fun reminderItemDao(): ReminderItemDao
    abstract fun alarmItemDao(): AlarmItemDao
    abstract fun reviewLogDao(): ReviewLogDao

    companion object {
        @Volatile
        private var INSTANCE: GemmyDatabase? = null

        fun getDatabase(context: Context): GemmyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GemmyDatabase::class.java,
                    "gemmy_memory_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
