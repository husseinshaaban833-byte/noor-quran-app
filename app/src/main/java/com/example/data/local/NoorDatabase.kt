package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        BookmarkEntity::class,
        MemorizationPlanEntity::class,
        RecitationAttemptEntity::class,
        KhatmahPlanEntity::class,
        DownloadedItemEntity::class,
        TasbeehLogEntity::class,
        AdhkarProgressEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NoorDatabase : RoomDatabase() {
    abstract fun noorDao(): NoorDao

    companion object {
        @Volatile
        private var INSTANCE: NoorDatabase? = null

        fun getInstance(context: Context): NoorDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NoorDatabase::class.java,
                    "noor_al_quran.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
