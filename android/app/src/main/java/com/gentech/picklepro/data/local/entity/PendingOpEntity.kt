package com.gentech.picklepro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Generic offline sync queue (spec §5.10 `pending_ops`): every mutation
 * made while scoring is enqueued here and flushed to Supabase by
 * [com.gentech.picklepro.data.sync.SyncWorker] when back online, in
 * creation order. Conflict policy is organizer-device-wins: a flush is an
 * unconditional push of local state, never a merge.
 */
@Entity(tableName = "pending_ops")
data class PendingOpEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val opType: String,
    val payloadJson: String,
    val createdAt: Long,
    val attempts: Int = 0,
)
