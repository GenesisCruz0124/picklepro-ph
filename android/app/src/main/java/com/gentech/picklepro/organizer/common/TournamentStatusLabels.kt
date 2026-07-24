package com.gentech.picklepro.organizer.common

import androidx.annotation.StringRes
import com.gentech.picklepro.R

/** Draft → Registration → Ongoing → Finished (spec §5.2 status chips). */
val TOURNAMENT_STATUS_ORDER = listOf("draft", "registration", "ongoing", "finished")

@StringRes
fun tournamentStatusLabelRes(status: String): Int = when (status) {
    "registration" -> R.string.tournament_status_registration
    "ongoing" -> R.string.tournament_status_ongoing
    "finished" -> R.string.tournament_status_finished
    else -> R.string.tournament_status_draft
}

/** Next status in the forward-only progression, or null if already finished. */
fun nextTournamentStatus(status: String): String? {
    val index = TOURNAMENT_STATUS_ORDER.indexOf(status)
    if (index < 0 || index == TOURNAMENT_STATUS_ORDER.lastIndex) return null
    return TOURNAMENT_STATUS_ORDER[index + 1]
}
