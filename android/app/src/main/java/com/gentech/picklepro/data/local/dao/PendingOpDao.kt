package com.gentech.picklepro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gentech.picklepro.data.local.entity.PendingOpEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingOpDao {
    @Insert
    suspend fun enqueue(op: PendingOpEntity): Long

    @Query("SELECT * FROM pending_ops ORDER BY createdAt ASC")
    suspend fun listAll(): List<PendingOpEntity>

    @Query("DELETE FROM pending_ops WHERE id = :id")
    suspend fun remove(id: Long)

    @Query("UPDATE pending_ops SET attempts = attempts + 1 WHERE id = :id")
    suspend fun incrementAttempts(id: Long)

    @Query("SELECT COUNT(*) FROM pending_ops")
    fun observeCount(): Flow<Int>
}
