package com.gentech.picklepro.organizer.bracket

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gentech.picklepro.data.remote.dto.BracketMatchDto
import com.gentech.picklepro.data.remote.dto.DivisionDto
import com.gentech.picklepro.data.repository.BracketRepository
import com.gentech.picklepro.data.repository.DivisionRepository
import com.gentech.picklepro.data.repository.RegistrationRepository
import com.gentech.picklepro.data.repository.StandingsRow
import com.gentech.picklepro.data.repository.TeamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BracketViewUiState(
    val division: DivisionDto? = null,
    val matchesByRound: Map<Int, List<BracketMatchDto>> = emptyMap(),
    val standings: List<StandingsRow> = emptyList(),
    val nameByRef: Map<String, String> = emptyMap(),
    val isLoading: Boolean = true,
)

/** Renders a generated bracket (SE) or standings table (RR) — spec §5.5. */
class BracketViewViewModel(
    private val divisionId: String,
    private val divisionRepository: DivisionRepository,
    private val registrationRepository: RegistrationRepository,
    private val teamRepository: TeamRepository,
    private val bracketRepository: BracketRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BracketViewUiState())
    val uiState: StateFlow<BracketViewUiState> = _uiState

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val division = runCatching { divisionRepository.get(divisionId) }.getOrNull()
            val registrations = runCatching { registrationRepository.listForDivision(divisionId) }.getOrDefault(emptyList())
            val teams = if (division?.eventType == "singles") {
                emptyList()
            } else {
                runCatching { teamRepository.listForDivision(divisionId) }.getOrDefault(emptyList())
            }
            val entrants = division?.let { resolveEntrants(it, registrations, teams) } ?: emptyList()
            val nameByRef = entrants.associate { it.ref to it.name }

            if (division?.format == "round_robin") {
                val standings = runCatching { bracketRepository.computeStandings(divisionId, entrants) }.getOrDefault(emptyList())
                _uiState.update {
                    it.copy(division = division, standings = standings, nameByRef = nameByRef, isLoading = false)
                }
            } else {
                val matches = runCatching { bracketRepository.listMatches(divisionId) }.getOrDefault(emptyList())
                _uiState.update {
                    it.copy(
                        division = division,
                        matchesByRound = matches.groupBy { m -> m.round },
                        nameByRef = nameByRef,
                        isLoading = false,
                    )
                }
            }
        }
    }
}

class BracketViewViewModelFactory(
    private val appContext: Context,
    private val divisionId: String,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return BracketViewViewModel(
            divisionId = divisionId,
            divisionRepository = DivisionRepository(appContext),
            registrationRepository = RegistrationRepository(appContext),
            teamRepository = TeamRepository(appContext),
            bracketRepository = BracketRepository(appContext),
        ) as T
    }
}
