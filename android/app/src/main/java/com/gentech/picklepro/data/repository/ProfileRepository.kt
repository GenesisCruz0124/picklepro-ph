package com.gentech.picklepro.data.repository

import android.content.Context
import com.gentech.picklepro.data.local.PickleProDatabase
import com.gentech.picklepro.data.local.entity.ProfileEntity
import com.gentech.picklepro.data.remote.SupabaseModule
import com.gentech.picklepro.data.remote.dto.ProfileDto
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val TABLE = "profiles"

/**
 * Profile read-through cache (spec §5.10 pattern, applied here to the
 * player's own profile so Profile/My QR render instantly and work
 * offline). [observeCached] is the source of truth for the UI; [refresh]
 * pulls the latest row from Supabase and updates the cache.
 */
class ProfileRepository(context: Context) {

    private val client = SupabaseModule.get(context)
    private val dao = PickleProDatabase.get(context).profileDao()

    fun observeCached(playerId: String): Flow<ProfileEntity?> = dao.observe(playerId)

    suspend fun refresh(playerId: String): ProfileDto {
        val dto = client.postgrest.from(TABLE)
            .select(Columns.ALL) { filter { eq("id", playerId) } }
            .decodeSingle<ProfileDto>()
        dao.upsert(dto.toEntity())
        return dto
    }

    /** Looks up any player by id without touching the "self" Room cache — used for QR-scan registration (spec §5.4). */
    suspend fun findById(playerId: String): ProfileDto? =
        client.postgrest.from(TABLE)
            .select(Columns.ALL) { filter { eq("id", playerId) } }
            .decodeList<ProfileDto>()
            .firstOrNull()

    /**
     * Only the fields the caller passes non-null are sent — a JsonObject
     * patch, not a DTO with the rest defaulting to null, since a
     * serialized null would PATCH those columns to null too.
     */
    suspend fun updateProfile(playerId: String, name: String?, location: String?, photoUrl: String?) {
        val patch = buildJsonObject {
            name?.let { put("name", it) }
            location?.let { put("location", it) }
            photoUrl?.let { put("photo_url", it) }
        }
        if (patch.isEmpty()) return
        client.postgrest.from(TABLE).update(patch) { filter { eq("id", playerId) } }
        refresh(playerId)
    }
}

private fun ProfileDto.toEntity() = ProfileEntity(
    id = id,
    name = name,
    photoUrl = photoUrl,
    location = location,
    role = role,
    shortCode = shortCode,
    duprId = duprId,
    duprRating = duprRating,
    duprVerified = duprVerified,
    duprProofUrl = duprProofUrl,
)
