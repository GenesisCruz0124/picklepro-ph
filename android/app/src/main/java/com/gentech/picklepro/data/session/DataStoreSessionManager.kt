package com.gentech.picklepro.data.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal val Context.sessionDataStore by preferencesDataStore(name = "picklepro_session")

/**
 * Persists the Supabase auth session in DataStore instead of the SDK's
 * built-in store, so session state lives alongside the rest of the app's
 * local preferences (spec §8: DataStore for local persistence).
 */
class DataStoreSessionManager(private val context: Context) : SessionManager {

    private val json = Json { ignoreUnknownKeys = true }
    private val sessionKey = stringPreferencesKey("user_session")

    override suspend fun saveSession(session: UserSession) {
        context.sessionDataStore.edit { prefs ->
            prefs[sessionKey] = json.encodeToString(session)
        }
    }

    override suspend fun loadSession(): UserSession? {
        val raw = context.sessionDataStore.data.first()[sessionKey] ?: return null
        return runCatching { json.decodeFromString(UserSession.serializer(), raw) }.getOrNull()
    }

    override suspend fun deleteSession() {
        context.sessionDataStore.edit { prefs -> prefs.remove(sessionKey) }
    }
}
