package com.gentech.picklepro.data.repository

import android.content.Context
import com.gentech.picklepro.data.remote.SupabaseModule
import com.gentech.picklepro.data.remote.dto.DivisionDto
import com.gentech.picklepro.data.remote.dto.DivisionLockUpdateDto
import com.gentech.picklepro.data.remote.dto.DivisionPublishUpdateDto
import com.gentech.picklepro.data.remote.dto.DivisionUpsertDto
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns

private const val TABLE = "divisions"

/** Division CRUD (spec §5.3) — direct Postgrest; RLS scopes writes to the owning organizer. */
class DivisionRepository(context: Context) {

    private val client = SupabaseModule.get(context)

    suspend fun listForTournament(tournamentId: String): List<DivisionDto> =
        client.postgrest.from(TABLE)
            .select(Columns.ALL) { filter { eq("tournament_id", tournamentId) } }
            .decodeList()

    suspend fun get(divisionId: String): DivisionDto =
        client.postgrest.from(TABLE)
            .select(Columns.ALL) { filter { eq("id", divisionId) } }
            .decodeSingle()

    /** Locked on bracket generation, per spec §5.5. */
    suspend fun setLocked(divisionId: String, locked: Boolean) {
        client.postgrest.from(TABLE)
            .update(DivisionLockUpdateDto(locked)) { filter { eq("id", divisionId) } }
    }

    /** Results publish toggle — makes results visible to players (spec §5.8). */
    suspend fun setPublished(divisionId: String, published: Boolean) {
        client.postgrest.from(TABLE)
            .update(DivisionPublishUpdateDto(published)) { filter { eq("id", divisionId) } }
    }

    suspend fun create(division: DivisionUpsertDto): DivisionDto =
        client.postgrest.from(TABLE).insert(division) { select() }.decodeSingle()

    suspend fun update(divisionId: String, division: DivisionUpsertDto) {
        client.postgrest.from(TABLE).update(division) { filter { eq("id", divisionId) } }
    }

    suspend fun delete(divisionId: String) {
        client.postgrest.from(TABLE).delete { filter { eq("id", divisionId) } }
    }
}
