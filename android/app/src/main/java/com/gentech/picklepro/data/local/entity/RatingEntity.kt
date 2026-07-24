package com.gentech.picklepro.data.local.entity

import androidx.room.Entity

/** Offline cache of the signed-in player's per-event-type rating (spec §3.2). */
@Entity(tableName = "rating_cache", primaryKeys = ["playerId", "eventType"])
data class RatingEntity(
    val playerId: String,
    val eventType: String,
    val elo: Int,
    val display: Double,
    val matchesPlayed: Int,
    val provisional: Boolean,
    val override: Double?,
)
