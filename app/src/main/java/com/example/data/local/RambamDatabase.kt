package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ContentSectionEntity::class,
        ChapterEntity::class,
        HalachaEntity::class,
        ReadingPositionEntity::class,
        ChapterCompletionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class RambamDatabase : RoomDatabase() {
    abstract fun rambamDao(): RambamDao

    companion object {
        @Volatile
        private var INSTANCE: RambamDatabase? = null

        fun getInstance(context: Context): RambamDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RambamDatabase::class.java,
                    "rambam_study_v2.db"
                )
                .createFromAsset("database/rambam.db")
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
