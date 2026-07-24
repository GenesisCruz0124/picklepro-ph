package com.gentech.picklepro.data.local

import com.gentech.picklepro.core.scoring.ScoreEvent
import com.gentech.picklepro.core.scoring.Team
import com.gentech.picklepro.data.remote.dto.LoggedEvent
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Codec between the domain [ScoreEvent] model and [LoggedEvent] — the
 * jsonb-shaped DTO also used to sync `matches.point_log` (spec §5.6). Local
 * (Room) and remote storage share the exact same JSON shape, so encoding
 * here and decoding on the remote side (or vice versa) round-trips exactly.
 */
private val loggedEventJson = Json { ignoreUnknownKeys = true }

fun encodeEventLog(events: List<Pair<Int, ScoreEvent>>): String =
    loggedEventJson.encodeToString(
        events.map { (game, event) ->
            when (event) {
                is ScoreEvent.RallyWon -> LoggedEvent(game, "rally_won", event.team.name)
                is ScoreEvent.Timeout -> LoggedEvent(game, "timeout", event.team.name)
            }
        },
    )

fun decodeEventLog(raw: String): List<Pair<Int, ScoreEvent>> {
    if (raw.isBlank()) return emptyList()
    return loggedEventJson.decodeFromString<List<LoggedEvent>>(raw).map { logged ->
        val team = requireNotNull(logged.team?.let { Team.valueOf(it) }) { "logged event missing team" }
        val event: ScoreEvent = if (logged.type == "timeout") ScoreEvent.Timeout(team) else ScoreEvent.RallyWon(team)
        logged.game to event
    }
}
