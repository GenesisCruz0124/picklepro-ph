package com.gentech.picklepro.data.repository

import android.content.Context
import com.gentech.picklepro.core.rating.displayToElo
import com.gentech.picklepro.data.remote.SupabaseModule
import com.gentech.picklepro.data.remote.dto.ProfileDto
import com.gentech.picklepro.data.remote.dto.RatingInsertDto
import com.gentech.picklepro.data.remote.dto.ShellProfileInsertDto
import io.github.jan.supabase.postgrest.postgrest

/**
 * Manual add (spec §5.4): creates a shell player (claimable later via its
 * server-generated `claim_code`, spec §12) with a rating row for the
 * division's event type, seeded from the declared starting tier.
 */
class ShellPlayerRepository(context: Context) {

    private val client = SupabaseModule.get(context)

    suspend fun create(name: String, declaredRating: Double, eventType: String): ProfileDto {
        val profile = client.postgrest.from("profiles")
            .insert(ShellProfileInsertDto(name = name)) { select() }
            .decodeSingle<ProfileDto>()
        client.postgrest.from("ratings").insert(
            RatingInsertDto(playerId = profile.id, eventType = eventType, elo = displayToElo(declaredRating)),
        )
        return profile
    }
}
