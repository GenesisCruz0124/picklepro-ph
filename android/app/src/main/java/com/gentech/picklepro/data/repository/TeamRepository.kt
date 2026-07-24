package com.gentech.picklepro.data.repository

import android.content.Context
import com.gentech.picklepro.data.remote.SupabaseModule
import com.gentech.picklepro.data.remote.dto.TeamDto
import com.gentech.picklepro.data.remote.dto.TeamInsertDto
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class RegistrationTeamLinkDto(@SerialName("team_id") val teamId: String?)

/** Doubles/mixed pairing (spec §5.4): pairs two unpaired registrations into a team. */
class TeamRepository(context: Context) {

    private val client = SupabaseModule.get(context)

    suspend fun listForDivision(divisionId: String): List<TeamDto> =
        client.postgrest.from("teams")
            .select(Columns.ALL) { filter { eq("division_id", divisionId) } }
            .decodeList()

    suspend fun pair(
        divisionId: String,
        registrationId1: String,
        playerId1: String,
        registrationId2: String,
        playerId2: String,
    ): TeamDto {
        val team = client.postgrest.from("teams")
            .insert(TeamInsertDto(divisionId = divisionId, player1Id = playerId1, player2Id = playerId2)) { select() }
            .decodeSingle<TeamDto>()
        client.postgrest.from("registrations")
            .update(RegistrationTeamLinkDto(teamId = team.id)) {
                filter { isIn("id", listOf(registrationId1, registrationId2)) }
            }
        return team
    }

    /** Deleting the team nulls both registrations' team_id via the FK's ON DELETE SET NULL. */
    suspend fun unpair(teamId: String) {
        client.postgrest.from("teams").delete { filter { eq("id", teamId) } }
    }
}
