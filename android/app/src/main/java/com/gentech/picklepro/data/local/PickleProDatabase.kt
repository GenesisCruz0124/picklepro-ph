package com.gentech.picklepro.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.gentech.picklepro.data.local.dao.ProfileDao
import com.gentech.picklepro.data.local.dao.RatingDao
import com.gentech.picklepro.data.local.entity.ProfileEntity
import com.gentech.picklepro.data.local.entity.RatingEntity

/**
 * Offline cache for M2 (profile + ratings, read-through so the Profile and
 * My QR screens work with zero connectivity). Heavier offline-first tables
 * (registrations, matches, pending_ops sync queue) join this database from
 * M4/M5 onward per spec §5.10.
 */
@Database(
    entities = [ProfileEntity::class, RatingEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class PickleProDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun ratingDao(): RatingDao

    companion object {
        @Volatile
        private var instance: PickleProDatabase? = null

        fun get(context: Context): PickleProDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PickleProDatabase::class.java,
                    "picklepro.db",
                ).build().also { instance = it }
            }
    }
}
