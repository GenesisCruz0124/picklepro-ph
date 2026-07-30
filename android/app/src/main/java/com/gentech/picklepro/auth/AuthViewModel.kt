package com.gentech.picklepro.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gentech.picklepro.data.preferences.UserPreferencesStore
import com.gentech.picklepro.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthMode { LOGIN, SIGNUP }

data class AuthUiState(
    val mode: AuthMode = AuthMode.LOGIN,
    val name: String = "",
    val email: String = "",
    val password: String = "",
    /** Self-declared starting tier, 2.0–8.0 (spec §3.2). Null = use the default. */
    val declaredRating: Float = 2.5f,
    val selectedEventTypes: Set<String> = setOf("singles", "doubles", "mixed"),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    /** True once signUp() returns without a session — e.g. email confirmation is required. */
    val awaitingEmailConfirmation: Boolean = false,
)

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val userPreferencesStore: UserPreferencesStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    fun onToggleMode() {
        _uiState.update {
            it.copy(
                mode = if (it.mode == AuthMode.LOGIN) AuthMode.SIGNUP else AuthMode.LOGIN,
                errorMessage = null,
            )
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, errorMessage = null) }
    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, errorMessage = null) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, errorMessage = null) }
    fun onDeclaredRatingChange(value: Float) = _uiState.update { it.copy(declaredRating = value) }

    fun onToggleEventType(eventType: String) = _uiState.update { state ->
        val updated = if (eventType in state.selectedEventTypes) {
            state.selectedEventTypes - eventType
        } else {
            state.selectedEventTypes + eventType
        }
        state.copy(selectedEventTypes = updated)
    }

    fun submit() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Kailangan ng email at password.") }
            return
        }
        if (state.mode == AuthMode.SIGNUP && state.name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Kailangan ng pangalan.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                if (state.mode == AuthMode.SIGNUP) {
                    authRepository.signUp(
                        email = state.email.trim(),
                        password = state.password,
                        name = state.name.trim(),
                        declaredRating = state.declaredRating.toDouble(),
                    )
                    userPreferencesStore.setPreferredEventTypes(state.selectedEventTypes)
                } else {
                    authRepository.signIn(email = state.email.trim(), password = state.password)
                }
                // Navigation itself is driven by AuthRepository.authState (see
                // PickleProNavHost) so a signup that didn't produce a session
                // (e.g. email confirmation required) doesn't route to a
                // screen that needs a signed-in user.
                val awaitingConfirmation = state.mode == AuthMode.SIGNUP && authRepository.currentUserId == null
                _uiState.update { it.copy(isLoading = false, awaitingEmailConfirmation = awaitingConfirmation) }
            } catch (t: Throwable) {
                _uiState.update { it.copy(isLoading = false, errorMessage = mapAuthError(t)) }
            }
        }
    }

    private fun mapAuthError(t: Throwable): String {
        val message = t.message.orEmpty()
        return when {
            message.contains("Invalid login credentials", ignoreCase = true) ->
                "Mali ang email o password."
            message.contains("already registered", ignoreCase = true) ||
                message.contains("already exists", ignoreCase = true) ->
                "Ginagamit na ang email na ito."
            message.contains("Password should be", ignoreCase = true) ->
                "Kailangan ng at least 6 characters ang password."
            message.contains("network", ignoreCase = true) ->
                "Walang connection. Subukan ulit."
            else -> "May problema. Subukan ulit."
        }
    }
}

class AuthViewModelFactory(private val appContext: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return AuthViewModel(
            authRepository = AuthRepository(appContext),
            userPreferencesStore = UserPreferencesStore(appContext),
        ) as T
    }
}
