package com.gentech.picklepro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gentech.picklepro.data.local.entity.RatingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RatingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(ratings: List<RatingEntity>)

    @Query("SELECT * FROM rating_cache WHERE playerId = :playerId ORDER BY eventType")
    fun observeForPlayer(playerId: String): Flow<List<RatingEntity>>

    @Query("DELETE FROM rating_cache WHERE playerId = :playerId")
    suspend fun clearForPlayer(playerId: String)
}
