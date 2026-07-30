package com.gentech.picklepro.core.qr

import android.net.Uri

private const val SCHEME = "picklepro"
private const val HOST = "player"

/**
 * Payload encoded in a player's personal QR (spec §4.3: player_id UUID +
 * short human code). This is the single source of truth for the format —
 * generation lives here (My QR, M2) and organizer scanning (registration,
 * M4) decodes with the same [decode].
 */
data class PlayerQrPayload(val playerId: String, val shortCode: String) {
    fun encode(): String = "$SCHEME://$HOST/$playerId/$shortCode"

    companion object {
        fun decode(raw: String): PlayerQrPayload? {
            val uri = runCatching { Uri.parse(raw) }.getOrNull() ?: return null
            if (uri.scheme != SCHEME || uri.host != HOST) return null
            val segments = uri.pathSegments
            if (segments.size < 2) return null
            return PlayerQrPayload(playerId = segments[0], shortCode = segments[1])
        }
    }
}
