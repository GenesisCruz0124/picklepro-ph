package com.gentech.picklepro.core.rating

import androidx.annotation.StringRes
import com.gentech.picklepro.R
import kotlin.math.roundToInt

/** Skill tier bands from spec §3.1, keyed off the 2.0–8.0 display rating. */
@StringRes
fun tierLabelRes(display: Double): Int = when {
    display < 2.5 -> R.string.tier_2_0
    display < 3.0 -> R.string.tier_2_5
    display < 3.5 -> R.string.tier_3_0
    display < 4.0 -> R.string.tier_3_5
    display < 4.5 -> R.string.tier_4_0
    else -> R.string.tier_4_5
}

/**
 * Inverse of the DB's generated `ratings.display` column (spec §3.2:
 * `display = 2.0 + (elo - 800) / 400`). Needed client-side only for shell
 * players (spec §5.4 manual add) — real signups get their starting elo from
 * the `handle_new_user` trigger, which applies this same formula
 * server-side from the declared_rating signup metadata.
 */
fun displayToElo(display: Double): Int =
    (800 + (display.coerceIn(2.0, 8.0) - 2.0) * 400).roundToInt()
