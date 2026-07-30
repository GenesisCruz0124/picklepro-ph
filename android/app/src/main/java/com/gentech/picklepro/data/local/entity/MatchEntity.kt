package com.gentech.picklepro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local mirror of `matches` — the source of truth during live scoring
 * (spec §5.10). [pointLogJson] holds the full point-by-point event log
 * across the whole match (each entry tagged with its game number); current
 * on-court state is always *derived* from it via `ScoreEngine.replay`,
 * never stored separately, so UNDO can never drift from the log.
 */
@Entity(tableName = "match_cache")
data class MatchEntity(
    @PrimaryKey val id: String,
    val divisionId: String,
    val round: Int,
    val position: Int,
    val court: Int?,
    val sideARef: String?,
    val sideBRef: String?,
    val status: String,
    val winnerRef: String?,
    /** JSON array of finished games' final scores, e.g. [{"a":11,"b":7}]. */
    val gamesJson: String,
    /** JSON array of LoggedEvent — the full point-by-point log (spec §5.6). */
    val pointLogJson: String,
    val currentGameNumber: Int,
    val startedAt: String?,
    val finishedAt: String?,
    val synced: Boolean,
)
