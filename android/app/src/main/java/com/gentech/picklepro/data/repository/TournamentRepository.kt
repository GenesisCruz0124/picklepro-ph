package com.gentech.picklepro.data.repository

import android.content.Context
import com.gentech.picklepro.data.remote.SupabaseModule
import com.gentech.picklepro.data.remote.dto.CreateTournamentRequest
import com.gentech.picklepro.data.remote.dto.CreateTournamentResponse
import com.gentech.picklepro.data.remote.dto.TournamentDto
import com.gentech.picklepro.data.remote.dto.TournamentLogoUpdateDto
import com.gentech.picklepro.data.remote.dto.TournamentStatusUpdateDto
import com.gentech.picklepro.data.remote.dto.TournamentUpdateDto
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import io.ktor.client.call.body

private const val TABLE = "tournaments"
private const val LOGO_BUCKET = "tournament-logos"

/**
 * Tournament creation (via the credit-consuming Edge Function) plus direct
 * Postgrest reads/updates — the M1 RLS policy already scopes update/select
 * to the owning organizer, so there's no privileged logic to centralize
 * server-side for anything but creation (spec §5.3).
 */
class TournamentRepository(context: Context) {

    private val client = SupabaseModule.get(context)

    suspend fun create(request: CreateTournamentRequest): CreateTournamentResponse =
        client.functions.invoke("consume-code-on-create", body = request).body()

    suspend fun listMine(organizerId: String): List<TournamentDto> =
        client.postgrest.from(TABLE)
            .select(Columns.ALL) {
                filter { eq("organizer_id", organizerId) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList()

    suspend fun get(tournamentId: String): TournamentDto =
        client.postgrest.from(TABLE)
            .select(Columns.ALL) { filter { eq("id", tournamentId) } }
            .decodeSingle()

    suspend fun update(tournamentId: String, patch: TournamentUpdateDto) {
        client.postgrest.from(TABLE).update(patch) { filter { eq("id", tournamentId) } }
    }

    /** Draft → Registration → Ongoing → Finished, forward-only (spec §5.2 status chips). */
    suspend fun advanceStatus(tournamentId: String, newStatus: String) {
        client.postgrest.from(TABLE)
            .update(TournamentStatusUpdateDto(newStatus)) { filter { eq("id", tournamentId) } }
    }

    /** Uploads the logo, persists its public URL, and returns it. */
    suspend fun uploadLogo(tournamentId: String, bytes: ByteArray, fileExtension: String): String {
        val path = "$tournamentId/logo.$fileExtension"
        client.storage.from(LOGO_BUCKET).upload(path, bytes) { upsert = true }
        val url = client.storage.from(LOGO_BUCKET).publicUrl(path)
        client.postgrest.from(TABLE)
            .update(TournamentLogoUpdateDto(url)) { filter { eq("id", tournamentId) } }
        return url
    }
}
