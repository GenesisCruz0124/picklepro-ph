package com.gentech.picklepro.core.rating

import androidx.annotation.StringRes
import com.gentech.picklepro.R

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
