package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 5,
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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminder_items ADD COLUMN advanceMinutes INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminder_items ADD COLUMN alarmEnabled INTEGER NOT NULL DEFAULT 1")
            }
        }

        fun getDatabase(context: Context): GemmyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GemmyDatabase::class.java,
                    "gemmy_memory_database"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
