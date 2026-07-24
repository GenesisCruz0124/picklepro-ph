package com.gentech.picklepro.organizer.tournament

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gentech.picklepro.data.remote.dto.CreateTournamentRequest
import com.gentech.picklepro.data.repository.AuthRepository
import com.gentech.picklepro.data.repository.OrganizerRepository
import com.gentech.picklepro.data.repository.TournamentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NewTournamentUiState(
    val name: String = "",
    val venue: String = "",
    val description: String = "",
    val entryFeeNote: String = "",
    val courtCount: String = "1",
    val startDate: String = "",
    val endDate: String = "",
    val credits: Int? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val createdTournamentId: String? = null,
)

class NewTournamentViewModel(
    private val tournamentRepository: TournamentRepository,
    private val organizerRepository: OrganizerRepository,
    private val organizerId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewTournamentUiState())
    val uiState: StateFlow<NewTournamentUiState> = _uiState

    init {
        viewModelScope.launch {
            val credits = runCatching { organizerRepository.getUnconsumedCreditCount(organizerId) }.getOrNull()
            _uiState.update { it.copy(credits = credits) }
        }
    }

    fun onNameChange(v: String) = _uiState.update { it.copy(name = v, errorMessage = null) }
    fun onVenueChange(v: String) = _uiState.update { it.copy(venue = v) }
    fun onDescriptionChange(v: String) = _uiState.update { it.copy(description = v) }
    fun onEntryFeeNoteChange(v: String) = _uiState.update { it.copy(entryFeeNote = v) }
    fun onCourtCountChange(v: String) = _uiState.update { it.copy(courtCount = v.filter(Char::isDigit)) }
    fun onStartDateChange(v: String) = _uiState.update { it.copy(startDate = v) }
    fun onEndDateChange(v: String) = _uiState.update { it.copy(endDate = v) }

    fun submit() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Kailangan ng pangalan ng tournament.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val response = tournamentRepository.create(
                    CreateTournamentRequest(
                        name = state.name.trim(),
                        venue = state.venue.trim().ifBlank { null },
                        description = state.description.trim().ifBlank { null },
                        entryFeeNote = state.entryFeeNote.trim().ifBlank { null },
                        courtCount = state.courtCount.toIntOrNull() ?: 1,
                        startDate = state.startDate.trim().ifBlank { null },
                        endDate = state.endDate.trim().ifBlank { null },
                    ),
                )
                if (response.ok && response.tournamentId != null) {
                    _uiState.update { it.copy(isLoading = false, createdTournamentId = response.tournamentId) }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = mapError(response.error)) }
                }
            } catch (t: Throwable) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "May problema. Subukan ulit.") }
            }
        }
    }

    private fun mapError(error: String?): String = when (error) {
        "no_credits" -> "Wala kang tournament credit. Pumunta sa Wallet."
        "credit_conflict_retry" -> "May kasabay na request. Subukan ulit."
        "account_suspended" -> "Suspended ang account mo. I-contact si admin."
        else -> "May problema. Subukan ulit."
    }
}

class NewTournamentViewModelFactory(private val appContext: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val userId = AuthRepository(appContext).currentUserId
            ?: error("NewTournamentViewModel requires a signed-in user")
        @Suppress("UNCHECKED_CAST")
        return NewTournamentViewModel(
            tournamentRepository = TournamentRepository(appContext),
            organizerRepository = OrganizerRepository(appContext),
            organizerId = userId,
        ) as T
    }
}
