package com.gentech.picklepro.data.repository

import android.content.Context
import com.gentech.picklepro.data.remote.SupabaseModule
import com.gentech.picklepro.data.remote.dto.RegistrationCheckInUpdateDto
import com.gentech.picklepro.data.remote.dto.RegistrationDto
import com.gentech.picklepro.data.remote.dto.RegistrationInsertDto
import com.gentech.picklepro.data.remote.dto.RegistrationWithProfileDto
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.Serializable

enum class RejectReason { LEVEL_GATE, SLOTS_FULL }

sealed interface RegisterOutcome {
    data class Registered(val registration: RegistrationDto) : RegisterOutcome
    data class CheckedIn(val registration: RegistrationDto) : RegisterOutcome
    data class Rejected(val reason: RejectReason) : RegisterOutcome
}

@Serializable
private data class IdOnly(val id: String)

private const val TABLE = "registrations"

/** Registration + check-in (spec §5.4) — direct Postgrest; RLS scopes writes to the owning organizer. */
class RegistrationRepository(context: Context) {

    private val client = SupabaseModule.get(context)

    suspend fun listForDivision(divisionId: String): List<RegistrationWithProfileDto> =
        client.postgrest.from(TABLE)
            .select(
                Columns.raw(
                    "id, division_id, player_id, team_id, checked_in, effective_rating_at_reg, " +
                        "profiles(name, short_code, is_shell)",
                ),
            ) { filter { eq("division_id", divisionId) } }
            .decodeList()

    suspend fun findExisting(divisionId: String, playerId: String): RegistrationDto? =
        client.postgrest.from(TABLE)
            .select(Columns.ALL) {
                filter {
                    eq("division_id", divisionId)
                    eq("player_id", playerId)
                }
            }
            .decodeList<RegistrationDto>()
            .firstOrNull()

    /**
     * First scan/add of a player into a division registers them (after
     * gate + slot checks); scanning an already-registered player checks
     * them in — spec §5.4 "second scan on event day = check-in".
     */
    suspend fun registerOrCheckIn(
        divisionId: String,
        playerId: String,
        maxRating: Double?,
        maxSlots: Int?,
        effectiveRating: Double,
    ): RegisterOutcome {
        findExisting(divisionId, playerId)?.let { existing ->
            val updated = client.postgrest.from(TABLE)
                .update(RegistrationCheckInUpdateDto(checkedIn = true)) {
                    filter { eq("id", existing.id) }
                    select()
                }
                .decodeSingle<RegistrationDto>()
            return RegisterOutcome.CheckedIn(updated)
        }

        // Level gate (spec §3.4): effective_rating <= max_rating + 0.25; null max_rating = Open, no gate.
        if (maxRating != null && effectiveRating > maxRating + 0.25) {
            return RegisterOutcome.Rejected(RejectReason.LEVEL_GATE)
        }
        if (maxSlots != null) {
            val currentCount = client.postgrest.from(TABLE)
                .select(Columns.list("id")) { filter { eq("division_id", divisionId) } }
                .decodeList<IdOnly>()
                .size
            if (currentCount >= maxSlots) {
                return RegisterOutcome.Rejected(RejectReason.SLOTS_FULL)
            }
        }

        val created = client.postgrest.from(TABLE)
            .insert(
                RegistrationInsertDto(
                    divisionId = divisionId,
                    playerId = playerId,
                    effectiveRatingAtReg = effectiveRating,
                ),
            ) { select() }
            .decodeSingle<RegistrationDto>()
        return RegisterOutcome.Registered(created)
    }

    suspend fun setCheckedIn(registrationId: String, checkedIn: Boolean) {
        client.postgrest.from(TABLE)
            .update(RegistrationCheckInUpdateDto(checkedIn)) { filter { eq("id", registrationId) } }
    }

    suspend fun unregister(registrationId: String) {
        client.postgrest.from(TABLE).delete { filter { eq("id", registrationId) } }
    }
}
