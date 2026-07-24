package com.gentech.picklepro.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.gentech.picklepro.data.local.dao.MatchDao
import com.gentech.picklepro.data.local.dao.PendingOpDao
import com.gentech.picklepro.data.local.dao.ProfileDao
import com.gentech.picklepro.data.local.dao.RatingDao
import com.gentech.picklepro.data.local.entity.MatchEntity
import com.gentech.picklepro.data.local.entity.PendingOpEntity
import com.gentech.picklepro.data.local.entity.ProfileEntity
import com.gentech.picklepro.data.local.entity.RatingEntity

/**
 * Offline cache. M2 added profile + ratings (read-through, for Profile/My
 * QR). M5 adds the real offline-first tables per spec §5.10: `match_cache`
 * is the source of truth during live scoring, and `pending_ops` is the
 * sync queue a WorkManager worker flushes to Supabase when online.
 *
 * Version 1 -> 2 uses [fallbackToDestructiveMigration] rather than a
 * hand-written Migration: no version of this app has shipped externally
 * yet (all builds so far are internal 0.x), so there's no real user data
 * at risk. A real migration path is needed before v1.0.0.
 */
@Database(
    entities = [ProfileEntity::class, RatingEntity::class, MatchEntity::class, PendingOpEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class PickleProDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun ratingDao(): RatingDao
    abstract fun matchDao(): MatchDao
    abstract fun pendingOpDao(): PendingOpDao

    companion object {
        @Volatile
        private var instance: PickleProDatabase? = null

        fun get(context: Context): PickleProDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PickleProDatabase::class.java,
                    "picklepro.db",
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
