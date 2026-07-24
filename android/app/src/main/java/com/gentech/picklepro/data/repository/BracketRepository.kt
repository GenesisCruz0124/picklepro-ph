package com.gentech.picklepro.data.repository

import android.content.Context
import com.gentech.picklepro.data.remote.SupabaseModule
import com.gentech.picklepro.data.remote.dto.BracketMatchDto
import com.gentech.picklepro.data.remote.dto.DivisionLockUpdateDto
import com.gentech.picklepro.data.remote.dto.MatchInsertDto
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order

/** One bracket/round-robin entrant: a registration (singles) or team (doubles/mixed). */
data class Entrant(val ref: String, val name: String, val seedRating: Double)

sealed interface BracketGenerationResult {
    data object Success : BracketGenerationResult
    data object NotEnoughEntrants : BracketGenerationResult
    data object AlreadyHasResults : BracketGenerationResult
}

data class StandingsRow(
    val entrantRef: String,
    val name: String,
    val wins: Int,
    val losses: Int,
    val pointDiff: Int,
    val pointsAgainst: Int,
)

/**
 * Bracket generation (spec §5.5). Scope note: generation only places
 * entrants and resolves byes that are known immediately (a bye's opponent
 * advances at generation time); auto-advancing a winner into the next
 * round when a *match* completes is spec'd under the live scorer (§5.6)
 * and is M5's responsibility, not M4's — so round 2+ matches whose feeders
 * aren't byes are created with null sides, to be filled in once that
 * logic exists.
 */
class BracketRepository(context: Context) {

    private val client = SupabaseModule.get(context)

    suspend fun listMatches(divisionId: String): List<BracketMatchDto> =
        client.postgrest.from("matches")
            .select(Columns.ALL) {
                filter { eq("division_id", divisionId) }
                order("round", Order.ASCENDING)
                order("position", Order.ASCENDING)
            }
            .decodeList()

    /** Regenerate is blocked once any match has moved past pending (spec §5.5). */
    suspend fun canRegenerate(divisionId: String): Boolean =
        listMatches(divisionId).all { it.status == "pending" }

    suspend fun generateSingleElimination(
        divisionId: String,
        entrants: List<Entrant>,
        bronzeMatch: Boolean,
    ): BracketGenerationResult {
        if (entrants.size < 2) return BracketGenerationResult.NotEnoughEntrants
        if (!canRegenerate(divisionId)) return BracketGenerationResult.AlreadyHasResults

        clearMatches(divisionId)

        val ordered = entrants.sortedByDescending { it.seedRating }
        val bracketSize = nextPowerOfTwo(ordered.size)
        val seedOrder = standardSeedOrder(bracketSize)
        val round1Slots: List<String?> = seedOrder.map { seedNumber -> ordered.getOrNull(seedNumber - 1)?.ref }

        val inserts = mutableListOf<MatchInsertDto>()
        var feed = mutableListOf<String?>()
        var pos = 0
        var i = 0
        while (i < round1Slots.size) {
            val a = round1Slots[i]
            val b = round1Slots.getOrNull(i + 1)
            if (a != null && b != null) {
                inserts += MatchInsertDto(divisionId, round = 1, position = pos, sideARef = a, sideBRef = b)
                feed.add(null)
            } else {
                feed.add(a ?: b) // bye: the real entrant advances immediately, no round-1 match
            }
            // pos advances for every pairing, byes included, so it always equals the pairing
            // index — the same index `feed` uses. That's what lets bracket advancement (spec
            // §5.6, wired up when a match completes) derive "round R position P feeds round
            // R+1 position P/2, side A if P even else B" with pure arithmetic. Round 2+ never
            // skip a position (every pairing there is a real match), so this only matters here.
            pos++
            i += 2
        }

        var round = 2
        while (feed.size > 1) {
            val nextFeed = mutableListOf<String?>()
            var roundPos = 0
            var j = 0
            while (j < feed.size) {
                val a = feed[j]
                val b = feed.getOrNull(j + 1)
                inserts += MatchInsertDto(divisionId, round = round, position = roundPos, sideARef = a, sideBRef = b)
                // Unlike round 1, a round-2+ pairing is always a real match that must be played —
                // even a bye-advanced entrant (a known, b null) has to win this match to progress,
                // so what feeds the *next* round is always unresolved here.
                nextFeed.add(null)
                roundPos++
                j += 2
            }
            feed = nextFeed
            round++
        }

        if (bronzeMatch && bracketSize >= 4) {
            inserts += MatchInsertDto(divisionId, round = round - 1, position = 1)
        }

        client.postgrest.from("matches").insert(inserts)
        lockDivision(divisionId)
        return BracketGenerationResult.Success
    }

    /** Standard circle method; odd entrant counts get one bye per round (spec §5.5). */
    suspend fun generateRoundRobin(divisionId: String, entrants: List<Entrant>): BracketGenerationResult {
        if (entrants.size < 2) return BracketGenerationResult.NotEnoughEntrants
        if (!canRegenerate(divisionId)) return BracketGenerationResult.AlreadyHasResults

        clearMatches(divisionId)

        val refs: MutableList<String?> = entrants.map { it.ref }.toMutableList()
        if (refs.size % 2 != 0) refs.add(null)
        val n = refs.size
        var rotating = refs.toMutableList()
        val inserts = mutableListOf<MatchInsertDto>()

        for (round in 1 until n) {
            var pos = 0
            for (i in 0 until n / 2) {
                val a = rotating[i]
                val b = rotating[n - 1 - i]
                if (a != null && b != null) {
                    inserts += MatchInsertDto(divisionId, round = round, position = pos, sideARef = a, sideBRef = b)
                    pos++
                }
            }
            val fixed = rotating[0]
            val rest = rotating.subList(1, n).toMutableList()
            val last = rest.removeAt(rest.size - 1)
            rest.add(0, last)
            rotating = (listOf(fixed) + rest).toMutableList()
        }

        client.postgrest.from("matches").insert(inserts)
        lockDivision(divisionId)
        return BracketGenerationResult.Success
    }

    /**
     * Standings with the spec §5.5 tiebreaker order: (1) head-to-head, (2)
     * point differential, (3) points against. Head-to-head only reliably
     * resolves a pure 2-way tie — anything still tied falls through to
     * point diff / points against, leaving a genuine remaining tie for the
     * organizer's manual coin flip (spec's own step 4) rather than an
     * arbitrary automatic order.
     */
    suspend fun computeStandings(divisionId: String, entrants: List<Entrant>): List<StandingsRow> {
        val matches = listMatches(divisionId).filter { it.status == "done" || it.status == "walkover" }
        val wins = mutableMapOf<String, Int>()
        val losses = mutableMapOf<String, Int>()
        val pointDiff = mutableMapOf<String, Int>()
        val pointsAgainst = mutableMapOf<String, Int>()
        val headToHeadWinner = mutableMapOf<Pair<String, String>, String>()

        for (m in matches) {
            val a = m.sideARef ?: continue
            val b = m.sideBRef ?: continue
            val winner = m.winnerRef ?: continue
            if (winner == a) {
                wins[a] = (wins[a] ?: 0) + 1
                losses[b] = (losses[b] ?: 0) + 1
            } else if (winner == b) {
                wins[b] = (wins[b] ?: 0) + 1
                losses[a] = (losses[a] ?: 0) + 1
            }
            for (g in m.games) {
                pointDiff[a] = (pointDiff[a] ?: 0) + (g.a - g.b)
                pointDiff[b] = (pointDiff[b] ?: 0) + (g.b - g.a)
                pointsAgainst[a] = (pointsAgainst[a] ?: 0) + g.b
                pointsAgainst[b] = (pointsAgainst[b] ?: 0) + g.a
            }
            val key = if (a < b) a to b else b to a
            headToHeadWinner[key] = winner
        }

        val rows = entrants.map { e ->
            StandingsRow(
                entrantRef = e.ref,
                name = e.name,
                wins = wins[e.ref] ?: 0,
                losses = losses[e.ref] ?: 0,
                pointDiff = pointDiff[e.ref] ?: 0,
                pointsAgainst = pointsAgainst[e.ref] ?: 0,
            )
        }

        return rows.sortedWith(
            compareByDescending<StandingsRow> { it.wins }
                .thenComparator { r1, r2 ->
                    val key = if (r1.entrantRef < r2.entrantRef) {
                        r1.entrantRef to r2.entrantRef
                    } else {
                        r2.entrantRef to r1.entrantRef
                    }
                    when (headToHeadWinner[key]) {
                        r1.entrantRef -> -1
                        r2.entrantRef -> 1
                        else -> 0
                    }
                }
                .thenByDescending { it.pointDiff }
                .thenBy { it.pointsAgainst },
        )
    }

    private suspend fun clearMatches(divisionId: String) {
        client.postgrest.from("matches").delete { filter { eq("division_id", divisionId) } }
    }

    private suspend fun lockDivision(divisionId: String) {
        client.postgrest.from("divisions")
            .update(DivisionLockUpdateDto(true)) { filter { eq("id", divisionId) } }
    }
}

private fun nextPowerOfTwo(n: Int): Int {
    var size = 2
    while (size < n) size *= 2
    return size
}

/** Standard bracket seeding order so seed 1 and seed 2 can only meet in the final. */
private fun standardSeedOrder(size: Int): List<Int> {
    var seeds = listOf(1)
    while (seeds.size < size) {
        val n = seeds.size * 2
        val next = mutableListOf<Int>()
        for (s in seeds) {
            next.add(s)
            next.add(n + 1 - s)
        }
        seeds = next
    }
    return seeds
}
