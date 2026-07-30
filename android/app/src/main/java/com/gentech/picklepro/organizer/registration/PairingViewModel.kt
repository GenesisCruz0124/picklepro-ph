package com.gentech.picklepro.organizer.registration

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gentech.picklepro.R
import com.gentech.picklepro.data.remote.dto.RegistrationWithProfileDto
import com.gentech.picklepro.data.repository.RegistrationRepository
import com.gentech.picklepro.data.repository.TeamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PairingUiState(
    val unpaired: List<RegistrationWithProfileDto> = emptyList(),
    /** Paired registrations grouped by team id — each entry has exactly 2 members. */
    val teams: List<List<RegistrationWithProfileDto>> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

/** Doubles/mixed pairing (spec §5.4): pairs two unpaired registrations into a team. */
class PairingViewModel(
    private val context: Context,
    private val divisionId: String,
    private val registrationRepository: RegistrationRepository,
    private val teamRepository: TeamRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PairingUiState())
    val uiState: StateFlow<PairingUiState> = _uiState

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val all = runCatching { registrationRepository.listForDivision(divisionId) }.getOrDefault(emptyList())
            val unpaired = all.filter { it.teamId == null }
            val teams = all.filter { it.teamId != null }.groupBy { it.teamId }.values.toList()
            _uiState.update { it.copy(unpaired = unpaired, teams = teams, selectedIds = emptySet(), isLoading = false) }
        }
    }

    fun toggleSelect(registrationId: String) = _uiState.update { state ->
        val selected = state.selectedIds
        val updated = when {
            registrationId in selected -> selected - registrationId
            selected.size < 2 -> selected + registrationId
            else -> selected // already 2 selected — ignore further taps until one is deselected
        }
        state.copy(selectedIds = updated)
    }

    fun pairSelected() {
        val state = _uiState.value
        if (state.selectedIds.size != 2) return
        val (r1, r2) = state.unpaired.filter { it.id in state.selectedIds }
        viewModelScope.launch {
            try {
                teamRepository.pair(divisionId, r1.id, r1.playerId, r2.id, r2.playerId)
                refresh()
            } catch (t: Throwable) {
                _uiState.update { it.copy(errorMessage = context.getString(R.string.pairing_error_pair_failed)) }
            }
        }
    }

    fun unpair(teamId: String) {
        viewModelScope.launch {
            try {
                teamRepository.unpair(teamId)
                refresh()
            } catch (t: Throwable) {
                _uiState.update { it.copy(errorMessage = context.getString(R.string.pairing_error_unpair_failed)) }
            }
        }
    }
}

class PairingViewModelFactory(
    private val appContext: Context,
    private val divisionId: String,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return PairingViewModel(
            context = appContext,
            divisionId = divisionId,
            registrationRepository = RegistrationRepository(appContext),
            teamRepository = TeamRepository(appContext),
        ) as T
    }
}
