package com.gentech.picklepro.data.repository

import android.content.Context
import com.gentech.picklepro.data.remote.SupabaseModule
import com.gentech.picklepro.data.remote.dto.CertificateInsertDto
import io.github.jan.supabase.postgrest.postgrest

/** Records generated certificates (spec §5.9); best-effort — generation itself never depends on it. */
class CertificateRepository(context: Context) {

    private val client = SupabaseModule.get(context)

    suspend fun record(tournamentId: String, divisionId: String, recipientRef: String, kind: String) {
        client.postgrest.from("certificates").insert(
            CertificateInsertDto(
                tournamentId = tournamentId,
                divisionId = divisionId,
                recipientRef = recipientRef,
                kind = kind,
            ),
        )
    }
}
