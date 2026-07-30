package com.gentech.picklepro.organizer.wallet

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gentech.picklepro.data.repository.AuthRepository
import com.gentech.picklepro.data.repository.OrganizerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WalletUiState(
    val credits: Int? = null,
    val isLoading: Boolean = true,
)

class WalletViewModel(
    private val userId: String,
    private val organizerRepository: OrganizerRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WalletUiState())
    val uiState: StateFlow<WalletUiState> = _uiState

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val credits = runCatching { organizerRepository.getUnconsumedCreditCount(userId) }.getOrNull()
            _uiState.update { it.copy(credits = credits, isLoading = false) }
        }
    }
}

class WalletViewModelFactory(private val appContext: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val userId = AuthRepository(appContext).currentUserId
            ?: error("WalletViewModel requires a signed-in user")
        @Suppress("UNCHECKED_CAST")
        return WalletViewModel(userId, OrganizerRepository(appContext)) as T
    }
}
