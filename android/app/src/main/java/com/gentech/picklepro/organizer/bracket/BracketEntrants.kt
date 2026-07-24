package com.gentech.picklepro.organizer.bracket

import com.gentech.picklepro.data.remote.dto.DivisionDto
import com.gentech.picklepro.data.remote.dto.RegistrationWithProfileDto
import com.gentech.picklepro.data.remote.dto.TeamDto
import com.gentech.picklepro.data.repository.Entrant

/**
 * Resolves a division's bracket entrants: registrations for singles, paired
 * teams for doubles/mixed (unpaired registrations are excluded — spec §5.4
 * pairing must happen before bracket generation).
 */
fun resolveEntrants(
    division: DivisionDto,
    registrations: List<RegistrationWithProfileDto>,
    teams: List<TeamDto>,
): List<Entrant> = if (division.eventType == "singles") {
    registrations.map { r -> Entrant(ref = r.id, name = r.profiles.name, seedRating = r.effectiveRatingAtReg ?: 2.5) }
} else {
    val ratingByPlayer = registrations.associate { it.playerId to (it.effectiveRatingAtReg ?: 2.5) }
    val nameByPlayer = registrations.associate { it.playerId to it.profiles.name }
    teams.map { t ->
        val r1 = ratingByPlayer[t.player1Id] ?: 2.5
        val r2 = t.player2Id?.let { ratingByPlayer[it] } ?: r1
        val n1 = nameByPlayer[t.player1Id] ?: "?"
        val n2 = t.player2Id?.let { nameByPlayer[it] } ?: "?"
        Entrant(ref = t.id, name = "$n1 / $n2", seedRating = (r1 + r2) / 2.0)
    }
}
