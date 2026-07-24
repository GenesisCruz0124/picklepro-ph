package com.gentech.picklepro.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPrefsDataStore by preferencesDataStore(name = "picklepro_user_prefs")

/**
 * Local-only onboarding preferences (spec §4.1: "event-type preferences").
 * Not part of the Supabase schema — used to default tournament list filters
 * once that screen exists (M2 collects it, a later milestone consumes it).
 */
class UserPreferencesStore(private val context: Context) {

    private val eventTypesKey = stringSetPreferencesKey("preferred_event_types")

    val preferredEventTypes: Flow<Set<String>> =
        context.userPrefsDataStore.data.map { it[eventTypesKey] ?: emptySet() }

    suspend fun setPreferredEventTypes(eventTypes: Set<String>) {
        context.userPrefsDataStore.edit { it[eventTypesKey] = eventTypes }
    }
}
