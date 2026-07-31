package com.gentech.picklepro.organizer.activation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gentech.picklepro.R
import com.gentech.picklepro.data.repository.AuthRepository
import com.gentech.picklepro.data.repository.OrganizerRepository
import com.gentech.picklepro.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BecomeOrganizerUiState(
    val code: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val success: Boolean = false,
)

class BecomeOrganizerViewModel(
    private val context: Context,
    private val userId: String,
    private val organizerRepository: OrganizerRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BecomeOrganizerUiState())
    val uiState: StateFlow<BecomeOrganizerUiState> = _uiState

    fun onCodeChange(value: String) = _uiState.update { it.copy(code = value, errorMessage = null) }

    fun submit() {
        val code = _uiState.value.code.trim()
        if (code.isEmpty()) {
            _uiState.update { it.copy(errorMessage = context.getString(R.string.organizer_error_missing_code)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val response = organizerRepository.redeemActivationCode(code)
                if (response.ok) {
                    // Refresh the cached profile so nav (gated on role) picks up
                    // the upgrade to organizer immediately.
                    profileRepository.refresh(userId)
                    _uiState.update { it.copy(isLoading = false, success = true) }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = mapError(response.error)) }
                }
            } catch (t: Throwable) {
                _uiState.update { it.copy(isLoading = false, errorMessage = context.getString(R.string.common_error_generic)) }
            }
        }
    }

    private fun mapError(error: String?): String = when (error) {
        "code_not_found" -> context.getString(R.string.organizer_error_code_not_found)
        "code_redeemed" -> context.getString(R.string.organizer_error_code_redeemed)
        "code_revoked" -> context.getString(R.string.organizer_error_code_revoked)
        "account_suspended" -> context.getString(R.string.organizer_error_account_suspended)
        else -> context.getString(R.string.common_error_generic)
    }
}

class BecomeOrganizerViewModelFactory(private val appContext: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val userId = AuthRepository(appContext).currentUserId
            ?: error("BecomeOrganizerViewModel requires a signed-in user")
        @Suppress("UNCHECKED_CAST")
        return BecomeOrganizerViewModel(
            context = appContext,
            userId = userId,
            organizerRepository = OrganizerRepository(appContext),
            profileRepository = ProfileRepository(appContext),
        ) as T
    }
}
