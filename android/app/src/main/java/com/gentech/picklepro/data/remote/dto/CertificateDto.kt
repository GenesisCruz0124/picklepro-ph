package com.gentech.picklepro.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Maps to public.certificates (spec §7). pdf_url stays null in Phase 1 — the PDF lives on-device. */
@Serializable
data class CertificateInsertDto(
    @SerialName("tournament_id") val tournamentId: String,
    @SerialName("division_id") val divisionId: String,
    @SerialName("recipient_ref") val recipientRef: String,
    val kind: String,
)
