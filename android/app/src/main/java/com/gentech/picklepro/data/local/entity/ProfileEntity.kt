package com.gentech.picklepro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Offline cache of the signed-in player's own profile, so Profile/My QR
 * render instantly and work fully offline (spec §4.3 "works offline").
 * Only the current user's row is ever stored here in M2.
 */
@Entity(tableName = "profile_cache")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val photoUrl: String?,
    val location: String?,
    val role: String,
    val shortCode: String?,
    val duprId: String?,
    val duprRating: Double?,
    val duprVerified: Boolean,
    val duprProofUrl: String?,
)
