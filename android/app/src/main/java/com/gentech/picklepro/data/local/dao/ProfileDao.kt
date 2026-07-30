package com.gentech.picklepro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gentech.picklepro.data.local.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: ProfileEntity)

    @Query("SELECT * FROM profile_cache WHERE id = :playerId LIMIT 1")
    fun observe(playerId: String): Flow<ProfileEntity?>

    @Query("DELETE FROM profile_cache")
    suspend fun clear()
}
