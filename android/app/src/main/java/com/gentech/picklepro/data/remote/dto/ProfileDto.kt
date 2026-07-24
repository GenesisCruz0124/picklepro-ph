package com.gentech.picklepro.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Maps to public.profiles (spec §7). */
@Serializable
data class ProfileDto(
    val id: String,
    val name: String = "",
    @SerialName("photo_url") val photoUrl: String? = null,
    val location: String? = null,
    val role: String = "player",
    @SerialName("short_code") val shortCode: String? = null,
    @SerialName("dupr_id") val duprId: String? = null,
    @SerialName("dupr_rating") val duprRating: Double? = null,
    @SerialName("dupr_verified") val duprVerified: Boolean = false,
    @SerialName("dupr_proof_url") val duprProofUrl: String? = null,
    @SerialName("is_shell") val isShell: Boolean = false,
)

/** Partial update payload — only fields the player can self-edit. */
@Serializable
data class ProfileUpdateDto(
    val name: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    val location: String? = null,
)

/** Manual-add shell player insert (spec §5.4) — RLS requires is_shell = true from an organizer. */
@Serializable
data class ShellProfileInsertDto(
    val name: String,
    @SerialName("is_shell") val isShell: Boolean = true,
)
