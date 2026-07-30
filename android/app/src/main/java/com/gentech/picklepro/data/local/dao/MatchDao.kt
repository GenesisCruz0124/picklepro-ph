package com.gentech.picklepro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gentech.picklepro.data.local.entity.MatchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(match: MatchEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(matches: List<MatchEntity>)

    @Query("SELECT * FROM match_cache WHERE id = :matchId")
    fun observe(matchId: String): Flow<MatchEntity?>

    @Query("SELECT * FROM match_cache WHERE id = :matchId")
    suspend fun get(matchId: String): MatchEntity?

    @Query("SELECT * FROM match_cache WHERE divisionId = :divisionId ORDER BY round, position")
    suspend fun listForDivision(divisionId: String): List<MatchEntity>

    @Query("SELECT * FROM match_cache WHERE synced = 0")
    suspend fun listUnsynced(): List<MatchEntity>
}
