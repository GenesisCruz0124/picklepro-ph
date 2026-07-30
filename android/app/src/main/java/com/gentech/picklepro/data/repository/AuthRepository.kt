package com.gentech.picklepro.data.repository

import android.content.Context
import com.gentech.picklepro.data.remote.SupabaseModule
import io.github.jan.supabase.auth.SessionStatus
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Domain-level session state, decoupled from the Supabase SDK's own type. */
sealed interface AuthState {
    data object Loading : AuthState
    data class SignedIn(val userId: String) : AuthState
    data object SignedOut : AuthState
}

/**
 * Wraps Supabase Auth for signup/login (spec §4.1). Signup passes the
 * self-declared starting tier and display name as user metadata, which the
 * `handle_new_user` Postgres trigger (M1) reads to seed the profile + the
 * three per-event-type rating rows server-side.
 */
class AuthRepository(context: Context) {

    private val client = SupabaseModule.get(context)

    val authState: Flow<AuthState> = client.auth.sessionStatus.map { status ->
        when (status) {
            is SessionStatus.Authenticated -> AuthState.SignedIn(status.session.user?.id.orEmpty())
            is SessionStatus.NotAuthenticated -> AuthState.SignedOut
            is SessionStatus.RefreshFailure -> AuthState.SignedOut
            else -> AuthState.Loading
        }
    }

    val currentUserId: String? get() = client.auth.currentUserOrNull()?.id

    suspend fun signUp(email: String, password: String, name: String, declaredRating: Double?) {
        client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            data = buildJsonObject {
                put("name", name)
                if (declaredRating != null) put("declared_rating", declaredRating)
            }
        }
    }

    suspend fun signIn(email: String, password: String) {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signOut() {
        client.auth.signOut()
    }
}
