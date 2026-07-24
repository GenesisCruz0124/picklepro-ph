package com.gentech.picklepro.data.repository

import android.content.Context
import com.gentech.picklepro.data.local.PickleProDatabase
import com.gentech.picklepro.data.local.entity.ProfileEntity
import com.gentech.picklepro.data.remote.SupabaseModule
import com.gentech.picklepro.data.remote.dto.ProfileDto
import com.gentech.picklepro.data.remote.dto.ProfileUpdateDto
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow

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

    suspend fun updateProfile(playerId: String, name: String?, location: String?, photoUrl: String?) {
        client.postgrest.from(TABLE)
            .update(ProfileUpdateDto(name = name, location = location, photoUrl = photoUrl)) {
                filter { eq("id", playerId) }
            }
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
